package com.termux.view;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import com.termux.terminal.*;
import java.util.*;

/** Paint-only append reveal. The emulator receives every byte immediately, including all controls. */
final class TerminalTextReveal {
    private final TerminalView view;
    private final Paint mask=new Paint();
    private String[] previous;
    private int columns,rows,cursor,reveal,end,index;
    private final ArrayList<Integer> glyphs=new ArrayList<>();
    private boolean scheduled;
    private long last,suppressUntil;
    private double budget;
    TerminalTextReveal(TerminalView view){this.view=view;}
    private boolean eligible(){TerminalEmulator e=view.mEmulator;return e!=null&&view.isShown()&&view.isAttachedToWindow()&&view.getTopRow()==0&&!view.isSelectingText()&&!e.isAlternateBufferActive()&&!e.isMouseTrackingActive()&&ValueAnimator.areAnimatorsEnabled()&&SystemClock.uptimeMillis()>=suppressUntil;}
    private String[] cells(TerminalEmulator e){
        String[] result=new String[e.mRows*e.mColumns];Arrays.fill(result,"");
        TerminalBuffer screen=e.getScreen();
        for(int row=0;row<e.mRows;row++){
            TerminalRow line=screen.allocateFullLineIfNecessary(screen.externalToInternalRow(row));
            for(int at=0,col=0,lastLead=-1;at<line.getSpaceUsed()&&col<e.mColumns;){
                int cp=Character.codePointAt(line.mText,at,line.getSpaceUsed()),count=Character.charCount(cp),width=WcWidth.width(cp);
                if(width<=0){if(lastLead>=0)result[row*e.mColumns+lastLead]+=new String(Character.toChars(cp));}
                else{lastLead=col;result[row*e.mColumns+col]=new String(Character.toChars(cp));col+=width;}at+=count;
            }
        }
        return result;
    }
    void update(int scrolled){
        TerminalEmulator e=view.mEmulator;if(e==null||!eligible()){reset();return;}
        String[] next=cells(e);int nextCursor=e.getCursorRow()*e.mColumns+e.getCursorCol();
        boolean append=eligible()&&previous!=null&&columns==e.mColumns&&rows==e.mRows&&scrolled>=0&&scrolled<rows&&e.getCursorCol()!=e.mColumns-1;
        int shift=scrolled*e.mColumns,start=cursor-shift;
        if(start<0||nextCursor<=start)append=false;
        if(append)for(int i=0;i<start;i++){if(!next[i].equals(previous[i+shift])){append=false;break;}}
        if(append)for(int i=Math.max(0,cursor);i<Math.min(previous.length,nextCursor+shift);i++){if(!previous[i].trim().isEmpty()){append=false;break;}}
        if(append){
            int from=pending()?Math.max(0,reveal-shift):start;glyphs.clear();index=0;end=nextCursor;
            appendGlyphs(next,from,end,e.mColumns);
            reveal=glyphs.isEmpty()?end:glyphs.get(0);
            if(pending()&&!scheduled){budget=0;last=SystemClock.uptimeMillis();schedule();}
        }else finish();
        previous=next;columns=e.mColumns;rows=e.mRows;cursor=nextCursor;
    }
    private void appendGlyphs(String[] cells,int from,int until,int width){
        for(int row=from/width;row<=until/width;row++){
            StringBuilder text=new StringBuilder();ArrayList<Integer> positions=new ArrayList<>();
            int limit=Math.min(until,(row+1)*width);
            for(int i=Math.max(from,row*width);i<limit;i++)for(int c=0;c<cells[i].length();c++){text.append(cells[i].charAt(c));positions.add(i);}
            android.icu.text.BreakIterator boundaries=android.icu.text.BreakIterator.getCharacterInstance();boundaries.setText(text.toString());
            for(int begin=boundaries.first(),next=boundaries.next();next!=android.icu.text.BreakIterator.DONE;begin=next,next=boundaries.next())
                if(!text.substring(begin,next).trim().isEmpty())glyphs.add(positions.get(begin));
        }
    }
    boolean pending(){return index<glyphs.size();}
    private void schedule(){scheduled=true;view.postOnAnimation(tick);}
    private final Runnable tick=()->step();
    private void step(){scheduled=false;if(!pending())return;if(!eligible()){finish();return;}
        long now=SystemClock.uptimeMillis();budget+=Math.min(64,Math.max(0,now-last));last=now;
        while(pending()){
            int pending=glyphs.size()-index;double delay=pending<=5?12:Math.max(.05,12.0/(1+(pending-5)/12.0));if(budget<delay)break;
            budget-=delay;index++;reveal=pending()?glyphs.get(index):end;
        }
        view.invalidate();if(pending())schedule();
    }
    void paint(Canvas canvas){
        if(!pending())return;if(!eligible()){finish();return;}
        TerminalEmulator e=view.mEmulator;float cell=view.mRenderer.getFontWidth();int line=view.mRenderer.getFontLineSpacing(),ascent=view.mRenderer.mFontLineSpacingAndAscent;
        mask.setColor(e.mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND]);
        for(int row=reveal/columns;row<=end/columns&&row<rows;row++){
            int left=row==reveal/columns?reveal%columns:0,right=row==end/columns?end%columns:columns;
            if(right>left)canvas.drawRect(left*cell,Math.max(0,ascent+row*line),(right)*cell,ascent+(row+1)*line,mask);
        }
    }
    void interaction(){suppressUntil=SystemClock.uptimeMillis()+400;finish();}
    void finish(){view.removeCallbacks(tick);scheduled=false;glyphs.clear();index=0;reveal=end;view.invalidate();}
    void reset(){finish();previous=null;}
}
