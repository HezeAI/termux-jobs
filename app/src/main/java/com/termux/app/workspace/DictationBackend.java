package com.termux.app.workspace;

/** A single capture. Implementations never send text to an agent. */
interface DictationBackend {
    interface Listener {
        void ready();
        void partial(String text);
        void result(String text);
        void error(String detail);
    }
    interface Factory { DictationBackend create(); }
    void start(Listener listener);
    void stop();
    void cancel();
}
