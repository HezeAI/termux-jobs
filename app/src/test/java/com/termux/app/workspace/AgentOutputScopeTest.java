package com.termux.app.workspace;

import android.app.Application;
import com.termux.terminal.TerminalSession;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class AgentOutputScopeTest {
    private boolean frames(TerminalSession s)throws Exception{Field f=TerminalSession.class.getDeclaredField("synchronizedFrames");f.setAccessible(true);return f.get(s)!=null;}
    @Test public void onlyGeminiAndOpenCodeOptIntoFrameAndTranscriptHandling()throws Exception{
        for(String agent:new String[]{"gemini","opencode","codex","claude","ssh","termux","ubuntu"}){
            TerminalSession s=new TerminalSession("unused","unused",new String[0],new String[0],2000,null);WorkspaceStore.Session p=new WorkspaceStore.Session();p.geminiAgent=agent.equals("gemini");p.claudeAgent=agent.equals("claude");p.aiAgent=agent.equals("opencode")?agent:"";p.environment=agent;
            WorkspaceController.configureAgentOutput(s,p);boolean expected=agent.equals("gemini")||agent.equals("opencode");assertEquals(agent,expected,s.preservesTranscriptScroll());assertEquals(agent,expected,frames(s));assertEquals(agent,agent.equals("opencode"),s.usesStableResize());
        }
    }
    @Test public void attachmentRefreshDisablesFlagsForUnrelatedProfile()throws Exception{
        TerminalSession s=new TerminalSession("unused","unused",new String[0],new String[0],2000,null);WorkspaceStore.Session p=new WorkspaceStore.Session();p.aiAgent="opencode";WorkspaceController.configureAgentOutput(s,p);assertTrue(frames(s));
        WorkspaceController.configureAgentOutput(s,new WorkspaceStore.Session());assertFalse(frames(s));assertFalse(s.preservesTranscriptScroll());assertFalse(s.usesStableResize());WorkspaceController.configureAgentOutput(s,null);assertFalse(frames(s));
    }
}
