package fpv;

import arc.*;import arc.files.Fi;import arc.graphics.Gl;import arc.graphics.g3d.Camera3D;import arc.util.*;
import fpv.render.*;import mindustry.*;import mindustry.content.*;import mindustry.core.GameState.State;import mindustry.game.Team;import mindustry.world.Tile;

/** Native discovered-Bits reset, real renderer and interaction rays in all three styles. */
final class SmokeHarness {
    final FpvMod mod;final Fi out=new Fi(System.getProperty("fpv.lab.path"));final Camera3D camera=new Camera3D();
    FpvWorldRenderer draw;boolean started,done;int frame,checks;long revision;
    SmokeHarness(FpvMod m){mod=m;}void update(){}
    void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    void start(){try{
        check(Vars.dataDirectory.absolutePath().startsWith(out.absolutePath())&&!Vars.steam,"isolated standalone");
        Core.settings.put("fpscap",120);Core.settings.put("fpv-auto-horizon",false);Vars.logic.reset();
        Vars.world.loadGenerator(64,64,tiles->{for(int y=0;y<64;y++)for(int x=0;x<64;x++)tiles.set(x,y,new Tile(x,y,Blocks.stone,Blocks.air,x>=14&&x<50&&y>=14&&y<50?Blocks.stoneWall:Blocks.air));tiles.get(4,4).setBlock(Blocks.coreShard,Team.sharded,0);});
        Vars.state.map=new mindustry.maps.Map(arc.struct.StringMap.of("name","A217 native fog"));Vars.player.team(Team.sharded);var u=UnitTypes.gamma.create(Team.sharded);u.set(32,32);u.add();Vars.player.unit(u);Vars.logic.play();Vars.state.set(State.paused);Vars.state.rules.fog=true;Vars.state.rules.staticFog=true;Vars.state.rules.waves=false;
        var flag=FpvMod.class.getDeclaredField("autoEnable");flag.setAccessible(true);flag.set(mod,false);mod.setEnabled(false);draw=new FpvWorldRenderer();draw.setUnitAltitudes(mod.unitAltitudes);revision=draw.terrain().revision();started=true;
    }catch(Throwable t){fail(t);}}
    void afterDraw(){if(!started||done)return;try{
        int stage=frame/20,tick=frame%20;boolean shown=stage%2==0;int style=stage/2;
        if(tick==0){Core.settings.put(MountainStyle.SETTING,style);Vars.fogControl.resetFog();Vars.state.teams.updateTeamStats();Vars.fogControl.update();var bits=Vars.fogControl.getDiscovered(Team.sharded);check(bits!=null,"native fog initialized after reset/update");bits.clear();if(shown)for(int i=0;i<64*64;i++)bits.set(i);}
        camera.resize(Core.graphics.getWidth(),Core.graphics.getHeight());camera.near=.04f;camera.far=300;camera.fov=60;camera.up.set(0,0,-1);camera.position.set(32,55,-32+.01f);camera.lookAt(32,0,-32);camera.update();draw.render(camera);
        check(Gl.getError()==0,"fog GPU error");check(draw.terrain().revision()==revision,"fog/style does not change canonical field");
        if(tick==15){var hit=draw.pickInteraction(camera,Core.graphics.getWidth()/2f,Core.graphics.getHeight()/2f);check(Vars.fogControl.isDiscovered(Team.sharded,32,32)==shown,"actual native discovery mask");check(shown?hit!=null&&hit.height>1:hit==null||Math.abs(hit.height)<.001,"hidden mountain height is not exposed; native flat ground-ray fallback remains valid");ScreenUtils.saveScreenshot(out.child(MountainStyle.of(style).label().toLowerCase(java.util.Locale.ROOT)+(shown?"-shown":"-hidden")+".png"));}
        if(++frame==120){done=true;out.child("result.json").writeString("{\"status\":\"PASS\",\"scenes\":6,\"frames\":120,\"checks\":"+checks+",\"glErrors\":0,\"physicalAcceptance\":false}");draw.dispose();Core.app.post(Core.app::exit);}
    }catch(Throwable t){fail(t);}}
    void fail(Throwable t){done=true;out.child("failed.txt").writeString(t.toString());Log.err(t);Core.app.post(Core.app::exit);}
}
