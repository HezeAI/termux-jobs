package com.termux.x11;

import android.os.*;
/** Private, bounded same-UID protocol. No caller-supplied shell commands or paths. */
public final class X11Remote {
    public static final String DESCRIPTOR="com.termux.x11.PRIVATE_DESKTOP_V1";
    public static final int CALL=IBinder.FIRST_CALL_TRANSACTION;
    public static Bundle call(IBinder binder,Bundle request)throws RemoteException{
        Parcel data=Parcel.obtain(),reply=Parcel.obtain();
        try{data.writeInterfaceToken(DESCRIPTOR);data.writeBundle(request);if(!binder.transact(CALL,data,reply,0))throw new RemoteException("X11 interface unavailable");reply.readException();Bundle result=reply.readBundle(X11Remote.class.getClassLoader());return result==null?new Bundle():result;}
        finally{data.recycle();reply.recycle();}
    }
    public static Bundle command(String op){Bundle b=new Bundle();b.putString("op",op);return b;}
}
