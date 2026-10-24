package com.termux.x11;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE,sdk=28)
public class X11IntegrationTest {
    @Test public void styleCannotReachUnsupportedGuestsOrChangeDuringAnOwnedDesktopOperation()throws Exception{
        X11Service s=org.robolectric.Robolectric.buildService(X11Service.class).get();
        try{
            for(String target:new String[]{"jobs-x11-ubuntu","ubuntu-ai","jobs-ai-ubuntu","unknown","../debian"}){
                android.os.Bundle q=X11Remote.command("check");q.putString("target",target);q.putBoolean("termuxStyle",true);
                try{command(s,q);fail(target);}catch(IllegalArgumentException expected){}
                assertFalse(s.snapshot().getBoolean("termuxStyle"));
            }
            field(s,"target","termux");field(s,"desktopOwner","one:h1");
            for(String phase:new String[]{"checking","installing","preparing","running","stopping"}){
                field(s,"phase",phase);field(s,"started",phase.equals("running"));field(s,"stopping",phase.equals("stopping"));
                android.os.Bundle q=X11Remote.command("launch");q.putString("target","termux");q.putBoolean("termuxStyle",true);
                try{command(s,q);fail(phase);}catch(IllegalStateException expected){}
                assertFalse(s.snapshot().getBoolean("termuxStyle"));assertEquals("one:h1",s.snapshot().getString("desktopOwner"));
            }
        }finally{java.lang.reflect.Field worker=X11Service.class.getDeclaredField("worker");worker.setAccessible(true);((java.util.concurrent.ExecutorService)worker.get(s)).shutdown();}
    }

    @Test public void everyStyleUsesItsOwnPreparingServiceWithoutSpawningAnotherDesktop()throws Exception{
        String[] targets={"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"};
        Class<?>[] types={X11Service.class,X11Service.Ubuntu.class,X11Service.Debian.class,X11Service.Alpine.class,X11Service.Fedora.class,X11Service.OpenSuse.class,X11Service.VoidLinux.class,X11Service.ArchLinux.class};
        for(String target:new String[]{null,"","ubuntu-ai","jobs-ai-ubuntu","jobs-x11-ubuntu","unknown","../../ubuntu"})assertFalse(DesktopProcess.supportsStyle(target));
        for(int i=0;i<targets.length;i++){
            String target=targets[i];assertTrue(target,DesktopProcess.supportsStyle(target));
            X11Service s=(X11Service)org.robolectric.Robolectric.buildService((Class)types[i]).get();
            try{
                field(s,"ownProcess",true);field(s,"target",target);field(s,"phase","preparing");field(s,"termuxStyle",true);
                android.os.Bundle q=X11Remote.command("launch");q.putString("target",target);q.putBoolean("termuxStyle",true);q.putString("owner",target+"-owner");
                android.os.Bundle result=command(s,q);assertEquals(target,result.getString("target"));assertTrue(result.getBoolean("termuxStyle"));assertTrue(result.getStringArrayList("desktopOwners").contains(target+"-owner"));
                q.putBoolean("termuxStyle",false);try{command(s,q);fail("busy style changed: "+target);}catch(IllegalStateException expected){}
                assertTrue(s.snapshot().getBoolean("termuxStyle"));
                q.putString("target",target.equals("termux")?"ubuntu":"termux");q.putBoolean("termuxStyle",true);try{command(s,q);fail("wrong rootfs accepted: "+target);}catch(IllegalArgumentException expected){}
            }finally{java.lang.reflect.Field worker=X11Service.class.getDeclaredField("worker");worker.setAccessible(true);((java.util.concurrent.ExecutorService)worker.get(s)).shutdown();}
        }
    }

    @Test public void retiredStyleRequestsAreRejectedBeforeStateMutation()throws Exception{
        X11Service s=org.robolectric.Robolectric.buildService(X11Service.class).get();
        try{
            for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
                android.os.Bundle q=X11Remote.command("check");q.putString("target",target);q.putBoolean("touchWorkspace",true);
                try{command(s,q);fail(target);}catch(IllegalArgumentException expected){}
                assertEquals("",s.snapshot().getString("target"));assertFalse(s.snapshot().getBoolean("termuxStyle"));
            }
        }finally{java.lang.reflect.Field worker=X11Service.class.getDeclaredField("worker");worker.setAccessible(true);((java.util.concurrent.ExecutorService)worker.get(s)).shutdown();}
    }
    @Test public void virglAllowlistIncludesAllEightDesktopsButExcludesAgentsAndUnknownAliases(){
        for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"})assertTrue(target,VirglInstaller.supported(target));
        for(String target:new String[]{"ubuntu-ai","jobs-x11-ubuntu","../../ubuntu","debian;true",""})assertFalse(target,VirglInstaller.supported(target));assertFalse(VirglInstaller.supported(null));
    }
    @org.robolectric.annotation.Implements(X11Service.class)
    public static class StopService {
        static int calls;
        @org.robolectric.annotation.Implementation protected void requestStop(){calls++;}
    }
    @Test public void componentFailureSurvivesDesktopLogResetAndStaysSeparateFromDesktopExit()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        X11StartupLog components=new X11StartupLog();components.feed("ERROR: [trans] failed to download signature from https://user:secret@host/file: HTTP/1.1 404\nDESKTOP_CHILD_EXIT=19\n");
        X11InstallErrors errors=new X11InstallErrors();errors.feed("ERROR: [trans] failed to download signature from https://user:secret@host/file: Not Found\n");field(service,"componentErrors",errors);
        field(service,"componentLog",components);field(service,"componentOperation","install");field(service,"componentResult","failed");field(service,"componentGuardianExit",137);field(service,"startupLog",new X11StartupLog());field(service,"phase","error");
        android.os.Bundle b=service.snapshot();assertTrue(b.getString("componentErrors").contains("Not Found"));assertFalse(b.getString("componentErrors").contains("secret"));assertEquals("install",b.getString("componentOperation"));assertEquals("failed",b.getString("componentResult"));assertEquals(19,b.getInt("componentChildExit"));assertEquals(137,b.getInt("componentGuardianExit"));assertEquals(-1,b.getInt("desktopChildExit"));assertTrue(b.getString("componentDiagnostic").contains("HTTP_ERROR=404"));assertTrue(b.getString("componentDiagnostic").contains("PACKAGE_SIGNATURE_DOWNLOAD_FAILED"));assertFalse(b.getString("componentDiagnostic").contains("secret"));
        X11Service other=org.robolectric.Robolectric.buildService(X11Service.class).get();assertEquals("not-started",other.snapshot().getString("componentResult"));assertFalse(other.snapshot().getString("componentDiagnostic").contains("HTTP_ERROR"));
    }
    @Test public void fixedEnvironmentSlotsCannotAliasProcessesDisplaysOrPaths(){
        java.util.Set<String> processes=new java.util.HashSet<>(),paths=new java.util.HashSet<>();java.util.Set<Integer> displays=new java.util.HashSet<>(),ports=new java.util.HashSet<>();
        for(String t:new String[]{"termux","ubuntu","jobs-x11-ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
            X11Environment e=X11Environment.forTarget(t);assertTrue(processes.add(e.process));assertTrue(paths.add(e.directory));assertTrue(displays.add(e.display));assertTrue(ports.add(e.port));assertEquals(7900+e.display,e.port);
            assertTrue(e.owns("com.termux"+e.process,"com.termux"));assertFalse(e.owns("com.termux","com.termux"));assertFalse(e.owns("com.termux"+e.process+"-other","com.termux"));
        }
        assertEquals("workspace-x11",X11Environment.forTarget("termux").directory);
        try{X11Environment.forTarget("../ubuntu");fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void fedoraHasItsOwnServiceAndRejectsAnotherEnvironment()throws Exception{
        X11Service.Fedora s=org.robolectric.Robolectric.buildService(X11Service.Fedora.class).get();
        assertEquals("fedora",X11Environment.context(s).target);assertEquals(102,X11Environment.context(s).display);
        field(s,"ownProcess",true);android.os.Bundle q=X11Remote.command("state");q.putString("target","ubuntu");
        try{command(s,q);fail("cross-environment request accepted");}catch(IllegalArgumentException expected){}
        q.putString("target","fedora");assertFalse(command(s,q).getBoolean("started"));
    }
    @Test public void opensuseHasItsOwnServiceAndRejectsCrossEnvironmentRequests()throws Exception{
        X11Service.OpenSuse s=org.robolectric.Robolectric.buildService(X11Service.OpenSuse.class).get();
        assertEquals("opensuse",X11Environment.context(s).target);assertEquals(103,X11Environment.context(s).display);
        field(s,"ownProcess",true);android.os.Bundle q=X11Remote.command("state");q.putString("target","fedora");
        try{command(s,q);fail();}catch(IllegalArgumentException expected){}
        q.putString("target","opensuse");assertFalse(command(s,q).getBoolean("started"));
    }
    @Test public void voidHasPrivateServiceAndRejectsCrossEnvironmentCommands()throws Exception{
        X11Service.VoidLinux s=org.robolectric.Robolectric.buildService(X11Service.VoidLinux.class).get();
        assertEquals("void",X11Environment.context(s).target);assertEquals(104,X11Environment.context(s).display);
        field(s,"ownProcess",true);android.os.Bundle q=X11Remote.command("state");q.putString("target","opensuse");
        try{command(s,q);fail();}catch(IllegalArgumentException expected){}
        q.putString("target","void");assertFalse(command(s,q).getBoolean("started"));
    }
    @Test public void archHasPrivateServiceAndRejectsCrossEnvironmentCommands()throws Exception{
        X11Service.ArchLinux s=org.robolectric.Robolectric.buildService(X11Service.ArchLinux.class).get();
        assertEquals("archlinux",X11Environment.context(s).target);assertEquals(105,X11Environment.context(s).display);
        field(s,"ownProcess",true);android.os.Bundle q=X11Remote.command("state");q.putString("target","opensuse");
        try{command(s,q);fail();}catch(IllegalArgumentException expected){}
        q.putString("target","archlinux");assertFalse(command(s,q).getBoolean("started"));
    }
    @Test public void everyPrivateServiceRejectsAnotherRootfsBeforeWorkStarts()throws Exception{
        for(String target:new String[]{"termux","ubuntu","jobs-x11-ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
            X11Environment env=X11Environment.forTarget(target);
            X11Service service=org.robolectric.Robolectric.buildService(env.service).get();
            assertEquals(target,X11Environment.context(service).target);field(service,"ownProcess",true);
            android.os.Bundle q=X11Remote.command("state");q.putString("target",target.equals("termux")?"ubuntu":"termux");
            try{command(service,q);fail(target+" accepted cross-rootfs request");}catch(IllegalArgumentException expected){}
            q.putString("target",target);assertFalse(command(service,q).getBoolean("started"));
        }
    }
    @Test public void repeatedLaunchSharesOneDesktopAndOnlyLastOwnerClosureStopsIt()throws Exception{
        for(String phase:new String[]{"preparing","running"}){
            X11Service s=org.robolectric.Robolectric.buildService(X11Service.class).get();field(s,"target","alpine");field(s,"started",true);field(s,"componentsReady",true);field(s,"phase",phase);field(s,"desktopOwner","one:h1");
            java.lang.reflect.Field users=X11Service.class.getDeclaredField("desktopUsers");users.setAccessible(true);((java.util.Set<String>)users.get(s)).add("one:h1");
            android.os.Bundle q=X11Remote.command("launch");q.putString("target","alpine");q.putString("owner","two:h2");assertEquals(phase,command(s,q).getString("phase"));assertEquals(2,s.snapshot().getStringArrayList("desktopOwners").size());
            q=X11Remote.command("session-close");q.putString("target","alpine");q.putString("owner","one:stale");assertEquals(phase,command(s,q).getString("phase"));
            q.putString("owner","one:h1");assertEquals(phase,command(s,q).getString("phase"));assertEquals("two:h2",s.snapshot().getString("desktopOwner"));
            q.putString("target","debian");assertEquals(phase,command(s,q).getString("phase"));q.putString("target","alpine");q.putString("owner","two:h2");assertEquals("stopping",command(s,q).getString("phase"));
        }
    }
    @Test public void componentCancellationKeepsEveryTargetLockedUntilItsWorkerFinishes()throws Exception{
        for(String target:new String[]{"termux","ubuntu","debian","alpine","fedora","opensuse","void","archlinux"}){
            X11Service service=org.robolectric.Robolectric.buildService(X11Environment.forTarget(target).service).get();
            try{
                field(service,"ownProcess",true);field(service,"target",target);field(service,"phase","installing");field(service,"generation",10);
                android.os.Bundle cancel=X11Remote.command("cancel");cancel.putString("target",target);
                assertEquals(target,"cancelling",command(service,cancel).getString("phase"));assertEquals("cancelling",command(service,cancel).getString("phase"));
                for(String op:new String[]{"check","install","launch","programs-check","programs-install","programs-catalog"}){
                    android.os.Bundle q=X11Remote.command(op);q.putString("target",target);q.putString("program","editor");
                    try{command(service,q);fail("new work admitted before cleanup: "+target+":"+op);}catch(IllegalStateException expected){}
                }
                java.lang.reflect.Method finish=X11Service.class.getDeclaredMethod("finishComponentCancellation",int.class);finish.setAccessible(true);
                finish.invoke(service,9);assertEquals("cancelling",service.snapshot().getString("phase"));finish.invoke(service,10);assertEquals("cancelled",service.snapshot().getString("phase"));assertEquals("cancelled",service.snapshot().getString("componentResult"));
            }finally{java.lang.reflect.Field f=X11Service.class.getDeclaredField("worker");f.setAccessible(true);((java.util.concurrent.ExecutorService)f.get(service)).shutdownNow();}
        }
    }
    @Test public void ordinaryUbuntuOffersTheSameLauncherMaintenanceAsOtherDesktops(){
        assertTrue(DesktopPrograms.supportsMaintenance("ubuntu"));assertFalse(DesktopPrograms.supportsMaintenance("ubuntu-ai"));
    }
    @Test public void terminationGuardAcceptsOnlyTheDedicatedProcess(){
        assertTrue(X11Runtime.expectedProcess("com.termux:jobs_x11","com.termux"));
        for(String p:new String[]{"com.termux","com.termux:other","com.termux:jobs_x11-extra","other:jobs_x11","",null})assertFalse(X11Runtime.expectedProcess(p,"com.termux"));
    }
    @Test public void serviceRefusesMainProcessBeforeAnyNativeLoad(){
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        try{service.onCreate();fail("main process must be rejected");}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("private process"));}
    }
    private android.os.Bundle command(X11Service s,android.os.Bundle b)throws Exception{
        java.lang.reflect.Method method=X11Service.class.getDeclaredMethod("command",android.os.Bundle.class);method.setAccessible(true);
        try{return (android.os.Bundle)method.invoke(s,b);}catch(java.lang.reflect.InvocationTargetException e){throw (RuntimeException)e.getCause();}
    }
    @Test public void stateAndRejectedCommandsDoNotStartOrLoadTheServer()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        assertFalse(command(service,X11Remote.command("state")).getBoolean("ready"));
        for(String op:new String[]{"arbitrary-shell","attach","input"})try{command(service,X11Remote.command(op));fail(op);}catch(IllegalArgumentException|IllegalStateException expected){}
        android.os.Bundle b=command(service,X11Remote.command("state"));assertEquals("idle",b.getString("phase"));assertEquals("",b.getString("target"));
    }
    @Test public void unsupportedEnvironmentOrUncheckedLaunchCannotInstallComponents()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        android.os.Bundle q=X11Remote.command("install");q.putString("target","ubuntu-ai");
        try{command(service,q);fail();}catch(IllegalArgumentException expected){}
        q=X11Remote.command("launch");q.putString("target","jobs-x11-ubuntu");
        try{command(service,q);fail();}catch(IllegalStateException expected){}
        android.os.Bundle state=command(service,X11Remote.command("state"));assertEquals("idle",state.getString("phase"));assertFalse(state.getBoolean("components"));assertFalse(state.getBoolean("ready"));
    }
    private void field(X11Service s,String key,Object value)throws Exception{java.lang.reflect.Field f=X11Service.class.getDeclaredField(key);f.setAccessible(true);f.set(s,value);}
    @Test public void retiredDesktopChoiceIsRejectedAndMaintenanceRequiresStoppedTestUbuntu()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        for(String mode:new String[]{"lxqt","lxqt;true"}){
            android.os.Bundle q=X11Remote.command("install");q.putString("target","jobs-x11-ubuntu");q.putString("desktop",mode);
            try{command(service,q);fail("Retired desktop was accepted");}catch(IllegalArgumentException expected){}
        }
        for(String op:new String[]{"repair-launchers","firefox-compat-on","firefox-compat-off"}){
            android.os.Bundle q=X11Remote.command(op);q.putString("target","ubuntu-ai");
            try{command(service,q);fail("Unsupported maintenance accepted");}catch(IllegalArgumentException expected){}
            q.putString("target","jobs-x11-ubuntu");field(service,"started",true);
            try{command(service,q);fail("Maintenance accepted while desktop started");}catch(IllegalStateException expected){}
        }
    }
    @Test public void sessionClosureRequiresExactOwnerHandleAndEnvironment()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        field(service,"started",true);field(service,"phase","running");field(service,"target","jobs-x11-ubuntu");field(service,"desktopOwner","preset:handle-new");
        for(String[] wrong:new String[][]{{"","jobs-x11-ubuntu"},{"other:handle-new","jobs-x11-ubuntu"},{"preset:handle-old","jobs-x11-ubuntu"},{"preset:handle-new","ubuntu"}}){
            android.os.Bundle q=X11Remote.command("session-close");q.putString("owner",wrong[0]);q.putString("target",wrong[1]);assertEquals("running",command(service,q).getString("phase"));
        }
        android.os.Bundle q=X11Remote.command("session-close");q.putString("owner","preset:handle-new");q.putString("target","jobs-x11-ubuntu");assertEquals("stopping",command(service,q).getString("phase"));
        // No own-process permission in this fixture: even the matching close cannot kill the test VM.
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(151));
        assertEquals("stopping",command(service,q).getString("phase"));
    }
    @Test public void debianIsAnExactTargetAndCannotCrossTheRunningUbuntuDesktop()throws Exception{
        assertTrue(DesktopProcess.supported("debian"));assertTrue(DesktopPrograms.supportsMaintenance("debian"));
        for(String alias:new String[]{"debian-test","../debian","debian;true","ubuntu-ai"})assertFalse(DesktopProcess.supported(alias));
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        field(service,"started",true);field(service,"target","jobs-x11-ubuntu");field(service,"phase","running");
        for(String op:new String[]{"launch","check","install","programs-check","programs-firefox-compat-on","repair-launchers"}){
            android.os.Bundle q=X11Remote.command(op);q.putString("target","debian");
            try{command(service,q);fail("Cross-environment command accepted: "+op);}catch(IllegalStateException expected){}
            assertEquals("jobs-x11-ubuntu",service.snapshot().getString("target"));
        }
    }
    @Test public void alpineAndTermuxAreExactTargetsAndCannotTakeOverRunningDebian()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();field(service,"started",true);field(service,"target","debian");field(service,"phase","running");
        for(String target:new String[]{"alpine","termux"}){
            assertTrue(DesktopPrograms.supportsTarget(target));assertTrue(DesktopPrograms.supportsMaintenance(target));
            for(String op:new String[]{"check","install","launch","programs-check","programs-firefox-compat-on"}){
                android.os.Bundle q=X11Remote.command(op);q.putString("target",target);
                try{command(service,q);fail();}catch(IllegalStateException expected){}
                assertEquals("debian",service.snapshot().getString("target"));
            }
        }
    }
    @Test public void retiredObserverCommandsCannotStartHiddenRecording()throws Exception{
        X11Service s=org.robolectric.Robolectric.buildService(X11Service.class).get();
        for(String op:new String[]{"observe-start","observe-stop"}){
            try{command(s,X11Remote.command(op));fail();}catch(IllegalArgumentException expected){}
            assertFalse(s.snapshot().getBoolean("observationReady"));assertFalse(s.snapshot().getBoolean("observationActive"));
        }
    }
    @Test public void invalidOwnerCannotStartDesktop()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();field(service,"componentsReady",true);field(service,"target","jobs-x11-ubuntu");
        android.os.Bundle q=X11Remote.command("launch");q.putString("target","jobs-x11-ubuntu");q.putString("owner","bad/owner");
        try{command(service,q);fail();}catch(IllegalArgumentException expected){}
        assertFalse(service.snapshot().getBoolean("started"));
    }
    @Test public void stopIsScopedAndLocksNewWorkForEverySupportedEnvironment()throws Exception{
        for(String target:new String[]{"ubuntu","jobs-x11-ubuntu","debian","alpine","termux","fedora","opensuse","void","archlinux"}){
            X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
            field(service,"target",target);field(service,"started",true);field(service,"ready",true);field(service,"phase","running");
            android.os.Bundle wrong=X11Remote.command("stop");wrong.putString("target","another-environment");
            try{command(service,wrong);fail();}catch(IllegalStateException expected){}
            assertEquals("running",service.snapshot().getString("phase"));assertTrue(service.snapshot().getBoolean("started"));
            android.os.Bundle stop=X11Remote.command("stop");stop.putString("target",target);
            android.os.Bundle state=command(service,stop);assertEquals("stopping",state.getString("phase"));assertFalse(state.getBoolean("started"));assertFalse(state.getBoolean("ready"));
            for(String op:new String[]{"launch","check","install","programs-check","cancel"}){
                android.os.Bundle q=X11Remote.command(op);q.putString("target",target);
                try{command(service,q);fail(op);}catch(IllegalStateException expected){}
            }
            assertEquals("stopping",command(service,stop).getString("phase"));
        }
    }
    @Test public void cancellingPreparationLocksTheDisplayAndLateReadyCannotLaunchIt()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        field(service,"started",true);field(service,"target","termux");field(service,"phase","preparing");
        assertEquals("stopping",command(service,X11Remote.command("cancel")).getString("phase"));
        java.lang.reflect.Method ready=X11Service.class.getDeclaredMethod("serverReady");ready.setAccessible(true);ready.invoke(service);
        assertFalse(service.snapshot().getBoolean("ready"));assertFalse(service.snapshot().getBoolean("started"));
        android.os.Bundle q=X11Remote.command("launch");q.putString("target","alpine");
        try{command(service,q);fail();}catch(IllegalStateException expected){}
        assertEquals("termux",service.snapshot().getString("target"));
    }
    @Test @Config(shadows=StopService.class) public void finishedDesktopClearsReadinessAndSchedulesPrivateServiceStop()throws Exception{
        for(String target:new String[]{"ubuntu","jobs-x11-ubuntu","debian","alpine","termux","fedora","opensuse","void","archlinux"}){
            X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
            StopService.calls=0;
            field(service,"target",target);field(service,"started",true);field(service,"ready",true);field(service,"desktopReady",true);field(service,"textFocus",true);field(service,"phase","running");
            field(service,"detail","tail without the earlier child marker");java.lang.reflect.Field logField=X11Service.class.getDeclaredField("startupLog");logField.setAccessible(true);((X11StartupLog)logField.get(service)).feed("DESKTOP_CHILD_EXIT=7\n");
            java.lang.reflect.Method end=X11Service.class.getDeclaredMethod("desktopEnded",int.class);end.setAccessible(true);end.invoke(service,137);
            android.os.Bundle state=service.snapshot();assertEquals("ended",state.getString("phase"));assertEquals(7,state.getInt("desktopChildExit"));assertEquals(137,state.getInt("desktopGuardianExit"));
            assertFalse(state.getBoolean("started"));assertFalse(state.getBoolean("ready"));assertFalse(state.getBoolean("desktopReady"));assertFalse(state.getBoolean("textFocus"));
            assertTrue(state.getString("status").contains("(7)"));assertFalse(state.getString("status").contains("(137)"));
            android.os.Bundle q=X11Remote.command("check");q.putString("target",target.equals("termux")?"alpine":"termux");
            try{command(service,q);fail("retiring native display was reused");}catch(IllegalStateException expected){}
            // Duplicate completion and stale readiness callback cannot restart or extend cleanup.
            end.invoke(service,9);java.lang.reflect.Method ready=X11Service.class.getDeclaredMethod("serverReady");ready.setAccessible(true);ready.invoke(service);
            assertEquals("ended",service.snapshot().getString("phase"));
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1201));
            assertEquals(1,StopService.calls);
        }
    }
    @Test @Config(shadows=StopService.class) public void startupFailureAlsoReleasesDisplayWithoutWaitingForAVisibleOwner()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        StopService.calls=0;field(service,"started",true);field(service,"phase","preparing");
        java.lang.reflect.Method finish=X11Service.class.getDeclaredMethod("finishDesktop",String.class,String.class,String.class);finish.setAccessible(true);finish.invoke(service,"error","Ошибка запуска","Startup failure");
        assertEquals("error",service.snapshot().getString("phase"));assertFalse(service.snapshot().getBoolean("started"));
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1201));
        assertEquals(1,StopService.calls);
    }
    @Test public void oldFocusOutCannotHideAnotherTextContext()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        java.lang.reflect.Method focus=X11Service.class.getDeclaredMethod("focusEvent",String.class,boolean.class);focus.setAccessible(true);
        focus.invoke(service,"a",true);focus.invoke(service,"a",false);focus.invoke(service,"b",true);
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(151));
        assertTrue(command(service,X11Remote.command("state")).getBoolean("textFocus"));
        focus.invoke(service,"a",false);assertTrue(command(service,X11Remote.command("state")).getBoolean("textFocus"));
        focus.invoke(service,"b",false);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(151));
        assertFalse(command(service,X11Remote.command("state")).getBoolean("textFocus"));
    }
    @Test public void pointerRetapRequiresStillFocusedTextFieldAfterSettling()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        java.lang.reflect.Field ready=X11Service.class.getDeclaredField("ready");ready.setAccessible(true);ready.setBoolean(service,true);
        java.lang.reflect.Method focus=X11Service.class.getDeclaredMethod("focusEvent",String.class,boolean.class);focus.setAccessible(true);
        service.pointerTapped();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(201));assertEquals(0,command(service,X11Remote.command("state")).getLong("fieldTapSerial"));
        focus.invoke(service,"a",true);service.pointerTapped();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(201));assertEquals(1,command(service,X11Remote.command("state")).getLong("fieldTapSerial"));
        service.pointerTapped();focus.invoke(service,"a",false);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(201));assertEquals(1,command(service,X11Remote.command("state")).getLong("fieldTapSerial"));
    }
    @Test public void noncontiguousResourceMaskUsesOnlyServerAllowedBits()throws Exception{
        byte[] setup=setup();ByteBuffer.wrap(setup).order(ByteOrder.LITTLE_ENDIAN).putInt(16,0x55);
        ByteArrayOutputStream out=new ByteArrayOutputStream();X11Protocol client=new X11Protocol(new ByteArrayInputStream(setup),out);client.setup(new byte[16]);client.createWindow();
        ByteBuffer b=ByteBuffer.wrap(out.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);assertEquals(0x400004,b.getInt(92));
    }
    @Test public void keyboardPathsCannotEscapePrivateExtraction()throws Exception{
        File base=new File("/private/keyboard");assertEquals(new File(base,"symbols/us"),X11Runtime.target(base,"symbols/us"));
        for(String name:new String[]{"../secret","symbols/../../outside","/sdcard/file","symbols\\file","."})try{X11Runtime.target(base,name);fail(name);}catch(IOException expected){}
    }
    @Test public void handshakeHasExactWirePaddingAndCookieSize(){
        byte[] cookie=new byte[16];for(int n=0;n<16;n++)cookie[n]=(byte)(n+1);
        byte[] wire=X11Protocol.handshake(cookie);assertEquals(48,wire.length);assertEquals('l',wire[0]);
        ByteBuffer b=ByteBuffer.wrap(wire).order(ByteOrder.LITTLE_ENDIAN);assertEquals(11,b.getShort(2));assertEquals(18,b.getShort(6));assertEquals(16,b.getShort(8));
        assertArrayEquals(cookie,java.util.Arrays.copyOfRange(wire,32,48));assertEquals(0,wire[30]);assertEquals(0,wire[31]);
        try{X11Protocol.handshake(new byte[15]);fail();}catch(IllegalArgumentException expected){}
    }
    private byte[] setup(){
        ByteBuffer b=ByteBuffer.allocate(80).order(ByteOrder.LITTLE_ENDIAN);b.put(0,(byte)1);b.putShort(6,(short)18);
        b.putInt(8+4,0x400000);b.putInt(8+8,0x1fffff);b.put(8+20,(byte)1);b.putInt(8+32,0x100);b.putInt(8+32+32,0x21);b.put(8+32+38,(byte)24);return b.array();
    }
    @Test public void realProtocolWindowUsesReturnedRootVisualAndAllocatedIds()throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();X11Protocol client=new X11Protocol(new ByteArrayInputStream(setup()),out);client.setup(new byte[16]);client.createWindow();
        byte[] wire=out.toByteArray();ByteBuffer b=ByteBuffer.wrap(wire).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(1,wire[48]);assertEquals(24,wire[49]);assertEquals(10,b.getShort(50));assertEquals(0x400001,b.getInt(52));assertEquals(0x100,b.getInt(56));assertEquals(0x21,b.getInt(72));
        assertEquals(55,wire[88]);assertEquals(8,wire[108]);assertEquals(42,wire[116]);
    }
    @Test public void refusedAuthAndMalformedSetupFailWithoutUsingServerContent()throws Exception{
        for(byte[] reply:new byte[][]{new byte[8],new byte[]{1,0,11,0,0,0,0,0}}){try{new X11Protocol(new ByteArrayInputStream(reply),new ByteArrayOutputStream()).setup(new byte[16]);fail();}catch(IOException expected){assertFalse(expected.getMessage().contains("cookie"));}}
    }
    @Test public void genericEventLengthIsBoundedBeforeAllocation()throws Exception{
        byte[] event=new byte[32];event[0]=35;ByteBuffer.wrap(event).order(ByteOrder.LITTLE_ENDIAN).putInt(4,Integer.MAX_VALUE);
        try{new X11Protocol(new ByteArrayInputStream(event),new ByteArrayOutputStream()).event();fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("too large"));}
    }
    @Test public void keyboardEventIsMetadataOnly()throws Exception{
        byte[] event=new byte[32];event[0]=2;event[1]=38;assertEquals("key",new X11Protocol(new ByteArrayInputStream(event),new ByteArrayOutputStream()).event());
    }
    @Test public void programCommandsCannotCrossAnActiveDesktopOrAcceptArbitraryPackages()throws Exception{
        X11Service service=org.robolectric.Robolectric.buildService(X11Service.class).get();
        android.os.Bundle q=X11Remote.command("programs-install");q.putString("target","jobs-x11-ubuntu");q.putString("program","mousepad; touch /tmp/no");
        try{command(service,q);fail();}catch(IllegalArgumentException expected){}
        java.lang.reflect.Field started=X11Service.class.getDeclaredField("started");started.setAccessible(true);started.setBoolean(service,true);
        java.lang.reflect.Field target=X11Service.class.getDeclaredField("target");target.setAccessible(true);target.set(service,"ubuntu");
        q.putString("program","editor");try{command(service,q);fail();}catch(IllegalStateException expected){}
        android.os.Bundle state=command(service,X11Remote.command("state"));assertTrue(state.getBoolean("started"));assertFalse(state.getBoolean("ready"));assertEquals("ubuntu",state.getString("target"));
    }

}
