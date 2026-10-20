package com.termux.app.workspace;

import java.util.Locale;

/** Presentation only: preserve native error details and never retry a model/tool action. */
final class AgentChatErrors {
    private AgentChatErrors() {}
    static String explain(String raw, boolean ru) {
        String s=raw.toLowerCase(Locale.ROOT);
        if(s.contains("user location is not supported")||s.contains("unsupported_country")||s.contains("unsupported region"))
            return ru?"Сервис недоступен в текущем регионе.\nПроверьте доступность сервиса для вашего подключения. Повтор без изменения подключения не устранит этот отказ.":"The service is unavailable in your current region.\nCheck service availability for your connection. Retrying with the same connection will not resolve this refusal.";
        if(s.contains("история выбранного чата пока недоступна")||s.contains("thread not loaded")||s.contains("not materialized"))
            return ru?"Выбранный чат ещё не сохранён или недоступен.\nОткройте настройки сессии и выберите «Новый чат» либо другой сохранённый разговор. Существующая история автоматически не удаляется.":"The selected chat has not been saved yet or is unavailable.\nOpen session settings and choose New chat or another saved conversation. Existing history is not deleted automatically.";
        if((s.contains("model")&&(s.contains("not found")||s.contains("not_found")||s.contains("does not exist")||s.contains("is invalid")))||s.contains("model_not_found"))
            return ru?"Выбранная модель не найдена или недоступна.\nВыберите доступную модель в верхней карточке и повторите запрос.":"The selected model was not found or is unavailable.\nChoose an available model in the top card and send your request again.";
        if(s.contains("insufficient_quota")||s.contains("billing")||s.contains("credit balance"))
            return ru?"Запрос отклонён из-за квоты или оплаты.\nПроверьте лимиты и оплату у провайдера перед повтором.":"The request was rejected because of quota or billing.\nCheck your provider's limits and billing before retrying.";
        if(s.contains("429")||s.contains("rate limit")||s.contains("resource_exhausted")||s.contains("quota exceeded"))
            return ru?"Достигнут лимит запросов сервиса.\nПодождите восстановления лимита или выберите другую доступную модель, затем повторите запрос.":"The service's request limit was reached.\nWait for the limit to reset or select another available model, then retry.";
        if(s.contains("401")||s.contains("unauthenticated")||s.contains("invalid api key")||s.contains("api key not valid"))
            return ru?"Сервис не принял авторизацию.\nПроверьте вход или API-ключ в настройках сессии, затем повторите запрос.":"The service rejected authentication.\nCheck your sign-in or API key in session settings, then retry.";
        if(s.contains("403")||s.contains("permission_denied"))
            return ru?"Сервис отказал в доступе.\nПроверьте права аккаунта, ключа и доступность выбранной модели.":"The service denied access.\nCheck account/key permissions and availability of the selected model.";
        if(s.contains("context_length")||s.contains("context window")||s.contains("too many tokens"))
            return ru?"Запрос превышает доступный контекст модели.\nСократите сообщение или начните новый чат, сохранив нужную информацию.":"The request exceeds the model's context window.\nShorten the message or start a new chat with the information you need.";
        if(s.contains("503")||s.contains("overloaded")||s.contains("at capacity"))
            return ru?"Сервис временно перегружен.\nПодождите и повторите запрос либо выберите другую доступную модель.":"The service is temporarily overloaded.\nWait and retry, or choose another available model.";
        if(s.contains("timeout")||s.contains("timed out")||s.contains("econn")||s.contains("fetch failed")||s.contains("network"))
            return ru?"Не удалось получить ответ от сервиса.\nПроверьте интернет-соединение. Перед повтором действия проверьте, не было ли оно уже выполнено.":"Could not obtain a response from the service.\nCheck your internet connection. Before retrying an action, check whether it already ran.";
        return ru?"Агент сообщил об ошибке.\nОткройте подробности, чтобы узнать причину. Перед повтором действия проверьте, не было ли оно уже выполнено.":"The agent reported an error.\nOpen details to see the cause. Before retrying an action, check whether it already ran.";
    }
    static boolean nativeDiagnosticHint(String body){return body.startsWith("This request failed.")&&body.contains("Press F12")&&body.contains("Error Verbosity");}
}
