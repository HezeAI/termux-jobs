package com.termux.app.fragments.settings;

import android.app.Application;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.*;
import android.widget.FrameLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.*;
import com.termux.shared.interact.MessageDialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowToast;
import static org.junit.Assert.*;

/** Real preference widgets/backends with private package contexts; no installed addon or session. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class,shadows=PreferenceLifecycleTest.Errors.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class PreferenceLifecycleTest {
    private static final String BASE="com.termux.app.fragments.settings.";
    private static final String[] STORES={
        "TermuxPreferencesDataStore","TermuxAPIPreferencesDataStore","TermuxFloatPreferencesDataStore",
        "TermuxTaskerPreferencesDataStore","TermuxWidgetPreferencesDataStore","termux.TerminalIOPreferencesDataStore",
        "termux.TerminalViewPreferencesDataStore","termux.DebuggingPreferencesDataStore","termux_api.DebuggingPreferencesDataStore",
        "termux_float.DebuggingPreferencesDataStore","termux_tasker.DebuggingPreferencesDataStore","termux_widget.DebuggingPreferencesDataStore"
    };
    public static class Screen extends FragmentActivity {
        static Locale nextLocale=Locale.ENGLISH;
        final Set<String> missing=new HashSet<>();
        final List<String> requests=new ArrayList<>();
        @Override protected void attachBaseContext(Context base){Configuration c=new Configuration(base.getResources().getConfiguration());c.setLocale(nextLocale);super.attachBaseContext(base.createConfigurationContext(c));}
        @Override protected void onCreate(Bundle state){setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_DarkActionBar);super.onCreate(state);setContentView(new FrameLayout(this));}
        @Override public Context createPackageContext(String name,int flags)throws PackageManager.NameNotFoundException{
            requests.add(name+":"+flags);
            if(missing.contains(name))throw new PackageManager.NameNotFoundException("fixture-missing");
            // Preserve package separation/locale while routing all writes to Robolectric storage.
            Context configured=RuntimeEnvironment.application.createConfigurationContext(new Configuration(getResources().getConfiguration()));
            return new ContextWrapper(configured){
                @Override public String getPackageName(){return name;}
                @Override public SharedPreferences getSharedPreferences(String file,int mode){return RuntimeEnvironment.application.getSharedPreferences("settings-fixture-"+name+"-"+file,mode);}
            };
        }
    }
    @Implements(value=MessageDialogUtils.class,isInAndroidSdk=false)
    public static class Errors {
        static Context owner;static int count;
        @Implementation public static void exitAppWithErrorMessage(Context context,String title,String message){owner=context;count++;}
    }
    private final List<ActivityController<Screen>> screens=new ArrayList<>();
    private int oldLevel;
    private Screen screen(Locale locale){Screen.nextLocale=locale;ActivityController<Screen> c=Robolectric.buildActivity(Screen.class).setup();screens.add(c);return c.get();}
    private void close(Screen screen){ActivityController<Screen> c=screens.stream().filter(x->x.get()==screen).findFirst().get();c.pause().stop().destroy();screens.remove(c);idle();}
    private void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
    private Class<?> type(String name)throws Exception{return Class.forName(BASE+name);}
    private PreferenceDataStore store(String name,Context context)throws Exception{
        Class<?> type=type(name);Method method;
        try{method=type.getDeclaredMethod("create",Context.class);}catch(NoSuchMethodException legacy){method=type.getDeclaredMethod("getInstance",Context.class);}
        method.setAccessible(true);return (PreferenceDataStore)method.invoke(null,context);
    }
    private Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private void legacyReset()throws Exception{
        for(String name:STORES)for(Field field:type(name).getDeclaredFields())if(Modifier.isStatic(field.getModifiers())&&PreferenceDataStore.class.isAssignableFrom(field.getType())){field.setAccessible(true);field.set(null,null);}
    }
    @Before public void start()throws Exception{legacyReset();oldLevel=Logger.getLogLevel();Errors.owner=null;Errors.count=0;}
    @After public void finish()throws Exception{for(ActivityController<Screen> c:new ArrayList<>(screens))close(c.get());legacyReset();Logger.setLogLevel(null,oldLevel);Errors.owner=null;idle();}

    @Test public void reopeningAfterDestroyNeverReusesTheClosedScreensAdapter()throws Exception{
        Screen first=screen(Locale.ENGLISH);List<PreferenceDataStore> previous=new ArrayList<>();for(String name:STORES)previous.add(store(name,first));close(first);
        Screen second=screen(new Locale("ru"));List<String> reused=new ArrayList<>();
        for(int i=0;i<STORES.length;i++){PreferenceDataStore next=store(STORES[i],second);if(next==previous.get(i))reused.add(STORES[i]);}
        assertTrue("Closed-screen adapters reused: "+reused,reused.isEmpty());
    }
    @Test public void noSettingsClassKeepsAStaticAdapterOrContextRoot()throws Exception{
        List<String> roots=new ArrayList<>();for(String name:STORES)for(Field field:type(name).getDeclaredFields())
            if(Modifier.isStatic(field.getModifiers())&&(PreferenceDataStore.class.isAssignableFrom(field.getType())||Context.class.isAssignableFrom(field.getType())))roots.add(name+"."+field.getName());
        assertTrue("Static screen roots: "+roots,roots.isEmpty());
    }
    @Test public void keyboardSwitchPersistsThroughRealFragmentAndScreenRecreation()throws Exception{
        Screen first=screen(Locale.ENGLISH);PreferenceFragmentCompat fragment=new com.termux.app.fragments.settings.termux.TerminalIOPreferencesFragment();
        first.getSupportFragmentManager().beginTransaction().replace(android.R.id.content,fragment).commitNow();idle();
        SwitchPreferenceCompat toggle=fragment.findPreference("soft_keyboard_enabled");assertNotNull(toggle);boolean desired=!toggle.isChecked();toggle.setChecked(desired);idle();
        PreferenceDataStore old=fragment.getPreferenceManager().getPreferenceDataStore();close(first);
        Screen second=screen(Locale.ENGLISH);PreferenceFragmentCompat restored=new com.termux.app.fragments.settings.termux.TerminalIOPreferencesFragment();second.getSupportFragmentManager().beginTransaction().replace(android.R.id.content,restored).commitNow();idle();
        assertNotSame(old,restored.getPreferenceManager().getPreferenceDataStore());assertEquals(desired,((SwitchPreferenceCompat)restored.findPreference("soft_keyboard_enabled")).isChecked());
    }
    @Test public void separatelyOwnedScreensShareOnlyTheExistingPreferenceValues()throws Exception{
        Screen first=screen(Locale.ENGLISH),second=screen(Locale.ENGLISH);PreferenceDataStore a=store("termux.TerminalViewPreferencesDataStore",first),b=store("termux.TerminalViewPreferencesDataStore",second);
        assertNotSame(a,b);boolean desired=!a.getBoolean("terminal_margin_adjustment",false);a.putBoolean("terminal_margin_adjustment",desired);assertEquals(desired,b.getBoolean("terminal_margin_adjustment",false));b.putBoolean("terminal_margin_adjustment",!desired);assertEquals(!desired,a.getBoolean("terminal_margin_adjustment",false));
    }
    @Test public void logLevelToastUsesCurrentScreensLanguageAfterReopen()throws Exception{
        for(String name:Arrays.copyOfRange(STORES,7,STORES.length)){
            legacyReset();Screen first=screen(Locale.ENGLISH);store(name,first);close(first);Screen current=screen(new Locale("ru"));PreferenceDataStore adapter=store(name,current);adapter.putString("log_level",String.valueOf(Logger.LOG_LEVEL_VERBOSE));idle();
            assertEquals(name,current.getString(com.termux.shared.R.string.log_level_value,Logger.getLogLevelLabel(current,Logger.LOG_LEVEL_VERBOSE,false)),ShadowToast.getTextOfLatestToast());
            assertEquals(name,String.valueOf(Logger.LOG_LEVEL_VERBOSE),adapter.getString("log_level",null));close(current);
        }
    }
    @Test public void missingAddonErrorUsesLiveScreenAndCanRecoverOnNextOpen()throws Exception{
        String[] packages={TermuxConstants.TERMUX_API_PACKAGE_NAME,TermuxConstants.TERMUX_FLOAT_PACKAGE_NAME,TermuxConstants.TERMUX_TASKER_PACKAGE_NAME,TermuxConstants.TERMUX_WIDGET_PACKAGE_NAME};
        for(int i=0;i<packages.length;i++){
            legacyReset();Errors.count=0;Screen first=screen(Locale.ENGLISH);first.missing.add(packages[i]);PreferenceDataStore failed=store(STORES[8+i],first);assertSame(first,Errors.owner);assertEquals(1,Errors.count);assertNull(failed.getString("log_level",null));close(first);Errors.owner=null;
            Screen second=screen(Locale.ENGLISH);PreferenceDataStore ready=store(STORES[8+i],second);assertNotNull(ready.getString("log_level",null));assertEquals(1,Errors.count);assertNotSame(failed,ready);close(second);
        }
    }
    @Test public void packageRoutingAndUnknownKeysKeepTheirExistingBoundaries()throws Exception{
        Screen screen=screen(Locale.ENGLISH);String[] packages={"com.termux","com.termux.api","com.termux.window","com.termux.tasker","com.termux.widget","com.termux","com.termux","com.termux","com.termux.api","com.termux.window","com.termux.tasker","com.termux.widget"};
        for(int i=0;i<STORES.length;i++){
            PreferenceDataStore adapter=store(STORES[i],screen);assertEquals(packages[i]+":"+Context.CONTEXT_RESTRICTED,screen.requests.get(screen.requests.size()-1));assertNotNull(field(adapter,"mPreferences"));
            if(i==5||i==6||i==7){String key=i==5?"soft_keyboard_enabled":i==6?"terminal_margin_adjustment":"terminal_view_key_logging_enabled";boolean before=adapter.getBoolean(key,false);adapter.putBoolean("unknown-fixture-key",true);assertEquals(before,adapter.getBoolean(key,false));}
            if(i>=7){String before=adapter.getString("log_level",null);adapter.putString("unknown-fixture-key","value");assertEquals(before,adapter.getString("log_level",null));}
        }
        assertEquals(0,Errors.count);
    }
}
