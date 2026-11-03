package com.termux.app.workspace;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Finite protocol state. Unknown installer bytes never become a report or UI text. */
final class EnvironmentSetupStatus {
    private static final Set<String> COMPONENTS=new HashSet<>(Arrays.asList("environment","shell","proot-distro","python3"));
    private static final Set<String> STAGES=new HashSet<>(Arrays.asList("extracting-environment","install-python","install-proot-distro","prepare-operation-tools","check-environment-installer","install-environment","verify","ubuntu-ai-installer-5.9.0","checking-image-cache","connecting-image-download","reconnecting-image-download","verifying-image-download","verified-rootfs-sha256","prepare-archlinux-keyring","prepare-manjaro-keyring"));
    private static final Set<String> TARGETS=new HashSet<>(Arrays.asList("ubuntu-ai","ubuntu","debian","alpine","archlinux","fedora","opensuse","void","kali","manjaro"));
    private static final Set<String> ERRORS=new HashSet<>(Arrays.asList("SETUP_FAILED","INSTALL_CANCELLED","INCOMPLETE_ROOTFS_REQUIRES_REMOVAL","UNSUPPORTED_DISTRIBUTION","PKG_REQUIRED","NATIVE_LOCKS_REQUIRED","PROOT_REQUIRED","IMAGE_ARM64_REQUIRED","NATIVE_ARCHIVE_INSTALL_REQUIRED","PYTHON3_REQUIRED","ROOTFS_IMAGE_REQUIRED","ROOTFS_DOWNLOAD_FAILED","INVALID_ALIAS","INVALID_CONTROL_PATH","NONREAPING_WAIT_REQUIRED"));
    private static final Pattern IMAGE=Pattern.compile("IMAGE_PROGRESS: [0-9]{1,3}% \\(([0-9]+)/([0-9]+) bytes\\)");
    final Set<String> ready=new LinkedHashSet<>(),missing=new LinkedHashSet<>();
    String stage="",image="",error="";boolean complete;
    void record(String line){
        if(line.equals("CHECK:done")){complete=true;return;}
        for(String prefix:new String[]{"READY:","MISSING:"})if(line.startsWith(prefix)){
            String id=line.substring(prefix.length());if(COMPONENTS.contains(id))(prefix.equals("READY:")?ready:missing).add(id);return;
        }
        if(line.startsWith("STAGE:")){
            String id=line.substring(6);
            if(STAGES.contains(id)||id.startsWith("download-pinned-")&&TARGETS.contains(id.substring(16))){stage=line;image="";}return;
        }
        if(line.startsWith("ERROR:")){
            if(ERRORS.contains(line.substring(6))||!AgentPreparationOutput.error(line).isEmpty())error=line;return;
        }
        Matcher m=IMAGE.matcher(line);if(m.matches())try{
            long done=Long.parseLong(m.group(1)),total=Long.parseLong(m.group(2));
            if(done>=0&&total>0&&done<=total&&total<=2147483648L)image="IMAGE_PROGRESS: "+(100*done/total)+"% ("+done+"/"+total+" bytes)";
        }catch(NumberFormatException ignored){}
    }
    String snapshot(){
        StringBuilder out=new StringBuilder();for(String id:ready)out.append("READY:").append(id).append('\n');for(String id:missing)out.append("MISSING:").append(id).append('\n');
        if(!stage.isEmpty())out.append(stage).append('\n');if(!image.isEmpty())out.append(image).append('\n');if(!error.isEmpty())out.append(error).append('\n');if(complete)out.append("CHECK:done\n");return out.toString();
    }
    String progress(){return image.isEmpty()?stage:image;}
    static EnvironmentSetupStatus parse(String text){EnvironmentSetupStatus s=new EnvironmentSetupStatus();try{AgentPreparationOutput.drain(new StringReader(text+"\n"),s::record);}catch(IOException impossible){s.error="ERROR:SETUP_FAILED";}return s;}
    static String description(String report,boolean ru){
        EnvironmentSetupStatus s=parse(report);String e=s.error;
        switch(e){
            case "ERROR:INSTALL_CANCELLED":return ru?"Установка остановлена.":"Installation stopped.";
            case "ERROR:INCOMPLETE_ROOTFS_REQUIRES_REMOVAL":return ru?"Найдена неполная распаковка. Существующее окружение не заменялось.":"Incomplete extraction found. Existing environment was not replaced.";
            case "ERROR:IMAGE_ARM64_REQUIRED":return ru?"Для образа требуется ARM64.":"This image requires ARM64.";
            case "ERROR:UNSUPPORTED_DISTRIBUTION":return ru?"Выбранная система не поддерживается установщиком.":"The installer does not support this distribution.";
            default:if(!e.isEmpty())return AgentPreparationOutput.description(e.substring(6),ru);
        }
        if(s.missing.contains("shell"))return ru?"Папка системы найдена, но оболочка отсутствует.":"System directory found, but its shell is missing.";
        if(s.missing.contains("environment"))return ru?"Установленная система не найдена.":"Installed system not found.";
        if(s.missing.contains("proot-distro"))return ru?"Не найден proot-distro.":"proot-distro not found.";
        return ru?"Проверка или установка не завершена. Повторите действие.":"Check or installation did not finish. Retry the action.";
    }
}
