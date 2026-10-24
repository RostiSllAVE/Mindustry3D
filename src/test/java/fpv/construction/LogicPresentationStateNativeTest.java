package fpv.construction;

import arc.util.*;import mindustry.*;import mindustry.content.*;import mindustry.gen.*;import mindustry.game.*;import mindustry.world.*;import mindustry.world.blocks.logic.*;import mindustry.logic.*;

/** Real native instruction assembly/execution, memory, display queues and same-floor ownership. */
public final class LogicPresentationStateNativeTest {
    static int checks;static long id=12000;static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        LayerCampaignNativeTest.main(args);Vars.logicVars=new GlobalVars();Vars.logicVars.init();Time.delta=1;
        var owner=new LayerBuildingAdapter(null);
        for(Block type:new Block[]{Blocks.microProcessor,Blocks.logicProcessor,Blocks.hyperProcessor})for(int floor:new int[]{0,1,3,5}){
            var p=(LogicBlock.LogicBuild)place(owner,type,8,8,floor);
            for(var consumer:type.consumers)if(consumer instanceof mindustry.world.consumers.ConsumeLiquid liquid)p.liquids.add(liquid.liquid,100);
            var memory=(MemoryBlock.MemoryBuild)place(owner,Blocks.memoryCell,12,8,floor);
            var sw=(SwitchBlock.SwitchBuild)place(owner,Blocks.switchBlock,12,11,floor);
            var display=(LogicDisplay.LogicDisplayBuild)place(owner,Blocks.logicDisplay,8,14,floor);
            var message=(MessageBlock.MessageBuild)place(owner,Blocks.message,15,10,floor);
            var address=new LVar("address");var value=new LVar("value");address.setnum(0);value.setnum(41);memory.write(address,value);
            sw.enabled=true;
            p.links.clear();p.links.add(new LogicBlock.LogicLink(12,8,"cell1",true));p.links.add(new LogicBlock.LogicLink(12,11,"switch1",true));p.links.add(new LogicBlock.LogicLink(8,14,"display1",true));p.links.add(new LogicBlock.LogicLink(15,10,"message1",true));
            String code="read n cell1 0\nop add n n 1\nwrite n cell1 0\ncontrol enabled switch1 0 0 0 0\ndraw clear 11 22 33 0 0 0\ndraw color 240 80 30 255 0 0\ndraw rect 3 4 8 9 0 0\ndrawflush display1\nprint \"LIVE\"\nprintflush message1\nend";
            if(floor==0)p.updateCode(code);else{long key=owner.entries().stream().filter(e->e.building()==p).findFirst().orElseThrow().id();check(owner.configureNative(key,LogicBlock.compress(code,p.relativeConnections())),"existing same-floor configuration accepted");}
            check(p.executor.initialized(),"native program assembled");
            // Native DrawI intentionally does nothing on a dedicated headless server.
            Vars.headless=false;
            for(int t=0;t<80;t++){Time.time++;Vars.state.updateId++;p.efficiency=1;if(floor==0)p.updateTile();else owner.tick();}
            Vars.headless=true;
            memory.read(address,value);check(value.num()>41,"actual read/write instructions update memory: "+type.name+" Z"+floor+" value="+value.num()+" ipt="+p.ipt+" counter="+p.executor.counter.num());check(!sw.enabled,"native control instruction changes switch");
            check(display.commands.size>0,"real draw/drawflush reached native display queue");check(message.message.toString().contains("LIVE"),"native printflush remains functional");
            int commands=display.commands.size;byte[] payload=floor==0?null:owner.writeState(owner.entries().stream().filter(e->e.building()==memory).findFirst().orElseThrow().id());
            if(payload!=null){long key=owner.entries().stream().filter(e->e.building()==memory).findFirst().orElseThrow().id();owner.readState(key,payload);memory.read(address,value);check(value.num()>41,"memory native save state retained");}
            check(display.commands.size==commands,"memory serialization does not consume display queue");
            if(floor>0){var foreign=place(owner,Blocks.memoryBank,12,8,floor+1);check(!LayerLogicBehavior.valid(p,foreign),"renderer does not invent cross-floor logic links");}
            if(floor==0){Vars.world.tile(8,8).remove();Vars.world.tile(12,8).remove();Vars.world.tile(12,11).remove();Vars.world.tile(8,14).remove();Vars.world.tile(15,10).remove();}else owner.clear();
        }
        owner.clear();System.out.println("LogicPresentationStateNativeTest: "+checks+" real native processor/memory/control/draw/message/save/floor checks PASS");
    }
    static Building place(LayerBuildingAdapter owner,Block type,int x,int y,int floor){if(floor==0){Vars.world.tile(x,y).setBlock(type,Team.sharded,0);return Vars.world.build(x,y);}return owner.create(++id,x,y,floor,type,Team.sharded,0);}
}
