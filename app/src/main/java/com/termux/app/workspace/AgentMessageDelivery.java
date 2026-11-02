package com.termux.app.workspace;

import org.json.JSONObject;

/** Admission evidence only: neither transport acknowledgement nor a queue proves model uptake. */
final class AgentMessageDelivery {
    final boolean accepted;
    final String state,reason;
    final boolean replayed;
    AgentMessageDelivery(JSONObject result){
        reason=result==null?"":result.optString("error");
        accepted=result!=null&&result.optBoolean("ok")&&result.optBoolean("accepted");
        String value=result==null?"":result.optString("delivery");
        state=accepted?(java.util.Arrays.asList("current","queued","started","shell","submitted").contains(value)?value:"submitted"):
            result!=null&&value.equals("rejected")?"rejected":"unconfirmed";
        replayed=result!=null&&result.optBoolean("replayed");
    }
    String notice(boolean ru){
        if(state.equals("rejected")&&reason.equals("CANCELLED"))return ru?"Сообщение отменено в очереди CLI. Текст и вложения сохранены.":"The message was cancelled in the CLI queue. Text and attachments were kept.";
        if(state.equals("rejected")&&reason.equals("ACTIVE_INPUT_UNSUPPORTED"))return ru?"Эта версия или запуск Claude не подтвердили безопасный активный ввод и отмену очереди. Текст и вложения сохранены. Дождитесь окончания запроса; процесс не перезапускался.":"This Claude version or launch did not confirm safe active input and queue cancellation. Text and attachments were kept. Wait for the request to finish; the process was not restarted.";
        if(state.equals("rejected"))return ru?"Отправка отклонена до передачи агенту. Текст и вложения сохранены; после устранения причины можно повторить.":"Rejected before delivery to the agent. Text and attachments were kept; retry after resolving the cause.";
        if(state.equals("unconfirmed"))return (replayed?(ru?"Повтор вернул сохранённый неподтверждённый результат; новой отправки не было. ":"Retry returned the stored unconfirmed result; nothing was resent. "):"")+(ru?"Доставка не подтверждена. Текст и вложения сохранены. Проверьте терминал и историю. Этот ID зарезервирован: неоднозначно принятый запрос, включая Shell, автоматически не повторяется.":"Delivery unconfirmed. Text and attachments were kept. Check the terminal and history. This ID is reserved: ambiguously admitted requests, including Shell, are not automatically replayed.");
        if(state.equals("queued"))return ru?"CLI принял сообщение в очередь. Оно не обязательно попадёт в текущий запрос; подтверждения учёта моделью нет.":"The CLI admitted the message to its queue. It may not join the current request; model uptake is not confirmed.";
        if(state.equals("current"))return ru?"CLI подтвердил уточнение для текущего запроса. Это не подтверждает, что модель уже учла его.":"The CLI acknowledged steering for the current request. This does not confirm that the model has incorporated it.";
        if(state.equals("shell"))return ru?"Запуск Shell подтверждён; завершение команды отслеживается отдельно.":"Shell admission was confirmed; command completion is tracked separately.";
        if(state.equals("started"))return ru?"CLI подтвердил новый запрос. Учёт сообщения моделью этим не подтверждается.":"The CLI acknowledged a new request. This does not confirm model uptake.";
        return ru?"Сообщение передано входному каналу CLI. Подтверждения обработки моделью нет.":"The message was written to the CLI input channel. Model processing is not confirmed.";
    }
    String caption(boolean ru){
        if(state.equals("current"))return ru?"CLI принял уточнение ▾":"CLI admitted clarification ▾";
        if(state.equals("queued"))return ru?"В очереди CLI ▾":"In CLI queue ▾";
        return ru?"Принято CLI ▾":"Admitted by CLI ▾";
    }
}
