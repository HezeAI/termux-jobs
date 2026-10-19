package com.termux.app.workspace;

/** One paste/submit per captured Gemini run, with cancellation that can never type in a new tab. */
final class GeminiInputDispatch {
    interface Target { boolean current(); void paste(String text); void enter(); void cancelled(String text); }
    interface Scheduler { void later(Runnable task,long milliseconds); }
    private long generation;
    private boolean pending;
    boolean pending(){return pending;}
    void cancel(){generation++;pending=false;}
    boolean send(String text,Target target,Scheduler scheduler){
        if(pending||!target.current())return false;
        pending=true;long token=++generation;target.paste(text);
        // Pinned CLI has 30ms fast-Return and 40ms unsafe-paste guards.
        scheduler.later(()->{if(token!=generation)return;pending=false;if(target.current())target.enter();else target.cancelled(text);},160);
        return true;
    }
}
