package com.termux.app.workspace;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Fixed protocol only. Arbitrary child output is consumed, never retained as a report. */
final class AgentPreparationOutput {
    private static final int MAX_RECORD=256;
    private static final Set<String> STAGES=new HashSet<>(Arrays.asList(
        "checking-ubuntu","installing-ubuntu","failed-ubuntu","checking-components",
        "installing-components","checking-cli","failed-components","failed-cli","ready"));
    private static final Set<String> IMAGE_STAGES=new HashSet<>(Arrays.asList(
        "ubuntu-ai-installer-5.9.0","download-pinned-ubuntu-ai","download-pinned-ubuntu",
        "check-environment-installer","checking-image-cache","connecting-image-download",
        "reconnecting-image-download","verifying-image-download","verified-rootfs-sha256","install-environment"));
    private static final Set<String> ERRORS=new HashSet<>(Arrays.asList(
        "INSTALL_CANCELLED","IMAGE_CHECKSUM_MISMATCH","IMAGE_DOWNLOAD_NETWORK",
        "IMAGE_DOWNLOAD_FAILED","IMAGE_HTTP_STATUS","IMAGE_RANGE_MISMATCH","IMAGE_SIZE_LIMIT",
        "IMAGE_INSECURE_REDIRECT","IMAGE_INSECURE_RESPONSE","IMAGE_UNSAFE_FILE",
        "IMAGE_UNSAFE_LOCK","IMAGE_DOWNLOAD_BUSY","IMAGE_CACHE_DIRECTORY","IMAGE_CACHE_CONTROL",
        "IMAGE_CATALOG","IMAGE_CATALOG_VERSION","IMAGE_CATALOG_TARGET","IMAGE_CATALOG_PIN",
        "IMAGE_ARGUMENTS","IMAGE_TARGET","IMAGE_SETUP_FAILED"));
    static String stage(String line){
        if(line.startsWith("PREPARATION:")&&STAGES.contains(line.substring(12)))return line.substring(12);
        if(line.startsWith("STAGE:")&&IMAGE_STAGES.contains(line.substring(6)))return line;
        return "";
    }
    static String error(String line){
        if(line.startsWith("ERROR:")&&ERRORS.contains(line.substring(6)))return line.substring(6);
        return "";
    }
    /** Keep reading even after oversized/unknown lines. No final partial record is accepted. */
    static void drain(Reader reader,Consumer<String> records)throws IOException{
        char[] buffer=new char[2048],line=new char[MAX_RECORD];int used=0,count;boolean discard=false;
        try{while((count=reader.read(buffer))!=-1)for(int i=0;i<count;i++){
            char c=buffer[i];
            if(c=='\r'||c=='\n'){
                if(!discard&&used>0)records.accept(new String(line,0,used));
                java.util.Arrays.fill(line,'\0');used=0;discard=false;
            }else if(!discard){if(used<line.length)line[used++]=c;else{discard=true;java.util.Arrays.fill(line,'\0');used=0;}}
        }
        }finally{java.util.Arrays.fill(buffer,'\0');java.util.Arrays.fill(line,'\0');}
    }
    static String description(String code,boolean ru){
        switch(code){
            case "INSTALL_CANCELLED":return ru?"Подготовка прервана. Повторите проверку.":"Preparation interrupted. Retry the check.";
            case "IMAGE_DOWNLOAD_NETWORK":case "IMAGE_DOWNLOAD_FAILED":case "IMAGE_HTTP_STATUS":
                return ru?"Не удалось скачать образ. Проверьте сеть и повторите проверку.":"Image download failed. Check the network and retry.";
            case "IMAGE_CHECKSUM_MISMATCH":case "IMAGE_RANGE_MISMATCH":case "IMAGE_SIZE_LIMIT":
                return ru?"Образ не прошёл проверку целостности.":"Image integrity check failed.";
            case "IMAGE_DOWNLOAD_BUSY":return ru?"Образ занят другой загрузкой. Повторите позже.":"Another image download is active. Retry later.";
            case "EXISTING_ENVIRONMENT_CHECK_FAILED":return ru?"Не удалось проверить существующую среду. Она не заменялась.":"Existing environment check failed. It was not replaced.";
            case "COMPONENT_CHECK_FAILED":return ru?"Компоненты не прошли итоговую проверку.":"Final component check failed.";
            case "PREPARATION_OUTPUT_READ_FAILED":return ru?"Не удалось прочитать состояние процесса подготовки.":"Could not read preparation process status.";
            case "PREPARATION_EXIT_FAILED":return ru?"Процесс подготовки завершился ошибкой.":"Preparation process exited with an error.";
            default:return ru?"Подготовка не завершена. Повторите проверку среды.":"Preparation did not complete. Retry the environment check.";
        }
    }
}
