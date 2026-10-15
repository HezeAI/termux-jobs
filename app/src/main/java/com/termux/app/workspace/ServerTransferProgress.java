package com.termux.app.workspace;

/** Presentation only: average throughput for the current file, no network probes. */
final class ServerTransferProgress {
    static String describe(long bytes,long total,long elapsedMs,boolean ru){
        String text=ServerFileInfo.size(bytes,ru)+(total<0?"":" / "+ServerFileInfo.size(total,ru));
        if(bytes<=0||elapsedMs<500)return text;
        long rate=(long)(bytes*1000.0/elapsedMs);if(rate<=0)return text;
        text+=" · "+ServerFileInfo.size(rate,ru)+(ru?"/с":"/s");
        if(total>bytes){long seconds=(long)Math.ceil((total-bytes)/(double)rate);text+=(ru?" · осталось ≈ ":" · remaining ≈ ")+(seconds<60?seconds+(ru?" с":" s"):(seconds+59)/60+(ru?" мин":" min"));}
        return text;
    }
}
