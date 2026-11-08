package com.termux.app.workspace;
import android.app.*;import android.view.*;import android.widget.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentBubbleTest {
 @Test public void shortContentWrapsAndLongUnbrokenTextStaysWithin85Percent(){Activity a=Robolectric.buildActivity(Activity.class).setup().get();try{for(float size:new float[]{14,24}){AgentBubble bubble=new AgentBubble(a);TextView text=new TextView(a);text.setTextSize(size);text.setTextIsSelectable(true);text.setPadding(10,8,10,8);bubble.addView(text,new LinearLayout.LayoutParams(-2,-2));text.setText("Hi");measure(bubble);assertTrue(bubble.getMeasuredWidth()<160);int shortHeight=bubble.getMeasuredHeight();text.setText(new String(new char[400]).replace('\0','W')+"\nnext line");measure(bubble);assertTrue(bubble.getMeasuredWidth()<=272);assertTrue(bubble.getMeasuredHeight()>shortHeight);assertTrue(text.isTextSelectable());}}finally{a.finish();}}
 void measure(View v){v.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.AT_MOST),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));v.layout(0,0,v.getMeasuredWidth(),v.getMeasuredHeight());}
}
