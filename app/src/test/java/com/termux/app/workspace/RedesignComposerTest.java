package com.termux.app.workspace;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class RedesignComposerTest {
    public static class SpeechActivity extends Activity {int launches;Intent sent;boolean denied;@Override public void startActivityForResult(Intent intent,int request){if(denied)throw new SecurityException();launches++;sent=intent;}}
    @Test public void dictationOnlyEditsUnchangedOwnerDraftAndNeverSends(){
        SpeechActivity a=Robolectric.buildActivity(SpeechActivity.class).setup().get();EditText input=new EditText(a);input.setText("before ");input.setSelection(7);String[] owner={"first"};List<String> notices=new ArrayList<>();AgentDictation voice=new AgentDictation(a,input,()->owner[0],notices::add);
        Intent result=new Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS,new ArrayList<>(Arrays.asList("hello 😀")));
        voice.start();voice.start();assertEquals(1,a.launches);assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH,a.sent.getAction());assertEquals("before ",input.getText().toString());
        voice.result(AgentDictation.REQUEST,Activity.RESULT_OK,result);assertEquals("before hello 😀",input.getText().toString());assertEquals(1,a.launches);
        voice.start();voice.result(AgentDictation.REQUEST,Activity.RESULT_CANCELED,null);assertEquals("before hello 😀",input.getText().toString());
        voice.start();owner[0]="second";voice.result(AgentDictation.REQUEST,Activity.RESULT_OK,result);assertEquals("before hello 😀",input.getText().toString());
        voice.start();input.setText("edited");voice.result(AgentDictation.REQUEST,Activity.RESULT_OK,result);assertEquals("edited",input.getText().toString());
        a.denied=true;voice.start();assertEquals("edited",input.getText().toString());assertFalse(notices.isEmpty());a.finish();
    }
    @Test public void composerWrapsLargeActionsWithoutLosingDraftAndKeepsStopAboveSend(){
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();LinearLayout root=new LinearLayout(a);EditText input=new EditText(a);input.setMinHeight(48);input.setMaxLines(6);input.setText("one\ntwo\nthree");input.setSelection(2);TextView star=new TextView(a),send=new TextView(a);send.setText("Send");send.setMinHeight(48);send.setMinWidth(48);WorkspaceIconButton clip=new WorkspaceIconButton(a);View stop=new View(a);LinearLayout status=new LinearLayout(a);TextView model=new TextView(a);model.setText("a long model name and reasoning");model.setTextSize(24);status.addView(model);
        AgentComposerControls c=new AgentComposerControls(a,root,input,star,send,clip);c.actions(status,stop,()->{},()->{},n->{},v->{},v->{});c.present(true);a.setContentView(root);
        for(int width:new int[]{280,360,740,280}){root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.AT_MOST));root.layout(0,0,width,root.getMeasuredHeight());assertTrue(send.getWidth()>0);assertTrue(send.getHeight()>=48);assertTrue(input.getWidth()>0);assertTrue(c.pair.getTop()>=input.getBottom());assertEquals("one\ntwo\nthree",input.getText().toString());assertEquals(2,input.getSelectionStart());}
        c.present(false);c.present(true);assertEquals("one\ntwo\nthree",input.getText().toString());assertEquals(2,input.getSelectionStart());a.finish();
    }
    @Test public void compactLimitsKeepUnknownAndExpiredValuesWithoutInventingReset()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();ComposerLimits meter=new ComposerLimits(a);meter.claude(null,100000);assertEquals(View.GONE,meter.bars[0].getVisibility());
        JSONObject sub=new JSONObject().put("source","native-get-usage").put("available",true).put("updatedAt",100000).put("fiveHour",new JSONObject().put("usedPercent",25).put("resetsAt",110000));meter.claude(sub,100000);assertEquals(75,meter.bars[0].getProgress());assertEquals(View.GONE,meter.bars[1].getVisibility());meter.claude(sub,120000);assertEquals(75,meter.bars[0].getProgress());assertTrue(meter.captions[0].getText().toString().contains("stale")||meter.captions[0].getText().toString().contains("устарело"));
        sub.put("available",false);meter.claude(sub,120000);assertEquals(View.GONE,meter.bars[0].getVisibility());
        CodexStatus status=new CodexStatus();status.updated=120000;status.limits.put("primary",new JSONObject().put("usedPercent",10).put("windowDurationMins",60));meter.codex(status,120000);assertEquals(View.GONE,meter.bars[0].getVisibility());status.limits.getJSONObject("primary").put("windowDurationMins",300);meter.codex(status,120000);assertEquals(90,meter.bars[0].getProgress());meter.unavailable();assertEquals(View.GONE,meter.captions[0].getVisibility());assertEquals(View.GONE,meter.bars[0].getVisibility());meter.codex(status,120000);assertEquals(View.VISIBLE,meter.captions[0].getVisibility());assertEquals(90,meter.bars[0].getProgress());a.finish();
    }
    @Test public void globalSessionTextSizePersistsRangeWithoutChangingDesktopPreferences()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();
        a.getSharedPreferences("workspace-appearance",0).edit().clear().commit();
        java.util.Map<String,?> desktopBefore=new java.util.HashMap<>(a.getSharedPreferences("workspace-desktop",0).getAll());
        android.widget.LinearLayout row=new android.widget.LinearLayout(a);java.util.concurrent.atomic.AtomicInteger changed=new java.util.concurrent.atomic.AtomicInteger();
        SessionTextSize.control(row,changed::incrementAndGet);
        android.widget.SeekBar slider=(android.widget.SeekBar)row.findViewWithTag("session-text-size");
        assertEquals(100,SessionTextSize.percent(a));assertEquals(100,slider.getMax());assertEquals(50,slider.getProgress());
        java.lang.reflect.Field field=android.widget.SeekBar.class.getDeclaredField("mOnSeekBarChangeListener");field.setAccessible(true);
        android.widget.SeekBar.OnSeekBarChangeListener listener=(android.widget.SeekBar.OnSeekBarChangeListener)field.get(slider);
        for(int value:new int[]{50,150,100}){
            listener.onProgressChanged(slider,value-50,true);assertEquals(value,SessionTextSize.percent(a));
            assertEquals(value,SessionTextSize.agentPercent(a,100));assertEquals(value*2,SessionTextSize.terminalPixels(a,200));
            android.widget.LinearLayout reopened=new android.widget.LinearLayout(a);SessionTextSize.control(reopened,()->{});
            assertEquals(value-50,((android.widget.SeekBar)reopened.findViewWithTag("session-text-size")).getProgress());
        }
        listener.onProgressChanged(slider,0,false);assertEquals(100,SessionTextSize.percent(a));assertEquals(3,changed.get());
        assertEquals(desktopBefore,a.getSharedPreferences("workspace-desktop",0).getAll());a.finish();
    }

}
