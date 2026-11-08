package com.termux.app.workspace;
import com.termux.app.TermuxActivity;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Independent saved-chat reader. No transport, send, launch or permission actions. */
final class OfflineProjectChat implements AgentProjectPanel.Chat {
    final TermuxActivity activity;final WorkspaceStore.Session profile;final ProjectFiles files;final ChatAttachmentUi media;
    final ExecutorService worker=Executors.newSingleThreadExecutor();final List<JSONObject> data=new ArrayList<>();
    volatile boolean closed;boolean loading,more=true;long offset;String error="";Runnable changed=()->{};
    OfflineProjectChat(TermuxActivity a,WorkspaceStore.Session p){activity=a;profile=ChatAttachmentUi.source(p);files=new ProjectFiles(a,profile);
        media=new ChatAttachmentUi(a,new AgentChatView(a,j->{},()->{},()->{},()->{},()->{}));
        JSONObject saved=AgentOfflineData.read(a,p,"files").optJSONObject("data");if(saved!=null){JSONArray rows=saved.optJSONArray("rows");if(rows!=null)for(int i=0;i<rows.length();i++)if(rows.optJSONObject(i)!=null)data.add(rows.optJSONObject(i));}
    }
    public boolean valid(){return !closed;}public List<JSONObject> rows(){return new ArrayList<>(data);}public boolean hasOlder(){return more;}public boolean loadingOlder(){return loading;}public String historyError(){return error;}
    public void observe(Runnable listener){changed=listener;older();}
    public void refresh(){if(!loading){offset=0;more=true;older();}}
    public void older(){if(closed||loading||!more)return;loading=true;error="";changed.run();final long cursor=offset;
        worker.execute(()->{JSONObject response=null;String failure="";try{
            String script;try(java.io.InputStream in=activity.getAssets().open("agent-offline-files.py");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);script=out.toString("UTF-8");}
            String agent=profile.claudeAgent?"claude":profile.geminiAgent?"gemini":profile.aiAgent.equals("opencode")?"opencode":"codex";
            String thread=profile.claudeAgent?profile.claudeThread:profile.geminiAgent?profile.geminiThread:profile.aiAgent.equals("opencode")?profile.aiThread:profile.codexThread;
            JSONObject q=new JSONObject().put("agent",agent).put("thread",thread).put("offset",cursor);
            response=new JSONObject(new String(files.readOnly("python3 -I -c "+SessionShell.quote(script)+" "+SessionShell.quote(q.toString()),2*1024*1024),java.nio.charset.StandardCharsets.UTF_8));
            if(!response.optBoolean("ok"))failure=response.optString("error");
        }catch(Exception e){failure=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
        final JSONObject value=response;final String reason=failure;activity.runOnUiThread(()->{if(closed)return;loading=false;if(reason.isEmpty()&&value!=null){JSONArray rows=value.optJSONArray("rows");if(cursor==0)data.clear();if(rows!=null)for(int i=0;i<rows.length();i++)if(rows.optJSONObject(i)!=null)data.add(rows.optJSONObject(i));more=!value.isNull("next");offset=value.optLong("next",0);AgentOfflineData.files(activity,profile,data,!more);}else error=AgentStatusUi.tr(activity,"Не удалось прочитать сохранённую историю: ","Could not read saved history: ")+reason(reason);changed.run();});});
    }
    String reason(String code){String ru=code.equals("SAVED_HISTORY_NOT_FOUND")?"Файл истории этого чата не найден в сохранённом окружении":code.equals("INVALID_SAVED_CHAT")?"В настройках нет допустимого ID сохранённого чата":code.equals("AMBIGUOUS_SAVED_HISTORY")?"Найдено несколько файлов истории с этим ID":code.equals("HISTORY_RECORD_TOO_LARGE")?"Запись истории превышает безопасный размер чтения":code.equals("UNSAFE_HISTORY_PATH")?"Путь истории является символической ссылкой":code;return WorkspaceLanguage.code(activity).equals("ru")?ru:code;}
    public void open(JSONObject file){if(valid())media.openProjectFile(profile,file);}public boolean jump(String key){return false;}public boolean canJump(String key){return false;}
    public void close(){closed=true;files.close();media.close();worker.shutdown();}
}
