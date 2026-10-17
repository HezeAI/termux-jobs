package com.termux.app.workspace;

/** Presentation only: raw answers, including thinking, remain in history. */
final class LocalAiText {
    static final class Parts {final String answer,thinking;final boolean pending;Parts(String a,String t,boolean p){answer=a;thinking=t;pending=p;}}
    static Parts split(String raw){
        StringBuilder a=new StringBuilder(),t=new StringBuilder();boolean inside=false;int pos=0;
        while(pos<raw.length()){
            String tag=inside?"</think>":"<think>";int next=raw.indexOf(tag,pos);
            if(next<0){String tail=raw.substring(pos);int partial=0;for(int n=1;n<tag.length()&&n<=tail.length();n++)if(tag.startsWith(tail.substring(tail.length()-n)))partial=n;
                (inside?t:a).append(tail.substring(0,tail.length()-partial));break;}
            (inside?t:a).append(raw,pos,next);if(inside)t.append('\n');inside=!inside;pos=next+tag.length();
        }
        return new Parts(a.toString().trim(),t.toString().trim(),inside);
    }
}
