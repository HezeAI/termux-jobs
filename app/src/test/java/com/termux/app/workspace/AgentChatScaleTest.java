package com.termux.app.workspace;

import android.app.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentChatScaleTest {
    private Activity a;
    @Before public void setup(){a=Robolectric.buildActivity(Activity.class).setup().get();}
    @After public void close(){a.finish();}
    private JSONObject row(String key,String kind,String body)throws JSONException{return new JSONObject().put("key",key).put("kind",kind).put("body",body);}
    private AgentChatView chat(){return new AgentChatView(a,r->{},()->{},()->{},()->{},()->{});}
    private void layout(AgentChatView v){for(int i=0;i<3;i++){v.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));v.layout(0,0,600,700);Shadows.shadowOf(Looper.getMainLooper()).idle();}}
    private List<TextView> texts(View root){List<TextView> out=new ArrayList<>();if(root instanceof TextView)out.add((TextView)root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)out.addAll(texts(((ViewGroup)root).getChildAt(i)));return out;}
    @Test public void smallerScaleOccupiesLessLayoutAndRestoresExactlyWithoutReplacingRows()throws Exception{
        AgentChatView v=chat();a.setContentView(v);List<JSONObject> rows=new ArrayList<>();for(int i=0;i<20;i++)rows.add(row("r"+i,"text","A long reply with enough words to wrap across the screen.\nAnd another line."));v.rows(rows,false);layout(v);
        TextView first=(TextView)v.timeline.getChildAt(1);float base=first.getTextSize();int height=v.timeline.getHeight(),padding=first.getPaddingLeft(),margin=((ViewGroup.MarginLayoutParams)first.getLayoutParams()).bottomMargin;
        float shellBase=v.shellInput.getTextSize();v.scale(50);layout(v);assertSame(first,v.timeline.getChildAt(1));assertEquals(base*.5,first.getTextSize(),.01);assertEquals(shellBase*.5,v.shellInput.getTextSize(),.01);assertTrue(v.timeline.getHeight()<height);assertEquals(Math.round(padding*.5f),first.getPaddingLeft());assertEquals(Math.round(margin*.5f),((ViewGroup.MarginLayoutParams)first.getLayoutParams()).bottomMargin);
        v.scale(150);layout(v);assertEquals(base*1.5,first.getTextSize(),.01);assertTrue(v.timeline.getHeight()>height);v.scale(100);layout(v);assertEquals(base,first.getTextSize(),.01);assertEquals(height,v.timeline.getHeight());assertSame(first,v.timeline.getChildAt(1));
    }
    @Test public void streamingMarkdownCodeTablesAndThumbnailsKeepCurrentScaleWithoutCompounding()throws Exception{
        AgentChatView v=chat();a.setContentView(v);JSONObject markdown=row("md","text","# Heading\n\n```sh\necho hello\n```\n\n| One | Two |\n| --- | --- |\n| 1 | 2 |");JSONObject image=row("image","attachment","").put("attachment",new JSONObject().put("type","image").put("name","photo.jpg"));v.rows(Arrays.asList(markdown,image),false);
        View md=v.timeline.getChildAt(1);List<TextView> all=texts(md);float[] base=new float[all.size()];for(int i=0;i<all.size();i++)base[i]=all.get(i).getTextSize();ImageView thumbnail=(ImageView)((ViewGroup)v.timeline.getChildAt(2)).getChildAt(1);int imageHeight=thumbnail.getLayoutParams().height;
        v.scale(75);for(int i=0;i<all.size();i++)assertEquals(base[i]*.75,all.get(i).getTextSize(),.01);assertEquals(Math.round(imageHeight*.75f),thumbnail.getLayoutParams().height);
        markdown.put("body",markdown.getString("body")+"\n\nStreaming paragraph");v.rows(Arrays.asList(markdown,image),false);assertSame(md,v.timeline.getChildAt(1));TextView code=md.findViewWithTag("markdown-code");assertNotNull(code);float codeSize=code.getTextSize();for(int i=0;i<3;i++)v.rows(Arrays.asList(markdown,image),false);assertEquals(codeSize,code.getTextSize(),.01);v.scale(100);assertEquals(imageHeight,thumbnail.getLayoutParams().height);assertEquals(codeSize/.75,code.getTextSize(),.01);
    }
    @Test public void groupedToolsRetainExpansionAndReadingPositionAfterScaling()throws Exception{
        AgentChatView v=chat();a.setContentView(v);v.actionCards(true);JSONObject tool=row("tool","tool","output").put("title","Test").put("toolType","shell").put("command","printf test").put("status","completed");List<JSONObject> rows=new ArrayList<>();rows.add(tool);for(int i=0;i<30;i++)rows.add(row("r"+i,"text","Reading line "+i+"\nmore content\nthird line"));v.rows(rows,false);layout(v);CodexActionsView card=(CodexActionsView)v.timeline.getChildAt(1);card.heading.performClick();assertEquals(View.VISIBLE,card.steps.getVisibility());v.scroll.scrollTo(0,600);AgentChatView.Position held=v.position();v.scale(75);layout(v);assertSame(card,v.timeline.getChildAt(1));assertEquals(View.VISIBLE,card.steps.getVisibility());assertEquals(held.anchor,v.position().anchor);assertTrue(v.scroll.getScrollY()>0);
    }
    @Test public void controlsHaveRequestedRangeDefaultResetAndSessionGuard()throws Exception{
        WorkspaceStore.Session first=new WorkspaceStore.Session(),other=new WorkspaceStore.Session();LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);a.setContentView(body);AtomicBoolean owns=new AtomicBoolean(true);AtomicInteger previews=new AtomicInteger(),saves=new AtomicInteger();AgentChatScale.controls(body,first,owns::get,previews::incrementAndGet,saves::incrementAndGet);SeekBar slider=body.findViewWithTag("agent-chat-scale");assertEquals(100,slider.getMax());assertEquals(50,slider.getProgress());slider.setProgress(25);assertEquals(75,first.chatScale);assertEquals(100,other.chatScale);assertEquals(1,previews.get());Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(300));assertEquals(1,saves.get());body.findViewWithTag("agent-status-chat-scale-reset").performClick();assertEquals(100,first.chatScale);slider.setProgress(100);assertEquals(150,first.chatScale);slider.setProgress(0);assertEquals(50,first.chatScale);owns.set(false);slider.setProgress(70);assertEquals(50,first.chatScale);
    }
    @Test public void sessionScaleRoundtripsCopiesAndOldDataDefaultsTo100()throws Exception{
        WorkspaceStore store=new WorkspaceStore(a,e->fail(e));try{store.sessions.clear();WorkspaceStore.Session p=new WorkspaceStore.Session();p.chatScale=75;store.sessions.add(p);JSONObject encoded=store.encode();assertEquals(75,p.copy().chatScale);store.decode(encoded.toString());assertEquals(75,store.sessions.get(0).chatScale);encoded.getJSONArray("sessions").getJSONObject(0).remove("chatScale");store.decode(encoded.toString());assertEquals(100,store.sessions.get(0).chatScale);for(Object invalid:new Object[]{49,151,75.5,"75"}){encoded.getJSONArray("sessions").getJSONObject(0).put("chatScale",invalid);try{store.decode(encoded.toString());fail();}catch(JSONException expected){}assertEquals(100,store.sessions.get(0).chatScale);}}finally{store.close();}
    }
}
