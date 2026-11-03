#!/usr/bin/env python3
"""Compile current production preparation code; use synthetic Android/tool boundaries only."""
from pathlib import Path
import json,subprocess,tempfile
REPO=Path(__file__).resolve().parents[2];BASE=REPO/'.verification/preparation-structured-status';BASE.mkdir(parents=True,exist_ok=True);root=Path(tempfile.mkdtemp(prefix='prepare-pipeline-',dir=BASE));src=root/'stubs';classes=root/'classes';prefix=root/'prefix';files=root/'files';tmp=root/'tmp'
for p in (src,classes,prefix/'bin',files/'.agent-preparation',tmp):p.mkdir(parents=True,exist_ok=True)
def write(name,text):
 p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
write('android/content/Context.java','package android.content; public class Context { public java.io.File getFilesDir(){return new java.io.File(System.getProperty("audit.files"));} public Context getApplicationContext(){return this;} }')
write('android/os/Looper.java','package android.os; public class Looper {private static final Looper MAIN=new Looper();public static Looper getMainLooper(){return MAIN;} }')
write('android/os/Handler.java','package android.os; public class Handler {public Handler(Looper x){} public boolean post(Runnable r){r.run();return true;} }')
write('android/os/SystemClock.java','package android.os; public class SystemClock {public static long elapsedRealtime(){return System.nanoTime()/1000000;} }')
write('com/termux/shared/termux/TermuxConstants.java','package com.termux.shared.termux; public class TermuxConstants {public static final String TERMUX_BIN_PREFIX_DIR_PATH='+json.dumps(str(prefix/'bin'))+';public static final String TERMUX_HOME_DIR_PATH='+json.dumps(str(root/'home'))+';}')
write('com/termux/shared/shell/TermuxShellUtils.java','package com.termux.shared.shell; public class TermuxShellUtils {public static String[] buildEnvironment(android.content.Context c,boolean x,String h){return new String[]{"PATH='+str(prefix/'bin')+':/usr/bin:/bin","LANG=C.UTF-8"};}}')
write('com/termux/app/workspace/Dependencies.java','''package com.termux.app.workspace;
import android.content.Context;import android.app.AlertDialog;import java.io.*;
class WorkspaceStore {static class Session {boolean agentSetupAllowed,geminiAgent;String geminiAuth="api";Session copy(){return this;}}}
class AgentEnvironments {static String alias(WorkspaceStore.Session s){return "ubuntu-ai";}}
class AgentPreparation {
 static final java.util.Set<String> COMPONENTS=new java.util.HashSet<>(java.util.Arrays.asList("node","ubuntu"));static String GUEST="/synthetic/guest";static class Plan {String error="";boolean authNeeded;boolean ready(){return true;}}
 interface Runner{Plan run(Context c,WorkspaceStore.Session s)throws Exception;}
 static Runner SYSTEM=(c,s)->new Plan();static void prepare(Context c){}
 static File directory(File f){return new File(f,".agent-preparation");}static String language(Context c){return "en";}
 static String tr(Context c,String ru,String en){return en;}static String agent(WorkspaceStore.Session p){return "codex";}
 static boolean supports(WorkspaceStore.Session p){return true;}static String stageText(String s,boolean ru){return s;}
 static String summary(Context c,Plan p){return "synthetic";}
}
class SessionShell {static String quote(String s){return "'"+s.replace("'","'\\\"'\\\"'")+"'";}}
class WorkspaceDialogs {static int TEXT=0;static int dp(Context c,int n){return n;}static AlertDialog.Builder builder(Context c){return new AlertDialog.Builder(c);}}
class DesktopProgramProgress{static String lastLine(String s){return s;}}
class WorkspaceLocalFilePicker{static boolean validAlias(String s){return true;}}
''')
write('com/termux/app/workspace/PreparationPipelineProbe.java','''package com.termux.app.workspace;
import java.nio.file.*;import java.lang.reflect.*;
public class PreparationPipelineProbe {
 public static void main(String[] args)throws Exception{
  android.content.Context c=new android.content.Context();WorkspaceStore.Session p=new WorkspaceStore.Session();
  Path ops=AgentPreparation.directory(c.getFilesDir()).toPath().resolve("operations");Files.createDirectories(ops);
  Path old=ops.resolve("ubuntu-ai-codex.log");Files.writeString(old,"EXISTING_FIXTURE_LOG");
  AgentPreparationOperation.Job job=new AgentPreparationOperation.Job(c,p,(ctx,s)->new AgentPreparation.Plan(),AgentPreparationOperation::prepare);job.run();
  for(Field f:job.getClass().getDeclaredFields())if(f.getType()==String.class){f.setAccessible(true);String v=(String)f.get(job);if(v!=null&&v.contains("SYNTHETIC_SECRET_BODY_ONLY"))throw new AssertionError("retained "+f.getName());}
  if(!Files.readString(old).equals("EXISTING_FIXTURE_LOG")||Files.list(ops).count()!=1)throw new AssertionError("old log modified or new log created");
  if(!job.stage.equals(args[0]))throw new AssertionError("stage "+job.stage);
  if(!job.error.equals(args[1]))throw new AssertionError("error "+job.error);
  if(job.exitCode==null||job.exitCode.intValue()!=Integer.parseInt(args[2]))throw new AssertionError("exit code");
  System.out.println("PASS "+args[0]+" "+args[1]+" "+job.exitCode+"; no retained secret; old log unchanged");
 }
}
''')
bash=prefix/'bin/bash'
if not bash.exists():bash.symlink_to('/bin/bash')
proot=prefix/'bin/proot-distro';proot.write_text('#!/bin/sh\n# Synthetic boundary: no real proot, guest or CLI is invoked.\nexit 0\n');proot.chmod(0o700)
android=REPO/'.tools/sdk/platforms/android-35/android.jar'
assert android.exists(),android
original=REPO/'app/src/main/java/com/termux/app/workspace'
cmd=['javac','-J-Djava.io.tmpdir='+str(tmp),'-cp',str(android),'-d',str(classes),*(str(p) for p in src.rglob('*.java')),*(str(original/n) for n in ('AgentPreparationOperation.java','AgentPreparationOutput.java','EnvironmentInstallProgress.java','EnvironmentSetupStatus.java'))]
subprocess.run(cmd,check=True,capture_output=True,text=True)
results=[]
scenarios=[
 ('stdout-multiline','printf -- "-----BEGIN PRIVATE KEY-----\\nSYNTHETIC_SECRET_BODY_ONLY\\n-----END PRIVATE KEY-----\\n"', 'complete','',0),
 ('stderr-fragmented-unfinished','printf -- "-----BE" >&2; printf -- "GIN PRIVATE KEY-----\\nSYNTHETIC_" >&2; printf -- "SECRET_BODY_ONLY" >&2','complete','',0),
 ('large-output-drained','for ((i=0;i<100000;i++)); do printf SYNTHETIC_SECRET_BODY_ONLY; done; printf "\\nPREPARATION:checking-cli\\n"','complete','',0),
 ('unknown-error','printf "ERROR:SYNTHETIC_SECRET_BODY_ONLY\\n"; return 42','failed','PREPARATION_EXIT_FAILED',42),
 ('known-error','printf "ERROR:IMAGE_DOWNLOAD_NETWORK\\n"; return 1','failed','IMAGE_DOWNLOAD_NETWORK',1),
 ('cancelled-child','printf "SYNTHETIC_SECRET_BODY_ONLY\\n"; return 130','failed','INSTALL_CANCELLED',130),
 ('retry-after-cancellation','printf "PREPARATION:checking-components\\n"','complete','',0)]
for name,script,stage,error,code in scenarios:
 (files/'.agent-preparation/agent-host-setup.sh').write_text('jobs_agent_host_prepare() { '+script+'; }\n')
 r=subprocess.run(['java','-Djava.io.tmpdir='+str(tmp),'-Duser.home='+str(tmp),'-Daudit.files='+str(files),'-cp',str(classes)+':'+str(android),'com.termux.app.workspace.PreparationPipelineProbe',stage,error,str(code)],check=True,capture_output=True,text=True,timeout=30)
 results.append(dict(scenario=name,result=r.stdout.strip()))
(BASE/'process-pipeline-results.json').write_text(json.dumps(dict(androidUiExecuted=False,realInstallation=False,networkUsed=False,dependencies='synthetic Context/Handler/checker/tool paths; current production prepare/drain/Job; real child pipes',results=results),indent=2)+'\n')
print(json.dumps(results,indent=2))
