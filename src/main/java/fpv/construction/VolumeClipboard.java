package fpv.construction;

import arc.math.geom.Point2;
import java.io.*;
import java.util.*;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Player;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.MassDriver;
import mindustry.world.blocks.power.PowerNode;

/** Session clipboard, not a .msch or save format. Links contain clipboard indices, never entity IDs. */
public final class VolumeClipboard {
    public static final int VERSION=1,MAX_ENTRIES=1024,MAX_LINKS=2048,MAX_DIM=128,MAX_BYTES=LayerConfiguration.MAX_BYTES;
    private static final int MAGIC=0x56334350;
    public enum LinkKind { POINT,POWER,ITEMS,LIQUIDS,BRIDGE_ITEMS,BRIDGE_LIQUIDS }
    public record Link(int from,int to,LinkKind kind){public Link{if(kind==LinkKind.POWER&&from>to){int swap=from;from=to;to=swap;}}}
    public record Cell(int x,int y,int z,String name,int size,int rotation,boolean platform,byte[] config){
        public Cell{config=config==null?new byte[]{0}:config.clone();}
        @Override public byte[] config(){return config.clone();}
        public Block block(){return platform?mindustry.content.Blocks.air:Vars.content.getByName(mindustry.ctype.ContentType.block,name);}
    }
    public record Clip(double spacing,int width,int depth,int floors,List<Cell> cells,List<Link> links){
        public Clip{cells=List.copyOf(cells);links=List.copyOf(links);validate(spacing,width,depth,floors,cells,links);}
    }
    public record Capture(Clip clip,String warning){}
    public static final class Plan extends BuildPlan {
        public final int floor,index;public final boolean platform;
        public Plan(int x,int y,int floor,Cell c,int index){super(x,y,c.rotation(),c.block());this.floor=floor;this.index=index;platform=c.platform();}
    }
    public record Placement(Clip clip,int x,int y,int z){
        public List<Plan> plans(){var result=new ArrayList<Plan>();for(int i=0;i<clip.cells().size();i++){var c=clip.cells().get(i);result.add(new Plan(x+c.x(),y+c.y(),z+c.z(),c,i));}return List.copyOf(result);}
    }
    private VolumeClipboard(){}
    public static boolean safeConfig(Block block,Object value){
        if(value==null)return true;
        if(block instanceof mindustry.world.blocks.logic.LogicBlock||block instanceof ItemBridge||block instanceof MassDriver||block instanceof PowerNode)return false;
        if(value instanceof Integer i&&block instanceof mindustry.world.blocks.units.UnitFactory f&&(i< -1||i>=f.plans.size))return false;
        Class<?> type=value instanceof mindustry.type.Item?mindustry.type.Item.class:value instanceof mindustry.type.Liquid?mindustry.type.Liquid.class:value instanceof Block?Block.class:value instanceof mindustry.type.UnitType?mindustry.type.UnitType.class:value.getClass().isAnonymousClass()?value.getClass().getSuperclass():value.getClass();
        return block.configurations.containsKey(type)&&(value instanceof Boolean||value instanceof Integer||value instanceof mindustry.ctype.UnlockableContent||value instanceof mindustry.ai.UnitCommand
            ||value instanceof String&&block instanceof mindustry.world.blocks.logic.MessageBlock);
    }
    public static Capture capture(LayerRuntime runtime,StructureVolume volume,Player player){
        volume.refresh(arc.util.Time.millis());var entries=volume.entries();
        if(player==null||player.dead()||entries.isEmpty())throw new IllegalArgumentException("Select completed friendly structures first");
        if(entries.size()>MAX_ENTRIES)throw new IllegalArgumentException("3D clipboard limit: 1024 structures; select a smaller volume");
        int minX=32767,minY=32767,minZ=512,maxX=-1,maxY=-1,maxZ=0;
        var ids=new HashMap<Long,Integer>();var positions=new HashMap<String,Integer>();
        for(int i=0;i<entries.size();i++){var e=entries.get(i);
            if(e.team()!=player.team().id)throw new IllegalArgumentException("Only your team's structures may be copied");
            var current=e.id()>0?runtime.grid().get(e.id()):null;Building b=e.id()>0?runtime.building(e.id()):Vars.world.build(e.x(),e.y());
            if(e.id()>0?(current==null||current.health()<=0||current.team()!=player.team().id||!current.blockName().equals(e.blockName())):b==null||b.dead||b.team!=player.team()||!b.block.name.equals(e.blockName()))throw new IllegalArgumentException("Selection changed; select it again");
            minX=Math.min(minX,LayerCoordinates.minCell(e.x(),e.size()));minY=Math.min(minY,LayerCoordinates.minCell(e.y(),e.size()));minZ=Math.min(minZ,e.layer());
            maxX=Math.max(maxX,LayerCoordinates.minCell(e.x(),e.size())+e.size()-1);maxY=Math.max(maxY,LayerCoordinates.minCell(e.y(),e.size())+e.size()-1);maxZ=Math.max(maxZ,e.layer());
            ids.put(e.id(),i);if(!e.platform())positions.put(key(e.x(),e.y(),e.layer()),i);
        }
        var cells=new ArrayList<Cell>();var links=new LinkedHashSet<Link>();int omitted=0;
        for(int i=0;i<entries.size();i++){var e=entries.get(i);var b=e.id()>0?runtime.building(e.id()):Vars.world.build(e.x(),e.y());Object value=e.platform()||b==null?null:b.config();
            if(value instanceof Point2 point&&(b.block instanceof ItemBridge||b.block instanceof MassDriver)){
                Integer to=positions.get(key(e.x()+point.x,e.y()+point.y,e.layer()));if(to!=null&&to!=i)links.add(new Link(i,to,LinkKind.POINT));else omitted++;value=null;
            }else if(b!=null&&b.block instanceof PowerNode){
                if(value instanceof Point2[] points)for(var point:points){Integer to=positions.get(key(e.x()+point.x,e.y()+point.y,e.layer()));if(to!=null&&to!=i)links.add(power(i,to));else omitted++;}value=null;
            }
            if(b!=null&&!safeConfig(b.block,value)){value=null;omitted++;}
            cells.add(new Cell(e.x()-minX,e.y()-minY,e.layer()-minZ,e.platform()?LayerCatalog.PLATFORM:e.blockName(),e.size(),e.rotation(),e.platform(),LayerConfiguration.encode(value)));
        }
        for(var l:runtime.nativeBuildings().powerLinks()){Integer a=ids.get(l.first()),b=ids.get(l.second());if(a!=null&&b!=null&&(cells.get(a).block() instanceof PowerNode||cells.get(b).block() instanceof PowerNode))links.add(power(a,b));}
        for(var l:runtime.transfers().links()){Integer a=ids.get(l.from()),b=ids.get(l.to());if(a!=null&&b!=null)links.add(l.kind()==LayerVerticalTransfers.Kind.POWER?power(a,b):new Link(a,b,LinkKind.valueOf(l.kind().name())));else if(a!=null||b!=null)omitted++;}
        var clip=new Clip(runtime.grid().coordinates().spacing(),maxX-minX+1,maxY-minY+1,maxZ-minZ+1,cells,new ArrayList<>(links));encode(clip);
        return new Capture(clip,omitted==0?"":omitted+" external links / unsupported configurations cleared");
    }
    private static Link power(int a,int b){return new Link(Math.min(a,b),Math.max(a,b),LinkKind.POWER);}
    private static String key(int x,int y,int z){return x+","+y+","+z;}
    public static Clip rotate(Clip clip){
        var cells=new ArrayList<Cell>();for(var c:clip.cells()){
            int parity=(c.size()+1)%2,cx=2*c.x()+parity,cy=2*c.y()+parity;
            cells.add(new Cell((2*(clip.depth()-1)-cy-parity)/2,(cx-parity)/2,c.z(),c.name(),c.size(),(c.platform()||!c.block().rotate?c.rotation():(c.rotation()+1)%4),c.platform(),c.config()));
        }return new Clip(clip.spacing(),clip.depth(),clip.width(),clip.floors(),cells,clip.links());
    }
    public static byte[] encode(Clip clip){
        try{var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(MAGIC);out.writeByte(VERSION);out.writeDouble(clip.spacing());out.writeShort(clip.width());out.writeShort(clip.depth());out.writeShort(clip.floors());out.writeShort(clip.cells().size());
            var names=new LinkedHashMap<String,Integer>();for(var c:clip.cells())names.computeIfAbsent(c.name(),n->names.size());out.writeShort(names.size());for(String name:names.keySet())out.writeUTF(name);
            for(var c:clip.cells()){out.writeShort(c.x());out.writeShort(c.y());out.writeShort(c.z());out.writeShort(names.get(c.name()));out.writeByte(c.size());out.writeByte(c.rotation());out.writeBoolean(c.platform());byte[] config=c.config();out.writeShort(config.length);out.write(config);}
            out.writeShort(clip.links().size());for(var l:clip.links()){out.writeShort(l.from());out.writeShort(l.to());out.writeByte(l.kind().ordinal());}
            if(bytes.size()>MAX_BYTES)throw new IllegalArgumentException("3D clipboard exceeds 16 KiB request limit; select fewer blocks/configurations");return bytes.toByteArray();
        }catch(IOException e){throw new IllegalArgumentException("3D clipboard encoding failed",e);}
    }
    public static Clip decode(byte[] bytes){
        if(bytes==null||bytes.length<24||bytes.length>MAX_BYTES)throw new IllegalArgumentException("3D clipboard payload size");
        try{var in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readInt()!=MAGIC||in.readUnsignedByte()!=VERSION)throw new IOException("Clipboard version");double spacing=in.readDouble();int w=in.readUnsignedShort(),d=in.readUnsignedShort(),h=in.readUnsignedShort(),count=in.readUnsignedShort(),types=in.readUnsignedShort();
            if(count<1||count>MAX_ENTRIES||types<1||types>count)throw new IOException("Clipboard counts");var names=new ArrayList<String>();for(int i=0;i<types;i++){String name=in.readUTF();if(!name.matches("[a-zA-Z0-9_.-]{1,96}"))throw new IOException("Block name");names.add(name);}
            var cells=new ArrayList<Cell>();for(int i=0;i<count;i++){int x=in.readUnsignedShort(),y=in.readUnsignedShort(),z=in.readUnsignedShort(),type=in.readUnsignedShort(),size=in.readUnsignedByte(),rotation=in.readUnsignedByte();boolean platform=in.readBoolean();int length=in.readUnsignedShort();if(type>=types||length<1||length>in.available())throw new IOException("Cell/configuration bounds");byte[] config=new byte[length];in.readFully(config);cells.add(new Cell(x,y,z,names.get(type),size,rotation,platform,config));}
            int n=in.readUnsignedShort();if(n>MAX_LINKS)throw new IOException("Link limit");var links=new ArrayList<Link>();for(int i=0;i<n;i++){int a=in.readUnsignedShort(),b=in.readUnsignedShort(),kind=in.readUnsignedByte();if(kind>=LinkKind.values().length)throw new IOException("Link kind");links.add(new Link(a,b,LinkKind.values()[kind]));}
            if(in.available()!=0)throw new IOException("Trailing clipboard bytes");return new Clip(spacing,w,d,h,cells,links);
        }catch(IOException|RuntimeException bad){throw new IllegalArgumentException("Invalid 3D clipboard: "+bad.getMessage(),bad);}
    }
    private static void validate(double spacing,int w,int d,int h,List<Cell> cells,List<Link> links){
        if(!Double.isFinite(spacing)||spacing<.2||spacing>64||w<1||d<1||w>MAX_DIM||d>MAX_DIM||h<1||h>513||cells.isEmpty()||cells.size()>MAX_ENTRIES||links.size()>MAX_LINKS)throw new IllegalArgumentException("3D clipboard bounds (128 x 128 / 1024 entries)");
        var occupied=new HashSet<String>();
        for(var c:cells){Block block=c.block();if(c.size()<1||c.size()>16||c.rotation()<0||c.rotation()>3||c.z()<0||c.z()>=h||c.platform()&&(c.size()!=1||!c.name().equals(LayerCatalog.PLATFORM))||!c.platform()&&(block==null||block==mindustry.content.Blocks.air||block.size!=c.size()))throw new IllegalArgumentException("Invalid clipboard block/footprint");
            int x=LayerCoordinates.minCell(c.x(),c.size()),y=LayerCoordinates.minCell(c.y(),c.size());if(x<0||y<0||x+c.size()>w||y+c.size()>d)throw new IllegalArgumentException("Clipboard footprint outside bounds");
            for(int tx=x;tx<x+c.size();tx++)for(int ty=y;ty<y+c.size();ty++)if(!occupied.add(tx+","+ty+","+c.z()+","+c.platform()))throw new IllegalArgumentException("Overlapping clipboard entries");
            Object config=LayerConfiguration.decode(c.config());if(c.platform()?config!=null:!safeConfig(block,config))throw new IllegalArgumentException("Unsupported clipboard configuration");
        }
        var unique=new HashSet<Link>();var pointOutputs=new HashSet<Integer>();for(var l:links){if(l.kind()==null||l.from()<0||l.to()<0||l.from()>=cells.size()||l.to()>=cells.size()||l.from()==l.to()||!unique.add(l)||cells.get(l.from()).platform()||cells.get(l.to()).platform())throw new IllegalArgumentException("Invalid internal clipboard link");
            var from=cells.get(l.from());var to=cells.get(l.to());if(l.kind()==LinkKind.POINT&&(from.z()!=to.z()||!(from.block() instanceof ItemBridge||from.block() instanceof MassDriver)))throw new IllegalArgumentException("Unsupported point link");
            if(l.kind()==LinkKind.POINT){
                if(!pointOutputs.add(l.from()))throw new IllegalArgumentException("Conflicting internal point links");
                if(from.block() instanceof ItemBridge bridge&&(from.block()!=to.block()||Math.hypot(from.x()-to.x(),from.y()-to.y())>bridge.range))throw new IllegalArgumentException("Invalid internal bridge");
                if(from.block() instanceof MassDriver driver&&(!(to.block() instanceof MassDriver)||Math.hypot(from.x()-to.x(),from.y()-to.y())*Vars.tilesize>driver.range))throw new IllegalArgumentException("Invalid mass-driver link");
            }
            if(l.kind()==LinkKind.POWER&&!(from.block() instanceof PowerNode)&&!(to.block() instanceof PowerNode))throw new IllegalArgumentException("Power node required");
        }
    }
}
