package com.termux.x11;
import android.content.Context;
import android.content.res.Resources;
final class X11Text {
    static String tr(Context context,String ru,String en){String lang=context.getSharedPreferences("workspace-language",Context.MODE_PRIVATE).getString("language","system");if(lang.equals("system"))lang=Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();return lang.equals("ru")?ru:en;}
}
