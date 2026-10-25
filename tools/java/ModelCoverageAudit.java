package fpv.render;
import mindustry.*;import mindustry.content.*;import mindustry.world.*;import mindustry.world.blocks.units.*;
/** Read-only native 160.7 inventory. Compile with the diagnostic test classes for bootstrap. */
public final class ModelCoverageAudit {
 public static void main(String[] args)throws Exception{
  fpv.construction.LayerCampaignNativeTest.main(args);
  for(Block b:Vars.content.blocks()){
   String group=b instanceof UnitFactory?"Unit production":b instanceof Reconstructor?"Reconstructors":b instanceof UnitAssembler||b instanceof UnitAssemblerModule?"Assemblers":b.category==mindustry.type.Category.logic?"Logic":b.category==mindustry.type.Category.defense||b.category==mindustry.type.Category.turret?"Defense":b.buildVisibility==mindustry.world.meta.BuildVisibility.hidden&&b.requirements.length==0?"Environment/internal":"Miscellaneous / "+b.category;
   String coverage=SandboxModelProfile.forBlock(b)!=null?"Dedicated sandbox/payload/repair (A2-16o)":LogicModelProfile.forBlock(b)!=null?"Dedicated logic":UnitUpgradeProfile.forBlock(b)!=null?"Dedicated unit upgrade":UnitProductionProfile.forBlock(b)!=null?"Dedicated unit factory":CoreInfrastructureProfile.forBlock(b)!=null?"Dedicated core/radar":UtilityModelProfile.forBlock(b)!=null?"Dedicated utility":SupportModelProfile.forBlock(b)!=null?"Dedicated support/storage":LiquidModelProfile.forBlock(b)!=null?"Dedicated liquid":PowerModelProfile.forBlock(b)!=null?"Dedicated power":ChemicalModelProfile.forBlock(b)!=null?"Dedicated chemical":AdvancedManufacturingProfile.forBlock(b)!=null?"Dedicated advanced manufacturing":ManufacturingModelProfile.forBlock(b)!=null?"Dedicated manufacturing":DrillModelProfile.forBlock(b)!=null?"Dedicated drill":AdvancedLogisticsModelRenderer.supports(b)?"Dedicated advanced item logistics":LogisticsModelProfile.forBlock(b)!=null?"Dedicated item logistics":b instanceof mindustry.world.blocks.defense.Wall?"Shared textured wall":b instanceof mindustry.world.blocks.defense.turrets.Turret?"Native turret parts (atlas-dependent fallback)":b instanceof mindustry.world.blocks.units.UnitBlock?"Generic volume + native payload/progress":group.equals("Environment/internal")?"Environment/internal path; not a factory target":"Generic/native fallback";
   System.out.println("ROW\t"+b.id+"\t"+b.name+"\t"+b.size+"\t"+group+"\t"+coverage+"\t"+b.getClass().getName());
  }
  for(var p:SandboxModelProfile.values())for(int lod=0;lod<3;lod++){var model=SandboxModelGeometry.model(p,lod);System.out.println("GEOMETRY\t"+p.block().name+"\t"+lod+"\t"+model.vertices()+"\t"+model.vertices()/3+"\t"+model.vertices()*32L);}
 }
}
