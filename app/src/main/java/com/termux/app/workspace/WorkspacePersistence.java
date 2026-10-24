package com.termux.app.workspace;

import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** One FIFO writer per workspace file, shared across overlapping Activity lifetimes. */
final class WorkspacePersistence implements AutoCloseable {
    private static final Map<String,State> FILES=new HashMap<>();
    private static final class State {
        final String key;final AtomicFile file;
        final ExecutorService writer=Executors.newSingleThreadExecutor();
        final Set<WorkspacePersistence> owners=new HashSet<>();byte[] pending;Future<?> lastWrite;
        State(String key,AtomicFile file){this.key=key;this.file=file;}
    }
    private final State state;private final Runnable failed;private boolean closed;
    WorkspacePersistence(AtomicFile file,Runnable failed){
        this.failed=failed;
        String key;try{key=file.getBaseFile().getCanonicalPath();}catch(IOException error){key=file.getBaseFile().getAbsolutePath();}
        synchronized(FILES){State found=FILES.get(key);if(found==null){found=new State(key,file);FILES.put(key,found);}state=found;state.owners.add(this);}
    }
    byte[] read()throws IOException{
        synchronized(FILES){
            // A writer registers pending before it can touch AtomicFile. Reads
            // during queued/in-flight writes use immutable snapshots, not .bak.
            if(state.pending!=null)return state.pending;
            File base=state.file.getBaseFile();
            if(!base.exists()&&!new File(base.getPath()+".bak").exists())return null;
            return state.file.readFully();
        }
    }
    void write(byte[] snapshot,Runnable completed){
        synchronized(FILES){
            if(closed)return;
            state.pending=snapshot;
            state.lastWrite=state.writer.submit(()->{
                FileOutputStream out=null;boolean success=false;List<Runnable> failures=new ArrayList<>();
                try{out=state.file.startWrite();out.write(snapshot);state.file.finishWrite(out);success=true;}
                catch(IOException|RuntimeException error){if(out!=null)try{state.file.failWrite(out);}catch(RuntimeException ignored){}}
                finally{synchronized(FILES){if(state.pending==snapshot)state.pending=null;if(!success)for(WorkspacePersistence owner:state.owners)failures.add(owner.failed);dispose(state);}}
                if(success)completed.run();else for(Runnable notify:failures)notify.run();
            });
        }
    }
    void awaitWrites()throws Exception{Future<?> last;synchronized(FILES){last=state.lastWrite;}if(last!=null)last.get(10,TimeUnit.SECONDS);}
    private static void dispose(State state){
        if(state.owners.isEmpty()&&state.pending==null){FILES.remove(state.key,state);state.writer.shutdown();}
    }
    @Override public void close(){synchronized(FILES){if(closed)return;closed=true;state.owners.remove(this);dispose(state);}}
}
