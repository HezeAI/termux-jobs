package com.termux.app.workspace;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import android.text.*;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.URLSpan;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class,shadows=AgentChatLinksTest.TextMetrics.class)
public class AgentChatLinksTest {
    // Robolectric4.4 has no native glyph advances. Supply deterministic metrics
    // for pointer dispatch; physical font/layout and Android selection need device checks.
    @org.robolectric.annotation.Implements(Layout.class)
    public static class TextMetrics {
        @org.robolectric.annotation.RealObject private Layout layout;
        @org.robolectric.annotation.Implementation protected float getLineLeft(int line){return 0;}
        @org.robolectric.annotation.Implementation protected float getLineRight(int line){return Math.min(layout.getWidth(),(layout.getLineEnd(line)-layout.getLineStart(line))*8f);}
        @org.robolectric.annotation.Implementation protected float getPrimaryHorizontal(int offset){int line=layout.getLineForOffset(offset);return (offset-layout.getLineStart(line))*8f;}
        @org.robolectric.annotation.Implementation protected int getOffsetForHorizontal(int line,float x){return Math.min(layout.getLineEnd(line),layout.getLineStart(line)+Math.max(0,Math.round(x/8)));}
    }
    private Activity activity;
    @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();}
    private void layout(View view){for(int i=0;i<3;i++){view.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));view.layout(0,0,720,900);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}}
    private List<TextView> links(View v){List<TextView> result=new ArrayList<>();if(v instanceof TextView&&((TextView)v).getText() instanceof Spanned){Spanned s=(Spanned)((TextView)v).getText();if(s.getSpans(0,s.length(),URLSpan.class).length>0)result.add((TextView)v);}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)result.addAll(links(g.getChildAt(i)));}return result;}
    private float[] point(TextView t){Spanned s=(Spanned)t.getText();URLSpan link=s.getSpans(0,s.length(),URLSpan.class)[0];int offset=s.getSpanStart(link)+1,line=t.getLayout().getLineForOffset(offset);return new float[]{t.getTotalPaddingLeft()+t.getLayout().getPrimaryHorizontal(offset)-t.getScrollX(),t.getTotalPaddingTop()+(t.getLayout().getLineTop(line)+t.getLayout().getLineBottom(line))/2f-t.getScrollY()};}
    private void event(TextView t,long start,long time,int action,float x,float y){MotionEvent e=MotionEvent.obtain(start,time,action,x,y,0);try{t.dispatchTouchEvent(e);}finally{e.recycle();}}
    private void tap(TextView t){float[] p=point(t);long now=SystemClock.uptimeMillis();event(t,now,now,MotionEvent.ACTION_DOWN,p[0],p[1]);event(t,now,now+40,MotionEvent.ACTION_UP,p[0],p[1]);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}
    private AlertDialog dialog(){AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d);assertTrue(d.isShowing());assertNull(Shadows.shadowOf(activity).getNextStartedActivity());return d;}
    private void dismiss(){AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();if(d!=null)d.dismiss();}
    @Test public void namedAndBareLinksWorkInAllFourProviderRowsAndKeepSelection()throws Exception{
        for(String provider:new String[]{"Codex","Claude","Gemini","OpenCode"})for(String source:new String[]{"Read [Source](https://example.org/docs)","Read https://example.org/docs"}){
            AgentChatView chat=new AgentChatView(activity,r->{},()->{},()->{},()->{},()->{});chat.assistantName(provider);activity.setContentView(chat);chat.rows(Collections.singletonList(new JSONObject().put("key","a").put("kind","text").put("body",source)),false);layout(chat);
            TextView t=links(chat).get(0);assertTrue(t.isTextSelectable());assertTrue(t.getMovementMethod() instanceof ArrowKeyMovementMethod);tap(t);dialog();dismiss();
        }
    }
    @Test public void previewCopiesFullUrlAndOnlyOpenLaunchesBrowserChooser(){
        String url="https://example.org/docs?q=one%20two#part";AgentMarkdown.openLink(new View(activity),url);AlertDialog d=dialog();assertEquals(url,((TextView)d.findViewById(android.R.id.custom).findViewWithTag("agent-link-address")).getText().toString());
        d.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(url,((android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip().getItemAt(0).getText().toString());assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        AgentMarkdown.openLink(new View(activity),url);d=dialog();assertTrue(d.getButton(AlertDialog.BUTTON_NEUTRAL).getText().length()>0);assertTrue(d.getButton(AlertDialog.BUTTON_POSITIVE).getText().length()>0);d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Intent chooser=Shadows.shadowOf(activity).getNextStartedActivity();assertEquals(Intent.ACTION_CHOOSER,chooser.getAction());Intent browser=chooser.getParcelableExtra(Intent.EXTRA_INTENT);assertEquals(Intent.ACTION_VIEW,browser.getAction());assertEquals(url,browser.getDataString());assertTrue(browser.hasCategory(Intent.CATEGORY_BROWSABLE));
    }
    @Test public void richBlocksAndTableCellsUseSamePreviewAfterScaleAndStreaming(){
        AgentMarkdownView markdown=new AgentMarkdownView(activity);activity.setContentView(markdown);String body="## Heading\n\n[Source](https://example.org/docs)\n\n| Link |\n| --- |\n| [Table](https://example.org/table) |";markdown.update("AI",body,null);
        for(int scale:new int[]{50,100,150}){AgentChatScale.prepare(markdown);markdown.update("AI",body+"\n\nstream "+scale,null);AgentChatScale.apply(markdown,scale);layout(markdown);List<TextView> ts=links(markdown);assertEquals(2,ts.size());for(TextView t:ts){t.scrollTo(0,0);tap(t);dialog();dismiss();}}
    }
    @Test public void scrollLongPressCancelAndTextReplacementDoNotOpenLinks(){
        AgentMarkdownView markdown=new AgentMarkdownView(activity);activity.setContentView(markdown);markdown.update("AI","[Source](https://example.org/docs)",null);layout(markdown);TextView t=links(markdown).get(0);t.setTextIsSelectable(false);t.setMovementMethod(null);t.setClickable(true);float[] p=point(t);long now=SystemClock.uptimeMillis();
        event(t,now,now,MotionEvent.ACTION_DOWN,p[0],p[1]);event(t,now,now+40,MotionEvent.ACTION_MOVE,p[0]+100,p[1]);event(t,now,now+60,MotionEvent.ACTION_UP,p[0],p[1]);assertNull(ShadowAlertDialog.getLatestAlertDialog());
        now+=2000;event(t,now,now,MotionEvent.ACTION_DOWN,p[0],p[1]);event(t,now,now+ViewConfiguration.getLongPressTimeout()+10,MotionEvent.ACTION_UP,p[0],p[1]);assertNull(ShadowAlertDialog.getLatestAlertDialog());
        now+=2000;event(t,now,now,MotionEvent.ACTION_DOWN,p[0],p[1]);event(t,now,now+20,MotionEvent.ACTION_CANCEL,p[0],p[1]);event(t,now,now+40,MotionEvent.ACTION_UP,p[0],p[1]);assertNull(ShadowAlertDialog.getLatestAlertDialog());
        now+=2000;event(t,now,now,MotionEvent.ACTION_DOWN,p[0],p[1]);markdown.update("AI","[Changed](https://example.org/other)",null);layout(markdown);event(t,now,now+40,MotionEvent.ACTION_UP,p[0],p[1]);assertNull(ShadowAlertDialog.getLatestAlertDialog());
    }
    @Test public void linkTapDoesNotAlsoOpenTruncatedMessageAndWhitespaceIsNotLink()throws Exception{
        AtomicInteger details=new AtomicInteger();AgentChatView chat=new AgentChatView(activity,r->details.incrementAndGet(),()->{},()->{},()->{},()->{});activity.setContentView(chat);chat.rows(Collections.singletonList(new JSONObject().put("key","a").put("kind","text").put("body","[Source](https://example.org/docs)").put("more",true)),false);layout(chat);TextView t=links(chat).get(0);tap(t);dialog();assertEquals(0,details.get());dismiss();
        long now=SystemClock.uptimeMillis();float y=point(t)[1];event(t,now,now,MotionEvent.ACTION_DOWN,t.getWidth()-t.getTotalPaddingRight()-2,y);event(t,now,now+40,MotionEvent.ACTION_UP,t.getWidth()-t.getTotalPaddingRight()-2,y);assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());t.performClick();assertEquals(1,details.get());
    }
    @Test public void unsafeSchemesMalformedHostsAndCodeLiteralsStayInert(){
        for(String url:new String[]{"javascript:alert(1)","file:///root/key","intent://settings","data:text/html,x","https:///missing","https://user:pass@example.org/","https://example.org/\nnext"})AgentMarkdown.openLink(new View(activity),url);
        assertNull(ShadowAlertDialog.getLatestAlertDialog());assertNull(Shadows.shadowOf(activity).getNextStartedActivity());AgentMarkdownView markdown=new AgentMarkdownView(activity);markdown.update("AI","```sh\nhttps://example.org/docs\n```",null);assertTrue(links(markdown).isEmpty());
    }
    @Test public void accessibilitySpanActionShowsPreviewWithoutNavigating(){
        AgentMarkdownView markdown=new AgentMarkdownView(activity);markdown.update("AI","[Source](https://example.org/docs)",null);TextView t=links(markdown).get(0);Spanned text=(Spanned)t.getText();text.getSpans(0,text.length(),URLSpan.class)[0].onClick(t);dialog();
    }
    public static class NoBrowserActivity extends Activity {
        @Override public void startActivity(Intent intent){throw new ActivityNotFoundException("No browser");}
    }
    @Test public void missingBrowserDoesNotCrashAndCancelDoesNotNavigate(){
        Activity noBrowser=Robolectric.buildActivity(NoBrowserActivity.class).setup().get();AgentMarkdown.openLink(new View(noBrowser),"https://example.org/docs");AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertNotNull(org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
        AgentMarkdown.openLink(new View(activity),"https://example.org/docs");d=dialog();d.cancel();assertFalse(d.isShowing());assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
    }

}
