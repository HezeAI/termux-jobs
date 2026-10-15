package com.termux.app.workspace;

import androidx.media3.common.PlaybackException;
import androidx.media3.datasource.HttpDataSource;
import java.net.*;

/** Reports allowlisted codes/types, never exception messages or capability URLs. */
final class ServerMediaDiagnostics {
    static String describe(PlaybackException error,boolean ru){
        String reason=ru?"Не удалось воспроизвести файл.":"Could not play the file.";String detail="";
        Throwable cause=error;java.util.Set<Throwable> seen=new java.util.HashSet<>();
        for(int depth=0;cause!=null&&depth<10&&seen.add(cause);depth++,cause=cause.getCause()){
            if(cause instanceof HttpDataSource.CleartextNotPermittedException)reason=ru?"Android заблокировал локальный HTTP. Проверьте версию приложения.":"Android blocked local HTTP. Check the app version.";
            else if(cause instanceof HttpDataSource.InvalidResponseCodeException){int code=((HttpDataSource.InvalidResponseCodeException)cause).responseCode;detail="HTTP "+code;reason=code==404?(ru?"Временная ссылка недоступна или истекла. Откройте файл заново.":"Temporary link unavailable or expired. Open the file again."):code==409?(ru?"Файл изменился на сервере. Откройте его заново.":"The server file changed. Open it again."):code==416?(ru?"Сервер отклонил диапазон чтения файла.":"The server rejected the requested byte range."):(ru?"Сервер отклонил чтение файла.":"Server rejected file access.");}
            else if(cause instanceof ConnectException)reason=ru?"Нет соединения с локальным SSH-туннелем. Откройте файл заново.":"Cannot connect to the local SSH tunnel. Open the file again.";
            else if(cause instanceof SocketTimeoutException)reason=ru?"Истекло время чтения. Проверьте сеть и SSH-туннель.":"Read timed out. Check network and SSH tunnel.";
        }
        if(error.errorCode==PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED)reason=ru?"Плеер не распознал контейнер видео/аудио.":"Player could not recognize the media container.";
        if(error.errorCode==PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED||error.errorCode==PlaybackException.ERROR_CODE_DECODER_INIT_FAILED)reason=ru?"Декодер устройства не смог открыть этот формат.":"The device decoder could not open this format.";
        String code=error.getErrorCodeName();if(!code.matches("[A-Z0-9_]{1,100}"))code="UNKNOWN";
        return reason+"\n"+(ru?"Код: ":"Code: ")+code+(detail.isEmpty()?"":" · "+detail)+"\n"+(ru?"Попробуйте внешний плеер или скачайте файл.":"Try an external player or download the file.");
    }
}
