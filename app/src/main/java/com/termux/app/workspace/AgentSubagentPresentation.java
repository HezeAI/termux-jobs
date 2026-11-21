package com.termux.app.workspace;
import org.json.*;
/** Human-readable rendering of supplied subagent events; never invents launches. */
final class AgentSubagentPresentation {
 static boolean matches(JSONObject row){String type=row.optString("toolType");return type.equals("subAgentActivity")||type.equals("collabAgentToolCall");}
 static String title(JSONObject row,boolean ru){switch(row.optString("toolName")){
 case "started":case "spawnAgent":return ru?"Запуск субагента":"Launch subagent";
 case "completed":return ru?"Субагент завершил работу":"Subagent finished";
 case "interrupted":case "interruptAgent":return ru?"Остановка субагента":"Interrupt subagent";
 case "wait":return ru?"Ожидание субагентов":"Wait for subagents";
 case "interacted":case "sendInput":case "sendMessage":return ru?"Сообщение субагенту":"Message subagent";
 case "followupTask":return ru?"Новая задача субагенту":"Follow-up task";
 case "resumeAgent":return ru?"Возобновление субагента":"Resume subagent";
 case "closeAgent":return ru?"Закрытие субагента":"Close subagent";
 case "listAgents":return ru?"Список субагентов":"List subagents";
 default:return ru?"Действие субагента":"Subagent activity";
 }}
 static String details(JSONObject row,boolean ru){try{JSONObject d=new JSONObject(row.optString("body"));StringBuilder b=new StringBuilder();
 field(b,d,"senderThreadId",ru?"Отправитель":"Sender");
 field(b,d,"agentPath",ru?"Субагент":"Subagent");field(b,d,"agentThreadId",ru?"Получатель":"Recipient");
 JSONArray ids=d.optJSONArray("receiverThreadIds");if(ids!=null&&ids.length()>0){b.append(ru?"Получатели: ":"Recipients: ");for(int i=0;i<ids.length();i++){if(i>0)b.append(", ");b.append(ids.optString(i));}b.append("\n");}
 field(b,d,"model",ru?"Модель":"Model");field(b,d,"reasoningEffort",ru?"Уровень рассуждений":"Reasoning effort");
 JSONArray participants=d.optJSONArray("agentParticipants");
 if(participants!=null)for(int i=0;i<participants.length();i++){
  JSONObject participant=participants.optJSONObject(i);
  if(participant==null)continue;
  field(b,participant,"id",ru?"Имя":"Name");
  field(b,participant,"type",ru?"Тип":"Type");
  field(b,participant,"role",ru?"Роль":"Role");
 }
 b.append(ru?"Событие: ":"Event: ").append(title(row,ru)).append("\n");
 if(!row.optString("status").isEmpty())b.append(ru?"Состояние события: ":"Event state: ").append(state(row.optString("status"),ru)).append("\n");
 String action=row.optString("toolName");
 if(!d.isNull("prompt")&&!d.optString("prompt").isEmpty()){
  b.append(ru?"\nЗадание / сообщение:\n":"\nTask / message:\n").append(d.optString("prompt")).append("\n");
 }else if(!action.equals("completed")){
  boolean launch=action.equals("started")||action.equals("spawnAgent")||action.equals("followupTask");
  b.append(launch?(ru?"\nТекст задания недоступен\n":"\nTask text unavailable\n")
                 :(ru?"\nТекст сообщения недоступен\n":"\nMessage text unavailable\n"));
 }
 JSONObject states=d.optJSONObject("agentsStates");if(states!=null)for(String key:keys(states)){JSONObject agent=states.optJSONObject(key);if(agent!=null){b.append("\n").append(ru?"Субагент: ":"Subagent: ").append(key).append("\n");if(!agent.isNull("status"))b.append(ru?"Состояние: ":"State: ").append(state(agent.optString("status"),ru)).append("\n");field(b,agent,"message",ru?"Ответ":"Response");}}
 boolean captured=d.has("sourceHistoryAvailable");
 JSONObject histories=d.optJSONObject("agentHistory");if(histories!=null)for(String id:keys(histories)){
  JSONObject h=histories.optJSONObject(id);if(h==null)continue;JSONArray messages=h.optJSONArray("messages");b.append("\n").append(ru?"История субагента: ":"Subagent history: ").append(id).append("\n");
  boolean hasMessages=messages!=null&&messages.length()>0;
  if(!h.optBoolean("relationVerified")||(!hasMessages&&h.optBoolean("unavailable"))){
   b.append(ru?"Не удалось загрузить историю субагента\n":"Could not load subagent history\n");continue;
  }
  if(captured)b.append(ru?"Сообщения, полученные для этого субагента.\n":"Messages captured for this subagent.\n");
  else b.append(ru?"Сообщения связанного агента; соответствие конкретному событию не подтверждено.\n":"Messages from the related agent; correspondence to this specific event is unconfirmed.\n");
  JSONObject status=h.optJSONObject("state");
  if(status!=null&&!(hasMessages&&status.optString("type").equals("notLoaded"))){
   b.append(ru?"Состояние при чтении: ":"State when read: ").append(state(status.optString("type"),ru)).append("\n");
  }
  if(messages!=null)for(int i=0;i<messages.length();i++){JSONObject message=messages.optJSONObject(i);if(message==null)continue;b.append("\n").append(message.optString("role").equals("userMessage")?(ru?"Сообщение / задание":"Message / task"):(ru?"Ответ субагента":"Subagent response")).append(" [").append(message.optString("id")).append("]:\n").append(message.optString("text")).append("\n");}
  if(messages==null||messages.length()==0)b.append(ru?"Текст сообщения недоступен\n":"Message text unavailable\n");
  if(!h.optBoolean("complete"))b.append(ru?"Получена не вся история субагента.\n":"The subagent history is incomplete.\n");
  if(h.optBoolean("refreshUnavailable"))b.append(ru?"Обновить историю не удалось; показаны ранее полученные сообщения.\n":"History could not be refreshed; previously received messages are shown.\n");
 }
 if(captured&&!d.optBoolean("sourceHistoryAvailable"))
  b.append(ru?"\nПолная история субагента недоступна. Показаны сведения из текущего чата.\n":"Full subagent history is unavailable. Showing information captured in this chat.\n");
 if(d.optBoolean("agentHistoryLimited"))b.append("\n").append(ru?"Показана история части получателей.":"History is shown for some recipients only.");
 return b.toString().trim();
 }catch(JSONException e){return ru?"Текст сообщения недоступен":"Message text unavailable";}}
 static java.util.List<String> keys(JSONObject d){java.util.List<String> keys=new java.util.ArrayList<>();java.util.Iterator<String> it=d.keys();while(it.hasNext())keys.add(it.next());java.util.Collections.sort(keys);return keys;}
 private static String state(String value,boolean ru){if(!ru)return value;switch(value){case "completed":return "Завершено";case "inProgress":return "Выполняется";case "running":case "active":return "Работает";case "idle":return "Ожидает";case "failed":case "errored":return "Ошибка";case "interrupted":return "Прервано";case "shutdown":return "Остановлен";case "notFound":return "Не найден";case "notLoaded":return "Не загружен";case "pendingInit":return "Подготовка";default:return value;}}
 private static void field(StringBuilder b,JSONObject d,String key,String label){if(!d.isNull(key)&&!d.optString(key).isEmpty())b.append(label).append(": ").append(d.optString(key)).append("\n");}
}
