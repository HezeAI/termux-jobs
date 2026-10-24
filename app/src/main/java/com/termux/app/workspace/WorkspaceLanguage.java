package com.termux.app.workspace;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

/** App-local language preference; no activity restart and no changes to shell locales. */
public final class WorkspaceLanguage {
    private WorkspaceLanguage() {}
    private static final String PREFS="workspace-language", KEY="language";
    public static String choice(Context context) {
        String value=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"system");
        return value.equals("ru") || value.equals("en") ? value : "system";
    }
    static String resolve(String choice,Locale system) {
        return choice.equals("ru") || choice.equals("en") ? choice : system.getLanguage().equals("ru") ? "ru" : "en";
    }
    public static String code(Context context) {
        return resolve(choice(context),Resources.getSystem().getConfiguration().getLocales().get(0));
    }
    public static Context wrap(Context context) {
        // Override only language. Copying the whole configuration pins portrait
        // orientation/window bounds/density in this context after phone rotation.
        Configuration config=new Configuration();
        config.setLocale(new Locale(code(context)));
        return context.createConfigurationContext(config);
    }
    @SuppressWarnings("deprecation")
    public static void initialize(Context context) {
        WorkspaceText.resources=wrap(context).getResources();
        Configuration english=new Configuration();english.setLocale(Locale.ENGLISH);
        WorkspaceText.englishResources=context.createConfigurationContext(english).getResources();
        Context app=context.getApplicationContext();
        if(app!=null){Configuration config=new Configuration(app.getResources().getConfiguration());config.setLocale(new Locale(code(context)));app.getResources().updateConfiguration(config,app.getResources().getDisplayMetrics());}
    }
    @SuppressWarnings("deprecation")
    public static void apply(Activity activity) {
        initialize(activity);
        Configuration config=new Configuration(activity.getResources().getConfiguration());
        config.setLocale(new Locale(code(activity)));
        activity.getResources().updateConfiguration(config,activity.getResources().getDisplayMetrics());
    }
    /** Preserve the selected UI language, but take window geometry from the system event. */
    @SuppressWarnings("deprecation")
    public static void configurationChanged(Activity activity,Configuration current){
        Configuration localized=new Configuration(current);
        localized.setLocale(new Locale(code(activity)));
        activity.getResources().updateConfiguration(localized,activity.getResources().getDisplayMetrics());
    }
    static void select(Context context,String value) {
        if(!value.equals("system")&&!value.equals("ru")&&!value.equals("en"))throw new IllegalArgumentException("Invalid app language");
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,value).apply();
    }
    public static void showPicker(Activity activity,Runnable changed) {
        String[] codes={"system","ru","en"}; String current=choice(activity); int selected=0;
        for(int i=0;i<codes.length;i++)if(codes[i].equals(current))selected=i;
        WorkspaceDialogs.builder(activity).setTitle(WorkspaceText.tr("Язык / Language"))
            .setSingleChoiceItems(new String[]{WorkspaceText.tr("Системный"),"Русский","English"},selected,(dialog,which)->{
                select(activity,codes[which]); apply(activity); dialog.dismiss(); changed.run();
            }).setNegativeButton(WorkspaceText.tr("Отмена"),null).show();
    }
}
