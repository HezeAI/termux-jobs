package com.termux.app.workspace;

import android.app.Application;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class WorkspaceEnvironmentsTest {
    @Test public void detectsConcreteAliasesFromBothLayoutsAndKeepsCatalogAbsentEntries()throws Exception{
        File base=Files.createTempDirectory("environments").toFile(),home=new File(base,"home"),proot=new File(base,"proot");home.mkdirs();new File(proot,"containers/debian/rootfs/root").mkdirs();new File(proot,"installed-rootfs/ubuntu/root").mkdirs();new File(proot,"containers/ubuntu-work/rootfs/root").mkdirs();
        List<WorkspaceEnvironments.Choice> choices=WorkspaceEnvironments.choices(home,proot);assertFalse(choices.stream().anyMatch(c->c.alias.equals("jobs-x11-ubuntu")));assertEquals("termux",choices.get(0).environment);assertTrue(choices.stream().anyMatch(c->c.environment.equals("proot:debian")&&c.installed));assertTrue(choices.stream().anyMatch(c->c.environment.equals("ubuntu")&&c.installed));assertTrue(choices.stream().anyMatch(c->c.alias.equals("ubuntu-work")&&c.installed));assertTrue(choices.stream().anyMatch(c->c.alias.equals("alpine")&&!c.installed));assertNull(WorkspaceEnvironments.root(proot,"../escape"));
        File outside=new File(base,"outside");outside.mkdirs();Files.createSymbolicLink(new File(proot,"containers/evil").toPath(),outside.toPath());assertNull(WorkspaceEnvironments.root(proot,"evil"));
    }
    @Test public void sessionFilesAndLaunchStayInSelectedAliasAndExistingUbuntuKeepsItsRoute()throws Exception{
        File files=Files.createTempDirectory("environments-shell").toFile();WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="termux";s.prootAlias="alpine";s.directory="/root/project";s.startup="printf a";
        String shell=new String(Files.readAllBytes(SessionShell.prepare(files,s).toPath()),StandardCharsets.UTF_8);assertTrue(shell.contains("'alpine' -- /bin/sh"));assertFalse(shell.contains(" ubuntu --"));assertEquals("proot:alpine",ProjectFiles.environment(s));assertEquals("Alpine",ProjectFiles.environmentLabel(s));assertTrue(ProjectFiles.command(s,"pwd").contains("'alpine' -- /bin/sh"));assertEquals("alpine",s.copy().prootAlias);
        s.prootIsolated=true;assertTrue(ProjectFiles.command(s,"pwd").contains("login --isolated 'alpine'"));String isolated=new String(Files.readAllBytes(SessionShell.prepare(files,s).toPath()),StandardCharsets.UTF_8);assertTrue(isolated.contains("proot-distro login --isolated --bind"));s.prootIsolated=false;
        s.prootAlias="";s.environment="ubuntu";shell=new String(Files.readAllBytes(SessionShell.prepare(files,s).toPath()),StandardCharsets.UTF_8);assertTrue(shell.contains(" ubuntu -- /bin/bash --rcfile"));
    }
    @Test public void storeRoundTripKeepsAliasWhileOlderReadersSeeOnlySafeStartupGuard()throws Exception{
        WorkspaceStore store=new WorkspaceStore(RuntimeEnvironment.application,m->{});store.sessions.clear();WorkspaceStore.Session s=new WorkspaceStore.Session();s.name="Alpine";s.environment="termux";s.prootAlias="alpine";s.prootIsolated=true;s.startup="touch /root/inside-only";s.startup2="pwd";store.sessions.add(s);
        try{JSONObject data=store.encode(),row=data.getJSONArray("sessions").getJSONObject(0);assertEquals("termux",row.getString("environment"));assertEquals(WorkspaceEnvironments.OLD_READER_GUARD,row.getString("startup"));assertEquals("",row.getString("startup2"));assertEquals("touch /root/inside-only",row.getJSONArray("prootStartup").getString(0));
            store.decode(data.toString());assertEquals("alpine",store.sessions.get(0).prootAlias);assertTrue(store.sessions.get(0).prootIsolated);assertTrue(store.sessions.get(0).copy().prootIsolated);assertEquals("touch /root/inside-only",store.sessions.get(0).startup);
            row.put("prootAlias","../escape");try{store.decode(data.toString());fail("invalid alias accepted");}catch(JSONException expected){}
        }finally{store.close();}
    }
    @Test public void nativeCheckReadinessDoesNotClaimMissingComponentsOrNonzeroExitReady(){assertTrue(new EnvironmentSetup.Result("READY:environment\nREADY:shell\nCHECK:done",0).ready());assertFalse(new EnvironmentSetup.Result("READY:environment\nMISSING:shell\nCHECK:done",0).ready());assertFalse(new EnvironmentSetup.Result("READY:environment\nREADY:shell\nCHECK:done",1).ready());}

    @Test public void portableStartupRunsOnceStopsOnFailureAndHonorsCancellation()throws Exception{
        File base=Files.createTempDirectory("portable-startup").toFile(),cancel=new File(base,"cancel"),rc=new File(base,"rc"),result=new File(base,"result");
        List<String> commands=Arrays.asList("printf first >> "+SessionShell.quote(result.getPath()),"false","printf WRONG >> "+SessionShell.quote(result.getPath()));
        Files.write(rc.toPath(),SessionShell.portableRc(new File(base,"history").getPath(),cancel.getPath(),commands).getBytes(StandardCharsets.UTF_8));
        Process process=new ProcessBuilder("/bin/sh","-c",". "+SessionShell.quote(rc.getPath())+"; . "+SessionShell.quote(rc.getPath())).redirectErrorStream(true).start();
        while(process.getInputStream().read()!=-1){}assertEquals(0,process.waitFor());assertEquals("first",new String(Files.readAllBytes(result.toPath()),StandardCharsets.UTF_8));
        Files.delete(result.toPath());Files.createFile(cancel.toPath());process=new ProcessBuilder("/bin/sh","-c",". "+SessionShell.quote(rc.getPath())).start();assertEquals(0,process.waitFor());assertFalse(result.exists());
    }
    @Test public void selectedAliasesCannotOverrideAgentOrSshExecution()throws Exception{
        File files=Files.createTempDirectory("invalid-environment-binding").toFile();
        for(String env:Arrays.asList("codex","ssh","ubuntu")){WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment=env;s.prootAlias="alpine";try{SessionShell.prepare(files,s);fail("mixed binding accepted");}catch(java.io.IOException expected){}}
        WorkspaceStore.Session s=new WorkspaceStore.Session();s.environment="termux";s.prootAlias="alpine";s.geminiAgent=true;try{SessionShell.prepare(files,s);fail("agent binding accepted");}catch(java.io.IOException expected){}
    }

    @Test public void ociWithMissingRootBashUsesRunOnlyForAnEmptyEntrypoint()throws Exception{
        File base=Files.createTempDirectory("void-shell").toFile(),proot=new File(base,"proot"),container=new File(proot,"containers/void"),fs=new File(container,"rootfs");new File(fs,"bin").mkdirs();new File(fs,"etc").mkdirs();Files.write(new File(fs,"bin/sh").toPath(),new byte[]{1});File passwd=new File(fs,"etc/passwd"),manifest=new File(container,"manifest.json");Files.write(passwd.toPath(),"root:x:0:0:root:/root:/bin/bash\n".getBytes(StandardCharsets.UTF_8));
        JSONObject config=new JSONObject().put("Entrypoint",JSONObject.NULL).put("Cmd",new JSONArray().put("/bin/sh"));JSONObject data=new JSONObject().put("image_config",new JSONObject().put("config",config));Files.write(manifest.toPath(),data.toString().getBytes(StandardCharsets.UTF_8));
        assertEquals("run",WorkspaceEnvironments.verb(proot,"void"));
        config.put("Entrypoint",new JSONArray().put("/entrypoint"));Files.write(manifest.toPath(),data.toString().getBytes(StandardCharsets.UTF_8));assertEquals("login",WorkspaceEnvironments.verb(proot,"void"));
        config.put("Entrypoint",JSONObject.NULL);Files.write(manifest.toPath(),data.toString().getBytes(StandardCharsets.UTF_8));Files.write(new File(fs,"bin/bash").toPath(),new byte[]{1});assertEquals("login",WorkspaceEnvironments.verb(proot,"void"));
        Files.delete(new File(fs,"bin/bash").toPath());Files.write(passwd.toPath(),"root:x:0:0:root:/root:/bin/sh\n".getBytes(StandardCharsets.UTF_8));assertEquals("login",WorkspaceEnvironments.verb(proot,"void"));
        Files.write(passwd.toPath(),"root:x:0:0:root:/root:/bin/bash\n".getBytes(StandardCharsets.UTF_8));Files.delete(manifest.toPath());assertEquals("login",WorkspaceEnvironments.verb(proot,"void"));
    }
}
