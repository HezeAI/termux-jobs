package com.termux.app.workspace;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
/** One content scale; native layout and hit testing retain the same physical coordinates. */
final class SidebarStyle {
    static final float SCALE=.8f;
    private SidebarStyle(){}
    static Context content(Context base){
        Configuration config=new Configuration(base.getResources().getConfiguration());
        int density=config.densityDpi>0?config.densityDpi:base.getResources().getDisplayMetrics().densityDpi;
        config.densityDpi=Math.max(1,Math.round((density>0?density:DisplayMetrics.DENSITY_DEFAULT)*SCALE));
        ContextThemeWrapper context=new ContextThemeWrapper(base,0);context.applyOverrideConfiguration(config);context.getTheme().setTo(base.getTheme());return context;
    }
}
