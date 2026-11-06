package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicInteger;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentComposerControlsTest {
 private Activity a;private LinearLayout row;private EditText input;private WorkspaceIconButton star,send,clip;private AgentComposerControls controls;
 @Before public void setup(){a=Robolectric.buildActivity(Activity.class).setup().get();row=new LinearLayout(a);input=new EditText(a);input.setMaxLines(6);star=new WorkspaceIconButton(a);send=new WorkspaceIconButton(a);clip=new WorkspaceIconButton(a);controls=new AgentComposerControls(a,row,input,star,send,clip);a.setContentView(row);}
 @After public void close(){a.finish();}
 private void measure(int width){for(int i=0;i<2;i++){row.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1500,View.MeasureSpec.AT_MOST));row.layout(0,0,width,row.getMeasuredHeight());}}
 private View text(View v,String label){if(v instanceof TextView&&label.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=text(((ViewGroup)v).getChildAt(i),label);if(found!=null)return found;}return null;}
 @Test public void independentTargetsSurviveNarrowLargeDraftAndLanguageChanges(){
  AtomicInteger notes=new AtomicInteger(),files=new AtomicInteger(),sends=new AtomicInteger();star.setOnClickListener(v->notes.incrementAndGet());clip.setOnClickListener(v->files.incrementAndGet());send.setOnClickListener(v->sends.incrementAndGet());int calls=0;
  for(String language:new String[]{"ru","en"})for(int width:new int[]{240,280,360})for(int size:new int[]{14,24,36}){
   WorkspaceLanguage.select(a,language);input.setText("draft\nline two\nline three");input.setTextSize(size);input.setSelection(2);controls.present(true);measure(width);
   View note=row.findViewWithTag("agent-notes-input");assertEquals(language.equals("ru")?"Заметки":"Notes",note.getContentDescription());assertNotNull(note);for(View target:new View[]{note,send,clip}){assertTrue(target.getWidth()>=44);assertTrue(target.getHeight()>=44);target.performClick();}
   assertEquals("draft\nline two\nline three",input.getText().toString());assertEquals(2,input.getSelectionStart());assertEquals(((View)send.getParent()).getWidth(),send.getRight());assertTrue(controls.pair.getTop()>=input.getHeight());calls++;
  }
  assertEquals(calls,notes.get());assertEquals(calls,files.get());assertEquals(calls,sends.get());
 }
 @Test public void oneLineManyLinesReopenOneLineKeepsActionBarHeightExactly(){
  input.setTextSize(13);input.setText("one");controls.present(true);measure(320);int field=input.getHeight(),bar=controls.pair.getHeight();
  String draft="one"+String.join("",java.util.Collections.nCopies(20,"\nnext"));input.setText(draft);input.setSelection(2);controls.present(true);measure(320);assertTrue(input.getHeight()>field);assertEquals(bar,controls.pair.getHeight());
  controls.present(false);controls=new AgentComposerControls(a,row,input,star,send,clip);controls.present(true);measure(320);assertEquals(bar,controls.pair.getHeight());assertEquals(draft,input.getText().toString());assertEquals(2,input.getSelectionStart());
  input.setText("one");controls.present(true);measure(320);assertEquals(field,input.getHeight());assertEquals(bar,controls.pair.getHeight());
 }
 @Test public void hiddenStopReservesGeometryAndDoesNotReappearOnPresentationRefresh(){
  View stop=new View(a);controls.actions(new TextView(a),stop,()->{},()->{},n->{},on->{},on->{});controls.present(true);controls.stopAvailable(true);measure(360);int height=row.getHeight(),inputWidth=input.getWidth(),bar=controls.pair.getTop();assertEquals(View.VISIBLE,stop.getVisibility());
  controls.stopAvailable(false);controls.present(true);measure(360);assertEquals(View.INVISIBLE,stop.getVisibility());assertFalse(stop.isEnabled());assertEquals(height,row.getHeight());assertEquals(inputWidth,input.getWidth());assertEquals(bar,controls.pair.getTop());controls.stopAvailable(true);assertEquals(View.VISIBLE,stop.getVisibility());controls.present(false);assertNull(stop.getParent());
 }
 @Test public void terminalUsesSeparatePickersAndIndependentModeSwitches(){
  AtomicInteger library=new AtomicInteger(-1),direct=new AtomicInteger(),keys=new AtomicInteger();controls.actions(null,null,()->{},()->{},library::set,on->direct.set(on?1:0),on->keys.set(on?1:0));WorkspaceLanguage.select(a,"ru");controls.present(false);measure(320);
  String[] labels={"Команды","Aliases","Заметки"};for(int i=0;i<labels.length;i++){text(row,labels[i]).performClick();assertEquals(i,library.get());}
  assertNull(clip.getParent());Switch interactive=(Switch)text(row,"Интерактивный ввод"),extra=(Switch)text(row,"Дополнительные клавиши");interactive.performClick();assertEquals(1,direct.get());assertEquals(0,keys.get());extra.performClick();assertEquals(1,keys.get());controls.terminalModes(false,false);assertEquals(1,direct.get());assertEquals(1,keys.get());
 }
 @Test public void bothModesKeepHostBehindComposerAndReportItsRealHeight(){
  RelativeLayout root=new RelativeLayout(a);FrameLayout host=new FrameLayout(a);row.setId(901);((ViewGroup)row.getParent()).removeView(row);RelativeLayout.LayoutParams hp=new RelativeLayout.LayoutParams(-1,-1);hp.addRule(RelativeLayout.ABOVE,901);root.addView(host,hp);RelativeLayout.LayoutParams rp=new RelativeLayout.LayoutParams(-1,-2);rp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);root.addView(row,rp);AtomicInteger inset=new AtomicInteger();controls.overlay(host,inset::set);a.setContentView(root);input.setText("draft\nmore");input.setSelection(3);
  for(boolean agent:new boolean[]{true,false,true})for(int height:new int[]{800,430,800}){controls.present(agent);for(int i=0;i<2;i++){root.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,320,height);}assertEquals(height,host.getBottom());assertEquals(height,row.getBottom());assertEquals(row.getHeight(),inset.get());assertNotNull(row.getBackground());assertTrue(row.isClickable());assertEquals("draft\nmore",input.getText().toString());assertEquals(3,input.getSelectionStart());}
 }
 @Test public void iconsAndRightActionsShareAxisAndQueueIsAbsent(){
  WorkspaceLanguage.select(a,"en");LinearLayout model=new LinearLayout(a);TextView title=new TextView(a);title.setText("Model / High");title.setTextSize(9.5f);model.addView(title);controls.actions(model,new View(a),()->fail("queue UI"),()->{},n->{},v->{},v->{});controls.present(true);measure(440);
  View note=row.findViewWithTag("agent-notes-input"),mic=row.findViewWithTag("agent-voice-input");assertTrue(note instanceof WorkspaceIconButton);assertTrue(mic instanceof WorkspaceIconButton);assertNull(text(row,"Queue"));assertNull(text(row,"Notes"));
  for(View v:new View[]{note,mic,model,send})assertEquals(clip.getTop()+clip.getHeight()/2,v.getTop()+v.getHeight()/2,1);
  assertTrue(note.getWidth()>=WorkspaceDialogs.dp(a,44));assertTrue(note.getHeight()>=WorkspaceDialogs.dp(a,44));
 }

}
