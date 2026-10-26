package com.termux.app.workspace;

import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import io.noties.markwon.Markwon;
import org.commonmark.node.*;
import org.commonmark.ext.gfm.tables.*;
import java.util.*;

/** Reuses stable block views while the final Markdown block streams. */
final class AgentMarkdownView extends LinearLayout {
    private final Markwon markdown;
    private final ArrayList<String> signatures=new ArrayList<>();
    private final ArrayList<View> blocks=new ArrayList<>();
    private final TextView author;
    AgentMarkdownView(Context c){this(c,AgentMarkdown.create(c));}
    AgentMarkdownView(Context c,Markwon renderer){super(c);setOrientation(VERTICAL);markdown=renderer;author=text(12);author.setTextColor(0xff79dfcd);addView(author);}
    private int dp(int n){return WorkspaceDialogs.dp(getContext(),n);}
    private TextView text(int size){TextView v=new TextView(getContext());v.setTextColor(0xffd5e4e0);v.setTextSize(size);v.setPadding(dp(10),dp(6),dp(10),dp(6));return v;}
    private GradientDrawable background(){GradientDrawable b=new GradientDrawable();b.setColor(0xff0b131a);b.setCornerRadius(dp(12));b.setStroke(dp(1),0xff30454e);return b;}
    void update(String name,String source,Runnable more){
        author.setText(name);String value=AgentMarkdown.checkboxes(source);
        Node document=markdown.parse(value);List<Node> nodes=new ArrayList<>();for(Node n=document.getFirstChild();n!=null;n=n.getNext())nodes.add(n);
        for(int i=0;i<nodes.size();i++){
            Node n=nodes.get(i);String signature=signature(n);View old=i<blocks.size()?blocks.get(i):null;
            if(old!=null&&signatures.get(i).equals(signature))continue;
            View v;
            if(n instanceof FencedCodeBlock||n instanceof IndentedCodeBlock){CodeBlock code=old instanceof CodeBlock?(CodeBlock)old:new CodeBlock();String language=n instanceof FencedCodeBlock?((FencedCodeBlock)n).getInfo():"";String literal=n instanceof FencedCodeBlock?((FencedCodeBlock)n).getLiteral():((IndentedCodeBlock)n).getLiteral();code.update(language,literal);v=code;}
            else if(n instanceof TableBlock)v=table((TableBlock)n);
            else {TextView t=old instanceof TextView?(TextView)old:text(14);t.setTextIsSelectable(true);markdown.setParsedMarkdown(t,markdown.render(n));v=t;}
            if(old!=v){if(old!=null){removeView(old);blocks.set(i,v);signatures.set(i,signature);}else{blocks.add(v);signatures.add(signature);}LayoutParams p=new LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(6));addView(v,i+1,p);}else signatures.set(i,signature);
        }
        while(blocks.size()>nodes.size()){int i=blocks.size()-1;removeView(blocks.remove(i));signatures.remove(i);}
        View existing=findViewWithTag("markdown-more");if(existing!=null)removeView(existing);
        if(more!=null){TextView button=text(12);button.setText(WorkspaceLanguage.code(getContext()).equals("ru")?"Нажмите для полного текста…":"Tap for full text…");button.setTextColor(0xff79dfcd);button.setTag("markdown-more");button.setOnClickListener(v->more.run());addView(button);}
    }
    private static String signature(Node n){StringBuilder b=new StringBuilder();signature(n,b);return b.toString();}
    private static void signature(Node n,StringBuilder b){b.append(n.getClass().getName()).append(':');if(n instanceof Text)b.append(((Text)n).getLiteral());if(n instanceof Code)b.append(((Code)n).getLiteral());if(n instanceof FencedCodeBlock)b.append(((FencedCodeBlock)n).getInfo()).append('\0').append(((FencedCodeBlock)n).getLiteral());if(n instanceof IndentedCodeBlock)b.append(((IndentedCodeBlock)n).getLiteral());if(n instanceof Heading)b.append(((Heading)n).getLevel());if(n instanceof TableCell)b.append(((TableCell)n).isHeader()).append(((TableCell)n).getAlignment());if(n instanceof Link)b.append(((Link)n).getDestination());if(n instanceof OrderedList)b.append(((OrderedList)n).getStartNumber());for(Node c=n.getFirstChild();c!=null;c=c.getNext())signature(c,b);b.append(';');}
    private View table(TableBlock node){
        HorizontalScrollView scroll=new HorizontalScrollView(getContext());scroll.setFillViewport(true);scroll.setBackground(background());TableLayout table=new TableLayout(getContext());table.setPadding(dp(4),dp(4),dp(4),dp(4));
        int count=0;boolean truncated=false;for(Node section=node.getFirstChild();section!=null;section=section.getNext())for(Node row=section.getFirstChild();row!=null;row=row.getNext(),count++){
            if(count>=200){truncated=true;break;}
            android.widget.TableRow line=new android.widget.TableRow(getContext());for(Node cell=row.getFirstChild();cell!=null;cell=cell.getNext()){
                TextView value=text(13);value.setTextIsSelectable(true);markdown.setParsedMarkdown(value,markdown.render(cell));if(cell instanceof TableCell&&((TableCell)cell).isHeader()){value.setTextColor(0xffa1decf);value.setTypeface(null,Typeface.BOLD);}
                if(cell instanceof TableCell){TableCell.Alignment alignment=((TableCell)cell).getAlignment();if(alignment==TableCell.Alignment.RIGHT)value.setGravity(Gravity.END);else if(alignment==TableCell.Alignment.CENTER)value.setGravity(Gravity.CENTER_HORIZONTAL);}
                value.setMinWidth(dp(120));value.setMaxWidth(dp(300));value.setPadding(dp(12),dp(10),dp(12),dp(10));line.addView(value,new android.widget.TableRow.LayoutParams(-2,-2));
            }table.addView(line);View divider=new View(getContext());divider.setBackgroundColor(0xff273a42);table.addView(divider,new TableLayout.LayoutParams(-1,dp(1)));
        }if(truncated){TextView hint=text(12);hint.setText(WorkspaceLanguage.code(getContext()).equals("ru")?"Показаны первые 200 строк таблицы.":"Showing the first 200 table rows.");table.addView(hint);}scroll.addView(table,new HorizontalScrollView.LayoutParams(-2,-2));return scroll;
    }
    private final class CodeBlock extends LinearLayout {
        private final TextView language,copy,body,numbers,expand;
        private final HorizontalScrollView viewport;
        private String literal="";private boolean expanded;
        CodeBlock(){super(AgentMarkdownView.this.getContext());setOrientation(VERTICAL);setBackground(background());LinearLayout header=new LinearLayout(getContext());header.setGravity(Gravity.CENTER_VERTICAL);language=text(11);language.setTextColor(0xff88a8b8);language.setTypeface(Typeface.MONOSPACE);copy=text(11);copy.setTextColor(0xff9de4d6);copy.setText(WorkspaceLanguage.code(getContext()).equals("ru")?"Копировать":"Copy");copy.setContentDescription(copy.getText());copy.setTag("markdown-copy");copy.setGravity(Gravity.CENTER);copy.setFocusable(true);copy.setBackground(WorkspaceDialogs.background(getContext(),false));copy.setOnClickListener(v->{android.content.ClipboardManager c=(android.content.ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);c.setPrimaryClip(ClipData.newPlainText("code",literal));});header.addView(language,new LayoutParams(0,-2,1));LayoutParams copyParams=new LayoutParams(-2,dp(36));copyParams.setMargins(0,dp(4),dp(8),dp(4));header.addView(copy,copyParams);addView(header);
            viewport=new HorizontalScrollView(getContext());LinearLayout line=new LinearLayout(getContext());numbers=text(12);numbers.setTextColor(0xff67828d);numbers.setTypeface(Typeface.MONOSPACE);body=text(12);body.setTypeface(Typeface.MONOSPACE);body.setTextIsSelectable(true);body.setTag("markdown-code");body.setHorizontallyScrolling(true);line.addView(numbers);line.addView(body);viewport.addView(line);addView(viewport);expand=text(11);expand.setTextColor(0xff79dfcd);expand.setTag("markdown-expand");expand.setOnClickListener(v->{expanded=!expanded;applyLines();});addView(expand);
        }
        void update(String info,String code){literal=code;String lang=info.trim();if(lang.length()>80)lang=lang.substring(0,80);language.setText(lang.isEmpty()?"code":lang);String grammar=lang.split("\\s+",2)[0];body.setText(AgentMarkdown.highlight(code,grammar));StringBuilder lines=new StringBuilder();int total=code.split("\n",-1).length-(code.endsWith("\n")?1:0);for(int i=1;i<=Math.max(1,total);i++){if(i>1)lines.append('\n');lines.append(i);}numbers.setText(lines);applyLines();}
        private void applyLines(){int lines=literal.split("\n",-1).length;boolean longCode=lines>14;body.setMaxLines(longCode&&!expanded?12:Integer.MAX_VALUE);numbers.setMaxLines(longCode&&!expanded?12:Integer.MAX_VALUE);expand.setVisibility(longCode?VISIBLE:GONE);expand.setText(WorkspaceLanguage.code(getContext()).equals("ru")?(expanded?"Свернуть ↑":"Показать весь код ↓"):(expanded?"Collapse ↑":"Show all code ↓"));}
    }
}
