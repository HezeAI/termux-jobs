package com.termux.app.workspace;

import java.util.concurrent.*;

/** Serial mutations/shared transport; bounded isolated lanes for independent read-only transports. */
final class ServerWorkQueues implements AutoCloseable {
    final ExecutorService foreground=Executors.newSingleThreadExecutor();
    final ThreadPoolExecutor monitor=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),new ThreadPoolExecutor.AbortPolicy());
    final ThreadPoolExecutor helpers=new ThreadPoolExecutor(2,2,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(2),new ThreadPoolExecutor.AbortPolicy());
    Future<?> helper(Runnable work){return helpers.submit(work);}
    void cancelHelper(Future<?> task){if(task!=null)task.cancel(true);helpers.purge();}
    private static void stop(ExecutorService executor){for(Runnable task:executor.shutdownNow())if(task instanceof Future<?>)((Future<?>)task).cancel(false);}
    @Override public void close(){stop(foreground);stop(monitor);stop(helpers);}
}
