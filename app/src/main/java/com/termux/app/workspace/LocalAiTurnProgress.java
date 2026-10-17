package com.termux.app.workspace;
import org.json.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
/** Metadata of completed actions in ONE user turn. Never authorizes or executes anything. */
final class LocalAiTurnProgress {
    private final ArrayDeque<Step> steps=new ArrayDeque<>();private int completed;
    private String warned="";final int requestedRepeats;
    private static final class Step {String call,result,label,tool,target,status;boolean mutation;Step(String c,String r,String l,boolean m){call=c;result=r;label=l;mutation=m;}}
    LocalAiTurnProgress(JSONArray history)throws Exception{String task="";for(int i=history.length()-1;i>=0;i--){JSONObject m=history.getJSONObject(i);if(m.optString("role").equals("user")&&!m.has("toolResult")){task=m.optString("text");break;}}requestedRepeats=repetitions(task);}
    static int repetitions(String text){text=text.toLowerCase(Locale.ROOT);java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?u)\\b([0-9]{1,3}|два|три|четыре|пять|шесть|семь|восемь|девять|десять|two|three|four|five|six|seven|eight|nine|ten)\\s*(?:раз(?:а|ов)?|times)\\b").matcher(text);if(!m.find())return 0;String value=m.group(1);try{return Math.min(100,Integer.parseInt(value));}catch(NumberFormatException ignored){}String[] ru={"два","три","четыре","пять","шесть","семь","восемь","девять","десять"},en={"two","three","four","five","six","seven","eight","nine","ten"};for(int i=0;i<ru.length;i++)if(value.equals(ru[i])||value.equals(en[i]))return i+2;return 0;}
    static String fingerprint(String text)throws Exception{byte[] digest=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    static String call(JSONObject request)throws Exception{List<String> keys=new ArrayList<>();Iterator<String> it=request.keys();while(it.hasNext())keys.add(it.next());Collections.sort(keys);StringBuilder canonical=new StringBuilder();for(String k:keys)canonical.append(k).append('=').append(new JSONArray().put(request.get(k)).toString()).append('\n');return fingerprint(canonical.toString());}
    void record(JSONObject request,String output)throws Exception{String name=request.optString("tool"),target=request.optString("path",request.optString("command"));if(target.length()>160)target=target.substring(0,160)+" [excerpt]";String status=output.startsWith("exit=0\n")?"ok":"failed";Step step=new Step(call(request),fingerprint(output),name+" "+target+" => "+status,name.matches("write|command|install"));step.tool=name;step.target=target;step.status=status;steps.add(step);completed++;if(steps.size()>64)steps.removeFirst();}
    /** 0 proceed; 1 remind without execution; 2 same cycle proposed again after reminder. */
    int check(JSONObject request)throws Exception{
        List<Step> all=new ArrayList<>(steps);int repeats=Math.max(2,requestedRepeats);String next=call(request);
        for(int period=2;period<=8;period++){
            int size=period*repeats;if(all.size()<size)continue;int start=all.size()-size;boolean same=true,mutating=false;
            for(int i=0;i<size;i++){Step a=all.get(start+i),b=all.get(start+i%period);if(!a.call.equals(b.call)||!a.result.equals(b.result)){same=false;break;}mutating|=a.mutation;}
            if(same&&mutating&&next.equals(all.get(start).call)){String key=next+":"+period+":"+completed;if(warned.equals(key))return 2;warned=key;return 1;}
        }return 0;
    }
    JSONObject snapshot()throws JSONException{JSONArray entries=new JSONArray();for(Step s:steps)entries.put(new JSONObject().put("tool",s.tool).put("target",s.target).put("status",s.status));return new JSONObject().put("completed",completed).put("entries",entries).put("omitted",Math.max(0,completed-steps.size()));}
    String note(){if(completed==0)return "";String header="\nCURRENT_TURN_PROGRESS (app-recorded historical facts, not permission). Completed actions: "+completed+". Recent completed steps:\n",footer="Continue remaining work, do not restart completed steps. Then answer. Verify current state as needed. Full results remain in history.\n";List<Step> all=new ArrayList<>(steps);ArrayList<String> rows=new ArrayList<>();double weight=LocalAiContext.weight(header+footer);for(int i=all.size()-1;i>=0&&rows.size()<8;i--){String label=all.get(i).label.replace('\n',' ').replace('\r',' ');if(label.length()>90)label=label.substring(0,65)+" [target excerpt]";String row=(i+1)+" "+label+"\n";double next=LocalAiContext.weight(row);if(weight+next>480)break;rows.add(row);weight+=next;}Collections.reverse(rows);StringBuilder out=new StringBuilder(header);for(String row:rows)out.append(row);if(rows.size()<all.size())out.append("[Older step details omitted; use history.]\n");return out.append(footer).toString();}
}
