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
        if(stage.startsWith("download-pinned-"))return ru?"Загрузка закреплённого образа":"Downloading pinned image";
        switch(stage){
            case "install-python":return ru?"Подготовка Python":"Preparing Python";
            case "install-proot-distro":return ru?"Подготовка proot-distro":"Preparing proot-distro";
            case "prepare-operation-tools":return ru?"Подготовка инструментов окружения":"Preparing environment tools";
            case "verify":return ru?"Проверка установленной системы":"Checking installed environment";
            case "ubuntu-ai-installer-5.9.0":return ru?"Установщик Ubuntu AI 5.9.0":"Ubuntu AI installer 5.9.0";
            case "download-pinned-ubuntu-ai":return ru?"Скачивание закреплённой Ubuntu AI":"Downloading pinned Ubuntu AI";
            case "check-environment-installer":return ru?"Проверка установщика окружения":"Checking environment installer";
            case "checking-image-cache":return ru?"Проверка сохранённого образа":"Checking cached image";
            case "connecting-image-download":return ru?"Подключение к источнику образа":"Connecting to image source";
            case "reconnecting-image-download":return ru?"Переподключение для продолжения загрузки":"Reconnecting to resume download";
            case "verifying-image-download":case "verified-rootfs-sha256":return ru?"Проверка целостности образа":"Verifying image integrity";
            case "install-environment":return ru?"Подготовка установки окружения":"Preparing environment installation";
            case "extracting-environment":return ru?"Распаковка Ubuntu AI":"Extracting Ubuntu AI";
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
        EnvironmentSetupStatus s=EnvironmentSetupStatus.parse(report);if(!s.error.isEmpty())return s.error;
        for(String id:s.missing)return "MISSING:"+id;
        return "";
    }

    void update(String report,long now){
        line=EnvironmentSetupStatus.parse(report).progress();
        Matcher m=PERCENT.matcher(line);int measured=-1;
        while(m.find()){int n=Integer.parseInt(m.group(1));if(n<=100)measured=n;}
        if(measured<0){percent=-1;eta=-1;firstTime=-1;return;}
        if(firstTime<0||measured<percent){firstTime=now;firstPercent=measured;}
        percent=measured;int delta=measured-firstPercent;
        eta=delta>0&&measured<100?Math.max(1,(now-firstTime)*(100-measured)/delta/1000):-1;
    }
}
