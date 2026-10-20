package fpv.render;
import fpv.background.*;import java.io.*;import java.util.zip.CRC32;
public final class BackgroundProfileMigrationTest {
    public static void main(String[] args)throws Exception{
        String key="a".repeat(64);var style=new BackgroundStyle();style.mode=BackgroundStyle.Mode.CUSTOM;style.paint(96,64).line(-100,32,-100,32,50,.8,BackgroundPaint.Brush.FOREST,0);
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(0x4d334247);out.writeInt(1);out.write(java.util.HexFormat.of().parseHex(key));out.writeInt(96);out.writeInt(64);out.writeByte(2);out.writeByte(0);out.writeBoolean(false);for(int i=0;i<4;i++)out.writeByte(-1);out.writeBoolean(true);out.writeInt(BackgroundPaint.NODES);
        for(int y=0;y<BackgroundPaint.NODES;y++)for(int x=0;x<BackgroundPaint.NODES;x++){out.writeByte(style.paint.material(x,y).ordinal());out.writeFloat(style.paint.coverage(x,y));out.writeFloat(style.paint.height(x,y));out.writeFloat(style.paint.heightCoverage(x,y));}out.flush();var crc=new CRC32();crc.update(bytes.toByteArray());out.writeInt((int)crc.getValue());out.close();
        var restored=BackgroundProfileStore.decode(bytes.toByteArray(),key,96,64);if(restored.fingerprint()!=style.fingerprint())throw new AssertionError("Legacy v1 must retain every channel");
        restored.paint.stroke(BackgroundPaint.Brush.SNOW,50,.8,.35,0,(x,y)->0).line(-100,32,-100,32);var upgraded=BackgroundProfileStore.decode(BackgroundProfileStore.encode(key,96,64,restored),key,96,64);if(upgraded.fingerprint()!=restored.fingerprint())throw new AssertionError("v2 blend roundtrip");
        System.out.println("BackgroundProfileMigrationTest: v1 exact data migration + v2 blended material roundtrip PASS");
    }
}
