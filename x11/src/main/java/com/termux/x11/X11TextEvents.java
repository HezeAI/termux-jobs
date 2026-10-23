package com.termux.x11;

import android.view.KeyEvent;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** IMEs can commit Enter/Tab as text; X11 needs keys, not Unicode control symbols. */
final class X11TextEvents {
    private X11TextEvents(){}
    static void send(String text,Consumer<String> plain,IntConsumer press){
        int start=0;
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);if(c!='\n'&&c!='\r'&&c!='\t')continue;
            if(start<i)plain.accept(text.substring(start,i));
            press.accept(c=='\t'?KeyEvent.KEYCODE_TAB:KeyEvent.KEYCODE_ENTER);
            if(c=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;
            start=i+1;
        }
        if(start<text.length())plain.accept(text.substring(start));
    }
}
