package com.termux.app.workspace;

import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.function.LongSupplier;

/** Estimates only. Contains no Activity, worker or file contents. No timer or disk persistence. */
final class BackupInventoryCache {
    static final BackupInventoryCache SHARED=new BackupInventoryCache(android.os.SystemClock::elapsedRealtime);
    private static final long MAX_AGE=600000;
    private static final int MAX_BYTES=4*1024*1024;
    static final class Entry {
        final JSONObject inventory,folders;final long created;final int bytes;
        Entry(JSONObject i,JSONObject f,long c,int b){inventory=i;folders=f;created=c;bytes=b;}
    }
    private final LongSupplier clock;
    private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();
    BackupInventoryCache(LongSupplier clock){this.clock=clock;}
    synchronized void clear(){entries.clear();}
    synchronized Entry get(String key){Entry e=entries.get(key);if(e!=null&&(clock.getAsLong()<e.created||clock.getAsLong()-e.created>=MAX_AGE)){entries.remove(key);return null;}return e;}
    synchronized void put(String key,JSONObject inventory,JSONObject folders){
        try{
            int bytes=inventory.toString().getBytes(StandardCharsets.UTF_8).length+folders.toString().getBytes(StandardCharsets.UTF_8).length;
            Entry previous=get(key);entries.remove(key);if(bytes>MAX_BYTES)return;
            long created=inventory.optBoolean("reusedEstimate")&&previous!=null?previous.created:clock.getAsLong();
            entries.put(key,new Entry(inventory,new JSONObject(folders.toString()),created,bytes));
            int used=0;for(Entry e:entries.values())used+=e.bytes;
            while(used>MAX_BYTES&&!entries.isEmpty()){String oldest=entries.keySet().iterator().next();used-=entries.remove(oldest).bytes;}
        }catch(Exception ignored){entries.remove(key);}
    }
}
