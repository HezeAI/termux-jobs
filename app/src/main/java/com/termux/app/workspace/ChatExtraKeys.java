package com.termux.app.workspace;

import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import com.termux.shared.terminal.io.TerminalExtraKeys;
import com.termux.shared.terminal.io.extrakeys.ExtraKeysView;
import com.termux.shared.terminal.io.extrakeys.SpecialButton;
import com.termux.view.TerminalView;
import java.util.function.Supplier;
import static com.termux.shared.terminal.io.extrakeys.ExtraKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS;

/** Chat extra keys edit a composer; they never write into the bridge PTY. */
final class ChatExtraKeys extends TerminalExtraKeys {
    private final ExtraKeysView keys;
    private final Supplier<EditText> target;
    ChatExtraKeys(TerminalView terminal,ExtraKeysView keys,Supplier<EditText> target){super(terminal);this.keys=keys;this.target=target;}
    private boolean down(SpecialButton b){return Boolean.TRUE.equals(keys.readSpecialButton(b,true));}
    @Override public void onTerminalExtraKeyButtonClick(View source,String key,boolean ctrl,boolean alt,boolean shift,boolean fn){
        ctrl|=down(SpecialButton.CTRL);alt|=down(SpecialButton.ALT);shift|=down(SpecialButton.SHIFT);fn|=down(SpecialButton.FN);
        EditText input=target.get();if(input==null||!input.isEnabled())return;
        if(key.equals("KEYBOARD")){input.requestFocus();((android.view.inputmethod.InputMethodManager)input.getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).showSoftInput(input,0);return;}
        if(key.equals("PASTE")){input.onTextContextMenuItem(android.R.id.paste);return;}
        if(key.equals("DRAWER"))return;
        Integer code=PRIMARY_KEY_CODES_FOR_STRINGS.get(key);
        if(code==null&&key.length()==1&&Character.toLowerCase(key.charAt(0))>='a'&&Character.toLowerCase(key.charAt(0))<='z'&&(ctrl||alt))code=KeyEvent.KEYCODE_A+Character.toLowerCase(key.charAt(0))-'a';
        if(code!=null){int meta=(ctrl?KeyEvent.META_CTRL_ON|KeyEvent.META_CTRL_LEFT_ON:0)|(alt?KeyEvent.META_ALT_ON|KeyEvent.META_ALT_LEFT_ON:0)|(shift?KeyEvent.META_SHIFT_ON|KeyEvent.META_SHIFT_LEFT_ON:0)|(fn?KeyEvent.META_FUNCTION_ON:0);input.dispatchKeyEvent(new KeyEvent(0,0,KeyEvent.ACTION_DOWN,code,0,meta));input.dispatchKeyEvent(new KeyEvent(0,0,KeyEvent.ACTION_UP,code,0,meta));}
        else if(!ctrl&&!alt){int start=Math.max(0,input.getSelectionStart()),end=Math.max(0,input.getSelectionEnd());input.getText().replace(Math.min(start,end),Math.max(start,end),key);}
    }
}
