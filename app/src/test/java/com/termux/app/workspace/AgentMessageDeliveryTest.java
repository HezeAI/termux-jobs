package com.termux.app.workspace;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE)
public class AgentMessageDeliveryTest {
 @Test public void onlyAcknowledgedAdmissionAllowsComposerClearing()throws Exception{
  for(String state:new String[]{"current","queued","started","shell","submitted"}){
   AgentMessageDelivery r=new AgentMessageDelivery(new JSONObject().put("ok",true).put("accepted",true).put("delivery",state));assertTrue(r.accepted);assertEquals(state,r.state);
  }
  for(JSONObject raw:new JSONObject[]{null,new JSONObject().put("ok",true),new JSONObject().put("ok",false).put("delivery","rejected"),new JSONObject().put("ok",false).put("delivery","unconfirmed")})assertFalse(new AgentMessageDelivery(raw).accepted);
 }
 @Test public void cachedUnconfirmedResultDoesNotInviteNewReplay()throws Exception{
  AgentMessageDelivery r=new AgentMessageDelivery(new JSONObject().put("ok",false).put("delivery","unconfirmed").put("replayed",true));
  assertTrue(r.notice(true).contains("новой отправки не было"));assertTrue(r.notice(false).contains("nothing was resent"));assertTrue(r.notice(false).contains("Shell"));
 }
 @Test public void queueAndCurrentAcksNeverClaimModelUptake()throws Exception{
  AgentMessageDelivery queued=new AgentMessageDelivery(new JSONObject().put("ok",true).put("accepted",true).put("delivery","queued"));assertTrue(queued.notice(false).contains("may not join the current"));
  AgentMessageDelivery current=new AgentMessageDelivery(new JSONObject().put("ok",true).put("accepted",true).put("delivery","current"));assertTrue(current.notice(false).contains("does not confirm"));assertTrue(current.notice(true).contains("не подтверждает"));
 }
 @Test public void claudeNativeQueueIsExplicitWithoutChangingOtherAgents()throws Exception{
  AgentMessageDelivery q=new AgentMessageDelivery(new JSONObject().put("ok",true).put("accepted",true).put("delivery","queued").put("ack","command-lifecycle:queued").put("queuedWhileBusy",true));assertEquals("В очереди — после текущей задачи ▾",q.caption(true));assertEquals("Queued — after the current task ▾",q.caption(false));assertTrue(q.notice(true).contains("Подтверждения включения"));
  AgentMessageDelivery idle=new AgentMessageDelivery(new JSONObject().put("ok",true).put("accepted",true).put("delivery","queued").put("ack","command-lifecycle:queued"));assertEquals("In CLI queue ▾",idle.caption(false));
  AgentMessageDelivery other=new AgentMessageDelivery(new JSONObject().put("ok",true).put("accepted",true).put("delivery","queued").put("ack","other-agent-queue"));assertEquals("In CLI queue ▾",other.caption(false));
  AgentMessageDelivery current=new AgentMessageDelivery(new JSONObject().put("ok",true).put("accepted",true).put("delivery","current").put("ack","native-user-message-association"));assertEquals("CLI подтвердил приём ▾",current.caption(true));assertFalse(current.notice(true).contains("для текущего запроса"));
 }
}
