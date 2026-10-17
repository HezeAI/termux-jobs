package com.termux.app.workspace;

import org.json.JSONObject;

/** Presentation only. Embedded examples/prose are NEVER promoted to executable requests. */
final class LocalAiToolText {
    static final class Preview {
        final String text;final JSONObject request;final boolean pending;
        Preview(String text,JSONObject request,boolean pending){this.text=text;this.request=request;this.pending=pending;}
    }
    static Preview preview(String answer){
        if(answer.trim().startsWith("<tool_call>")){try{JSONObject r=LocalAiQwenProtocol.request(answer);if(r!=null)return new Preview("",r,false);}catch(Exception ignored){}if(!answer.trim().endsWith("</tool_call>"))return new Preview("",null,true);}

        try{JSONObject direct=LocalAiTools.request(answer);if(direct!=null)return new Preview("",direct,false);}catch(Exception ignored){}
        // Only tool-shaped objects, not arbitrary user data or Markdown code, are collapsed.
        java.util.regex.Matcher start=java.util.regex.Pattern.compile("\\{\\s*\"tool\"\\s*:").matcher(answer);
        if(!start.find()){
            int at=answer.lastIndexOf('{');if(at>=0){String tail=answer.substring(at).trim();if((tail.equals("{")&&trimFence(answer.substring(0,at)).isEmpty())||tail.matches("\\{\\s*\"t(?:o(?:o(?:l)?)?)?\"?\\s*:?"))return new Preview(trimFence(answer.substring(0,at)),null,true);}
            return new Preview(answer,null,false);
        }
        int from=start.start(),depth=0,end=-1;boolean string=false,escape=false;
        for(int i=from;i<answer.length();i++){char c=answer.charAt(i);if(string){if(escape)escape=false;else if(c=='\\')escape=true;else if(c=='\"')string=false;}else if(c=='\"')string=true;else if(c=='{')depth++;else if(c=='}'&&--depth==0){end=i+1;break;}}
        if(end<0)return new Preview(trimFence(answer.substring(0,from)),null,true);
        try{JSONObject request=LocalAiTools.request(answer.substring(from,end));if(request==null)return new Preview(answer,null,false);
            return new Preview((trimFence(answer.substring(0,from))+"\n"+answer.substring(end).replaceFirst("^\\s*```","")).trim(),request,false);
        }catch(Exception ignored){return new Preview(answer,null,false);}
    }
    private static String trimFence(String text){return text.replaceFirst("(?s)\\s*```(?:json|tool)?\\s*$", "").trim();}
}
