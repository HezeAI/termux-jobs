package com.termux.app.workspace;
import android.app.*;
import android.os.Looper;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentTextRevealTest {
 private Activity a;private TextView view;private AgentTextReveal reveal;private final List<String> painted=new ArrayList<>();
 @Before public void setup()throws Exception{
  a=Robolectric.buildActivity(Activity.class).setup().visible().get();view=new TextView(a);a.setContentView(view);
  java.lang.reflect.Field attach=View.class.getDeclaredField("mAttachInfo");attach.setAccessible(true);Object info=attach.get(view);java.lang.reflect.Field visible=info.getClass().getDeclaredField("mWindowVisibility");visible.setAccessible(true);visible.setInt(info,View.VISIBLE);
  reveal=new AgentTextReveal(view,text->{painted.add(text.toString());view.setText(text);});
 }
 @After public void close(){reveal.finish();a.finish();}
 private void tick(int ms){Shadows.shadowOf(Looper.getMainLooper()).idleFor(ms,TimeUnit.MILLISECONDS);}
 @Test public void appendRevealsGraphemesInOrderAndKeepsSpans(){
  reveal.update("prefix ",false);String value="prefix A🙂e\u0301👩‍💻 xyz";SpannableString text=new SpannableString(value);text.setSpan(new ForegroundColorSpan(0xff55ff55),7,value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);reveal.update(text,true);
  assertEquals("prefix ",view.getText().toString());assertTrue(reveal.pending());tick(1000);assertEquals(value,view.getText().toString());assertFalse(reveal.pending());
  android.icu.text.BreakIterator boundaries=android.icu.text.BreakIterator.getCharacterInstance();boundaries.setText(value);
  int length=0;for(String visible:painted){assertTrue(value.startsWith(visible));assertTrue(boundaries.isBoundary(visible.length()));assertTrue(visible.length()>=length);length=visible.length();}
  assertEquals(1,((Spanned)view.getText()).getSpans(7,value.length(),ForegroundColorSpan.class).length);
 }
 @Test public void historyAndCorrectionsDoNotReplayAndHiddenViewStopsDecorativeWork(){
  reveal.update("history",false);assertFalse(reveal.pending());reveal.update("history + streaming text",true);assertTrue(reveal.pending());reveal.update("correction",true);assertEquals("correction",view.getText().toString());assertFalse(reveal.pending());
  reveal.update("correction and more arriving characters",true);view.setVisibility(View.GONE);tick(20);assertFalse(reveal.pending());int count=painted.size();tick(1000);assertEquals(count,painted.size());
 }
 @Test public void largeBacklogAcceleratesWithoutLosingText(){
  reveal.update("",false);StringBuilder text=new StringBuilder();for(int i=0;i<1500;i++)text.append("данные🙂 ");String expected=text.toString();
  reveal.update(expected,true);assertTrue(AgentTextReveal.delay(1000)<AgentTextReveal.delay(5));tick(4000);assertEquals(expected,view.getText().toString());assertFalse(reveal.pending());
 }
 @Test public void streamingLocalLinksKeepTheirCapturedActionAfterRevealAndTouch()throws Exception{
  AgentLinkTextView text=new AgentLinkTextView(a);a.setContentView(text);Object attach=org.robolectric.util.ReflectionHelpers.getField(text,"mAttachInfo");org.robolectric.util.ReflectionHelpers.setField(attach,"mWindowVisibility",View.VISIBLE);java.util.concurrent.atomic.AtomicInteger bound=new java.util.concurrent.atomic.AtomicInteger(),opened=new java.util.concurrent.atomic.AtomicInteger();io.noties.markwon.Markwon markdown=AgentMarkdown.create(a,path->{assertEquals("/root/original.gif",path);bound.incrementAndGet();return opened::incrementAndGet;});
  markdown.setMarkdown(text,"prefix");text.animateNextText(true,()->{});markdown.setMarkdown(text,"prefix [image](/root/original.gif)");tick(1000);assertEquals("prefix image",text.getText().toString());Spanned spans=(Spanned)text.getText();android.text.style.URLSpan[] links=spans.getSpans(0,spans.length(),android.text.style.URLSpan.class);assertEquals(1,links.length);links[0].onClick(text);assertEquals(1,opened.get());assertEquals(1,bound.get());assertNull(Shadows.shadowOf(a).getNextStartedActivity());text.setVisibility(View.GONE);assertSame(links[0],((Spanned)text.getText()).getSpans(0,text.length(),android.text.style.URLSpan.class)[0]);
 }

}
