package com.termux.app.workspace;
import org.json.*;import java.util.UUID;import java.util.concurrent.atomic.AtomicBoolean;
/** Synthetic inference only: this class has no Runner, shell or filesystem tool dispatcher. */
final class LocalAiToolProbe {
 interface Model {
  String schemas();void schemas(String s);String scope();void scope(String s);
  boolean thinking();void thinking(boolean value);boolean greedy();void greedy(boolean value);
  String infer(JSONArray messages,String system,int tokens)throws Exception;
 }
 interface Progress {void changed();}
 final JSONArray results=new JSONArray();volatile boolean running;volatile String phase="idle",failure="";
 void run(WorkspaceStore.Session profile,JSONArray all,Model model,AtomicBoolean cancel,Progress progress)throws Exception{
  if(running)throw new IllegalStateException("Probe already running");
  if(!profile.localAccess.equals("write"))throw new IllegalArgumentException("Tool probe requires the selected write mode");
  String schemas=model.schemas(),scope=model.scope();boolean thinking=model.thinking(),greedy=model.greedy();
  JSONArray write=new JSONArray();for(int i=0;i<all.length();i++)if(all.getJSONObject(i).getJSONObject("function").getString("name").equals("write"))write.put(all.getJSONObject(i));
  if(write.length()!=1)throw new IllegalArgumentException("Selected model has no write tool");
  running=true;phase="starting";failure="";synchronized(results){while(results.length()>0)results.remove(0);}
  try{
   progress.changed();
   String target=profile.localProject+(profile.localProject.endsWith("/")?"":"/")+"tool-probe-guide.txt";
   String question="Напиши короткую инструкцию из трёх пунктов, как установить Python в Ubuntu, и сохрани её в файл "+target+". Сам Python устанавливать не нужно.";
   String system=LocalAiLiteRt.prompt(profile,profile.localToolsOnly?"":profile.localBehavior);
   for(int i=0;i<5;i++){
    LocalAiFiles.check(cancel);String id=new String[]{"system_role","write_only","all_current","all_greedy","all_thinking"}[i];phase=id;progress.changed();
    model.schemas((i==0?new JSONArray():i==1?write:all).toString());model.scope(UUID.randomUUID().toString());model.greedy(i==3);model.thinking(i==4);
    String instruction=i==0?"Ответь только одним словом: ROLE_OK. Не добавляй пояснений.":system;
    String input=i==0?"Пожалуйста, ответь.":question;long started=System.nanoTime();JSONObject row=new JSONObject().put("case",id).put("thinking",i==4).put("greedy",i==3).put("toolExecutionEnabled",false).put("maxOutput",512);
    try{
     String text=model.infer(new JSONArray().put(new JSONObject().put("role","user").put("text",input)),instruction,512);LocalAiFiles.check(cancel);
     row.put("reply",text.length()>8000?text.substring(0,8000):text).put("replyExcerpt",text.length()>8000);
     if(i==0)row.put("observed",LocalAiText.split(text).answer.trim().equals("ROLE_OK")?"marker_match":"other_text");
     else{JSONObject call;try{call=LocalAiTools.request(text);}catch(IllegalArgumentException invalid){call=null;row.put("validation",invalid.getMessage());}
      String observed=call==null?"no_valid_call":call.optString("tool");
      if(call!=null&&observed.equals("write"))observed=!call.optString("path").equals(target)?"write_other_path":call.optString("text").trim().isEmpty()?"write_empty":"write_requested";
      row.put("observed",observed);
     }
    }catch(java.util.concurrent.CancellationException e){throw e;}catch(Exception e){LocalAiFiles.check(cancel);row.put("observed","error").put("error",LocalAiEngine.message(e));}
    row.put("elapsedMs",(System.nanoTime()-started)/1000000);synchronized(results){results.put(row);}progress.changed();
   }
   phase="completed";
  }catch(java.util.concurrent.CancellationException e){phase="stopped";throw e;}catch(Exception e){phase="failed";failure=LocalAiEngine.message(e);throw e;}
  finally{model.schemas(schemas);model.scope(scope);model.thinking(thinking);model.greedy(greedy);running=false;progress.changed();}
 }
 String report(boolean ru){try{StringBuilder b=new StringBuilder(ru?"Диагностика выбора инструментов LiteRT\n":"LiteRT tool selection probe\n");b.append(ru?"Состояние: ":"State: ").append(phase).append("\n");b.append(ru?"Синтетические запросы. Никакие вызовы инструментов НЕ выполняются, файлы не создаются. Тест показывает выбор модели, а не успешную запись файла. Максимум каждого ответа 512 включает рассуждение 128 в последнем случае; настройки обычного чата не меняются.\n":"Synthetic requests. NO tool calls are executed or files created. Observations show model choice, not successful file writes. Each reply has a 512-token cap including 128 thinking tokens in the last case; normal chat settings are unchanged.\n");JSONArray snapshot; synchronized(results){snapshot=new JSONArray(results.toString());}for(int i=0;i<snapshot.length();i++){JSONObject r=snapshot.getJSONObject(i);b.append('\n').append(r.getString("case")).append(" · ").append(r.optString("observed")).append(" · ").append(r.optLong("elapsedMs")).append(" ms\n").append(r.optString("reply",r.optString("error"))).append('\n');if(r.optBoolean("replyExcerpt"))b.append("[excerpt]\n");}if(!failure.isEmpty())b.append('\n').append(failure);return b.toString();}catch(Exception e){return "Probe report unavailable";}}
}
