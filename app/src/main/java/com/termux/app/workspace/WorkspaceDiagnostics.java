package com.termux.app.workspace;

import com.termux.terminal.TerminalSession;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Bounded, private, structured diagnostics. Never records terminal input, tokens or transcript. */
public final class WorkspaceDiagnostics {
    static final int LIMIT=2*1024*1024;
    private static final Map<String,Run> RUNS=new LinkedHashMap<>();
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static final Pattern CODES=Pattern.compile("\\b(?:RESOURCE_EXHAUSTED|UNAUTHENTICATED|PERMISSION_DENIED|INVALID_ARGUMENT|NOT_FOUND|UNAVAILABLE|DEADLINE_EXCEEDED|UNSUPPORTED_CLIENT|ENOENT|EACCES|ECONNRESET|ECONNREFUSED|ENOTFOUND|ETIMEDOUT|EAI_AGAIN|ENETUNREACH)\\b");
    private static final Pattern HTTP=Pattern.compile("(?i)\\b(?:http(?:/[0-9.]+)?|status(?:code)?|code)[\\s:=\"']{1,12}([45][0-9]{2})\\b");
    private static final class Run {
        String id,handle,agent,environment; boolean extended,extendedAtLaunch,paused;long startedAt,startedElapsed,endedElapsed,lastEvent;int launches,errorCount; long lastScan,lastOutput; int bytes,dropped; String lastError="";
        final ArrayDeque<String> lines=new ArrayDeque<>();
    }
    private WorkspaceDiagnostics() {}
    private static String agent(WorkspaceStore.Session s){return s.geminiAgent?"Gemini CLI":s.claudeAgent?"Claude Code":s.aiAgent.equals("opencode")?"OpenCode":s.environment.equals("codex")?"Codex":"shell";}
    private static String stamp(){return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z",Locale.ROOT).format(new Date());}
    private static void append(Run r,String message){
        r.lastEvent=System.currentTimeMillis();String line=stamp()+" "+message+"\n";r.lines.addLast(line);r.bytes+=line.length();
        while(r.bytes>LIMIT&&!r.lines.isEmpty()){r.bytes-=r.lines.removeFirst().length();r.dropped++;}
        int total=0;for(Run known:RUNS.values())total+=known.bytes;
        // Bound total retained data too, including closed profiles.
        while(total>16*LIMIT){boolean removed=false;for(Run known:RUNS.values())if(known.lines.size()>1){int n=known.lines.removeFirst().length();known.bytes-=n;known.dropped++;total-=n;removed=true;break;}if(!removed)break;}
    }
    public static synchronized void register(WorkspaceStore.Session s,boolean launched){
        Run r=RUNS.get(s.id);
        if(r==null){r=new Run();r.id=s.id;r.agent=agent(s);r.environment=s.environment;RUNS.put(s.id,r);append(r,"COLLECTOR attached; previous process output is not reconstructed");}
        r.handle=s.handle;if(launched){r.agent=agent(s);r.environment=s.environment;r.extended=s.extendedDebug;r.extendedAtLaunch=s.extendedDebug;r.startedAt=System.currentTimeMillis();r.startedElapsed=android.os.SystemClock.elapsedRealtime();r.endedElapsed=0;r.launches++;}
        if(launched){r.paused=false;append(r,"START environment="+r.environment+" agent="+r.agent+" extended="+r.extended+"; custom commands and credentials omitted");}
        while(RUNS.size()>64)RUNS.remove(RUNS.keySet().iterator().next());
    }
    static synchronized void failure(WorkspaceStore.Session s,Exception error){register(s,false);Run r=RUNS.get(s.id);if(!r.paused)append(r,"LAUNCH_FAILED category="+(error instanceof IOException?"IO":"runtime")+"; details omitted to protect credentials");}
    public static synchronized void finished(TerminalSession session){Run r=find(session);if(r!=null){r.endedElapsed=android.os.SystemClock.elapsedRealtime();if(!r.paused)append(r,"PROCESS_EXIT status="+session.getExitStatus());}}
    private static Run find(TerminalSession s){for(Run r:RUNS.values())if(s.mHandle.equals(r.handle))return r;return null;}
    /** Called regardless of diagnostics dialog visibility. Work is capped to 100 terminal rows / 2s. */
    public static synchronized void onOutput(TerminalSession session){
        Run r=find(session);if(r==null||r.paused)return;
        long now=android.os.SystemClock.elapsedRealtime();if(now-r.lastScan<2000)return;r.lastScan=now;
        com.termux.terminal.TerminalEmulator e=session.getEmulator();if(e==null)return;
        String tail=e.getScreen().getSelectedText(0,-Math.min(60,e.getScreen().getActiveTranscriptRows()),Math.min(400,e.mColumns),Math.min(40,e.mRows),true);
        String errors=classify(tail);
        if(!errors.equals(r.lastError)){if(!errors.isEmpty()){r.errorCount++;append(r,"TERMINAL_SIGNAL "+errors+" (observed text; not proof of a failed request)");}r.lastError=errors;}
        if(r.extended&&now-r.lastOutput>=15000){append(r,"OUTPUT_ACTIVITY pid="+session.getPid());r.lastOutput=now;}
    }
    /** Only allowlisted error categories/codes leave the terminal buffer. Unknown text never enters a report. */
    static String classify(String raw){
        Set<String> values=new LinkedHashSet<>();Matcher code=CODES.matcher(raw);while(code.find())values.add(code.group());
        Matcher http=HTTP.matcher(raw);while(http.find())values.add("HTTP_"+http.group(1));
        String lower=raw.toLowerCase(Locale.ROOT);
        if(lower.contains("quota")||lower.contains("rate limit"))values.add("QUOTA_OR_RATE_LIMIT_TEXT");
        if(lower.contains("timed out")||lower.contains("timeout"))values.add("TIMEOUT_TEXT");
        if(lower.contains("this client is no longer supported"))values.add("UNSUPPORTED_CLIENT_TEXT");
        if(lower.contains("skipping system settings file"))values.add("SYSTEM_SETTINGS_REJECTED_TEXT");
        if(lower.contains("taking a bit longer"))values.add("LONG_WAIT_TEXT");
        if(lower.contains("failed to sign in")||lower.contains("not logged in"))values.add("AUTH_FAILURE_TEXT");
        if(lower.contains("error")||lower.contains("exception"))values.add("ERROR_TEXT");
        StringBuilder summary=new StringBuilder();for(String value:values){if(summary.length()>0)summary.append(' ');summary.append(value);}return summary.toString();
    }
    static synchronized boolean paused(String id){Run r=RUNS.get(id);return r!=null&&r.paused;}
    static synchronized void pause(String id,boolean value){Run r=RUNS.get(id);if(r!=null&&r.paused!=value){r.paused=value;append(r,value?"COLLECTION_PAUSED":"COLLECTION_RESUMED; paused interval unavailable");}}
    static synchronized String report(WorkspaceStore.Session profile,TerminalSession terminal){
        Run r=RUNS.get(profile.id);
        StringBuilder b=new StringBuilder("Termux Job's diagnostics\nApp: "+com.termux.BuildConfig.VERSION_NAME+"\nFormat: 2\nGenerated: ").append(stamp()).append('\n');
        b.append("Session: ").append(profile.id).append("\nEnvironment: ").append(profile.environment).append("\nAgent: ").append(agent(profile));
        b.append("\nExtended requested: ").append(profile.extendedDebug).append("\nExtended active at launch: ").append(r!=null&&r.extendedAtLaunch);
        b.append("\nDetailed collection now: ").append(r!=null&&r.extended).append("\nLaunches observed: ").append(r==null?0:r.launches).append("\nLast event timestamp: ").append(r==null?0:r.lastEvent);
        b.append("\nCollector paused: ").append(r!=null&&r.paused).append("\nProcess: ").append(terminal==null?"unavailable":terminal.isRunning()?"running pid="+terminal.getPid():"exited status="+terminal.getExitStatus());
        b.append("\nLimit: 2 MiB per session, 32 MiB total; dropped old entries: ").append(r==null?0:r.dropped);
        b.append("\nPrivacy: structured events/codes; X11 may include redacted installer transfer failures. No terminal transcript is exported. URL, credential and path redaction is best-effort; review the report before sharing.\nHTTP/internal details depend on native agent output; this is not a network trace.\nNative verbose mode currently supported for Gemini only, enabled before launch. Other agents: extended activity timeline and observed error codes.\nRuntime events retained while app process lives; saved report files survive restart.\n\n");
        if(r==null)b.append("No collector attached for this session.\n");else for(String line:r.lines)b.append(line);
        return b.toString();
    }
    static synchronized boolean extended(String id){Run r=RUNS.get(id);return r!=null&&r.extended;}
    static synchronized void extended(String id,boolean value){Run r=RUNS.get(id);if(r!=null&&r.extended!=value){r.extended=value;if(!r.paused)append(r,value?"DETAILS_ENABLED; future activity only; native launch flags unchanged":"DETAILS_DISABLED");}}
    static final class Snapshot {
        final boolean paused,extended,nativeAtLaunch;final int bytes,dropped,launches,errors;final long lastEvent,uptime;final String signal;final List<String> events;
        Snapshot(Run r){paused=r.paused;extended=r.extended;nativeAtLaunch=r.extendedAtLaunch;bytes=r.bytes;dropped=r.dropped;launches=r.launches;errors=r.errorCount;lastEvent=r.lastEvent;uptime=r.startedAt==0?-1:Math.max(0,((r.endedElapsed==0?android.os.SystemClock.elapsedRealtime():r.endedElapsed)-r.startedElapsed)/1000);signal=r.lastError;events=new ArrayList<>();Iterator<String> i=r.lines.descendingIterator();while(i.hasNext()&&events.size()<200)events.add(i.next());}
    }
    static synchronized Snapshot snapshot(WorkspaceStore.Session s){register(s,false);return new Snapshot(RUNS.get(s.id));}
    static String friendly(String line,boolean ru){
        String time=line.length()>23?line.substring(11,19):"";String event=line.length()>29?line.substring(29).trim():line;
        String title;
        if(event.startsWith("COLLECTOR"))title=ru?"Сбор диагностики подключён. Предыдущие события недоступны.":"Collector attached. Earlier events are unavailable.";
        else if(event.startsWith("START"))title=ru?"Процесс запущен":"Process launched";
        else if(event.startsWith("PROCESS_EXIT"))title=(ru?"Процесс завершился · ":"Process exited · ")+event.substring("PROCESS_EXIT ".length());
        else if(event.startsWith("LAUNCH_FAILED"))title=ru?"Не удалось запустить процесс. Проверьте окружение и доступность программы.":"Process launch failed. Check the environment and executable.";
        else if(event.startsWith("TERMINAL_SIGNAL"))title=(ru?"В выводе замечен сигнал: ":"Observed in output: ")+event.substring("TERMINAL_SIGNAL ".length()).replace(" (observed text; not proof of a failed request)","")+(ru?" · проверьте контекст, это не подтверждение сбоя запроса.":" · check context; this does not prove a request failed.");
        else if(event.startsWith("OUTPUT_ACTIVITY"))title=ru?"Получен вывод процесса":"Process output activity";
        else if(event.startsWith("COLLECTION_PAUSED"))title=ru?"Журнал остановлен":"Collection paused";
        else if(event.startsWith("COLLECTION_RESUMED"))title=ru?"Журнал возобновлён. Пропущенные события недоступны.":"Collection resumed. Paused events are unavailable.";
        else if(event.startsWith("DETAILS_ENABLED"))title=ru?"Включён подробный сбор новых событий. Нативные флаги не менялись.":"Detailed collection enabled for future events. Native flags unchanged.";
        else if(event.startsWith("DETAILS_DISABLED"))title=ru?"Включён обычный сбор":"Normal collection enabled";
        else title=ru?"Событие диагностики":"Diagnostic event";
        return time+"  "+title+"\n    ["+event.split("[ =]",2)[0]+"]";
    }
    static void save(File files,String id,String report,java.util.function.Consumer<File> success,java.util.function.Consumer<Exception> failure){
        IO.execute(()->{try{File dir=new File(files,"workspace-diagnostics");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create diagnostics directory");File out=new File(dir,id+".txt");File temp=File.createTempFile("report-",".tmp",dir);try{try(OutputStream stream=new FileOutputStream(temp)){stream.write(report.getBytes(StandardCharsets.UTF_8));}if(!temp.setReadable(false,false)||!temp.setWritable(false,false)||!temp.setReadable(true,true)||!temp.setWritable(true,true))throw new IOException("Cannot protect report");if(!temp.renameTo(out))throw new IOException("Cannot save report");}finally{temp.delete();}success.accept(out);}catch(Exception e){failure.accept(e);}});
    }
}
