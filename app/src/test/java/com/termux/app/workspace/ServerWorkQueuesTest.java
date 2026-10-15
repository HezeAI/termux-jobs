package com.termux.app.workspace;
import org.junit.Test;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class ServerWorkQueuesTest {
    @Test public void blockedHelpersDoNotDelayForegroundOrMonitoring()throws Exception{
        ServerWorkQueues queues=new ServerWorkQueues();CountDownLatch started=new CountDownLatch(2),release=new CountDownLatch(1);
        Runnable slow=()->{started.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}};
        try{queues.helper(slow);queues.helper(slow);assertTrue(started.await(3,TimeUnit.SECONDS));assertEquals("manual",queues.foreground.submit(()->"manual").get(3,TimeUnit.SECONDS));assertEquals("metrics",queues.monitor.submit(()->"metrics").get(3,TimeUnit.SECONDS));}finally{release.countDown();queues.close();}
    }
    @Test public void queuedHelperCancellationFreesCapacityWithoutExecutingIt()throws Exception{
        ServerWorkQueues queues=new ServerWorkQueues();CountDownLatch started=new CountDownLatch(2),release=new CountDownLatch(1);Runnable slow=()->{started.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}};
        try{queues.helper(slow);queues.helper(slow);assertTrue(started.await(3,TimeUnit.SECONDS));Future<?> cancelled=queues.helper(()->fail("Cancelled queued request executed"));queues.helper(()->{});try{queues.helper(()->{});fail("Queue must be bounded");}catch(RejectedExecutionException expected){}queues.cancelHelper(cancelled);assertTrue(cancelled.isCancelled());assertEquals(1,queues.helpers.getQueue().size());queues.helper(()->{});}finally{release.countDown();queues.close();}
    }
    @Test public void closingQueuesRejectsNewWork(){ServerWorkQueues queues=new ServerWorkQueues();queues.close();assertTrue(queues.foreground.isShutdown());assertTrue(queues.monitor.isShutdown());assertTrue(queues.helpers.isShutdown());try{queues.helper(()->{});fail();}catch(RejectedExecutionException expected){}}
}
