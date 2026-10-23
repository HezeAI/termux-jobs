package com.termux.x11;

import java.util.function.BiConsumer;
/** Bounded metadata framing. Never parses commands, key events or text-field contents. */
final class X11FocusStream {
    private final String token;
    private final BiConsumer<String,Boolean> focus;
    private final java.util.function.Consumer<Boolean> startup;
    private final StringBuilder line=new StringBuilder();
    private boolean overflow;
    X11FocusStream(String token,BiConsumer<String,Boolean> focus){this(token,focus,ok->{});}
    X11FocusStream(String token,BiConsumer<String,Boolean> focus,java.util.function.Consumer<Boolean> startup){this.token=token;this.focus=focus;this.startup=startup;}
    void feed(String chunk){for(int i=0;i<chunk.length();i++){char c=chunk.charAt(i);if(c=='\n'){if(!overflow)parse(line.toString());line.setLength(0);overflow=false;}else if(c!='\r'){if(line.length()<512&&!overflow)line.append(c);else{overflow=true;line.setLength(0);}}}}
    private void parse(String value){
        String[] p=value.split(" ");
        if(p.length==2&&p[1].equals(token)){if(p[0].equals("JOBS_X11_DESKTOP_READY"))startup.accept(true);else if(p[0].equals("JOBS_X11_DESKTOP_TIMEOUT"))startup.accept(false);return;}
        if(p.length!=5||!p[0].equals("JOBS_X11_FOCUS_V1")||!p[1].equals(token))return;
        if(!p[3].matches(":[0-9]{1,12}\\.[0-9]{1,12}")||!p[4].matches("/[A-Za-z0-9_/]{1,240}"))return;
        if(p[2].equals("FocusIn"))focus.accept(p[3]+p[4],true);
        else if(p[2].equals("FocusOut")||p[2].equals("DestroyIC"))focus.accept(p[3]+p[4],false);
    }
}
