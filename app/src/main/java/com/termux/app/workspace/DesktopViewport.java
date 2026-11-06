package com.termux.app.workspace;

import android.view.View;
import android.widget.RelativeLayout;
import com.termux.R;

/** Resizes the existing host in place. No Surface reparent, engine or protocol change. */
final class DesktopViewport {
    private final View host;
    private boolean active;
    private int below,above,bottom;
    DesktopViewport(View host){this.host=host;}
    void present(boolean value){
        if(host==null||!(host.getLayoutParams() instanceof RelativeLayout.LayoutParams))return;
        RelativeLayout.LayoutParams p=(RelativeLayout.LayoutParams)host.getLayoutParams();
        if(value&&!active){below=p.getRule(RelativeLayout.BELOW);above=p.getRule(RelativeLayout.ABOVE);bottom=p.getRule(RelativeLayout.ALIGN_PARENT_BOTTOM);}
        if(value){p.addRule(RelativeLayout.BELOW,R.id.workspace_header);p.removeRule(RelativeLayout.ABOVE);p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);}
        else if(active){set(p,RelativeLayout.BELOW,below);set(p,RelativeLayout.ABOVE,above);set(p,RelativeLayout.ALIGN_PARENT_BOTTOM,bottom);}
        else return;
        active=value;host.setLayoutParams(p);
    }
    private void set(RelativeLayout.LayoutParams p,int rule,int value){if(value==0)p.removeRule(rule);else p.addRule(rule,value);}
}
