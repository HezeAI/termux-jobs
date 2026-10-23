package com.termux.x11;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Minimal bounded X11 test client. No shell, filesystem browsing or model calls. */
public final class X11Protocol {
    private final InputStream in;private final OutputStream out;
    private int base,root,visual,depth,window,gc;
    public X11Protocol(InputStream in,OutputStream out){this.in=in;this.out=out;}
    static ByteBuffer buffer(int n){return ByteBuffer.allocate(n).order(ByteOrder.LITTLE_ENDIAN);}
    public static byte[] handshake(byte[] cookie){
        if(cookie.length!=16)throw new IllegalArgumentException("Invalid cookie");byte[] name="MIT-MAGIC-COOKIE-1".getBytes(StandardCharsets.US_ASCII);
        ByteBuffer b=buffer(48);b.put((byte)'l').put((byte)0).putShort((short)11).putShort((short)0).putShort((short)name.length).putShort((short)cookie.length).putShort((short)0);b.put(name);b.position(32);b.put(cookie);return b.array();
    }
    private byte[] read(int length)throws IOException{if(length<0||length>262144)throw new IOException("X11 reply too large");byte[] bytes=new byte[length];int at=0;while(at<length){int n=in.read(bytes,at,length-at);if(n<0)throw new EOFException("X11 connection closed");at+=n;}return bytes;}
    public void setup(byte[] cookie)throws IOException {
        out.write(handshake(cookie));out.flush();byte[] header=read(8);ByteBuffer h=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);int length=Short.toUnsignedInt(h.getShort(6))*4;byte[] body=read(length);
        if(header[0]!=1)throw new IOException("X11 authentication or setup failed");
        if(body.length<32)throw new IOException("Short X11 setup");ByteBuffer b=ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        base=b.getInt(4);int vendor=Short.toUnsignedInt(b.getShort(16)),formats=body[21]&255;int screen=32+((vendor+3)&~3)+formats*8;
        if((body[20]&255)<1||screen+40>body.length)throw new IOException("Missing X11 screen");
        root=b.getInt(screen);visual=b.getInt(screen+32);depth=body[screen+38]&255;
        int mask=b.getInt(8);if(Integer.bitCount(mask)<4)throw new IOException("Invalid X11 resource mask");
        int bit=Integer.lowestOneBit(mask);window=base|bit;gc=base|Integer.lowestOneBit(mask^bit);
    }
    private void send(ByteBuffer b)throws IOException{out.write(b.array());out.flush();}
    private void color(int color)throws IOException{ByteBuffer b=buffer(16);b.put((byte)56).put((byte)0).putShort((short)4).putInt(gc).putInt(4).putInt(color);send(b);}
    private void rectangle(int x,int y,int w,int h)throws IOException{ByteBuffer b=buffer(20);b.put((byte)70).put((byte)0).putShort((short)5).putInt(window).putInt(gc).putShort((short)x).putShort((short)y).putShort((short)w).putShort((short)h);send(b);}
    public void createWindow()throws IOException {
        ByteBuffer b=buffer(40);b.put((byte)1).put((byte)depth).putShort((short)10).putInt(window).putInt(root).putShort((short)24).putShort((short)24).putShort((short)560).putShort((short)360).putShort((short)0).putShort((short)1).putInt(visual).putInt(2|2048).putInt(0x101b22).putInt(1|2|4|8|64|32768);send(b);
        b=buffer(20);b.put((byte)55).put((byte)0).putShort((short)5).putInt(gc).putInt(window).putInt(4).putInt(0x79dfcd);send(b);
        b=buffer(8);b.put((byte)8).put((byte)0).putShort((short)2).putInt(window);send(b);
        b=buffer(12);b.put((byte)42).put((byte)2).putShort((short)3).putInt(window).putInt(0);send(b);paint();
    }
    public void paint()throws IOException{color(0x79dfcd);rectangle(24,24,512,72);color(0x254758);rectangle(24,120,240,192);color(0x397469);rectangle(288,120,248,192);}
    /** Returns only event names/counts to UI; typed characters and cookie never enter logs. */
    public String event()throws IOException{
        byte[] bytes=read(32);int type=bytes[0]&127;
        if(type==0)throw new IOException("X11 protocol error "+(bytes[1]&255)+" (request "+(bytes[10]&255)+")");
        if(type==1||type==35){int n=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(4);if(n<0||n>65536)throw new IOException("X11 event too large");read(n*4);}
        if(type==12){paint();return "expose";}if(type==2)return "key";if(type==4)return "click";if(type==6)return "pointer";return "";
    }
}
