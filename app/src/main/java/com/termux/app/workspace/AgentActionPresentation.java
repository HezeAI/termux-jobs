package com.termux.app.workspace;

import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;

/** UI labels derived from native action metadata, never from shell execution or guessed outcomes. */
final class AgentActionPresentation {
    private AgentActionPresentation(){}
    static String webUrl(String value){if(value==null||value.length()>4096)return "";for(int i=0;i<value.length();i++)if(Character.isWhitespace(value.charAt(i))||Character.isISOControl(value.charAt(i)))return "";try{Uri uri=Uri.parse(value);return ("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))&&uri.getHost()!=null&&uri.getUserInfo()==null?value:"";}catch(Exception invalid){return "";}}
    static JSONObject preview(JSONObject row){
        JSONObject ready=row.optJSONObject("actionPreview");if(ready!=null)return ready;
        // Old bridges/history already contain structured native events. Never parse shell text as an action.
        String type=row.optString("toolType");try{
            if(type.equals("fileChange")){JSONArray files=new JSONArray(row.optString("body"));return new JSONObject().put("kind","files").put("count",files.length()).put("files",files);}
            if(type.equals("imageView")){JSONObject event=new JSONObject(row.optString("body"));String path=event.optString("path");if(path.startsWith("/")&&path.length()<=4096&&!path.matches("(?s).*[\\x00-\\x1f].*")&&path.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(png|jpe?g|webp|gif|bmp|avif|heic|heif)$"))return new JSONObject().put("kind","image").put("image",new JSONObject().put("type","image").put("path",path).put("name",new java.io.File(path).getName()).put("cacheKey",row.optString("key")));}
            if(type.equals("webSearch")){JSONObject event=new JSONObject(row.optString("body")),a=event.optJSONObject("action");if(a==null)a=new JSONObject();String action=a.optString("type");String operation=action.equals("openPage")?"open":action.equals("findInPage")?"find":"search";return new JSONObject().put("kind","web").put("operation",operation).put("query",a.optString("query",event.optString("query"))).put("url",webUrl(a.optString("url"))).put("pattern",a.optString("pattern")).put("title",event.optString("title"));}
        }catch(org.json.JSONException malformed){}return null;
    }
    static String title(JSONObject row,boolean ru){
        JSONObject p=preview(row);String kind=p==null?"":p.optString("kind");
        if(kind.equals("files")){
            int n=p.optInt("count");if(n<=0)return ru?"Работа с файлами":"File operations";
            if(CodexActionTimeline.status(row).equals("completed")&&!CodexActionTimeline.failed(row)){
                if(!ru)return "Changed "+n+(n==1?" file":" files");
                return n%10==1&&n%100!=11?"Изменён "+n+" файл":"Изменено "+n+(n%10>=2&&n%10<=4&&(n%100<12||n%100>14)?" файла":" файлов");
            }
            return (ru?"Изменение файлов: ":"Changing files: ")+n;
        }
        if(kind.equals("web")){
            String op=p.optString("operation"),q=p.optString("query"),url=p.optString("url"),site=p.optString("title");
            if(site.isEmpty()&&!url.isEmpty())try{site=Uri.parse(url).getHost();}catch(Exception ignored){}
            if(op.equals("open"))return (ru?"Чтение страницы":"Read page")+(site==null||site.isEmpty()?"":": "+site);
            if(op.equals("find"))return (ru?"Поиск на странице":"Find on page")+(p.optString("pattern").isEmpty()?"":": "+p.optString("pattern"));
            return (ru?"Поиск в интернете":"Search the web")+(q.isEmpty()?"":": "+q);
        }
        return "";
    }
    static String state(JSONObject row,boolean ru){
        String s=CodexActionTimeline.status(row),value;
        if(s.equals("attached"))value=ru?"Инструкции переданы агенту":"Instructions supplied to the agent";
        else if(s.equals("declined"))value=ru?"Отклонено":"Declined";
        else if(s.equals("aborted")||s.equals("cancelled")||s.equals("canceled"))value=ru?"Прервано":"Interrupted";
        else if(CodexActionTimeline.failed(row))value=ru?"Ошибка":"Failed";
        else if(CodexActionTimeline.running(row))value=ru?"Выполняется…":"Running…";
        else if(s.equals("awaiting_approval"))value=ru?"Ожидает разрешения":"Awaiting approval";
        else if(s.equals("pending")||s.equals("scheduled"))value=ru?"В очереди":"Queued";
        else if(s.equals("completed"))value=ru?"Готово":"Completed";
        else value=ru?"Статус неизвестен":"Status unknown";
        if(row.has("exitCode")&&!row.isNull("exitCode"))value+=(ru?" · код ":" · exit ")+row.optInt("exitCode");
        if(row.has("durationMs")&&!row.isNull("durationMs")&&row.optLong("durationMs",-1)>=0){long ms=row.optLong("durationMs");value+=" · "+(ms<1000?ms+(ru?" мс":" ms"):String.format(java.util.Locale.ROOT,"%.1f",ms/1000d).replace(".",ru?",":".")+(ru?" с":" s"));}
        return value;
    }
    static String fileOperation(JSONObject file,boolean ru){
        String op=file.optString("operation");JSONObject k=file.optJSONObject("kind");if(op.isEmpty())op=k==null?file.optString("kind"):k.optString("type");
        switch(op){case "created":case "add":return ru?"создан":"created";case "deleted":case "delete":return ru?"удалён":"deleted";case "renamed":case "move":case "rename":return ru?"переименован":"renamed";case "modified":case "update":return ru?"изменён":"modified";default:return ru?"изменение":"changed";}
    }
}
