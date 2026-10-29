package com.termux.app.workspace;

import android.app.Activity;
import android.app.Application;
import android.os.Looper;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentActionDetailsTest {
    private Activity activity(){Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceLanguage.select(a,"ru");return a;}
    private AgentChatView chat(Activity a){return new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});}
    private JSONObject row(String body,String state)throws Exception{return new JSONObject().put("key","tool").put("kind","tool").put("message","m").put("part","command").put("actionType","shell").put("toolType","Bash").put("title","Shell · "+state).put("body",body).put("bodyLength",body.length()).put("status",state);}
    private AgentActionDetailsView panel(Activity a,JSONObject row){return new AgentActionDetailsView(chat(a),row,(j,v)->{},j->{});}
    private void layout(View v){for(int i=0;i<3;i++){v.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));v.layout(0,0,500,600);Shadows.shadowOf(Looper.getMainLooper()).idle();}}
    @Test public void runningActionOpensBeforeCompletionAndKeepsTheSameEntry(){
        try{Activity a=activity();AtomicReference<JSONObject> opened=new AtomicReference<>();AgentChatView c=new AgentChatView(a,opened::set,()->{},()->{},()->{},()->{});c.actionCards(true);
            JSONObject running=row("/bin/bash -lc test\nfirst output","inProgress");c.rows(Collections.singletonList(running),false);CodexActionsView timeline=(CodexActionsView)c.timeline.getChildAt(1);assertEquals(View.VISIBLE,timeline.steps.getVisibility());TextView entry=(TextView)timeline.steps.getChildAt(0);assertNotNull(entry.getBackground());assertFalse(entry.getText().toString().startsWith("✓"));entry.performClick();assertSame(running,opened.get());
            JSONObject done=row("/bin/bash -lc test\nfirst output\nfinished","completed");c.rows(Collections.singletonList(done),false);assertSame(entry,timeline.steps.getChildAt(0));assertTrue(entry.getText().toString().startsWith("✓"));entry.performClick();assertSame(done,opened.get());
        }catch(Exception e){throw new AssertionError(e);}
    }
    @Test public void openOutputUpdatesInPlaceAndReportsFailureWithoutAFakeSuccess()throws Exception {
        Activity a=activity();AgentActionDetailsView p=panel(a,row("command\nfirst","inProgress"));TextView original=p.body;assertFalse(p.imagePreview());assertEquals(1f,((LinearLayout.LayoutParams)p.scroll.getLayoutParams()).weight,0f);assertTrue(p.status.getText().toString().contains("Выполняется"));p.update(row("command\nfirst\nsecond","inProgress"));assertSame(original,p.body);assertTrue(p.body.getText().toString().contains("second"));p.update(row("command\nfailed","completed").put("exitCode",2));assertTrue(p.status.getText().toString().contains("Ошибка"));assertFalse(p.status.getText().toString().contains("Готово"));
    }
    @Test public void selectingTextDefersReplacementAndCatchesUpWhenSelectionEnds()throws Exception {
        Activity a=activity();AgentActionDetailsView p=panel(a,row("first output","inProgress"));Selection.setSelection((Spannable)p.body.getText(),0,4);JSONObject next=row("first output\nsecond output","inProgress");p.update(next);assertEquals("first output",p.body.getText().toString());Selection.removeSelection((Spannable)p.body.getText());p.update(next);assertTrue(p.body.getText().toString().contains("second output"));
    }
    @Test public void scrollingUpPreservesReaderPositionWhileOutputGrows()throws Exception {
        Activity a=activity();StringBuilder lines=new StringBuilder();for(int i=0;i<180;i++)lines.append("line ").append(i).append('\n');AgentActionDetailsView p=panel(a,row(lines.toString(),"inProgress"));a.setContentView(p);layout(p);p.scroll.scrollTo(0,400);int y=p.scroll.getScrollY();assertTrue(y>0);assertFalse(p.followsOutput());p.update(row(lines+"new output\n","inProgress"));layout(p);assertEquals(y,p.scroll.getScrollY());
    }
    @Test public void viewedImageUsesTheExactActionPathAndOpensFromItsThumbnail()throws Exception {
        Activity a=activity();AtomicReference<JSONObject> loaded=new AtomicReference<>(),opened=new AtomicReference<>();AtomicReference<ImageView> image=new AtomicReference<>();JSONObject r=row("image event","completed").put("actionPreview",new JSONObject().put("kind","image").put("image",new JSONObject().put("type","image").put("path","/sdcard/Pictures/exact.jpg").put("name","exact.jpg")));
        AgentActionDetailsView p=new AgentActionDetailsView(chat(a),r,(j,v)->{loaded.set(j);image.set(v);},opened::set);assertTrue(p.imagePreview());assertEquals(LinearLayout.LayoutParams.WRAP_CONTENT,p.scroll.getLayoutParams().height);assertEquals(0f,((LinearLayout.LayoutParams)p.scroll.getLayoutParams()).weight,0f);assertEquals("/sdcard/Pictures/exact.jpg",loaded.get().getString("path"));assertEquals("tool",loaded.get().getString("cacheKey"));image.get().performClick();assertSame(loaded.get(),opened.get());p.update(r);assertSame(image.get(),p.findViewWithTag("agent-action-image"));
    }
    @Test public void filesHaveFriendlyNamesExpandableColoredDiffsAndBilingualTitles()throws Exception {
        Activity a=activity();JSONArray files=new JSONArray().put(new JSONObject().put("path","main.py").put("operation","modified").put("added",1).put("removed",1).put("diff","-old\n+new\n"));JSONObject r=row("files","inProgress").put("toolType","fileChange").put("actionPreview",new JSONObject().put("kind","files").put("count",1).put("files",files));AgentActionDetailsView p=panel(a,r);
        assertTrue(p.compactPreview());assertEquals(LinearLayout.LayoutParams.WRAP_CONTENT,p.scroll.getLayoutParams().height);TextView label=p.findViewWithTag("action-file:main.py");assertNotNull(label);LinearLayout card=(LinearLayout)label.getParent();TextView diff=(TextView)card.getChildAt(1);assertEquals(View.GONE,diff.getVisibility());label.performClick();assertEquals(View.VISIBLE,diff.getVisibility());assertEquals(2,((Spanned)diff.getText()).getSpans(0,diff.length(),ForegroundColorSpan.class).length);
        r.put("status","completed");p.update(r);assertEquals(View.VISIBLE,diff.getVisibility());assertEquals("Изменён 1 файл",CodexActionLabels.title(r,true));assertEquals("Changed 1 file",CodexActionLabels.title(r,false));assertEquals("File operations",CodexActionLabels.title(new JSONObject().put("title","Files"),false));
    }
    @Test public void webMetadataAndOldImageEventsRemainReadableWithoutFetchingPages()throws Exception {
        JSONObject web=row("web event","completed").put("actionPreview",new JSONObject().put("kind","web").put("operation","open").put("url","https://example.org/docs").put("title","Documentation"));assertEquals("Чтение страницы: Documentation",CodexActionLabels.title(web,true));assertEquals("Read page: Documentation",CodexActionLabels.title(web,false));
        JSONObject old=row("{\"type\":\"imageView\",\"path\":\"/sdcard/Pictures/a.png\"}","completed").put("toolType","imageView");assertEquals("/sdcard/Pictures/a.png",AgentActionPresentation.preview(old).getJSONObject("image").getString("path"));old.put("body","{\"path\":\"/private/key.txt\"}");assertNull(AgentActionPresentation.preview(old));
    }
    @Test public void fileToolsShowActualOutputWithProviderOffsetsAndEmptyRunningHint()throws Exception {
        Activity a=activity();JSONObject r=row("read /project/a\n\nactual result","completed").put("toolOutputOffset",17).put("toolOutputLength",13).put("actionPreview",new JSONObject().put("kind","fileTool").put("path","/project/a"));
        AgentActionDetailsView p=panel(a,r);assertEquals("actual result",p.body.getText().toString());
        r=row("{\"path\":\"/project/a\"}","inProgress").put("toolOutputLength",0).put("actionPreview",r.getJSONObject("actionPreview"));p.update(r);assertTrue(p.body.getText().toString().contains("ещё не передал"));
        r.put("body","file content\n\n{\"path\":\"/project/a\"}").put("toolOutputLength",12);p.update(r);assertEquals("file content",p.body.getText().toString());
    }

}
