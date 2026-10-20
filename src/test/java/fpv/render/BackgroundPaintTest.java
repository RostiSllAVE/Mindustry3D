package fpv.render;
import fpv.background.*;

public final class BackgroundPaintTest {
    static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void rejected(Runnable r){try{r.run();throw new AssertionError("Invalid input accepted");}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args){
        var p=new BackgroundPaint(96,64);var sample=new BackgroundPaint.Sample();check(p.empty(),"new mask is Auto");
        for(int i=1;i<BackgroundPaint.NODES;i++)check(p.xs[i]>p.xs[i-1]&&p.ys[i]>p.ys[i-1],"continuous ordered world-coordinate axes");
        check(p.xs[32]==-.5&&p.xs[56]==95.5,"protected playable bounds map to actual world coordinates");
        p.line(-120,32,120,32,32,.7,BackgroundPaint.Brush.FOREST,10);
        for(int y=0;y<BackgroundPaint.NODES;y++)for(int x=0;x<BackgroundPaint.NODES;x++)if(!p.editable(x,y))check(p.coverage(x,y)==0&&p.heightCoverage(x,y)==0,"painting never modifies protected map nodes");
        for(int x=-110;x<-8;x+=4){p.sample(x,32,sample);check(sample.coverage>.8,"continuous drag, no isolated cell randomness");}
        p.sample(32,32,sample);check(sample.coverage==0&&sample.heightCoverage==0,"sample inside playable map is always untouched");
        var same=p.copy();check(p.same(same)&&p.fingerprint()==same.fingerprint(),"copies retain deterministic coordinates/data");
        p.line(48,150,48,150,60,.8,BackgroundPaint.Brush.ISLAND,14);p.line(48,150,48,150,60,.8,BackgroundPaint.Brush.FOREST,0);p.sample(48,150,sample);check(sample.height>13&&sample.heightCoverage>.9&&sample.weights[BackgroundPaint.Material.FOREST.ordinal()]>.99,"material painting preserves sculpted island height");
        var island=new BackgroundPaint(96,64);island.line(48,150,48,150,65,.8,BackgroundPaint.Brush.ISLAND,14);var wooded=island.copy();wooded.line(48,150,48,150,50,.7,BackgroundPaint.Brush.FOREST,0);var other=new BackgroundPaint.Sample();
        for(int y=70;y<230;y++){island.sample(48,y,sample);wooded.sample(48,y,other);check(other.coverage>=sample.coverage-.0001f,"repainting island with forest never opens artificial water gaps");}
        p.line(48,150,48,150,60,.8,BackgroundPaint.Brush.OCEAN,0);p.sample(48,150,sample);check(sample.heightCoverage==0&&sample.weights[BackgroundPaint.Material.OCEAN.ordinal()]>.99,"ocean stroke removes previous positive height");
        p.line(48,150,48,150,80,.9,BackgroundPaint.Brush.ERASE,0);p.sample(48,150,sample);check(sample.coverage==0&&sample.heightCoverage==0,"eraser restores base rather than placing blocks");
        p.line(-100,32,-100,32,40,.8,BackgroundPaint.Brush.MOUNTAINS,1000);p.sample(-100,32,sample);check(sample.height<=BackgroundPaint.MAX_HEIGHT,"height clamp");
        var history=new BackgroundPaintHistory(p);for(int i=0;i<40;i++){history.begin();p.clear();p.line(-100,32,-100,32,40,.8,BackgroundPaint.Brush.ISLAND,i+1);history.end();}
        check(history.undoCount()==32&&history.maxBytes()<5_000_000,"bounded history and memory");var newest=p.copy();check(history.undo()&&!p.same(newest)&&history.redo()&&p.same(newest),"undo/redo exactly restore stroke data");
        history.undo();history.begin();p.clear();history.end();check(history.redoCount()==0,"new action discards redo branch");
        rejected(()->p.line(0,0,1,1,0,.5,BackgroundPaint.Brush.FOREST,0));rejected(()->p.line(0,0,Double.NaN,1,30,.5,BackgroundPaint.Brush.FOREST,0));rejected(()->p.node(40,40,1,1,20,1));rejected(()->p.node(0,0,999,1,0,0));
        check(p.bytes()==BackgroundPaint.COUNT*18,"bounded serialized numeric payload size");
        System.out.println("BackgroundPaintTest: "+checks+" world axes/read-only map/drag/height/material/eraser/undo/redo/bounds checks PASS");
    }
}
