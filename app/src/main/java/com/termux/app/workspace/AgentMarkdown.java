package com.termux.app.workspace;

import android.content.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import io.noties.markwon.*;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.node.*;
import java.util.*;
import java.util.regex.*;

/** Native CommonMark rendering; no WebView, remote image loading or command execution. */
final class AgentMarkdown {
    static Markwon create(Context c){return Markwon.builder(c).usePlugin(StrikethroughPlugin.create()).usePlugin(LinkifyPlugin.create()).usePlugin(new AbstractMarkwonPlugin(){
        @Override public void beforeRender(Node node){node.accept(new AbstractVisitor(){@Override public void visit(Text text){if(text.getParent() instanceof Paragraph&&text.getParent().getParent() instanceof ListItem&&text.getParent().getFirstChild()==text){String value=text.getLiteral();if(value.startsWith("[x] ")||value.startsWith("[X] "))text.setLiteral("☑ "+value.substring(4));else if(value.startsWith("[ ] "))text.setLiteral("☐ "+value.substring(4));}}});}
        @Override public void configureParser(Parser.Builder builder){builder.extensions(Collections.singletonList(TablesExtension.create()));}
        @Override public void configureTheme(MarkwonTheme.Builder b){b.linkColor(0xff79dfcd).blockQuoteColor(0xff79dfcd).blockQuoteWidth(3).codeTextColor(0xffbdddd2).codeBackgroundColor(0xff14252b).codeBlockBackgroundColor(0xff0b131a).headingBreakHeight(0).headingTextSizeMultipliers(new float[]{1.5f,1.35f,1.2f,1.1f,1.05f,1f});}
        @Override public void configureConfiguration(MarkwonConfiguration.Builder b){b.linkResolver((view,url)->openLink(view,url));}
    }).build();}
    static void openLink(View view,String url){
        try{Uri uri=Uri.parse(url);if(!"https".equalsIgnoreCase(uri.getScheme())&&!"http".equalsIgnoreCase(uri.getScheme()))return;Intent i=new Intent(Intent.ACTION_VIEW,uri);if(!(view.getContext() instanceof android.app.Activity))i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);view.getContext().startActivity(i);}catch(ActivityNotFoundException|IllegalArgumentException ignored){}
    }
    static String checkboxes(String source){return source;}
    static boolean rich(String source){return source.contains("```")||source.contains("~~~")||Pattern.compile("(?m)^(?: {4}|\\t|#{1,6}\\s| {0,3}>|.*\\|.*\\n[ |:-]+\\|)").matcher(source).find();}
    static SpannableStringBuilder highlight(String code,String language){
        SpannableStringBuilder s=new SpannableStringBuilder(code);if(code.length()>60000)return s;
        String lang=language.toLowerCase(Locale.ROOT);if(!Arrays.asList("python","py","javascript","js","typescript","ts","java","kotlin","kt","bash","sh","shell","json","c","cpp","rust","rs").contains(lang))return s;
        Set<String> keywords=new HashSet<>(Arrays.asList("from","import","def","class","return","if","else","elif","for","while","try","catch","except","finally","const","let","var","function","public","private","static","void","new","true","false","null","None","True","False","fn","use","val","fun","echo","export","then","fi","do","done"));
        for(int i=0;i<code.length();){int start=i,color=0;char ch=code.charAt(i);
            if(ch=='#'||ch=='/'&&i+1<code.length()&&code.charAt(i+1)=='/'){while(i<code.length()&&code.charAt(i)!='\n')i++;color=0xff718e99;}
            else if(ch=='\"'||ch=='\''){i++;while(i<code.length()){char c=code.charAt(i++);if(c=='\\'&&i<code.length())i++;else if(c==ch)break;}color=0xffdfc08b;}
            else if(Character.isJavaIdentifierStart(ch)){i++;while(i<code.length()&&Character.isJavaIdentifierPart(code.charAt(i)))i++;if(keywords.contains(code.substring(start,i)))color=0xff8bc7e5;}
            else if(Character.isDigit(ch)){i++;while(i<code.length()&&(Character.isDigit(code.charAt(i))||code.charAt(i)=='.'))i++;color=0xffc5a6de;}
            else i++;
            if(color!=0)s.setSpan(new ForegroundColorSpan(color),start,i,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }return s;
    }
}
