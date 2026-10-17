package com.termux.app.workspace;

import org.json.*;
import java.util.Locale;
import java.util.regex.*;

/** Conservative short requests only. Discovery and reads still use normal authorized tools. */
final class LocalAiRecent {
    final String folder, extension, filter; final boolean read;
    LocalAiRecent(String folder,String extension,String filter,boolean read){this.folder=folder;this.extension=extension;this.filter=filter;this.read=read;}
    static LocalAiRecent parse(JSONArray history){
        String text="";for(int i=history.length()-1;i>=0;i--){JSONObject m=history.optJSONObject(i);if(m!=null&&m.optString("role").equals("user")&&!m.has("toolResult")){text=m.optString("text");break;}}
        text=text.toLowerCase(Locale.ROOT).trim().replaceFirst(" (?:на телефоне|на устройстве|on (?:my |the )?(?:phone|device))[. ]*$", "");
        // Do not reinterpret explicit paths, filenames, questions, negations or compound tasks.
        if(text.contains("/")||text.contains("\\")||text.contains("?")||text.contains("\""))return null;
        text=text.replaceFirst("^(?:привет|hello|hi)[!,. ]+", "").replaceFirst("^(?:хорошо|окей|okay|ok)[, ]+", "");
        text=text.replaceAll("(?u)\\b(?:пожалуйста|please)\\b", " ").replaceAll("[,!]", " ").replaceAll("\\s+", " ").trim();
        boolean combined=text.matches("(?u)^(?:найди|find) .* (?:и прочитай|and read(?: it)?)[. ]*$");
        if(combined)text=text.replaceFirst("^(?:найди|find) ","read ").replaceFirst(" (?:и прочитай|and read(?: it)?)[. ]*$","");
        Matcher m=Pattern.compile("(?u)^(?:(посмотри|покажи|прочитай|найди|открой|show|read|find|open|inspect)(?: me)? )?(?:the )?(?:последн[а-яё]*|ласт|last|latest) (?:файл |file )?(тхт|txt|md|markdown|json|csv|xml|yaml|yml|ini|conf|pdf|zip|mp4|mp3|webp|log|лог|apk|апк|png|jpg|jpeg|изображение|image|фото|photo|видео|video)(?: файл| file)?(?: (?:в|из|in|from) (?:the )?(довнолоад|доунлоад|даунлоад|загрузк[а-яё]*|downloads?|документ[а-яё]*|documents|картинк[а-яё]*|pictures|музык[а-яё]*|music|movies))?[. ]*$").matcher(text);
        if(!m.matches())return null;
        String kind=m.group(2),folder=m.group(3)==null?"":m.group(3),extension=kind,filter="all";
        if(kind.equals("тхт"))extension="txt";if(kind.equals("markdown"))extension="md";if(kind.equals("лог"))extension="log";if(kind.equals("апк"))extension="apk";
        if(kind.matches("изображение|image|фото|photo")){extension="";filter="image";}else if(kind.matches("видео|video")){extension="";filter="video";}
        if(folder.matches("довнолоад|доунлоад|даунлоад|загрузк[а-яё]*|downloads?"))folder="Download";
        else if(folder.matches("документ[а-яё]*|documents"))folder="Documents";else if(folder.matches("картинк[а-яё]*|pictures"))folder="Pictures";else if(folder.matches("музык[а-яё]*|music"))folder="Music";else if(!folder.isEmpty())folder="Movies";
        boolean read=m.group(1)!=null&&m.group(1).matches("посмотри|покажи|прочитай|открой|show|read|open|inspect")&&extension.matches("txt|md|json|csv|xml|yaml|yml|ini|conf|log");
        return new LocalAiRecent(folder,extension,filter,read);
    }
    JSONObject listing(String locations)throws JSONException{
        String path="/sdcard/"+folder;
        if(locations.startsWith("exit=0\n"))for(String line:locations.substring(7).split("\n")){if(!line.startsWith("AVAILABLE\t"))continue;String candidate=line.substring(10);if(candidate.endsWith("/"+folder)||folder.equals("Download")&&candidate.endsWith("/Downloads")){path=candidate;break;}}
        JSONObject request=new JSONObject().put("tool","list").put("path",path).put("sort","modified_desc").put("filter",filter).put("limit",1);
        if(!extension.isEmpty())request.put("extension",extension);return request;
    }
    JSONObject reading(String result)throws JSONException{
        if(!read)return null;JSONObject data=LocalAiSearchResult.parse(result);if(data==null)return null;JSONArray files=data.optJSONArray("entries");if(files==null||files.length()!=1)return null;
        JSONObject file=files.getJSONObject(0);String path=file.optString("path");if(!file.optString("type").equals("file")||!path.startsWith("/")||!path.toLowerCase(Locale.ROOT).endsWith("."+extension))return null;
        return new JSONObject().put("tool","read").put("path",path);
    }
}
