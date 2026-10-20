package com.termux.app.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
public class AgentChatErrorsTest {
 @Test public void regionRefusalIsNotPresentedAsTransientOrGeneric403(){for(boolean ru:new boolean[]{true,false}){String s=AgentChatErrors.explain("403 User location is not supported for the API use.",ru);assertTrue(s.contains(ru?"регион":"region"));assertTrue(s.contains(ru?"не устранит":"will not resolve"));}}
 @Test public void modelQuotaAuthNetworkAndUnknownErrorsHaveDifferentNextSteps(){String[] raw={"Model x was not found or is invalid.","429 RESOURCE_EXHAUSTED","401 UNAUTHENTICATED","503 overloaded","fetch failed","unknown failure"};String[] expected={"Выберите","лимит","авторизац","перегружен","интернет","подробности"};for(int i=0;i<raw.length;i++)assertTrue(AgentChatErrors.explain(raw[i],true).contains(expected[i]));assertFalse(AgentChatErrors.explain("PRIVATE_ERROR_SENTINEL",false).contains("PRIVATE_ERROR_SENTINEL"));}
 @Test public void unsavedThreadExplainsRecoveryWithoutEchoingRawIdentifiers(){for(boolean ru:new boolean[]{true,false}){String value=AgentChatErrors.explain("Codex rejected thread/read: thread not loaded: PRIVATE_THREAD",ru);assertTrue(value.contains(ru?"Новый чат":"New chat"));assertFalse(value.contains("PRIVATE_THREAD"));}}
}
