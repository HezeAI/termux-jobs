package com.termux.app.workspace;

import java.util.regex.*;

/** Measured progress of the current installer phase; never invent a total ETA. */
final class EnvironmentInstallProgress {
    int percent=-1; long eta=-1;
    String line="";
    private long firstTime=-1;private int firstPercent;
    private static final Pattern PERCENT=Pattern.compile("(?<![0-9])([0-9]{1,3})(?:\\.[0-9]+)?\\s*%");
    private static final Pattern IMAGE=Pattern.compile("IMAGE_PROGRESS: [0-9]{1,3}% \\(([0-9]+)/([0-9]+) bytes\\)");
    String displayLine(boolean ru){
        String stage=line.startsWith("STAGE:")?line.substring(6):"";
        switch(stage){
            case "check-environment-installer":return ru?"Проверка установщика окружения":"Checking environment installer";
            case "checking-image-cache":return ru?"Проверка сохранённого образа":"Checking cached image";
            case "connecting-image-download":return ru?"Подключение к источнику образа":"Connecting to image source";
            case "reconnecting-image-download":return ru?"Переподключение для продолжения загрузки":"Reconnecting to resume download";
            case "verifying-image-download":case "verified-rootfs-sha256":return ru?"Проверка целостности образа":"Verifying image integrity";
            case "install-environment":return ru?"Подготовка и распаковка окружения":"Preparing and extracting environment";
            case "prepare-manjaro-keyring":return ru?"Подготовка ключей пакетов Manjaro":"Preparing Manjaro package keys";
            case "prepare-archlinux-keyring":return ru?"Подготовка ключей пакетов Arch Linux":"Preparing Arch Linux package keys";
            default:break;
        }
        Matcher image=IMAGE.matcher(line);
        if(image.matches())try{
            long done=Long.parseLong(image.group(1)),total=Long.parseLong(image.group(2));
            if(done>=0&&total>0&&done<=total&&total<=2147483648L)
                return (ru?"Загрузка образа: ":"Downloading image: ")+String.format(java.util.Locale.ROOT,"%.1f / %.1f MB",done/1e6,total/1e6);
        }catch(NumberFormatException ignored){}
        return line;
    }
    static String failureLine(String report){
        String[] lines=report.split("[\\r\\n]+");
        for(String value:lines){String line=value.trim();if(line.startsWith("ERROR:")||line.matches("(?i).*(?:error:|failed to install:|no match for argument|timed? out|connection reset|could not resolve|cannot download).*"))return line;}
        for(String value:lines){String line=value.trim();if(line.startsWith("MISSING:"))return line;}
        for(int i=lines.length-1;i>=0;i--){String line=lines[i].trim();if(!line.isEmpty()&&!line.equals("CHECK:done")&&!line.startsWith("READY:")&&!line.startsWith("STAGE:"))return line;}
        return "";
    }
    void update(String report,long now){
        line=DesktopProgramProgress.lastLine(report);
        Matcher m=PERCENT.matcher(line);int measured=-1;
        while(m.find()){int n=Integer.parseInt(m.group(1));if(n<=100)measured=n;}
        if(measured<0){percent=-1;eta=-1;firstTime=-1;return;}
        if(firstTime<0||measured<percent){firstTime=now;firstPercent=measured;}
        percent=measured;int delta=measured-firstPercent;
        eta=delta>0&&measured<100?Math.max(1,(now-firstTime)*(100-measured)/delta/1000):-1;
    }
}
