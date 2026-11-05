package com.termux.app.workspace;

import org.json.JSONObject;

/** Admission evidence only: neither transport acknowledgement nor a queue proves model uptake. */
final class AgentMessageDelivery {
    final boolean accepted;
    final String state,reason;
    final boolean replayed;
    private final JSONObject evidence;
    private final org.json.JSONArray batch;
    AgentMessageDelivery(JSONObject result){
        evidence=result;batch=result==null?null:result.optJSONArray("receipts");
        reason=result==null?"":result.optString("error");
        accepted=result!=null&&result.optBoolean("ok")&&result.optBoolean("accepted");
        String value=result==null?"":result.optString("delivery");
        state=accepted?(java.util.Arrays.asList("current","queued","started","shell","submitted","api").contains(value)?value:"submitted"):
            result!=null&&value.equals("rejected")?"rejected":"unconfirmed";
        replayed=result!=null&&result.optBoolean("replayed");
    }
    String notice(boolean ru){
        if(batch!=null){StringBuilder details=new StringBuilder(ru?"CLI может объединять несколько вводов в одно сообщение истории. Ниже — подтверждения каждого ID; это не доказывает учёт моделью.":"The CLI may combine inputs into one history message. These are per-ID acknowledgements, not proof of model uptake.");for(int i=0;i<Math.min(200,batch.length());i++){JSONObject entry=batch.optJSONObject(i);if(entry!=null)details.append("\n\n").append(new AgentMessageDelivery(entry).notice(ru));}return details.toString();}
        String result=explanation(ru);
        if(evidence!=null){String id=evidence.optString("clientId"),ack=evidence.optString("ack");if(id.matches("msg_[A-Za-z0-9_-]{1,124}"))result+="\n\nID: "+id;if(ack.matches("[A-Za-z0-9:_-]{1,100}"))result+="\n"+(ru?"Подтверждение: ":"Acknowledgement: ")+ack;}
        return result;
    }
    private boolean claudeQueued(){String ack=evidence==null?"":evidence.optString("ack");return state.equals("queued")&&evidence.optBoolean("queuedWhileBusy")&&(ack.equals("command-lifecycle:queued")||ack.equals("user-replay"));}
    private String explanation(boolean ru){
        if(claudeQueued())return ru?"В очереди — после текущей задачи. Подтверждения включения в текущую задачу пока нет. CLI может включить сообщение при продолжении работы; состояние обновится по отдельным событиям, а не по факту записи в канал.":"Queued — after the current task. Inclusion in the current task is not confirmed. The CLI may include it when work continues; separate events, not a channel write, update this state.";
        if(evidence!=null&&evidence.optString("ack").equals("native-submit-callback"))return ru?"Штатный обработчик Gemini принял ввод. Это не подтверждает запуск запроса или учёт сообщения моделью.":"Gemini's native input handler accepted the input. This does not confirm request execution or model uptake.";
        if(evidence!=null&&evidence.optString("ack").equals("permission-pending"))return ru?"CLI удерживает сообщение до решения о разрешении. Завершите действие в штатном терминале; передача модели ещё не подтверждена.":"The CLI is holding this input pending a permission decision. Complete the native-terminal action; model delivery is not confirmed.";
        if(state.equals("api"))return ru?"API подтвердил приём сообщения. Очередь CLI, текущий запрос и учёт моделью этим не подтверждены.":"The API acknowledged message admission. This does not confirm CLI queueing, current-turn admission or model uptake.";
        if(state.equals("rejected")&&reason.equals("CANCELLED"))return ru?"Сообщение отменено в очереди CLI. Текст и вложения сохранены.":"The message was cancelled in the CLI queue. Text and attachments were kept.";
        if(state.equals("rejected")&&reason.equals("ACTIVE_INPUT_UNSUPPORTED"))return ru?"Эта версия или запуск Claude не подтвердили безопасный активный ввод и отмену очереди. Текст и вложения сохранены. Дождитесь окончания запроса; процесс не перезапускался.":"This Claude version or launch did not confirm safe active input and queue cancellation. Text and attachments were kept. Wait for the request to finish; the process was not restarted.";
        if(state.equals("rejected"))return ru?"Отправка отклонена до передачи агенту. Текст и вложения сохранены; после устранения причины можно повторить.":"Rejected before delivery to the agent. Text and attachments were kept; retry after resolving the cause.";
        if(state.equals("unconfirmed"))return (replayed?(ru?"Повтор вернул сохранённый неподтверждённый результат; новой отправки не было. ":"Retry returned the stored unconfirmed result; nothing was resent. "):"")+(ru?"Доставка не подтверждена. Текст и вложения сохранены. Проверьте терминал и историю. Этот ID зарезервирован: неоднозначно принятый запрос, включая Shell, автоматически не повторяется.":"Delivery unconfirmed. Text and attachments were kept. Check the terminal and history. This ID is reserved: ambiguously admitted requests, including Shell, are not automatically replayed.");
        if(state.equals("queued"))return ru?"CLI принял сообщение в очередь. Оно не обязательно попадёт в текущий запрос; подтверждения учёта моделью нет.":"The CLI admitted the message to its queue. It may not join the current request; model uptake is not confirmed.";
        if(state.equals("current"))return ru?"CLI связал сообщение с запросом. Это не подтверждает, что модель уже учла его.":"The CLI associated the message with a request. This does not confirm that the model has incorporated it.";
        if(state.equals("shell"))return ru?"Запуск Shell подтверждён; завершение команды отслеживается отдельно.":"Shell admission was confirmed; command completion is tracked separately.";
        if(state.equals("started"))return ru?"CLI подтвердил новый запрос. Учёт сообщения моделью этим не подтверждается.":"The CLI acknowledged a new request. This does not confirm model uptake.";
        return ru?"Сообщение передано входному каналу CLI. Подтверждения обработки моделью нет.":"The message was written to the CLI input channel. Model processing is not confirmed.";
    }
    String caption(boolean ru){
        if(claudeQueued())return ru?"В очереди — после текущей задачи ▾":"Queued — after the current task ▾";
        if(batch!=null)return (ru?"Статус доставки · ":"Delivery status · ")+batch.length()+" ▾";
        if(evidence!=null&&evidence.optString("ack").equals("permission-pending"))return ru?"Ожидает разрешения CLI ▾":"Waiting for CLI permission ▾";
        if(state.equals("api"))return ru?"Принято API ▾":"Admitted by API ▾";
        if(state.equals("rejected"))return ru?"Отклонено ▾":"Rejected ▾";
        if(state.equals("unconfirmed"))return ru?"Доставка не подтверждена ▾":"Delivery unconfirmed ▾";
        if(state.equals("current"))return ru?"CLI подтвердил приём ▾":"CLI acknowledged admission ▾";
        if(state.equals("queued"))return ru?"В очереди CLI ▾":"In CLI queue ▾";
        return ru?"Принято CLI ▾":"Admitted by CLI ▾";
    }
}
