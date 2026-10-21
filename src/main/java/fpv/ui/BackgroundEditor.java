package fpv.ui;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.*;
import fpv.background.*;
import fpv.render.*;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

/** Modal, client-local world painter. Its input never reaches native construction controls. */
public final class BackgroundEditor extends BaseDialog {
    private final Object world=Vars.world.tiles;
    private final long profileContext=BackgroundProfiles.context();
    private final BackgroundStyle working;
    private final BackgroundPaint paint;
    private final BackgroundPaintHistory history;
    private final Canvas canvas;
    private final BackgroundPreview preview;
    private boolean previewing;
    private BackgroundPaint.Brush brush=BackgroundPaint.Brush.FOREST;
    private float radius=32,hardness=.7f,strength=.6f,brushHeight=12;
    private String status="Paint edits are staged. Preview / Apply rebuilds only the scenery.";
    public static BackgroundEditor open(){if(!Vars.state.isGame()||Vars.world.width()<1)return null;var editor=new BackgroundEditor();editor.show();return editor;}
    private BackgroundEditor(){super("Background Editor — local visual world");
        working=BackgroundProfiles.current().copy();if(working.mode!=BackgroundStyle.Mode.CUSTOM)working.customBaseManual=working.mode==BackgroundStyle.Mode.MANUAL;working.mode=BackgroundStyle.Mode.CUSTOM;
        paint=working.paint(Vars.world.width(),Vars.world.height());history=new BackgroundPaintHistory(paint);canvas=new Canvas();canvas.name="fpv-background-canvas";preview=new BackgroundPreview(canvas.mapTexture);preview.name="fpv-background-3d";preview.visible=false;
        float panelWidth=Math.max(600,Math.min(1180,Core.graphics.getWidth()/arc.scene.ui.layout.Scl.scl()-60));
        cont.defaults().pad(4).width(panelWidth);
        cont.label(()->previewing?"3D scenery preview · Central map reference · Edits are visual only":"N ↑ · White rectangle = read-only map · Paint outside the border").wrap().fontScale(.8f).row();
        cont.button("Sky / map appearance",()->BackgroundSkyControls.open(working,()->status="Sky changes staged. Preview / Apply to show.")).height(32).get().name="fpv-background-sky";cont.row();
        cont.table(body->{
            body.pane(tools->{tools.defaults().width(210).pad(2);tools.add("[accent]Biome / shape brush").fontScale(.8f).row();
                for(var b:BackgroundPaint.Brush.values())tools.button(b.label,()->{brush=b;if(b.shape())brushHeight=b.defaultHeight;}).height(32).update(button->{button.setText((brush==b?"> ":"")+b.label);button.getLabel().setFontScale(.8f);}).row();
            }).width(226).growY();
            body.stack(canvas,preview).minSize(340,180).grow();
        }).grow().width(Math.max(600,Math.min(1180,Core.graphics.getWidth()/arc.scene.ui.layout.Scl.scl()-60))).height(Math.max(190,Math.min(600,Core.graphics.getHeight()/arc.scene.ui.layout.Scl.scl()-320))).row();
        cont.table(options->{options.defaults().pad(3);options.add("Radius").fontScale(.8f);options.slider(8,512,4,radius,v->radius=v).width(200);options.label(()->(int)radius+" tiles").fontScale(.8f).width(80);options.add("Strength").fontScale(.8f);options.slider(0,100,1,strength*100,v->strength=v/100).width(180);options.label(()->(int)(strength*100)+"%").fontScale(.8f).width(55);options.row();
            options.add("Falloff").fontScale(.8f);options.slider(0,98,1,hardness*100,v->hardness=v/100).width(200);options.label(()->(int)(hardness*100)+"% hard").fontScale(.8f).width(80);options.add("Height / step").fontScale(.8f);
            var height=options.slider(0,BackgroundPaint.MAX_HEIGHT,1,brushHeight,v->brushHeight=v).width(180).get();height.update(()->height.setValue(brushHeight));options.label(()->(int)Math.min(brushHeight,radius*.8f)+" tiles").fontScale(.8f).width(80);
        }).row();
        cont.label(()->status).wrap().fontScale(.8f).row();
        cont.label(()->previewing?"LMB drag: orbit · RMB / MMB drag: pan · Wheel: distance · Edit map: return to painting":"LMB: paint · RMB / MMB: pan · Wheel: zoom · Ctrl+Z / Ctrl+Y: undo / redo\nHeight is limited by radius for safe slopes. No blocks, costs or server messages.").wrap().fontScale(.75f).row();
        buttons.defaults().height(42).width(panelWidth/8-6).pad(3);
        buttons.button("Undo",this::undo).disabled(b->history.undoCount()==0);buttons.button("Redo",this::redo).disabled(b->history.redoCount()==0);
        buttons.button("Clear",()->{history.begin();paint.clear();history.end();status="Custom paint cleared; base remains. Preview / Apply to show.";});
        buttons.button("Reset Auto",()->{working.mode=BackgroundStyle.Mode.AUTO;BackgroundProfiles.apply(working);status="Auto applied; paint retained. "+BackgroundProfiles.status();});
        buttons.button("Preview 3D",this::preview).get().name="fpv-background-preview";
        buttons.button("Edit map",()->{canvas.visible=true;preview.visible=false;previewing=false;}).get().name="fpv-background-edit";
        buttons.button("Apply",this::apply).get().name="fpv-background-apply";
        buttons.button("Cancel",this::hide);for(var child:buttons.getChildren())if(child instanceof arc.scene.ui.TextButton button)button.getLabel().setFontScale(.8f);addCloseListener();
        hidden(()->{BackgroundProfiles.revertPreview(profileContext);preview.close();canvas.close();});
        keyDown(key->{if(Core.input.ctrl()&&key==KeyCode.z){if(Core.input.shift())redo();else undo();}else if(Core.input.ctrl()&&key==KeyCode.y)redo();});
    }
    private boolean validWorld(){return world==Vars.world.tiles&&Vars.state.isGame()&&profileContext==BackgroundProfiles.context();}
    private void undo(){canvas.finishStroke();history.undo();status="Undone (staged).";}
    private void redo(){canvas.finishStroke();history.redo();status="Redone (staged).";}
    public void preview(){if(!validWorld()){hide();return;}canvas.finishStroke();working.mode=BackgroundStyle.Mode.CUSTOM;BackgroundProfiles.preview(working);preview.focus(canvas.cx,canvas.cy);canvas.visible=false;preview.visible=true;previewing=true;status="Preview active. Edit map returns to brushes. Apply saves; Cancel restores the applied background.";}
    public void apply(){if(!validWorld()){hide();return;}working.mode=BackgroundStyle.Mode.CUSTOM;BackgroundProfiles.apply(working);status=BackgroundProfiles.status()+" Further strokes remain staged.";}
    /** Diagnostic access to the same UI model; no separate test-only painter implementation. */
    public BackgroundPaint paint(){return paint;}public BackgroundPaintHistory history(){return history;}
    public boolean previewing(){return previewing;}public BackgroundPreview viewport(){return preview;}
    private final class Canvas extends Element implements AutoCloseable {
        private final Color color=new Color();
        private final int[] base=new int[(BackgroundPaint.NODES-1)*(BackgroundPaint.NODES-1)];
        private final BorderBiomeProfile.Cell[] cells=new BorderBiomeProfile.Cell[BackgroundPaint.MATERIALS.length];
        private final BackgroundPaint.Sample sample=new BackgroundPaint.Sample();
        private final Texture mapTexture;
        private final TextureRegion mapRegion;
        private double cx=(paint.width-1)*.5,cy=(paint.height-1)*.5,zoom=1,lastX,lastY;
        private float mouseX,mouseY;
        private boolean drawing,panning,hover,closed,initialized;
        private BackgroundPaint.Stroke stroke;
        private final BorderBiomeProfile source;private final HorizonLandscape baseLandscape;private final HorizonLandscape.Surface baseSurface=new HorizonLandscape.Surface();
        Canvas(){touchable=Touchable.enabled;
            var pix=new Pixmap(96,96);for(int y=0;y<96;y++)for(int x=0;x<96;x++){var t=Vars.world.tile(x*paint.width/96,y*paint.height/96);var c=t.block()!=mindustry.content.Blocks.air?t.block().mapColor:t.floor().mapColor;pix.set(x,95-y,c.rgba8888());}
            mapTexture=new Texture(pix);pix.dispose();mapTexture.setFilter(Texture.TextureFilter.nearest);mapRegion=new TextureRegion(mapTexture);
            for(var m:BackgroundPaint.MATERIALS)if(m.preset!=null)cells[m.ordinal()]=m.preset.cell();
            var nativeSource=BorderBiomeProfile.analyze(BorderBiomeProfile.nativeSource(new fpv.terrain.TerrainElevation()),8);var baseStyle=working.copy();baseStyle.mode=working.customBaseManual?BackgroundStyle.Mode.MANUAL:BackgroundStyle.Mode.AUTO;baseStyle.paint=null;source=baseStyle.profile(nativeSource);baseLandscape=new HorizonLandscape(source,HorizonComposition.EMPTY);var landscape=baseLandscape;var surface=new HorizonLandscape.Surface();
            int side=BackgroundPaint.NODES-1;for(int y=0;y<side;y++)for(int x=0;x<side;x++){float wx=(float)((paint.xs[x]+paint.xs[x+1])*.5),wy=(float)((paint.ys[y]+paint.ys[y+1])*.5);if(!paint.outside(wx,wy))continue;var p=HorizonGeometry.coordinates(source,wx,-wy);landscape.sample(p.along(),p.distance(),wx,-wy,surface);base[y*side+x]=color.set(surface.coast>=0?surface.waterR:surface.r,surface.coast>=0?surface.waterG:surface.g,surface.coast>=0?surface.waterB:surface.b,1).lerp(Color.black,surface.space).rgba8888();}
            addListener(new InputListener(){
                @Override public boolean touchDown(InputEvent e,float x,float y,int pointer,KeyCode button){if(pointer!=0)return false;if(button!=KeyCode.mouseLeft&&button!=KeyCode.mouseRight&&button!=KeyCode.mouseMiddle)return false;e.stop();Core.scene.setKeyboardFocus(Canvas.this);Core.scene.setScrollFocus(Canvas.this);mouseX=x;mouseY=y;lastX=worldX(x);lastY=worldY(y);
                    if(button==KeyCode.mouseLeft){drawing=true;history.begin();stroke=paint.stroke(brush,radius,hardness,strength,brushHeight,Canvas.this::baseHeight);stroke.line(lastX,lastY,lastX,lastY);}else panning=true;return true;}
                @Override public void touchDragged(InputEvent e,float x,float y,int pointer){if(pointer!=0)return;e.stop();if(drawing){double wx=worldX(x),wy=worldY(y);stroke.line(lastX,lastY,wx,wy);lastX=wx;lastY=wy;}else if(panning){cx-=(x-mouseX)/zoom;cy-=(y-mouseY)/zoom;cx=Math.max(paint.xs[0],Math.min(paint.xs[BackgroundPaint.NODES-1],cx));cy=Math.max(paint.ys[0],Math.min(paint.ys[BackgroundPaint.NODES-1],cy));}mouseX=x;mouseY=y;}
                @Override public void touchUp(InputEvent e,float x,float y,int pointer,KeyCode button){if(pointer!=0)return;e.stop();if(drawing){history.end();status="Stroke staged. Preview / Apply to rebuild the 3D scenery.";}drawing=panning=false;stroke=null;}
                @Override public boolean mouseMoved(InputEvent e,float x,float y){mouseX=x;mouseY=y;return true;}
                @Override public void enter(InputEvent e,float x,float y,int pointer,Element from){if(pointer==-1){hover=true;Core.scene.setScrollFocus(Canvas.this);}}
                @Override public void exit(InputEvent e,float x,float y,int pointer,Element to){if(pointer==-1){hover=false;if(Core.scene.getScrollFocus()==Canvas.this)Core.scene.setScrollFocus(null);}}
                @Override public boolean scrolled(InputEvent e,float x,float y,float amountX,float amountY){e.stop();double wx=worldX(x),wy=worldY(y);zoom=Math.max(.025,Math.min(12,zoom*Math.pow(1.22,-amountY)));cx=wx-(x-getWidth()*.5)/zoom;cy=wy-(y-getHeight()*.5)/zoom;return true;}
            });
        }
        private double baseHeight(double x,double y){if(!paint.outside(x,y))return 0;var pos=HorizonGeometry.coordinates(source,(float)x,(float)-y);baseLandscape.sample(pos.along(),pos.distance(),(float)x,(float)-y,baseSurface);return Math.max(0,baseSurface.height);}
        private double worldX(float x){return cx+(x-getWidth()*.5)/zoom;}private double worldY(float y){return cy+(y-getHeight()*.5)/zoom;}
        private void finishStroke(){history.end();drawing=panning=false;stroke=null;}
        private float sx(double x){return getX(arc.util.Align.left)+getWidth()*.5f+(float)((x-cx)*zoom);}private float sy(double y){return getY(arc.util.Align.bottom)+getHeight()*.5f+(float)((y-cy)*zoom);}
        @Override public void act(float delta){super.act(delta);if(!validWorld()){BackgroundEditor.this.hide();return;}if(!initialized&&getWidth()>0){zoom=Math.min(getWidth(),getHeight())/(Math.max(paint.width,paint.height)+180.0);initialized=true;}}
        @Override public void draw(){if(closed||!clipBegin())return;
            Draw.color(.025f,.035f,.055f,1);Fill.rect(getX(arc.util.Align.left)+getWidth()*.5f,getY(arc.util.Align.bottom)+getHeight()*.5f,getWidth(),getHeight());int side=BackgroundPaint.NODES-1;
            for(int y=0;y<side;y++)for(int x=0;x<side;x++){
                float left=sx(paint.xs[x]),right=sx(paint.xs[x+1]),bottom=sy(paint.ys[y]),top=sy(paint.ys[y+1]);if(right<getX(arc.util.Align.left)||left>getX(arc.util.Align.left)+getWidth()||top<getY(arc.util.Align.bottom)||bottom>getY(arc.util.Align.bottom)+getHeight())continue;
                double wx=(paint.xs[x]+paint.xs[x+1])*.5,wy=(paint.ys[y]+paint.ys[y+1])*.5;if(!paint.outside(wx,wy))continue;
                color.set(base[y*side+x]);paint.sample(wx,wy,sample);float r=0,g=0,b=0;for(int m=1;m<cells.length;m++)if(cells[m]!=null){r+=cells[m].r()*sample.weights[m];g+=cells[m].g()*sample.weights[m];b+=cells[m].b()*sample.weights[m];}
                float a=(float)HorizonLandscape.smooth(.2,.8,sample.coverage);color.lerp(r,g,b,1,a);if(sample.heightCoverage>0)color.mul(1+sample.heightCoverage*sample.height/BackgroundPaint.MAX_HEIGHT*.22f);Draw.color(color);Fill.rect((left+right)*.5f,(top+bottom)*.5f,right-left+.2f,top-bottom+.2f);
            }
            Draw.color();Draw.rect(mapRegion,sx((paint.width-1)*.5),sy((paint.height-1)*.5),(float)(paint.width*zoom),(float)(paint.height*zoom));Lines.stroke(2);Draw.color(Color.white);Lines.rect(sx(-.5),sy(-.5),(float)(paint.width*zoom),(float)(paint.height*zoom));
            if(hover){Draw.color(paint.outside(worldX(mouseX),worldY(mouseY))&&worldX(mouseX)>paint.xs[0]&&worldX(mouseX)<paint.xs[BackgroundPaint.NODES-1]&&worldY(mouseY)>paint.ys[0]&&worldY(mouseY)<paint.ys[BackgroundPaint.NODES-1]?Color.gold:Color.scarlet);Lines.stroke(1.5f);Lines.circle(getX(arc.util.Align.left)+mouseX,getY(arc.util.Align.bottom)+mouseY,(float)(radius*zoom));}
            Draw.reset();clipEnd();
        }
        @Override public void close(){if(closed)return;closed=true;history.end();mapTexture.dispose();if(Core.scene.getScrollFocus()==this)Core.scene.setScrollFocus(null);if(Core.scene.getKeyboardFocus()==this)Core.scene.setKeyboardFocus(null);}
    }
}
