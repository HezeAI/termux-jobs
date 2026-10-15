package com.termux.app.workspace;

import android.app.AlertDialog;
import com.termux.app.TermuxActivity;
import java.text.SimpleDateFormat;
import java.util.*;

final class ServerDiagnosticsUi {
    static AlertDialog show(TermuxActivity activity,ServerProfiles.Profile profile,ServerProfiles store){
        ServerDiagnostics.configure(profile);
        return DiagnosticsPanel.show(activity,new DiagnosticsPanel.Source(){
            ServerDiagnostics.Snapshot data=ServerDiagnostics.snapshot(profile);
            boolean ru(){return WorkspaceLanguage.code(activity).equals("ru");}String t(String a,String b){return ru()?a:b;}String ms(long n){return n<0?"—":n+t(" мс"," ms");}
            public void refresh(){data=ServerDiagnostics.snapshot(profile);}
            public String title(){return t("Диагностика сервера","Server diagnostics");}public String id(){return "server-"+profile.id;}
            public String summary(){String text=t("Приложение: ","App: ")+com.termux.BuildConfig.VERSION_NAME+"\n"+(data.paused?t("Журнал остановлен","Collection paused"):t("Сбор включён","Collecting"));
                if(data.events.isEmpty())return text+"\n\n"+t("Событий пока нет. Записываются завершённые операции. Открытие диагностики не подключается к серверу.","No events yet. Completed operations are recorded. Opening diagnostics does not connect to the server.");
                ServerDiagnostics.Event last=data.events.get(0);boolean transfer=last.operation==ServerDiagnostics.Op.UPLOAD||last.operation==ServerDiagnostics.Op.DOWNLOAD;
                return text+"\n"+t("Последняя операция: ","Last operation: ")+ServerDiagnostics.title(last.operation,ru())+"\n"+t("Очередь: ","Queue: ")+ms(last.waitMs)+"\n"+(transfer?t("Передача: ","Transfer: "):t("SSH-запрос: ","SSH request: "))+ms(last.networkMs)+"\n"+t("Разбор ответа: ","Response parse: ")+ms(last.parseMs)+"\n"+(transfer?t("Передано: ","Transferred: "):t("Ответ: ","Response: "))+(last.bytes<0?"—":ServerFileInfo.size(last.bytes,ru()))+"\n"+t("Результат запроса: ","Request result: ")+result(last.code,ru())+"\n\n"+t("Без дополнительных запросов. Успех запроса не означает завершение задачи. Неизвестные значения — прочерк. Журнал в памяти, экспорт — в файл.","No extra requests. Request success does not mean task completion. Unavailable values use a dash. In-memory journal; export saves a file.");}
            public String events(){StringBuilder b=new StringBuilder();SimpleDateFormat time=new SimpleDateFormat("HH:mm:ss",Locale.ROOT);for(ServerDiagnostics.Event e:data.events){b.append(time.format(new Date(e.time))).append("  ").append(ServerDiagnostics.title(e.operation,ru())).append(" · ").append(result(e.code,ru())).append('\n').append("    ").append(ms(e.networkMs));if(e.waitMs>=0)b.append(t(" · очередь "," · queue ")).append(ms(e.waitMs));if(e.bytes>=0)b.append(" · ").append(ServerFileInfo.size(e.bytes,ru()));if((e.operation==ServerDiagnostics.Op.UPLOAD||e.operation==ServerDiagnostics.Op.DOWNLOAD)&&e.networkMs>=500&&e.bytes>0)b.append(" · ").append(ServerFileInfo.size((long)(e.bytes*1000.0/e.networkMs),ru())).append(ru()?"/с":"/s");b.append('\n');}return b.toString();}
            public String report(){return ServerDiagnostics.report(profile);}public int bytes(){return data.bytes;}public int dropped(){return data.dropped;}
            public boolean paused(){return ServerDiagnostics.paused(profile);}
            public void paused(boolean value)throws Exception{boolean previous=profile.diagnosticsPaused;profile.diagnosticsPaused=value;try{store.save();}catch(Exception e){profile.diagnosticsPaused=previous;throw e;}ServerDiagnostics.pause(profile,value);}
        });
    }
    static String result(String code,boolean ru){switch(code){case "OK":return ru?"Успешно":"Success";case "CANCELLED":return ru?"Остановлено":"Cancelled";case "SSH_FAILED":return ru?"SSH недоступен · SSH_FAILED":"SSH unavailable · SSH_FAILED";case "TRUST_REQUIRED":return ru?"Подтвердите ключ сервера · TRUST_REQUIRED":"Verify host key · TRUST_REQUIRED";case "REMOTE_CHANGED":return ru?"Файл изменился · REMOTE_CHANGED":"File changed · REMOTE_CHANGED";case "PASSWORD_REQUIRED":return ru?"Нужен пароль · PASSWORD_REQUIRED":"Password required · PASSWORD_REQUIRED";default:return (ru?"Операция не завершена · ":"Operation incomplete · ")+code;}}
}
