package com.termux.app.workspace;

import java.util.concurrent.*;

/** Serial mutations/shared transport; bounded isolated lanes for independent read-only transports. */
final class ServerWorkQueues implements AutoCloseable {
    private interface Timed {long queuedAt();}
    private static final class Task<V> extends FutureTask<V> implements Timed {final long created=System.nanoTime();Task(Callable<V> work){super(work);}public long queuedAt(){return created;}}
    private static final class Work implements Runnable,Timed {final Runnable work;final long created=System.nanoTime();Work(Runnable r){work=r;}public long queuedAt(){return created;}public void run(){work.run();}}
    private static final class Queue extends ThreadPoolExecutor {
        Queue(int count,BlockingQueue<Runnable> waiting){super(count,count,0,TimeUnit.MILLISECONDS,waiting,new AbortPolicy());}
        @Override protected <T> RunnableFuture<T> newTaskFor(Callable<T> task){return new Task<>(task);}
        @Override protected <T> RunnableFuture<T> newTaskFor(Runnable task,T result){return new Task<>(Executors.callable(task,result));}
        @Override public void execute(Runnable task){super.execute(task instanceof Timed?task:new Work(task));}
        @Override protected void beforeExecute(Thread thread,Runnable task){ServerDiagnostics.queueStarted((System.nanoTime()-((Timed)task).queuedAt())/1000000);}
        @Override protected void afterExecute(Runnable task,Throwable error){ServerDiagnostics.queueFinished();}
    }
    final ExecutorService foreground=new Queue(1,new LinkedBlockingQueue<>());
    final ThreadPoolExecutor monitor=new Queue(1,new ArrayBlockingQueue<>(1));
    final ThreadPoolExecutor helpers=new Queue(2,new ArrayBlockingQueue<>(2));
    Future<?> helper(Runnable work){return helpers.submit(work);}
    void cancelHelper(Future<?> task){if(task!=null)task.cancel(true);helpers.purge();}
    private static void stop(ExecutorService executor){for(Runnable task:executor.shutdownNow())if(task instanceof Future<?>)((Future<?>)task).cancel(false);}
    @Override public void close(){stop(foreground);stop(monitor);stop(helpers);}
}
