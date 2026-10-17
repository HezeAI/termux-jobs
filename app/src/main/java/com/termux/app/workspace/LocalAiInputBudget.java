package com.termux.app.workspace;
import org.json.*;
/** Metadata only. Leave-one-group-out attribution is approximate; rendered total is exact for llama. */
final class LocalAiInputBudget {
 static JSONObject safeMeasure(JSONArray messages,String system,LocalAiContext.Counter count,boolean exact)throws Exception{
  try{return measure(messages,system,count,exact);}catch(java.util.concurrent.CancellationException e){throw e;}catch(Exception e){return new JSONObject().put("kind","input_breakdown").put("unavailable",true);}
 }
 static JSONObject measure(JSONArray messages,String system,LocalAiContext.Counter count,boolean exact)throws Exception{
  int total=count.count(messages,system),last=-1;for(int i=messages.length()-1;i>=0;i--){JSONObject m=messages.getJSONObject(i);if(LocalAiContext.userTask(m)){last=i;break;}}
  int[] groups=new int[messages.length()];for(int i=0;i<groups.length;i++){JSONObject m=messages.getJSONObject(i);groups[i]=m.has("toolResult")||m.has("toolRequest")||m.optBoolean("appFeedback")?1:i==last?2:0;}
  int[] parts=new int[3];for(int g=0;g<3;g++){JSONArray rest=new JSONArray();boolean found=false;for(int i=0;i<groups.length;i++)if(groups[i]!=g)rest.put(messages.getJSONObject(i));else found=true;parts[g]=found?total-count.count(rest,system):0;}
  int instructions=total-count.count(messages,"");int residual=total-instructions-parts[0]-parts[1]-parts[2];
  return new JSONObject().put("kind","input_breakdown").put("exact",exact).put("total",total).put("system",instructions).put("history",parts[0]).put("tools",parts[1]).put("current",parts[2]).put("format",residual).put("messages",messages.length());
 }
 static String report(JSONObject o,boolean ru){if(o==null)return "";if(o.optBoolean("unavailable"))return ru?"Состав входа: измерение недоступно; запрос продолжен.\n":"Input composition unavailable; inference continued.\n";StringBuilder s=new StringBuilder(ru?"Состав входа (токены):\n":"Input composition (tokens):\n");String[] keys={"system","history","tools","current","format"};String[] labels=ru?new String[]{"Инструкции и описание инструментов","Прежняя переписка","Запросы и результаты инструментов","Текущее сообщение","Оформление и взаимодействие границ"}:new String[]{"Instructions and tool definitions","Previous conversation","Tool requests and results","Current message","Formatting and boundary interactions"};int total=o.optInt("total");for(int i=0;i<keys.length;i++){int n=o.optInt(keys[i]);s.append(labels[i]).append(": ").append(n);if(total>0)s.append(String.format(java.util.Locale.ROOT," (%.1f%%)",n*100.0/total));s.append('\n');}s.append(ru?"Итого: ":"Total: ").append(total).append(o.optBoolean("exact")?(ru?" · точный токенизатор модели":" · exact model tokenizer"):(ru?" · оценка; точный токенизатор не использован":" · estimate; exact tokenizer not used")).append('\n').append(ru?"Части приблизительные: разность с исключённой группой. Границы и шаблон могут давать отрицательный остаток. Тексты не включены.\n":"Components are approximate leave-one-group-out differences. Boundaries/template may produce a negative residual. No text included.\n");return s.toString();}
}
