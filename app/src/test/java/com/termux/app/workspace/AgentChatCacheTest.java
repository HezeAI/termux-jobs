package com.termux.app.workspace;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=28,application=android.app.Application.class)
public class AgentChatCacheTest {
 @Test public void entriesAreBoundedAndTakingOneTransfersOwnership()throws Exception{AgentChatCache c=new AgentChatCache();for(int i=0;i<10;i++){AgentChatCache.State s=new AgentChatCache.State();s.rows.put("key",new JSONObject().put("body","owned"));c.put("owner"+i,s);}assertEquals(8,c.size());assertNull(c.take("owner0"));assertNotNull(c.take("owner9"));assertNull(c.take("owner9"));c.clear();assertEquals(0,c.characters());}
 @Test public void largePresentationsKeepRecentRowsWithinGlobalBudget()throws Exception{AgentChatCache c=new AgentChatCache();for(int i=0;i<12;i++){AgentChatCache.State s=new AgentChatCache.State();for(int j=0;j<60;j++)s.rows.put("r"+j,new JSONObject().put("body",new String(new char[12000]).replace('\0','x')));c.put("owner"+i,s);assertTrue(c.characters()<=AgentChatCache.MAX_TOTAL_CHARS);}AgentChatCache.State latest=c.take("owner11");assertNotNull(latest);assertTrue(latest.trimmed);assertTrue(latest.rows.containsKey("r59"));assertFalse(latest.rows.containsKey("r0"));}
}
