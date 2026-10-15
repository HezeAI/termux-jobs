package com.termux.app.workspace;

import org.json.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/** Presentation and fixed dependency commands; file/server text is never shell syntax. */
final class ServerFileInfo {
    private ServerFileInfo(){}
    static boolean media(String path){String name=path.toLowerCase(Locale.ROOT);int dot=name.lastIndexOf('.');return dot>=0&&Arrays.asList("mp4","mkv","webm","avi","mov","m4v","mpeg","mpg","m2ts","3gp","mp3","m4a","aac","flac","ogg","opus","wav","wma","aiff").contains(name.substring(dot+1));}
    static String size(long value,boolean ru){if(value<0)return "—";String[] units=ru?new String[]{"Б","КБ","МБ","ГБ","ТБ"}:new String[]{"B","KB","MB","GB","TB"};if(value<1024)return value+" "+units[0];double size=value;int i=0;while(size>=1024&&i<units.length-1){size/=1024;i++;}return String.format(ru?new Locale("ru"):Locale.US,"%.1f %s",size,units[i]);}
    static String installCommand(boolean root){String prefix=root?"env DEBIAN_FRONTEND=noninteractive ":"sudo -n env DEBIAN_FRONTEND=noninteractive ";return prefix+"apt-get update && "+prefix+"apt-get install --no-install-recommends -y ffmpeg";}
    static String properties(JSONObject data,boolean ru,TimeZone zone){
        String path=data.optString("path");StringBuilder out=new StringBuilder();out.append(ru?"Имя: ":"Name: ").append(new File(path).getName()).append('\n');out.append(ru?"Путь: ":"Path: ").append(path).append('\n');out.append(ru?"Размер: ":"Size: ").append(size(data.optLong("size",-1),ru)).append('\n');
        out.append(ru?"Изменён: ":"Modified: ");double seconds=data.optDouble("modified",Double.NaN);if(Double.isFinite(seconds)&&seconds>=0&&seconds<253402300800.0){SimpleDateFormat format=new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z",ru?new Locale("ru"):Locale.US);format.setTimeZone(zone);out.append(format.format(new Date((long)(seconds*1000))));}else out.append('—');
        if(media(path)){
            JSONObject info=data.optJSONObject("media");
            if(info!=null){JSONObject format=info.optJSONObject("format");double duration=format==null?Double.NaN:format.optDouble("duration",Double.NaN);if(Double.isFinite(duration)&&duration>=0){long s=(long)duration;out.append('\n').append(ru?"Длительность: ":"Duration: ").append(String.format(Locale.ROOT,"%02d:%02d:%02d",s/3600,s/60%60,s%60));}
                JSONArray streams=info.optJSONArray("streams");if(streams!=null)for(int i=0;i<Math.min(streams.length(),20);i++){JSONObject stream=streams.optJSONObject(i);if(stream==null)continue;out.append('\n').append(ru?"Дорожка ":"Stream ").append(i+1).append(": ").append(stream.optString("codec_name","—"));if(stream.optInt("width")>0&&stream.optInt("height")>0)out.append(" · ").append(stream.optInt("width")).append('×').append(stream.optInt("height"));}
            }else if(data.optJSONObject("components")!=null&&!data.optJSONObject("components").optBoolean("ffprobe"))out.append("\n\n").append(ru?"Для длительности, разрешения и кодеков установите FFmpeg на сервер. Сам просмотр видео не требует ffprobe.":"Install FFmpeg on the server for duration, resolution and codecs. Video streaming itself does not require ffprobe.");
            else out.append("\n\n").append(ru?"Медиаданные не прочитаны: формат может не поддерживаться или файл недоступен для анализа.":"Media details unavailable: unsupported format or analysis access failure.");
        }
        return out.toString();
    }
}
