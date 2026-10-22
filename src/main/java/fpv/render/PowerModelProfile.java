package fpv.render;
import mindustry.content.Blocks;import mindustry.world.Block;
/** Explicit Serpulo power scope; physical envelopes and native simulation are untouched. */
public enum PowerModelProfile {
 NODE(.92f),LARGE_NODE(.92f),SURGE(.94f),DIODE(.74f),BATTERY(.82f),LARGE_BATTERY(.86f),SOLAR(.41f),LARGE_SOLAR(.49f),COMBUSTION(.85f),STEAM(.88f),DIFFERENTIAL(.86f),RTG(.88f),THORIUM(.91f),IMPACT(.92f);
 public final float terminal;PowerModelProfile(float terminal){this.terminal=terminal;}
 public static PowerModelProfile forBlock(Block b){
  if(b==null)return null;
  if(b==Blocks.powerNode)return NODE;if(b==Blocks.powerNodeLarge)return LARGE_NODE;if(b==Blocks.surgeTower)return SURGE;if(b==Blocks.diode)return DIODE;
  if(b==Blocks.battery)return BATTERY;if(b==Blocks.batteryLarge)return LARGE_BATTERY;if(b==Blocks.solarPanel)return SOLAR;if(b==Blocks.largeSolarPanel)return LARGE_SOLAR;
  if(b==Blocks.combustionGenerator)return COMBUSTION;if(b==Blocks.steamGenerator)return STEAM;if(b==Blocks.differentialGenerator)return DIFFERENTIAL;if(b==Blocks.rtgGenerator)return RTG;if(b==Blocks.thoriumReactor)return THORIUM;if(b==Blocks.impactReactor)return IMPACT;return null;
 }
 public boolean solar(){return this==SOLAR||this==LARGE_SOLAR;}
 public boolean storage(){return this==BATTERY||this==LARGE_BATTERY;}
 public static float attachment(Block b,float height){var p=forBlock(b);return p==null?height:height*p.terminal;}
}
