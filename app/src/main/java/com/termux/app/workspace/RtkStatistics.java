package com.termux.app.workspace;

import org.json.JSONObject;
import java.util.Locale;

/** Versioned tool-output bytes; native token estimates remain a separate historical section. */
final class RtkStatistics {
    private RtkStatistics(){}
    static String summary(JSONObject stats,String selected,boolean ru){
        JSONObject value=stats!=null&&stats.optInt("version")==2?stats.optJSONObject(selected.equals("advanced")?"advanced":"basic"):null;
        if(value==null||value.optLong("commands")==0)return ru?"Вывод: пока нет данных":"Output: no data yet";
        boolean known=value.optLong("comparedCommands")>0;
        boolean partial=value.optLong("unknownBaselineCommands")>0||value.optLong("unconfirmedCommands")>0||value.optLong("unknownOutputCommands")>0;
        if(known){long before=value.optLong("inputBytes"),after=value.optLong("outputBytes");
            return (partial?(ru?"Вывод (измеренная часть): ":"Output (measured portion): "):(ru?"Вывод: ":"Output: "))+tokens(before,ru)+" → "+tokens(after,ru)
                    +(ru?" токенов · сокращение ":" tokens · reduction ")
                    +String.format(new Locale(ru?"ru":"en"),"%.1f%%",before==0?0:100d*(before-after)/before);}
        long output=value.optLong("knownOutputBytes",value.optLong("outputBytes"));
        boolean incomplete=value.optLong("unknownOutputCommands")>0||value.optLong("unconfirmedCommands")>0;
        String received=incomplete&&output==0?(ru?"объём неизвестен":"size unknown"):
                (incomplete?(ru?"не менее ":"at least "):"")+tokens(output,ru)+(ru?" токенов":" tokens");
        return (ru?"Получено: ":"Received: ")+received+(ru?" · сокращение неизвестно":" · reduction unknown");
    }
    private static String tokens(long bytes,boolean ru){
        // Display estimate only; measured byte counters remain unchanged.
        java.text.NumberFormat format=java.text.NumberFormat.getIntegerInstance(new Locale(ru?"ru":"en"));
        return "≈"+format.format(Math.round(bytes/4d));
    }
    static String text(JSONObject stats,boolean ru){
        if(stats==null||stats.optInt("version")!=2)return ru?"Сокращение вывода: пока нет новых данных":"Output reduction: no new data yet";
        StringBuilder out=new StringBuilder();
        mode(out,stats.optJSONObject("basic"),"RTK Lite",ru);out.append("\n\n");mode(out,stats.optJSONObject("advanced"),ru?"RTK штатный":"RTK native",ru);
        return out.toString();
    }
    private static String measured(long bytes,long unknown,boolean ru){return unknown>0?(bytes==0?(ru?"неизвестен":"unknown"):(ru?"не менее ":"at least ")+bytes+(ru?" байт":" bytes")):bytes+(ru?" байт":" bytes");}
    private static void mode(StringBuilder out,JSONObject value,String name,boolean ru){
        JSONObject s=value==null?new JSONObject():value;long count=s.optLong("commands"),compared=s.optLong("comparedCommands"),unknown=s.optLong("unknownBaselineCommands"),pending=s.optLong("unconfirmedCommands");
        out.append(name).append('\n').append(ru?"Сокращение вывода: ":"Output reduction: ");
        if(compared>0){long before=s.optLong("inputBytes"),after=s.optLong("outputBytes");out.append(before).append(" → ").append(after).append(ru?" байт · ":" bytes · ").append(String.format(Locale.ROOT,"%.1f%%",before==0?0:100d*(before-after)/before));if(unknown>0||pending>0)out.append(ru?" (только известная база)":" (known baselines only)");}
        else out.append(count==0?(ru?"пока нет данных":"no data yet"):(ru?"база неизвестна":"baseline unknown"));
        if(unknown>0)out.append(ru?"\nБаза неизвестна у команд: ":"\nCommands with unknown baseline: ").append(unknown).append(ru?". Получено всего: ":". Total received: ").append(measured(s.optLong("knownOutputBytes"),s.optLong("unknownOutputCommands"),ru));
        if(s.optLong("unknownOutputCommands")>0)out.append(ru?"\nОбъём результата неизвестен у команд: ":"\nUnknown result size for commands: ").append(s.optLong("unknownOutputCommands"));
        out.append(ru?"\nДополнительно через recall: ":"\nAdditional via recall: ").append(measured(s.optLong("recallBytes"),s.optLong("unknownRecallCommands"),ru)).append(ru?" · обращений: ":" · calls: ").append(s.optLong("recallCommands"));
        if(s.optLong("unknownRecallCommands")>0)out.append(ru?"; объём неизвестен: ":"; size unknown: ").append(s.optLong("unknownRecallCommands"));
        out.append(ru?"\nКоманд обработано: ":"\nCommands processed: ").append(count).append(ru?"; без сокращения: ":"; without reduction: ").append(s.optLong("noReductionCommands"));
        if(pending>0)out.append(ru?"\nПолучение результата не подтверждено: ":"\nResult delivery unconfirmed: ").append(pending);
    }
}
