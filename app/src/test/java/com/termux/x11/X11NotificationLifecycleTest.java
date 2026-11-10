package com.termux.x11;
import android.app.*;import android.content.*;import android.os.*;
import java.lang.reflect.*;import java.time.Duration;import java.util.concurrent.ExecutorService;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class,shadows={X11NotificationLifecycleTest.Identity.class,X11NotificationLifecycleTest.Signals.class})
public class X11NotificationLifecycleTest {
 @Implements(X11Service.class) public static class Identity extends org.robolectric.shadows.ShadowService {static String name;@Implementation protected static String processName(){return name;}}
 @Implements(android.os.Process.class) public static class Signals {static int kills,lastPid;@Implementation protected static void killProcess(int pid){kills++;lastPid=pid;}}
 X11Service service;NotificationManager notifications;
 @Before public void setup(){Signals.kills=0;Signals.lastPid=-1;Identity.name="com.termux:jobs_x11_archlinux";service=Robolectric.buildService(X11Service.ArchLinux.class).get();ReflectionHelpers.setField(service,"ownProcess",true);notifications=(NotificationManager)service.getSystemService(Context.NOTIFICATION_SERVICE);}
 @After public void cleanup(){((ExecutorService)ReflectionHelpers.getField(service,"worker")).shutdownNow();((Handler)ReflectionHelpers.getField(service,"main")).removeCallbacksAndMessages(null);}
 void control(){service.onStartCommand(new Intent().putExtra("controlOnly",true),0,1);}
 void tick(){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1100));}
 Notification note(){return Shadows.shadowOf(notifications).getNotification(8086);}
 void phase(String value){ReflectionHelpers.setField(service,"phase",value);}
 @Test public void controlOnlyDoesNotClaimAnIdleDesktopForever(){service.onBind(new Intent());control();assertNotNull(note());assertTrue(java.util.Arrays.asList("Рабочий стол","Desktop").contains(note().extras.getCharSequence(Notification.EXTRA_TEXT).toString()));tick();assertNull(note());assertEquals(0,Signals.kills);assertFalse(service.snapshot().getBoolean("started"));}
 @Test public void completionAndErrorClearNotificationWhileClientCanReadOutcome(){for(String result:new String[]{"installed","missing","error","cancelled"}){phase("checking");service.onBind(new Intent());control();tick();assertNotNull(note());phase(result);tick();assertNull(result,note());assertEquals(0,Signals.kills);}}
 @Test public void runningDesktopSurvivesUiDetachAndRebind(){ReflectionHelpers.setField(service,"started",true);phase("running");service.onBind(new Intent());control();service.onUnbind(new Intent());tick();assertNotNull(note());assertEquals(0,Signals.kills);service.onBind(new Intent());tick();assertNotNull(note());}
 @Test public void orphanedIdleControlServiceIsReleasedWithoutKillingOtherProcesses(){control();tick();assertNull(note());assertEquals(1,Signals.kills);assertEquals(android.os.Process.myPid(),Signals.lastPid);service.onDestroy();assertEquals(1,Signals.kills);}
 @Test public void nullRestartIntentDoesNotStartNativeTestServer(){service.onStartCommand(null,0,1);assertFalse(service.snapshot().getBoolean("started"));assertNull(note());assertEquals(1,Signals.kills);}
 @Test public void explicitStopRemovesOnlyThisNotification(){notifications.notify(8085,new Notification.Builder(service).setContentTitle("Neighbour").setSmallIcon(android.R.drawable.ic_menu_view).build());control();service.requestStop();assertNull(note());assertNotNull(Shadows.shadowOf(notifications).getNotification(8085));assertEquals(1,Signals.kills);}
 @Test public void identityMismatchNeverSignalsAProcess(){control();Identity.name="com.termux";service.requestStop();assertEquals(0,Signals.kills);assertNotNull(note());}
 @Test public void programCatalogKeepsForegroundUntilItsOwnWorkFinishes(){service.onBind(new Intent());X11ProgramInstaller programs=new X11ProgramInstaller(service);ReflectionHelpers.setField(service,"programs",programs);Bundle busy=new Bundle();busy.putString("phase","cataloguing");ReflectionHelpers.setField(programs,"state",busy);control();tick();assertNotNull(note());busy=new Bundle();busy.putString("phase","done");ReflectionHelpers.setField(programs,"state",busy);tick();assertNull(note());assertEquals(0,Signals.kills);programs.close();}
 @Test public void errorReasonKeepsItsDeliveryIntervalThenRemovesNotification()throws Exception{ReflectionHelpers.setField(service,"started",true);phase("running");service.onBind(new Intent());control();Method m=X11Service.class.getDeclaredMethod("finishDesktop",String.class,String.class,String.class);m.setAccessible(true);m.invoke(service,"error","Ошибка","Error");tick();assertNotNull(note());assertEquals(0,Signals.kills);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(101));assertNull(note());assertEquals(1,Signals.kills);}

}
