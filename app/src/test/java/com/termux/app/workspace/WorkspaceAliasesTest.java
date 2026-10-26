package com.termux.app.workspace;

import android.content.Context;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.File;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,manifest=Config.NONE)
public class WorkspaceAliasesTest {
    @Test public void oldLibraryAndAllEightEnvironmentAliasesRoundTripWithoutMixingCommands()throws Exception{
        Context c=RuntimeEnvironment.application;File file=new File(c.getFilesDir(),"workspace-v1.json");file.delete();new File(file+".bak").delete();WorkspaceStore s=new WorkspaceStore(c,e->fail(e));
        try{WorkspaceStore.Command command=new WorkspaceStore.Command();command.name="keep";command.command="pwd";command.favorite=true;s.commands.add(command);JSONObject old=s.encode();old.remove("aliases");s.decode(old.toString());assertTrue(s.aliases.isEmpty());
            for(String env:WorkspaceAliases.ENVIRONMENTS){WorkspaceStore.Alias a=new WorkspaceStore.Alias();a.environment=env;a.name="ll";a.command="ls -lah\nprintf '%s' 'hello'";a.category="Files";a.favorite=true;s.aliases.add(a);}
            s.save();s.awaitWrites();WorkspaceStore reopened=new WorkspaceStore(c,e->fail(e));try{assertEquals(8,reopened.aliases.size());assertTrue(reopened.aliases.get(0).favorite);assertEquals("Files",reopened.aliases.get(0).category);assertTrue(reopened.commands.get(0).favorite);assertEquals("pwd",reopened.commands.get(0).command);}finally{reopened.close();}
        }finally{s.close();}
    }
    @Test public void installedSelectorAndTransportRejectAiMissingAndOtherEnvironments(){
        List<WorkspaceEnvironments.Choice> all=new ArrayList<>();for(String env:WorkspaceAliases.ENVIRONMENTS)all.add(new WorkspaceEnvironments.Choice("termux",env.equals("termux")?"":env,env,"/root",true));
        all.add(new WorkspaceEnvironments.Choice("termux","ubuntu-ai","Ubuntu AI","/root",true));all.add(new WorkspaceEnvironments.Choice("termux","jobs-abcd","Imported","/root",true));
        assertEquals(8,WorkspaceAliases.installed(all).size());all.set(1,new WorkspaceEnvironments.Choice("ubuntu","ubuntu","Ubuntu","/root",false));assertEquals(7,WorkspaceAliases.installed(all).size());
        for(String bad:new String[]{"ubuntu-ai","ssh","../ubuntu",""})try{WorkspaceAliases.profile(bad);fail(bad);}catch(IllegalArgumentException expected){}
        for(String env:WorkspaceAliases.ENVIRONMENTS){WorkspaceStore.Session p=WorkspaceAliases.profile(env);assertEquals(env.equals("termux")?"termux":"proot:"+env,ProjectFiles.environment(p));String command=ProjectFiles.command(p,"printf test");if(!env.equals("termux"))assertTrue(command,command.contains("'"+env+"' -- /bin/sh -c"));assertFalse(command.contains("ubuntu-ai"));}
    }
    @Test public void invalidNamesDuplicateDefinitionsAndAiBindingCannotReplaceExistingLibrary()throws Exception{
        WorkspaceStore s=new WorkspaceStore(RuntimeEnvironment.application,e->{});try{s.aliases.clear();WorkspaceStore.Alias a=new WorkspaceStore.Alias();a.name="ll";a.command="ls";s.aliases.add(a);JSONObject good=s.encode();
            for(String name:new String[]{"", "9ll","a b","x;touch","if","a/b","a\n"})assertFalse(name,WorkspaceAliases.validName(name));
            assertTrue(WorkspaceAliases.validName("git-log"));JSONObject bad=new JSONObject(good.toString());bad.getJSONArray("aliases").getJSONObject(0).put("environment","ubuntu-ai");
            try{s.decode(bad.toString());fail();}catch(JSONException expected){}assertEquals("termux",s.aliases.get(0).environment);
            bad=new JSONObject(good.toString());JSONObject duplicate=new JSONObject(bad.getJSONArray("aliases").getJSONObject(0).toString()).put("id",UUID.randomUUID().toString());bad.getJSONArray("aliases").put(duplicate);
            try{s.decode(bad.toString());fail();}catch(JSONException expected){}assertEquals(1,s.aliases.size());
        }finally{s.close();}
    }
    @Test public void actualGeneratedStdinHelperPreservesBodyWithoutExecutingSubstitutions()throws Exception{
        java.nio.file.Path home=java.nio.file.Files.createTempDirectory("jobs-alias-stdin-");
        try{
            String helper=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/assets/workspace-aliases.sh")),java.nio.charset.StandardCharsets.UTF_8);
            WorkspaceStore.Alias a=new WorkspaceStore.Alias();a.name="ll";a.command="printf '%s' \"$(touch "+home.resolve("never")+")\"\nprintf '%s' 'last line'";
            ProcessBuilder builder=new ProcessBuilder("/bin/sh","-s");builder.environment().put("HOME",home.toString());builder.redirectErrorStream(true);Process child=builder.start();
            try(java.io.OutputStream in=child.getOutputStream()){in.write(WorkspaceAliases.script(helper,a,false,"").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            byte[] output=child.getInputStream().readAllBytes();assertEquals(new String(output,java.nio.charset.StandardCharsets.UTF_8),0,child.waitFor());assertEquals("ALIAS_OK\n",new String(output,java.nio.charset.StandardCharsets.UTF_8));assertFalse(java.nio.file.Files.exists(home.resolve("never")));
            ProcessBuilder inspect=new ProcessBuilder("/bin/bash","--noprofile","--norc","-c",". \"$HOME/.bashrc\"; printf '%s' \"${BASH_ALIASES[ll]}\"");inspect.environment().put("HOME",home.toString());inspect.redirectErrorStream(true);Process read=inspect.start();String body=new String(read.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertEquals(0,read.waitFor());assertEquals(a.command,body);assertFalse(java.nio.file.Files.exists(home.resolve("never")));
        }finally{try(java.util.stream.Stream<java.nio.file.Path> paths=java.nio.file.Files.walk(home)){for(java.nio.file.Path path:paths.sorted(java.util.Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList()))java.nio.file.Files.delete(path);}}
    }
    @Test public void removedEnvironmentAllowsMetadataDeletionButNeverHostFallbackOrSaving()throws Exception{
        WorkspaceStore.Alias a=new WorkspaceStore.Alias();a.environment="fedora";a.name="ll";a.command="ls";
        WorkspaceAliases.apply(null,a,true,"",Collections.emptyList());
        try{WorkspaceAliases.apply(null,a,false,"",Collections.emptyList());fail();}catch(java.io.IOException expected){assertEquals("ALIAS_ENVIRONMENT_NOT_INSTALLED",expected.getMessage());}
        a.environment="ubuntu-ai";try{WorkspaceAliases.apply(null,a,true,"",Collections.emptyList());fail();}catch(java.io.IOException expected){assertEquals("ALIAS_INVALID_DEFINITION",expected.getMessage());}
    }
    @Test public void failedMoveRestoresNewEnvironmentAndCollisionWithoutTouchingOtherAliases()throws Exception{
        WorkspaceStore.Alias before=new WorkspaceStore.Alias();before.name="ll";before.command="old";WorkspaceStore.Alias after=before.copy();after.environment="fedora";after.command="new";
        WorkspaceStore.Alias collision=new WorkspaceStore.Alias();collision.name="ll";collision.environment="fedora";collision.command="existing";
        Map<String,String> files=new HashMap<>();files.put("termux:"+before.id,"old");files.put("fedora:"+collision.id,"existing");files.put("debian:untouched","keep");
        try{WorkspaceAliases.change(before,after,collision,(a,delete)->{if(delete&&a==before)throw new java.io.IOException("fixture failure");String key=a.environment+":"+a.id;if(delete)files.remove(key);else files.put(key,a.command);});fail();}catch(java.io.IOException expected){assertEquals("fixture failure",expected.getMessage());}
        assertEquals(3,files.size());assertEquals("old",files.get("termux:"+before.id));assertEquals("existing",files.get("fedora:"+collision.id));assertEquals("keep",files.get("debian:untouched"));
    }
    @Test public void replacementAndRenameCommitOnlyIntendedIds()throws Exception{
        WorkspaceStore.Alias before=new WorkspaceStore.Alias();before.name="oldname";before.command="pwd";WorkspaceStore.Alias after=before.copy();after.name="ll";after.command="ls";WorkspaceStore.Alias collision=new WorkspaceStore.Alias();collision.name="ll";collision.command="old ls";
        Map<String,String> files=new HashMap<>();files.put(before.id,before.command);files.put(collision.id,collision.command);
        WorkspaceAliases.change(before,after,collision,(a,delete)->{if(delete)files.remove(a.id);else files.put(a.id,a.command);});assertEquals(1,files.size());assertEquals("ls",files.get(before.id));
        WorkspaceAliases.change(after,null,null,(a,delete)->{assertTrue(delete);files.remove(a.id);});assertTrue(files.isEmpty());
    }
    @Test public void commandIsOnlyQuotedDataAndPortableHooksAreScopedToSupportedGuest()throws Exception{
        WorkspaceStore.Alias a=new WorkspaceStore.Alias();a.name="ll";a.command="printf '%s' \"$(touch /never)\"";String script=WorkspaceAliases.script("printf ALIAS_OK",a,false,"");assertTrue(script.startsWith("set -- 'save' '"));assertTrue(script.contains(SessionShell.quote(a.command)));
        assertTrue(WorkspaceAliases.hook().contains("[ ! -L"));assertTrue(WorkspaceAliases.hook().contains("\\unset"));
        File files=new File(RuntimeEnvironment.application.getFilesDir(),"alias-session-test");WorkspaceStore.Session p=WorkspaceAliases.profile("alpine");SessionShell.prepare(files,p);File rc=new File(SessionShell.directory(files,p.id),"proot-shrc");String text=new String(java.nio.file.Files.readAllBytes(rc.toPath()),java.nio.charset.StandardCharsets.UTF_8);assertTrue(text.contains(".termux-jobs-aliases"));
    }
}
