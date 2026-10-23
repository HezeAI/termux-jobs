package com.termux.app.workspace;

import com.termux.app.TermuxActivity;
import com.termux.terminal.TerminalSession;
import java.util.function.Supplier;
import java.text.SimpleDateFormat;
import java.util.*;

/** Same structured collector for Termux/Ubuntu/SSH/AI; no process control or new requests. */
final class WorkspaceDiagnosticsUi {
    private WorkspaceDiagnosticsUi(){}
    static void show(TermuxActivity activity,WorkspaceStore.Session profile,Supplier<TerminalSession> terminal){
        show(activity,profile,terminal,()->"",Runnable::run);
    }
    static void show(TermuxActivity activity,WorkspaceStore.Session profile,Supplier<TerminalSession> terminal,Supplier<String> x11,java.util.function.Consumer<Runnable> prepare){
        DiagnosticsPanel.show(activity,new DiagnosticsPanel.Source(){
            public boolean hasX11(){return WorkspaceDesktops.diagnosticsEligible(profile);}
            public String x11(){String value=x11.get();return value.isEmpty()?t("X11 · Нет сведений о запуске","X11 · No startup snapshot"):value;}
            public void prepareX11(Runnable done){prepare.accept(done);}
            public void prepareExport(Runnable done){prepare.accept(done);}
            WorkspaceDiagnostics.Snapshot data=WorkspaceDiagnostics.snapshot(profile);TerminalSession live;
            boolean ru(){return WorkspaceLanguage.code(activity).equals("ru");}
            String t(String a,String b){return ru()?a:b;}
            public void refresh(){live=terminal.get();data=WorkspaceDiagnostics.snapshot(profile);}
            public String title(){return t("Диагностика сессии","Session diagnostics");}
            public String id(){return profile.id;}
            public String summary(){String state=live==null?t("Нет сведений о процессе","Process unavailable"):live.isRunning()?t("Работает · PID ","Running · PID ")+live.getPid():t("Завершён · код ","Exited · status ")+live.getExitStatus();
                return t("Приложение: ","App: ")+com.termux.BuildConfig.VERSION_NAME+"\n"+t("Среда: ","Environment: ")+profile.environment+"\n"+state+"\n"+t("Время работы: ","Uptime: ")+(data.uptime<0?t("неизвестно, процесс подключён ранее","unknown; process attached earlier"):data.uptime/60+t(" мин "," min ")+data.uptime%60+t(" с"," s"))+"\n"+t("Журнал: ","Journal: ")+(data.extended?t("подробный","detailed"):t("обычный","normal"))+" · "+(data.paused?t("остановлен","paused"):t("сбор включён","collecting"))+"\n"+t("Последнее событие: ","Last event: ")+(data.lastEvent==0?"—":new SimpleDateFormat("HH:mm:ss",Locale.ROOT).format(new Date(data.lastEvent)))+"\n"+t("Запусков в этом журнале: ","Launches observed: ")+data.launches+"\n"+t("Наблюдений ошибок: ","Error observations: ")+data.errors+"\n\n"+t("Состав зависит от среды и агента. Работающий процесс не доказывает успешный вход. Пауза событий не означает зависание.","Available details depend on the environment and agent. A running process does not prove authentication. Event silence does not imply a hang.")+"\n"+t("Нативный verbose Gemini задаётся при запуске. Переключение здесь меняет сбор новых событий, без перезапуска и изменения прав.","Gemini native verbose is set at launch. This switch changes future event collection, without restarting or changing permissions.");}
            public String events(){StringBuilder text=new StringBuilder();for(String line:data.events)text.append(WorkspaceDiagnostics.friendly(line,ru())).append('\n');return text.toString();}
            public String report(){return WorkspaceDiagnostics.report(profile,terminal.get())+(hasX11()?"\n\n"+x11():"");}
            public int bytes(){return data.bytes;}public int dropped(){return data.dropped;}
            public boolean paused(){return WorkspaceDiagnostics.paused(profile.id);}public void paused(boolean value){WorkspaceDiagnostics.pause(profile.id,value);}
            public boolean hasMode(){return true;}public boolean extended(){return WorkspaceDiagnostics.extended(profile.id);}public void extended(boolean value){WorkspaceDiagnostics.extended(profile.id,value);}
        });
    }
}
