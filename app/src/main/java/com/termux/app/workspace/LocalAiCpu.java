package com.termux.app.workspace;
/** Persisted CPU policy; zero preserves the previous automatic limit. */
final class LocalAiCpu {
    static void validate(int n){if(n!=0&&n!=2&&n!=4&&n!=6&&n!=8)throw new IllegalArgumentException("Invalid CPU thread count");}
    static int resolve(int n,int available){validate(n);return Math.max(1,Math.min(n==0?4:n,Math.min(8,available)));}
}
