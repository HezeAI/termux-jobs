package com.termux.app.workspace;

import android.app.Application;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class AdaptiveWorkspaceLayoutTest {
    @Test public void keyboardResizeKeepsPanelsUsableAndReturnsToIndependentScrolling() {
        Application context = RuntimeEnvironment.application;
        float density = context.getResources().getDisplayMetrics().density;
        AdaptiveWorkspaceLayout outer = new AdaptiveWorkspaceLayout(context);
        LinearLayout body = new LinearLayout(context); body.setOrientation(LinearLayout.VERTICAL);
        outer.addView(body);
        TextView heading = new TextView(context); heading.setText("Поиск и фильтры");
        body.addView(heading, new LinearLayout.LayoutParams(-1, Math.round(80 * density)));
        ScrollView[] panels = new ScrollView[2];
        for (int i = 0; i < 2; i++) {
            panels[i] = new ScrollView(context);
            View entries = new View(context); entries.setMinimumHeight(Math.round(1500 * density));
            panels[i].addView(entries, new ScrollView.LayoutParams(-1, Math.round(1500 * density)));
            body.addView(panels[i], new LinearLayout.LayoutParams(-1, 0, 1));
        }
        measure(outer, Math.round(320 * density), Math.round(800 * density));
        assertEquals(outer.getHeight(), body.getHeight());
        panels[0].scrollTo(0, 100); assertEquals(100, panels[0].getScrollY()); assertEquals(0, panels[1].getScrollY());
        measure(outer, Math.round(320 * density), Math.round(240 * density));
        assertTrue(body.getHeight() > outer.getHeight());
        for (ScrollView panel : panels) assertTrue(panel.getHeight() >= Math.round(160 * density));
        outer.scrollTo(0, 100); assertEquals(100, outer.getScrollY());
        measure(outer, Math.round(320 * density), Math.round(800 * density));
        assertEquals(outer.getHeight(), body.getHeight()); assertEquals(0, outer.getScrollY());
        assertEquals(100, panels[0].getScrollY());
    }
    @Test @Config(qualifiers = "w320dp-h640dp") public void largerSystemFontIncreasesMinimumPanelHeight() {
        Application context = RuntimeEnvironment.application;
        android.content.res.Configuration config = new android.content.res.Configuration(context.getResources().getConfiguration());
        config.fontScale = 1.6f;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        AdaptiveWorkspaceLayout outer = new AdaptiveWorkspaceLayout(context);
        LinearLayout body = new LinearLayout(context); body.setOrientation(LinearLayout.VERTICAL); outer.addView(body);
        for (int i = 0; i < 2; i++) body.addView(new ScrollView(context), new LinearLayout.LayoutParams(-1, 0, 1));
        float density = context.getResources().getDisplayMetrics().density;
        measure(outer, Math.round(320 * density), Math.round(240 * density));
        for (int i = 0; i < 2; i++) assertTrue(body.getChildAt(i).getHeight() >= Math.round(256 * density));
    }
    private void measure(View view, int width, int height) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, width, height);
    }
}
