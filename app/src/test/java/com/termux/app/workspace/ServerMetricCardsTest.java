package com.termux.app.workspace;

import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ServerMetricCardsTest {
    private TextView value(ServerMetricCards c,int i){return (TextView)((LinearLayout)c.getChildAt(i)).getChildAt(1);}
    private ProgressBar bar(ServerMetricCards c,int i){return (ProgressBar)((LinearLayout)c.getChildAt(i)).getChildAt(2);}
    private JSONObject metrics()throws Exception{return new JSONObject().put("cpu",8).put("ramUsed",536870912).put("ramTotal",2147483648L).put("diskUsed",3L*1073741824).put("diskTotal",25L*1073741824);}
    @Test public void cardsShowActualUsageAndStableProgressWithoutRecreatingViews()throws Exception{
        WorkspaceLanguage.select(RuntimeEnvironment.application,"en");ServerMetricCards c=new ServerMetricCards(RuntimeEnvironment.application);View cpu=c.getChildAt(0),ram=c.getChildAt(1),disk=c.getChildAt(2);c.update(metrics());assertEquals("8%",value(c,0).getText().toString());assertEquals("0.5 / 2.0 GB",value(c,1).getText().toString());assertEquals("3.0 / 25.0 GB",value(c,2).getText().toString());assertEquals(80,bar(c,0).getProgress());assertEquals(250,bar(c,1).getProgress());assertEquals(120,bar(c,2).getProgress());CharSequence before=value(c,0).getText();c.update(metrics());assertSame(before,value(c,0).getText());c.update(metrics().put("cpu",37));assertSame(cpu,c.getChildAt(0));assertSame(ram,c.getChildAt(1));assertSame(disk,c.getChildAt(2));assertEquals(370,bar(c,0).getProgress());
    }
    @Test public void missingMeasurementsAreNotShownAsZeroLoad()throws Exception{ServerMetricCards c=new ServerMetricCards(RuntimeEnvironment.application);c.update(new JSONObject().put("cpu",JSONObject.NULL).put("ramTotal",0).put("diskUsed",-1));for(int i=0;i<3;i++){assertEquals("—",value(c,i).getText().toString());assertEquals(0,bar(c,i).getProgress());assertFalse(bar(c,i).isIndeterminate());}assertEquals(0,ServerMetricCards.progress(Double.NaN));assertEquals(1000,ServerMetricCards.progress(300));assertEquals(0,ServerMetricCards.progress(-50));}
    @Test public void russianUnitsAndLabelsAreLocalized()throws Exception{WorkspaceLanguage.select(RuntimeEnvironment.application,"ru");ServerMetricCards c=new ServerMetricCards(RuntimeEnvironment.application);c.update(metrics());assertTrue(value(c,1).getText().toString().contains("0,5 / 2,0 ГБ"));assertTrue(c.getChildAt(2).getContentDescription().toString().startsWith("Диск"));}
    @Test public void threeCardsAndSquareIconsFitNarrowContainer()throws Exception{
        ServerMetricCards c=new ServerMetricCards(RuntimeEnvironment.application);c.update(metrics());int width=WorkspaceDialogs.dp(RuntimeEnvironment.application,280);c.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.AT_MOST));c.layout(0,0,width,c.getMeasuredHeight());assertEquals(3,c.getChildCount());for(int i=0;i<3;i++){View card=c.getChildAt(i);assertTrue(card.getWidth()>0);assertTrue(card.getRight()<=width);if(i>0)assertTrue(card.getLeft()>c.getChildAt(i-1).getRight());LinearLayout heading=(LinearLayout)((LinearLayout)card).getChildAt(0);ImageView icon=(ImageView)heading.getChildAt(0);assertEquals(icon.getLayoutParams().width,icon.getLayoutParams().height);assertNotNull(icon.getDrawable());}}
}
