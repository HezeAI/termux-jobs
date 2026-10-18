package com.termux.app.workspace;

import android.app.AlertDialog;

/** Main-thread presentation only. Hiding the window never owns/cancels the transport. */
final class ServerTransferPresentation {
    private String owner="",text="";
    private AlertDialog window;
    private boolean active,background;
    private long bytes,total=-1;
    private Runnable changed=()->{};
    void listener(Runnable listener){changed=listener==null?()->{}:listener;}
    boolean active(){return active;}
    boolean belongs(String id){return active&&owner.equals(id);}
    String text(){return text;}
    boolean indeterminate(){return total<0;}
    int progress(){return total<0?0:total==0?1000:(int)Math.max(0,Math.min(1000,bytes*1000.0/total));}
    void begin(String id,AlertDialog dialog,String initial){bytes=0;total=-1;owner=id;window=dialog;text=initial;active=true;background=false;changed.run();}
    void update(String value){text=value;if(active)changed.run();}
    void update(String value,long done,long size){text=value;bytes=done;total=size;if(active)changed.run();}
    void background(){if(!active||window==null)return;background=true;window.hide();changed.run();}
    void foreground(){if(!active||window==null)return;background=false;window.show();changed.run();}
    boolean finish(){boolean hidden=background;active=false;background=false;window=null;changed.run();return hidden;}
}
