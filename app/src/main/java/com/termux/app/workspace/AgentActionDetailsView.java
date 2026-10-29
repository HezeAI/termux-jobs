package com.termux.app.workspace;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.MotionEvent;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.function.*;

/** One live action window. Reuses the chat's scoped attachment loader; never runs the action. */
final class AgentActionDetailsView extends LinearLayout {
    final ScrollView scroll;
    final TextView status,body,more;
    private final AgentChatView owner;
    private final LinearLayout content,preview;
    private final BiConsumer<JSONObject,ImageView> thumbnail;
    private final Consumer<JSONObject> openImage;
    private final Set<String> expanded=new HashSet<>();
    private JSONObject row;
    private String previewStamp="",nativeText="";
    private boolean fullText,retainText,touching;
    private int interaction;
    AgentActionDetailsView(AgentChatView owner,JSONObject row,BiConsumer<JSONObject,ImageView> thumbnail,Consumer<JSONObject> openImage){
        super(owner.getContext());this.owner=owner;this.thumbnail=thumbnail;this.openImage=openImage;setOrientation(VERTICAL);
        status=owner.label("",13);status.setTag("agent-action-status");addView(status,new LayoutParams(-1,-2));
        scroll=new ScrollView(getContext()){
            @Override protected void onMeasure(int widthSpec,int heightSpec){
                if(compactPreview()){int cap=Math.max(owner.dp(100),Math.round(getResources().getDisplayMetrics().heightPixels*.55f));int mode=View.MeasureSpec.getMode(heightSpec);if(mode!=View.MeasureSpec.UNSPECIFIED)cap=Math.min(cap,View.MeasureSpec.getSize(heightSpec));heightSpec=View.MeasureSpec.makeMeasureSpec(cap,View.MeasureSpec.AT_MOST);}
                super.onMeasure(widthSpec,heightSpec);
            }
        };scroll.setFillViewport(false);content=new LinearLayout(getContext());content.setOrientation(VERTICAL);scroll.addView(content);
        scroll.setOnTouchListener((v,event)->{interaction++;int a=event.getActionMasked();if(a==MotionEvent.ACTION_DOWN)touching=true;else if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)touching=false;return false;});
        preview=new LinearLayout(getContext());preview.setOrientation(VERTICAL);content.addView(preview,new LayoutParams(-1,-2));
        body=owner.label("",13);body.setTag("agent-action-output");body.setTypeface(Typeface.MONOSPACE);body.setTextIsSelectable(true);content.addView(body,new LayoutParams(-1,-2));
        addView(scroll,new LayoutParams(-1,0,1));more=owner.pill(t("Полный вывод","Full output"),()->{});addView(more);update(row);
    }
    private String t(String ru,String en){return owner.tr(ru,en);}
    String key(){return row.optString("key");}
    boolean compactPreview(){if(row==null)return false;JSONObject p=AgentActionPresentation.preview(row);return p!=null&&(imagePreview()||p.optString("kind").equals("files"));}
    boolean imagePreview(){JSONObject p=AgentActionPresentation.preview(row);return p!=null&&p.optString("kind").equals("image")&&p.optJSONObject("image")!=null;}
    boolean followsOutput(){return !touching&&body.getSelectionStart()==body.getSelectionEnd()&&content.getHeight()-scroll.getHeight()-scroll.getScrollY()<owner.dp(48);}
    private boolean ru(){return WorkspaceLanguage.code(getContext()).equals("ru");}
    void update(JSONObject next){
        boolean following=followsOutput();
        int y=scroll.getScrollY(),heldInteraction=interaction;String previous=row==null?"":row.optString("body")+row.optLong("bodyLength");row=next;
        String state=AgentActionPresentation.state(row,ru());if(!status.getText().toString().equals(state))status.setText(state);status.setTextColor(CodexActionTimeline.failed(row)?0xffffb08f:0xff79dfcd);
        JSONObject p=AgentActionPresentation.preview(row);String stamp=String.valueOf(p);if(!previewStamp.equals(stamp)){previewStamp=stamp;renderPreview(p);}
        LayoutParams viewport=(LayoutParams)scroll.getLayoutParams();int height=compactPreview()?LayoutParams.WRAP_CONTENT:0;float weight=compactPreview()?0:1;
        if(viewport.height!=height||viewport.weight!=weight){viewport.height=height;viewport.weight=weight;scroll.setLayoutParams(viewport);}
        String current=row.optString("body")+row.optLong("bodyLength");if(!previous.equals(current))fullText=false;
        if(!retainText&&body.getSelectionStart()==body.getSelectionEnd()&&!nativeText.equals(row.optString("body"))){nativeText=row.optString("body");showBody(p);}
        if(nativeText.isEmpty()){nativeText=row.optString("body");showBody(p);}
        more.setVisibility(row.optBoolean("more")&&!fullText?VISIBLE:GONE);
        scroll.post(()->{if(getParent()==null||touching||interaction!=heldInteraction)return;if(following)scroll.fullScroll(View.FOCUS_DOWN);else scroll.scrollTo(0,y);});
    }
    void nativeText(String value,boolean complete){boolean follow=followsOutput();int y=scroll.getScrollY();nativeText=value;fullText=complete;retainText=true;showBody(AgentActionPresentation.preview(row));if(follow)scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));else scroll.post(()->scroll.scrollTo(0,y));}
    String nativeText(){return nativeText;}
    private void showBody(JSONObject p){
        String kind=p==null?"":p.optString("kind");
        if(kind.equals("files")){
            if(fullText)try{renderFiles(new JSONArray(nativeText),false);body.setVisibility(GONE);return;}catch(JSONException ignored){body.setVisibility(VISIBLE);body.setText(nativeText);return;}
            body.setVisibility(GONE);return;
        }
        if(kind.equals("fileTool")&&row.has("toolOutputLength")){
            int length=Math.max(0,row.optInt("toolOutputLength")),offset=Math.min(nativeText.length(),Math.max(0,row.optInt("toolOutputOffset")));String value=nativeText.substring(offset,offset+Math.min(length,nativeText.length()-offset));
            if(value.isEmpty()&&CodexActionTimeline.running(row))value=t("Инструмент ещё не передал вывод.","The tool has not produced output yet.");
            body.setVisibility(value.isEmpty()?GONE:VISIBLE);if(!body.getText().toString().equals(value))body.setText(value);return;
        }
        boolean structured=kind.equals("image")&&p.optJSONObject("image")!=null||kind.equals("fileTool")&&!p.optString("path").isEmpty()
            ||kind.equals("web")&&(!p.optString("query").isEmpty()||!p.optString("url").isEmpty()||!p.optString("pattern").isEmpty()||p.optJSONArray("results")!=null&&p.optJSONArray("results").length()>0);
        if(structured){body.setVisibility(fullText?VISIBLE:GONE);}
        else body.setVisibility(VISIBLE);
        String value=nativeText.isEmpty()&&CodexActionTimeline.running(row)?t("Инструмент ещё не передал вывод.","The tool has not produced output yet."):nativeText;
        if(!body.getText().toString().equals(value))body.setText(value);
    }
    private void renderPreview(JSONObject p){
        preview.removeAllViews();if(p==null)return;String kind=p.optString("kind");
        if(kind.equals("image")){
            JSONObject source=p.optJSONObject("image");if(source==null)return;JSONObject image;try{image=new JSONObject(source.toString());if(image.optString("cacheKey").isEmpty())image.put("cacheKey",row.optString("key"));}catch(JSONException invalid){return;}final JSONObject selected=image;
            TextView name=owner.label(image.optString("name"),13);preview.addView(name);
            ImageView photo=new ImageView(getContext());photo.setTag("agent-action-image");photo.setScaleType(ImageView.ScaleType.FIT_CENTER);photo.setBackgroundColor(0xff111917);photo.setContentDescription(t("Открыть просматриваемое изображение","Open the viewed image"));preview.addView(photo,new LayoutParams(-1,owner.dp(180)));photo.setOnClickListener(v->openImage.accept(selected));name.setOnClickListener(v->openImage.accept(selected));thumbnail.accept(selected,photo);
            preview.addView(owner.label(t("Нажмите на миниатюру, чтобы увеличить","Tap the thumbnail to zoom"),11));
        }else if(kind.equals("files")){JSONArray files=p.optJSONArray("files");if(files!=null)renderFiles(files,p.optBoolean("filesTruncated"));}
        else if(kind.equals("fileTool")){TextView file=owner.label(p.optString("path"),13);file.setTextIsSelectable(true);preview.addView(file);}
        else if(kind.equals("web")){
            if(!p.optString("query").isEmpty())preview.addView(owner.label(t("Запрос: ","Query: ")+p.optString("query"),14));
            if(!p.optString("pattern").isEmpty())preview.addView(owner.label(t("Искомый текст: ","Find: ")+p.optString("pattern"),14));
            link(p.optString("title"),p.optString("url"));JSONArray results=p.optJSONArray("results");if(results!=null)for(int i=0;i<Math.min(10,results.length());i++){JSONObject result=results.optJSONObject(i);if(result!=null)link(result.optString("title"),result.optString("url"));}
        }
    }
    private void link(String title,String value){String url=AgentActionPresentation.webUrl(value);if(url.isEmpty())return;TextView link=owner.label((title.isEmpty()?"":title+"\n")+url,13);link.setTextColor(0xff79dfcd);link.setOnClickListener(v->AgentMarkdown.openLink(v,url));preview.addView(link);}
    private void renderFiles(JSONArray files,boolean truncated){
        preview.removeAllViews();for(int i=0;i<Math.min(100,files.length());i++){
            JSONObject f=files.optJSONObject(i);if(f==null)continue;String path=f.optString("path");if(path.isEmpty())continue;String id=i+":"+path,diff=f.optString("diff");
            LinearLayout card=new LinearLayout(getContext());card.setOrientation(VERTICAL);GradientDrawable outline=new GradientDrawable();outline.setColor(0xff0d171b);outline.setCornerRadius(owner.dp(8));outline.setStroke(owner.dp(1),0xff28433d);card.setBackground(outline);
            LayoutParams lp=new LayoutParams(-1,-2);lp.setMargins(owner.dp(6),owner.dp(4),owner.dp(6),owner.dp(4));preview.addView(card,lp);
            String caption=path+"\n"+AgentActionPresentation.fileOperation(f,ru());if(f.has("added")&&f.has("removed"))caption+=" · +"+f.optInt("added")+" / −"+f.optInt("removed");
            else if(!diff.isEmpty()){int added=0,removed=0;for(String line:diff.split("\n")){if(line.startsWith("+")&&!line.startsWith("+++"))added++;if(line.startsWith("-")&&!line.startsWith("---"))removed++;}if(added+removed>0)caption+=" · +"+added+" / −"+removed;}
            TextView label=owner.label(caption+" ▾",13);label.setTag("action-file:"+path);card.addView(label);
            TextView changes=owner.label(diff.isEmpty()?t("Инструмент не передал разницу строк.","The tool did not provide a line diff."):diff,12);changes.setTypeface(Typeface.MONOSPACE);changes.setTextIsSelectable(true);
            if(!diff.isEmpty()){SpannableStringBuilder colored=new SpannableStringBuilder(diff);int start=0;for(String line:diff.split("\n",-1)){int end=start+line.length();if(line.startsWith("+")&&!line.startsWith("+++"))colored.setSpan(new ForegroundColorSpan(0xff79df9c),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);else if(line.startsWith("-")&&!line.startsWith("---"))colored.setSpan(new ForegroundColorSpan(0xffff9b9b),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);start=end+1;}changes.setText(colored);}
            if(f.optBoolean("diffTruncated"))changes.append(t("\n\nПоказана часть изменений. Нажмите «Полный вывод» ниже.","\n\nPartial diff. Tap Full output below."));
            changes.setVisibility(expanded.contains(id)?VISIBLE:GONE);card.addView(changes);label.setOnClickListener(v->{if(!expanded.remove(id))expanded.add(id);changes.setVisibility(expanded.contains(id)?VISIBLE:GONE);});
        }if(truncated)preview.addView(owner.label(t("Показана часть файлов. Полный список доступен в полном выводе.","Partial file list. The full list is available in Full output."),12));
    }
}
