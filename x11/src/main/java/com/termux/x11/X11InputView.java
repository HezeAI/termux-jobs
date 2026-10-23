package com.termux.x11;

import android.content.Context;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.*;

/** Activity-owned IME endpoint. No native core, clipboard or text history here. */
final class X11InputView extends View {
    interface Sink { void text(String text); void key(int code,boolean down); }
    private final Sink sink;
    private boolean active;
    private int generation;
    private Runnable windowFocused=()->{};
    void onWindowFocused(Runnable callback){windowFocused=callback;}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)windowFocused.run();}
    private final java.util.Set<Integer> pressed=new java.util.HashSet<>();
    X11InputView(Context context,Sink sink){super(context);this.sink=sink;setFocusable(true);setFocusableInTouchMode(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void active(boolean value){if(active&&!value)for(int code:new java.util.ArrayList<>(pressed))emit(code,false);if(active!=value){active=value;generation++;}if(!value)clearFocus();}
    void restart(){for(int code:new java.util.ArrayList<>(pressed))emit(code,false);generation++;}
    @Override public boolean onCheckIsTextEditor(){return active;}
    @Override public InputConnection onCreateInputConnection(EditorInfo info){
        if(!active)return null;
        int epoch=generation;
        info.inputType=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        info.imeOptions=EditorInfo.IME_FLAG_NO_EXTRACT_UI|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING|EditorInfo.IME_ACTION_NONE;
        return new BaseInputConnection(this,true){
            private String composing="";
            private boolean live(){return active&&epoch==generation;}
            // X11 has no Android preedit surface: mirror the current composing word
            // immediately, replacing only its changed suffix, never prior committed text.
            private void replaceComposing(String next){
                int prefix=0;
                while(prefix<composing.length()&&prefix<next.length()){
                    int old=composing.codePointAt(prefix),fresh=next.codePointAt(prefix);
                    if(old!=fresh)break;prefix+=Character.charCount(old);
                }
                for(int i=composing.codePointCount(prefix,composing.length());i>0;i--)press(KeyEvent.KEYCODE_DEL);
                if(prefix<next.length())X11TextEvents.send(next.substring(prefix),sink::text,X11InputView.this::press);
                composing=next;
            }
            private void clearLocal(){composing="";getEditable().clear();android.text.Selection.setSelection(getEditable(),0);}
            @Override public boolean commitText(CharSequence text,int position){
                if(!live())return true;super.commitText(text==null?"":text,position);replaceComposing(getEditable().toString());clearLocal();return true;
            }
            @Override public boolean setComposingText(CharSequence text,int position){
                if(!live())return true;super.setComposingText(text==null?"":text,position);replaceComposing(getEditable().toString());android.text.Selection.setSelection(getEditable(),getEditable().length());return true;
            }
            @Override public boolean finishComposingText(){
                if(!live())return true;super.finishComposingText();clearLocal();return true;
            }
            @Override public boolean sendKeyEvent(KeyEvent e){
                if(!live())return true;
                if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getKeyCode()==KeyEvent.KEYCODE_DEL&&!composing.isEmpty()){
                    int end=composing.offsetByCodePoints(composing.length(),-1);composing=composing.substring(0,end);getEditable().delete(end,getEditable().length());android.text.Selection.setSelection(getEditable(),end);
                }
                if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getKeyCode()==KeyEvent.KEYCODE_ENTER)clearLocal();
                return forward(e);
            }
            @Override public boolean deleteSurroundingText(int before,int after){
                if(!live())return true;
                int left=Math.max(0,Math.min(256,before)),right=Math.max(0,Math.min(256,after));
                int local=Math.min(left,composing.length()),end=composing.length()-local;
                if(end>0&&end<composing.length()&&Character.isHighSurrogate(composing.charAt(end-1))&&Character.isLowSurrogate(composing.charAt(end)))end--;
                if(local>0){getEditable().delete(end,getEditable().length());android.text.Selection.setSelection(getEditable(),end);replaceComposing(getEditable().toString());}
                for(int i=local;i<left;i++)press(KeyEvent.KEYCODE_DEL);
                for(int i=0;i<right;i++)press(KeyEvent.KEYCODE_FORWARD_DEL);
                return true;
            }
            @Override public boolean performEditorAction(int action){if(live()){finishComposingText();press(KeyEvent.KEYCODE_ENTER);}return true;}
        };
    }
    private void emit(int code,boolean down){if(down)pressed.add(code);else pressed.remove(code);sink.key(code,down);}
    private void press(int code){emit(code,true);emit(code,false);}
    private boolean forward(KeyEvent e){
        int code=e.getKeyCode();if(!active||code==KeyEvent.KEYCODE_BACK||code==KeyEvent.KEYCODE_VOLUME_UP||code==KeyEvent.KEYCODE_VOLUME_DOWN||code<0||code>=304)return false;
        if(e.getAction()!=KeyEvent.ACTION_DOWN&&e.getAction()!=KeyEvent.ACTION_UP)return false;
        emit(code,e.getAction()==KeyEvent.ACTION_DOWN);return true;
    }
    @Override public boolean onKeyDown(int code,KeyEvent e){return forward(e)||super.onKeyDown(code,e);}
    @Override public boolean onKeyUp(int code,KeyEvent e){return forward(e)||super.onKeyUp(code,e);}
}
