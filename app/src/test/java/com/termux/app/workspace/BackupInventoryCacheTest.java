package com.termux.app.workspace;
import org.json.JSONObject;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=28,application=android.app.Application.class)
public class BackupInventoryCacheTest {
    @Test public void repeatedEntryUsesEstimateWithoutRenewingExpiry()throws Exception{
        AtomicLong clock=new AtomicLong(100);BackupInventoryCache c=new BackupInventoryCache(clock::get);
        JSONObject value=new JSONObject().put("homeBytes",123);c.put("full",value,new JSONObject());assertSame(value,c.get("full").inventory);
        clock.set(500000);assertNotNull(c.get("full"));clock.set(600100);assertNull(c.get("full"));
    }
    @Test public void folderReclassificationDoesNotExtendSnapshotButFreshRefreshDoes()throws Exception{
        AtomicLong clock=new AtomicLong(100);BackupInventoryCache c=new BackupInventoryCache(clock::get);
        c.put("selected",new JSONObject(),new JSONObject());clock.set(500000);c.put("selected",new JSONObject().put("reusedEstimate",true),new JSONObject());
        clock.set(600100);assertNull(c.get("selected"));c.put("selected",new JSONObject(),new JSONObject());assertNotNull(c.get("selected"));c.clear();assertNull(c.get("selected"));
    }
    @Test public void foldersAreCopiedAndOversizedResultsAreNotKept()throws Exception{
        BackupInventoryCache c=new BackupInventoryCache(()->1L);JSONObject folders=new JSONObject().put("termux","initial");c.put("selected",new JSONObject(),folders);folders.put("termux","changed");assertEquals("initial",c.get("selected").folders.getString("termux"));
        char[] chars=new char[4*1024*1024];java.util.Arrays.fill(chars,'x');c.put("full",new JSONObject().put("large",new String(chars)),new JSONObject());assertNull(c.get("full"));assertNotNull(c.get("selected"));
    }
}
