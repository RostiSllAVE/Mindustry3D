package fpv.background;
import java.util.ArrayDeque;

/** One bounded snapshot per stroke/action, never a separate gameplay construction queue. */
public final class BackgroundPaintHistory {
    public static final int LIMIT=32;
    private final BackgroundPaint paint;
    private final ArrayDeque<BackgroundPaint> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private BackgroundPaint before;
    public BackgroundPaintHistory(BackgroundPaint paint){this.paint=paint;}
    public void begin(){if(before==null)before=paint.copy();}
    public boolean end(){if(before==null)return false;boolean changed=!paint.same(before);if(changed){undo.addLast(before);while(undo.size()>LIMIT)undo.removeFirst();redo.clear();}before=null;return changed;}
    public boolean undo(){end();if(undo.isEmpty())return false;redo.addLast(paint.copy());paint.copyFrom(undo.removeLast());return true;}
    public boolean redo(){end();if(redo.isEmpty())return false;undo.addLast(paint.copy());paint.copyFrom(redo.removeLast());return true;}
    public int undoCount(){return undo.size();}public int redoCount(){return redo.size();}
    public int maxBytes(){return (LIMIT+2)*paint.bytes();}
}
