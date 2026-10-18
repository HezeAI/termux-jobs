package com.termux.app.workspace;

import java.util.*;
import java.nio.charset.StandardCharsets;

/** Local completion telemetry. Accepts only typed operations/numbers and allowlisted errors. No RPCs. */
final class ServerDiagnostics {
    enum Op {METRICS,DIRECTORY,SEARCH,PROBE,COMPONENTS,PROPERTIES,INSTALL,SCHEDULE,TASK_START,TASK_CANCEL,TASK_LOG,TASK_REMOVE,FILE_RENAME,FILE_DELETE,STREAM,SERVICES,SERVICE_LOG,SERVICE_RESTART,SFTP_CONNECT,UPLOAD,DOWNLOAD,RPC,FILE_MOVE}
    static final int LIMIT=2*1024*1024, TOTAL=32*1024*1024;
    private static final LinkedHashMap<String,Run> runs=new LinkedHashMap<>();
    private static final ThreadLocal<Long> queued=new ThreadLocal<>();
    static void queueStarted(long elapsedMs){queued.set(Math.max(0,elapsedMs));}
    static void queueFinished(){queued.remove();}
    private static final Set<String> ERRORS=new HashSet<>(Arrays.asList("SSH_FAILED","TRUST_REQUIRED","PASSWORD_REQUIRED","OPENSSH_REQUIRED","CLOSED","CANCELLED","RESPONSE_LIMIT","PYTHON_OR_AGENT_UNAVAILABLE","AGENT_ERROR","SEARCH_FAILED","SCHEDULE_ERROR","SFTP_FAILED","SFTP_WRITE_FAILED","SFTP_ID","SFTP_PACKET","SFTP_OPEN","SFTP_STAT","SFTP_REPLACE_UNSUPPORTED","REMOTE_CHANGED","FILE_LIMIT","INVALID_PATH","ACTIVE_TASKS","TASK_NOT_FOUND","TASK_NOT_MANAGED","QUEUE_FULL","PERMISSION_DENIED","SERVICE_PERMISSION_OR_UNAVAILABLE","DOWNLOAD_FAILED","NOT_DIRECTORY","TIMEOUT","SFTP_READ","SFTP_SHORT_READ","SFTP_READDIR","SFTP_DIRECTORY","TREE_LIMIT","DOWNLOAD_SIZE_LIMIT","DESTINATION_EXISTS","MOVE_INTO_SELF","UNSUPPORTED_TYPE","REMOTE_SYMLINK_OR_CHANGED","DUPLICATE_OR_UNSAFE_NAME","DELETE_FAILED","DELETE_MOUNT"));
    static String error(Exception e){return e==null?"OK":e instanceof java.util.concurrent.CancellationException||e instanceof InterruptedException?"CANCELLED":ERRORS.contains(e.getMessage())?e.getMessage():"OTHER_ERROR";}
    static final class Event {
        final long time,waitMs,networkMs,parseMs,bytes;final Op operation;final String code;
        Event(Op op,long wait,long net,long parse,long size,String error){time=System.currentTimeMillis();operation=op;waitMs=wait;networkMs=Math.max(0,net);parseMs=Math.max(0,parse);bytes=size;code=error;}
        String line(){return time+" "+operation.name()+" queue_ms="+waitMs+" request_ms="+networkMs+" parse_ms="+parseMs+" bytes="+bytes+" result="+code+"\n";}
    }
    private static final class Run {boolean paused;int epoch,bytes,dropped;final ArrayDeque<Event> events=new ArrayDeque<>();Run(boolean p){paused=p;}}
    static synchronized void configure(ServerProfiles.Profile p){if(!runs.containsKey(p.id))runs.put(p.id,new Run(p.diagnosticsPaused));while(runs.size()>64)runs.remove(runs.keySet().iterator().next());}
    static synchronized void pause(ServerProfiles.Profile p,boolean paused){configure(p);Run r=runs.get(p.id);r.paused=paused;r.epoch++;}
    static synchronized boolean paused(ServerProfiles.Profile p){configure(p);return runs.get(p.id).paused;}
    static final class Trace {
        final String id;final Op op;final int epoch;final boolean enabled;final long waitMs,started=System.nanoTime();private boolean done;
        Trace(String id,Op op,int epoch,boolean enabled,long wait){this.id=id;this.op=op;this.epoch=epoch;this.enabled=enabled;waitMs=wait;}
        long elapsed(){return Math.max(0,(System.nanoTime()-started)/1000000);}
        synchronized void finish(long network,long parse,long size,Exception failure){if(done)return;done=true;if(enabled)record(this,new Event(op,waitMs,network,parse,size,error(failure)));}
    }
    static synchronized Trace begin(ServerProfiles.Profile p,Op operation){configure(p);Run r=runs.get(p.id);Long wait=queued.get();queued.remove();return new Trace(p.id,operation,r.epoch,!r.paused,wait==null?-1:wait);}
    private static synchronized void record(Trace trace,Event event){Run r=runs.get(trace.id);if(r==null||r.paused||r.epoch!=trace.epoch)return;r.events.addLast(event);r.bytes+=event.line().getBytes(StandardCharsets.UTF_8).length;while(r.bytes>LIMIT&&!r.events.isEmpty())drop(r);int total=0;for(Run run:runs.values())total+=run.bytes;while(total>TOTAL){boolean changed=false;for(Run run:runs.values())if(!run.events.isEmpty()){int n=run.bytes;drop(run);total-=n-run.bytes;changed=true;break;}if(!changed)break;}}
    private static void drop(Run r){r.bytes-=r.events.removeFirst().line().getBytes(StandardCharsets.UTF_8).length;r.dropped++;}
    static final class Snapshot {final boolean paused;final int bytes,dropped;final List<Event> events;Snapshot(Run r){paused=r.paused;bytes=r.bytes;dropped=r.dropped;List<Event> last=new ArrayList<>();Iterator<Event> i=r.events.descendingIterator();while(i.hasNext()&&last.size()<200)last.add(i.next());events=last;}}
    static synchronized Snapshot snapshot(ServerProfiles.Profile p){configure(p);return new Snapshot(runs.get(p.id));}
    static synchronized String report(ServerProfiles.Profile p){configure(p);Run r=runs.get(p.id);StringBuilder b=new StringBuilder("Termux Job's server diagnostics\nApp: "+com.termux.BuildConfig.VERSION_NAME+"\nFormat: 1\nCollection paused: "+r.paused+"\nDropped: "+r.dropped+"\nQueue -1 / bytes -1 mean unavailable. Completed operations only.\nNo addresses, paths, commands, payloads or secrets. Memory journal resets with app process; exported files persist.\n\n");for(Event e:r.events)b.append(e.line());return b.toString();}
    static Op operation(String op){switch(op){case "status":return Op.METRICS;case "list":return Op.DIRECTORY;case "start":return Op.TASK_START;case "cancel":return Op.TASK_CANCEL;case "log":return Op.TASK_LOG;case "forget":return Op.TASK_REMOVE;case "rename":return Op.FILE_RENAME;case "delete":return Op.FILE_DELETE;case "stream":return Op.STREAM;case "services":return Op.SERVICES;case "service-log":return Op.SERVICE_LOG;case "service-restart":return Op.SERVICE_RESTART;default:return Op.RPC;}}
    static String title(Op op,boolean ru){String[] ruNames={"Метрики","Чтение каталога","Поиск имён","Проверка подключения","Компоненты","Свойства файла","Установка агента","Расписания","Запуск задачи","Остановка задачи","Журнал задачи","Удаление записи","Переименование","Удаление файлов","Открытие медиа","Службы","Журнал службы","Перезапуск службы","Подключение SFTP","Отправка файла","Скачивание файла","Запрос","Перемещение файлов"};return ru?ruNames[op.ordinal()]:op.name().replace('_',' ');}
}
