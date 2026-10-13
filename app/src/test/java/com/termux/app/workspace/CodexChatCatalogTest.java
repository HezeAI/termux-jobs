package com.termux.app.workspace;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE)
public class CodexChatCatalogTest {
    @Test public void allChatsIncludesRootHistoryForAnotherProject() {
        CodexChatCatalog.Entry chat=new CodexChatCatalog.Entry("thread-one","Форк Termux","/root",1);
        assertTrue(CodexChatCatalog.visible(chat,"ubuntu","/root/termux-fork",false,""));
        assertFalse(CodexChatCatalog.visible(chat,"ubuntu","/root/termux-fork",true,""));
        assertTrue(CodexChatCatalog.visible(chat,"ubuntu","/root/",true,""));
        assertTrue(CodexChatCatalog.visible(chat,"ubuntu","/root/termux-fork",false,"/root"));
        assertFalse(CodexChatCatalog.visible(chat,"ubuntu","/root/termux-fork",false,"other"));
    }
    @Test public void serverListingDoesNotRestrictDirectoryAndKeepsPagination() throws Exception {
        JSONObject params=CodexChatCatalog.params("cursor-value");assertFalse(params.has("cwd"));assertEquals("cursor-value",params.getString("cursor"));assertFalse(params.getBoolean("archived"));assertFalse(CodexChatCatalog.params(null).has("cursor"));
    }
    @Test public void titlesAndOriginsArePreservedButInvalidIdsAreExcluded() throws Exception {
        JSONObject data=new JSONObject().put("id","valid-thread").put("name","Работа над проектом").put("cwd","/root").put("updatedAt",123);
        CodexChatCatalog.Entry chat=CodexChatCatalog.parse(data);assertNotNull(chat);assertEquals("/root",chat.cwd);assertEquals(123,chat.updated);
        data.put("id","bad; command");assertNull(CodexChatCatalog.parse(data));
    }
}
