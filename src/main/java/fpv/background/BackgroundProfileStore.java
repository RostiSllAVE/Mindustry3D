package fpv.background;

import fpv.render.BorderBiomeProfile;
import java.io.*;
import java.nio.file.*;
import java.util.HexFormat;
import java.util.zip.CRC32;

/** Versioned, bounded local art data, never a native save chunk or network payload. */
public final class BackgroundProfileStore {
    public static final int VERSION=2,MAX_BYTES=196608,MAX_PROFILES=128;
    private static final int MAGIC=0x4d334247;
    private final Path root;
    public record Loaded(BackgroundStyle style,String warning){}
    public BackgroundProfileStore(Path root){this.root=root;}
    public Path file(String key){if(key==null||!key.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Hashed map key required");return root.resolve(key+".m3bg");}
    public static byte[] encode(String key,int width,int height,BackgroundStyle style)throws IOException{
        if(style.paint!=null&&(style.paint.width!=width||style.paint.height!=height))throw new IOException("Paint belongs to another map");
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeInt(MAGIC);out.writeInt(VERSION);out.write(HexFormat.of().parseHex(key));out.writeInt(width);out.writeInt(height);
        out.writeByte(style.mode.ordinal());out.writeByte(style.whole.ordinal());out.writeBoolean(style.customBaseManual);
        for(var side:BorderBiomeProfile.Side.values())out.writeByte(style.sides.containsKey(side)?style.sides.get(side).ordinal():-1);
        out.writeBoolean(style.paint!=null);
        if(style.paint!=null){out.writeInt(BackgroundPaint.NODES);for(int y=0;y<BackgroundPaint.NODES;y++)for(int x=0;x<BackgroundPaint.NODES;x++){
            out.writeByte(style.paint.material(x,y).ordinal());out.writeFloat(style.paint.coverage(x,y));out.writeFloat(style.paint.height(x,y));out.writeFloat(style.paint.heightCoverage(x,y));out.writeByte(style.paint.secondary(x,y));out.writeFloat(style.paint.mix(x,y));
        }}out.flush();var crc=new CRC32();crc.update(bytes.toByteArray());out.writeInt((int)crc.getValue());out.close();
        if(bytes.size()>MAX_BYTES)throw new IOException("Background profile exceeds limit");return bytes.toByteArray();
    }
    public static BackgroundStyle decode(byte[] bytes,String key,int width,int height)throws IOException{
        if(bytes.length<60||bytes.length>MAX_BYTES)throw new IOException("Invalid profile length");
        var crc=new CRC32();crc.update(bytes,0,bytes.length-4);var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readInt()!=MAGIC)throw new IOException("Unknown background profile type");int version=in.readInt();if(version!=1&&version!=VERSION)throw new IOException("Unknown background profile version");
        if(!HexFormat.of().formatHex(in.readNBytes(32)).equals(key)||in.readInt()!=width||in.readInt()!=height)throw new IOException("Background map identity differs");
        var style=new BackgroundStyle();int mode=in.readUnsignedByte(),whole=in.readUnsignedByte();
        if(mode>=BackgroundStyle.Mode.values().length||whole>=BackgroundPreset.values().length)throw new IOException("Invalid background mode / preset");
        style.mode=BackgroundStyle.Mode.values()[mode];style.whole=BackgroundPreset.values()[whole];style.customBaseManual=in.readBoolean();
        for(var side:BorderBiomeProfile.Side.values()){int value=in.readByte();if(value< -1||value>=BackgroundPreset.values().length)throw new IOException("Invalid directional preset");if(value>=0)style.sides.put(side,BackgroundPreset.values()[value]);}
        try{if(in.readBoolean()){if(in.readInt()!=BackgroundPaint.NODES)throw new IOException("Unsupported paint resolution");style.paint=new BackgroundPaint(width,height);
            for(int y=0;y<BackgroundPaint.NODES;y++)for(int x=0;x<BackgroundPaint.NODES;x++){int id=in.readUnsignedByte();if(version==1&&id>=14)throw new IOException("Invalid v1 material");style.paint.node(x,y,id,in.readFloat(),in.readFloat(),in.readFloat());if(version>=2)style.paint.blendNode(x,y,in.readUnsignedByte(),in.readFloat());}
        }}catch(IllegalArgumentException bad){throw new IOException("Invalid paint data",bad);}
        if(in.readInt()!=(int)crc.getValue()||in.available()!=0)throw new IOException("Background checksum / trailing data invalid");return style;
    }
    private BackgroundStyle read(Path file,String key,int width,int height)throws IOException{
        if(Files.size(file)>MAX_BYTES)throw new IOException("Oversize local background");
        try(var input=Files.newInputStream(file)){byte[] bytes=input.readNBytes(MAX_BYTES+1);return decode(bytes,key,width,height);}
    }
    public Loaded load(String key,int width,int height){Path file=file(key);if(!Files.exists(file))return new Loaded(new BackgroundStyle(),"");
        try{return new Loaded(read(file,key,width,height),"");}catch(IOException|RuntimeException invalid){
            try{return new Loaded(read(file.resolveSibling(file.getFileName()+".bak"),key,width,height),"Recovered the previous local background. Invalid file preserved.");}
            catch(IOException|RuntimeException noBackup){return new Loaded(new BackgroundStyle(),"Invalid or outdated local background: using Auto. Original file preserved.");}
        }
    }
    public void save(String key,int width,int height,BackgroundStyle style)throws IOException{
        byte[] bytes=encode(key,width,height,style);Path file=file(key);Files.createDirectories(root);
        if(!Files.exists(file)){try(var files=Files.list(root)){if(files.filter(p->p.getFileName().toString().endsWith(".m3bg")).limit(MAX_PROFILES).count()>=MAX_PROFILES)throw new IOException("Local background profile limit reached (128)");}}
        if(Files.exists(file)){
            try{read(file,key,width,height);Files.copy(file,file.resolveSibling(file.getFileName()+".bak"),StandardCopyOption.REPLACE_EXISTING);}
            catch(IOException invalid){Path preserved=file.resolveSibling(file.getFileName()+".invalid");if(!Files.exists(preserved))Files.copy(file,preserved);}
        }
        Path temp=Files.createTempFile(root,key+"-",".tmp");
        try{Files.write(temp,bytes);try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException unsupported){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}
        finally{Files.deleteIfExists(temp);}
    }
}
