package com.termux.app.workspace;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, application=Application.class)
public class AgentBubbleTest {
    @Test public void shortContentWrapsAndLongUnbrokenTextStaysWithin88Percent() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        try {
            for (float textSize : new float[]{14, 24}) {
                AgentBubble bubble = new AgentBubble(activity);
                TextView text = new TextView(activity);
                text.setTextSize(textSize);
                text.setTextIsSelectable(true);
                text.setPadding(10, 8, 10, 8);
                bubble.addView(text, new LinearLayout.LayoutParams(-2, -2));
                text.setText("Hi");
                measure(bubble);
                assertTrue(bubble.getMeasuredWidth() < 160);
                int shortHeight = bubble.getMeasuredHeight();
                text.setText("W".repeat(400) + "\nnext line");
                measure(bubble);
                assertTrue(bubble.getMeasuredWidth() <= 281);
                assertTrue(bubble.getMeasuredHeight() > shortHeight);
                assertTrue(text.isTextSelectable());
            }
        } finally {
            activity.finish();
        }
    }

    private void measure(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }
}
