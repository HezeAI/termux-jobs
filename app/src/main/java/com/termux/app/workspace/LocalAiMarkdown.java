package com.termux.app.workspace;

import android.content.*;
import android.graphics.Typeface;
import android.text.*;
import android.text.style.*;
import android.view.View;
import java.util.regex.*;

/** Small offline Markdown renderer. No HTML, remote images or executable links. */
final class LocalAiMarkdown {
    static CharSequence render(Context context,String source){
        SpannableStringBuilder out=new SpannableStringBuilder();boolean code=false;StringBuilder block=new StringBuilder();int codeStart=0;
        for(String line:source.split("\n",-1)){
            if(line.startsWith("```")){
                if(code){int end=out.length();out.setSpan(new TypefaceSpan("monospace"),codeStart,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);String payload=block.toString();out.append(WorkspaceLanguage.code(context).equals("ru")?"[Копировать]\n":"[Copy]\n");out.setSpan(new ClickableSpan(){public void onClick(View v){((android.content.ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Code",payload));}},end,out.length()-1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);block.setLength(0);}
                else codeStart=out.length();code=!code;continue;
            }
            if(code){out.append(line).append('\n');block.append(line).append('\n');continue;}
            int heading=0;while(heading<line.length()&&heading<6&&line.charAt(heading)=='#')heading++;
            boolean title=heading>0&&line.length()>heading&&line.charAt(heading)==' ';
            if(title)line=line.substring(heading+1);if(line.startsWith("- ")||line.startsWith("* "))line="• "+line.substring(2);
            int start=out.length();inline(out,line);if(title){out.setSpan(new StyleSpan(Typeface.BOLD),start,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);out.setSpan(new RelativeSizeSpan(1.15f),start,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}out.append('\n');
        }
        if(code&&out.length()>codeStart)out.setSpan(new TypefaceSpan("monospace"),codeStart,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return out;
    }
    private static void inline(SpannableStringBuilder out,String s){
        Pattern p=Pattern.compile("(`[^`\\n]+`)|(\\*\\*[^*\\n]+\\*\\*)|(\\[[^]\\n]+\\]\\(https?://[^)\\s]+\\))");Matcher m=p.matcher(s);int last=0;
        while(m.find()){out.append(s,last,m.start());String v=m.group();int start=out.length();if(v.startsWith("`")){out.append(v,1,v.length()-1);out.setSpan(new TypefaceSpan("monospace"),start,out.length(),33);}else if(v.startsWith("**")){out.append(v,2,v.length()-2);out.setSpan(new StyleSpan(Typeface.BOLD),start,out.length(),33);}else{int cut=v.indexOf("](");out.append(v,1,cut);out.setSpan(new URLSpan(v.substring(cut+2,v.length()-1)),start,out.length(),33);}last=m.end();}out.append(s,last,s.length());
    }
}
