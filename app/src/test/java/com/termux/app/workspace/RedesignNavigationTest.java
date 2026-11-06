package com.termux.app.workspace;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.widget.*;
import com.termux.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class RedesignNavigationTest {
    @Test public void titlesOnlyExpandAndExplicitOpenUsesItsOwnOwner(){
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();WorkspaceSidebar sidebar=new WorkspaceSidebar(a);
        AtomicInteger opened=new AtomicInteger(-1),mutations=new AtomicInteger();
        String[] agents={"Codex","Claude","Gemini","OpenCode","Termux"};
        LinearLayout[] actions=new LinearLayout[agents.length];TextView[] titles=new TextView[agents.length];
        for(int i=0;i<agents.length;i++){
            final int owner=i;actions[i]=sidebar.session("id"+i,agents[i],"/synthetic/project",i==4,false,false,()->false,mutations::incrementAndGet,mutations::incrementAndGet,mutations::incrementAndGet);
            LinearLayout frame=(LinearLayout)actions[i].getParent();titles[i]=(TextView)((LinearLayout)frame.getChildAt(0)).getChildAt(0);
            sidebar.action(actions[i],"Open chat",R.drawable.workspace_terminal,()->opened.set(owner));
        }
        for(int i=0;i<agents.length;i++){
            titles[i].performClick();assertEquals(View.VISIBLE,actions[i].getVisibility());assertEquals(-1,opened.get());assertEquals(0,mutations.get());
            for(int j=0;j<i;j++)assertEquals(View.GONE,actions[j].getVisibility());
            titles[i].performClick();assertEquals(View.GONE,actions[i].getVisibility());assertEquals(-1,opened.get());
            titles[i].performClick();actions[i].getChildAt(0).performClick();assertEquals(i,opened.get());opened.set(-1);
        }
        LinearLayout base=(LinearLayout)actions[4].getParent();assertEquals(1,((LinearLayout)base.getChildAt(0)).getChildCount());a.finish();
    }
    @Test public void desktopFillsBelowHeaderAcrossResizeAndRestoresWithoutReparent(){
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();RelativeLayout root=new RelativeLayout(a);
        View header=new View(a);header.setId(R.id.workspace_header);root.addView(header,new RelativeLayout.LayoutParams(-1,72));
        View input=new View(a);input.setId(R.id.workspace_input);RelativeLayout.LayoutParams ip=new RelativeLayout.LayoutParams(-1,170);ip.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);root.addView(input,ip);
        View keys=new View(a);keys.setId(R.id.terminal_toolbar_view_pager);RelativeLayout.LayoutParams kp=new RelativeLayout.LayoutParams(-1,80);kp.addRule(RelativeLayout.ABOVE,input.getId());root.addView(keys,kp);
        FrameLayout host=new FrameLayout(a);RelativeLayout.LayoutParams hp=new RelativeLayout.LayoutParams(-1,-1);hp.addRule(RelativeLayout.BELOW,header.getId());hp.addRule(RelativeLayout.ABOVE,keys.getId());root.addView(host,hp);
        View surface=new View(a);host.addView(surface,new FrameLayout.LayoutParams(-1,-1));a.setContentView(root);DesktopViewport viewport=new DesktopViewport(host);
        measure(root,400,900);int terminalHeight=host.getHeight();
        input.setVisibility(View.GONE);keys.setVisibility(View.GONE);viewport.present(true);
        for(int[] size:new int[][]{{400,900},{900,400},{320,420},{400,900}}){measure(root,size[0],size[1]);assertEquals(header.getBottom(),host.getTop());assertEquals(size[1],host.getBottom());assertEquals(size[1]-72,host.getHeight());assertEquals(host.getHeight(),surface.getHeight());assertSame(root,host.getParent());assertSame(host,surface.getParent());}
        assertTrue(host.getHeight()>terminalHeight);viewport.present(false);input.setVisibility(View.VISIBLE);keys.setVisibility(View.VISIBLE);measure(root,400,900);
        assertEquals(terminalHeight,host.getHeight());assertEquals(keys.getTop(),host.getBottom());assertSame(host,surface.getParent());a.finish();
    }
    private void measure(View v,int width,int height){v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));v.layout(0,0,width,height);}
}
