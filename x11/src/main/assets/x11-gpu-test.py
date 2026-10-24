#!/usr/bin/env python3
"""Jobs GPU Test: standard-library GLX diagnostic; no network or driver changes."""
import argparse
import collections
import ctypes as c
import datetime
import json
import math
import os
from pathlib import Path
import statistics
import sys
import tempfile
import time

VERSION = '1.1'
SCENES = ('TEXTURED CUBE', 'MANY OBJECTS', 'FRAME PATTERN')
# A small built-in pixel font avoids platform fonts, toolkit packages and downloads.
FONT = dict(zip('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', (
 '01110/10001/10001/11111/10001/10001/10001','11110/10001/10001/11110/10001/10001/11110',
 '01111/10000/10000/10000/10000/10000/01111','11110/10001/10001/10001/10001/10001/11110',
 '11111/10000/10000/11110/10000/10000/11111','11111/10000/10000/11110/10000/10000/10000',
 '01111/10000/10000/10111/10001/10001/01111','10001/10001/10001/11111/10001/10001/10001',
 '11111/00100/00100/00100/00100/00100/11111','00111/00010/00010/00010/10010/10010/01100',
 '10001/10010/10100/11000/10100/10010/10001','10000/10000/10000/10000/10000/10000/11111',
 '10001/11011/10101/10101/10001/10001/10001','10001/11001/10101/10011/10001/10001/10001',
 '01110/10001/10001/10001/10001/10001/01110','11110/10001/10001/11110/10000/10000/10000',
 '01110/10001/10001/10001/10101/10010/01101','11110/10001/10001/11110/10100/10010/10001',
 '01111/10000/10000/01110/00001/00001/11110','11111/00100/00100/00100/00100/00100/00100',
 '10001/10001/10001/10001/10001/10001/01110','10001/10001/10001/10001/10001/01010/00100',
 '10001/10001/10001/10101/10101/10101/01010','10001/10001/01010/00100/01010/10001/10001',
 '10001/10001/01010/00100/00100/00100/00100','11111/00001/00010/00100/01000/10000/11111',
 '01110/10001/10011/10101/11001/10001/01110','00100/01100/00100/00100/00100/00100/01110',
 '01110/10001/00001/00010/00100/01000/11111','11110/00001/00001/01110/00001/00001/11110',
 '00010/00110/01010/10010/11111/00010/00010','11111/10000/10000/11110/00001/00001/11110',
 '01110/10000/10000/11110/10001/10001/01110','11111/00001/00010/00100/01000/01000/01000',
 '01110/10001/10001/01110/10001/10001/01110','01110/10001/10001/01111/00001/00001/01110')))
FONT.update({' ': '00000/'*6+'00000', '.':'00000/00000/00000/00000/00000/00110/00110',
 ':':'00000/00110/00110/00000/00110/00110/00000', '-':'00000/00000/00000/11111/00000/00000/00000',
 '/':'00001/00010/00010/00100/01000/01000/10000', '?':'01110/10001/00001/00010/00100/00000/00100',
 '(':'00010/00100/01000/01000/01000/00100/00010', ')':'01000/00100/00010/00010/00010/00100/01000',
 '+':'00000/00100/00100/11111/00100/00100/00000', '%':'11001/11010/00100/01000/10110/00110/00000'})

P = c.c_void_p
U = c.c_ulong
I = c.c_int
F = c.c_float
D = c.c_double

class Visual(c.Structure):
    _fields_ = [('visual', P), ('visualid', U), ('screen', I), ('depth', I), ('class_', I),
                ('red', U), ('green', U), ('blue', U), ('size', I), ('bits', I)]
class Attributes(c.Structure):
    _fields_ = [(n,t) for n,t in [('background_pixmap',U),('background_pixel',U),('border_pixmap',U),('border_pixel',U),
      ('bit_gravity',I),('win_gravity',I),('backing_store',I),('backing_planes',U),('backing_pixel',U),('save_under',I),
      ('event_mask',c.c_long),('do_not_propagate_mask',c.c_long),('override_redirect',I),('colormap',U),('cursor',U)]]
COMMON = [('type', I), ('serial', U), ('send_event', I), ('display', P)]
class Key(c.Structure):
    _fields_ = COMMON + [('window',U),('root',U),('subwindow',U),('time',U),('x',I),('y',I),('x_root',I),('y_root',I),('state',c.c_uint),('keycode',c.c_uint),('same_screen',I)]
class Configure(c.Structure):
    _fields_ = COMMON + [('event',U),('window',U),('x',I),('y',I),('width',I),('height',I),('border',I),('above',U),('override_redirect',I)]
class MessageData(c.Union):
    _fields_ = [('b',c.c_char*20),('s',c.c_short*10),('l',c.c_long*5)]
class Message(c.Structure):
    _fields_ = COMMON + [('window',U),('message_type',U),('format',I),('data',MessageData)]
class Event(c.Union):
    _fields_ = [('pad',c.c_long*24),('type',I),('key',Key),('configure',Configure),('message',Message)]

def library(names):
    for name in names:
        try:return c.CDLL(name)
        except OSError:pass
    raise RuntimeError('Missing graphics library: '+names[0]+'. Reinstall Jobs GPU Test in Programs > Recommended.')

def bind(lib,name,result,args):
    fn=getattr(lib,name);fn.restype=result;fn.argtypes=args;return fn

def percentile(values, fraction):
    data=sorted(values)
    return data[min(len(data)-1, int((len(data)-1)*fraction))] if data else 0.0

def write_report(path, data):
    path=Path(path).absolute()
    if not path.parent.is_dir() or path.is_symlink():raise ValueError('Report destination unavailable or linked')
    fd,temporary=tempfile.mkstemp(prefix='.jobs-gpu-',dir=path.parent)
    try:
        with os.fdopen(fd,'w') as f:
            os.fchmod(f.fileno(),0o600);json.dump(data,f,indent=2);f.write('\n')
        os.replace(temporary,path)
    finally:
        if os.path.exists(temporary):os.unlink(temporary)

def write_new_report(directory, data):
    # Explicit Report presses get separate files; --report retains its fixed path.
    stamp=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    interval=data.get('requestedSwapInterval')
    mode=str(interval) if interval in (0,1) else 'na'
    fd,name=tempfile.mkstemp(prefix='Jobs-GPU-Test-'+stamp+'-vsync-'+mode+'-',suffix='.json',dir=directory)
    os.close(fd)
    path=Path(name)
    try:write_report(path,data)
    except BaseException:
        path.unlink(missing_ok=True)
        raise
    return path

class App:
    def __init__(self,args):
        self.args=args;self.x=library(('libX11.so.6','libX11.so'));self.g=library(('libGL.so.1','libGL.so'))
        self.display=None;self.window=0;self.context=None;self.visual=None;self.colormap=0
        self.running=True;self.scene=args.scene;self.interval=0;self.swap_control=None;self.message=''
        self.samples=collections.deque(maxlen=10000);self.chart=collections.deque(maxlen=120)
        self.frames=0;self.fps=0.;self.ms=0.;self.p95=0.;self.mapped=True;self.snapshot=0.;self.count=0;self.start=time.perf_counter();self.last=self.start
        self.setup_x();self.setup_gl()

    def setup_x(self):
        x=self.x
        signatures={'XOpenDisplay':(P,[c.c_char_p]),'XDefaultScreen':(I,[P]),'XRootWindow':(U,[P,I]),
          'XDisplayWidth':(I,[P,I]),'XDisplayHeight':(I,[P,I]),'XCreateColormap':(U,[P,U,P,I]),
          'XCreateWindow':(U,[P,U,I,I,c.c_uint,c.c_uint,c.c_uint,I,c.c_uint,P,U,c.POINTER(Attributes)]),
          'XStoreName':(I,[P,U,c.c_char_p]),'XMapWindow':(I,[P,U]),'XSync':(I,[P,I]),'XPending':(I,[P]),
          'XNextEvent':(I,[P,c.POINTER(Event)]),'XLookupKeysym':(U,[c.POINTER(Key),I]),
          'XInternAtom':(U,[P,c.c_char_p,I]),'XSetWMProtocols':(I,[P,U,c.POINTER(U),I]),
          'XSendEvent':(I,[P,U,I,c.c_long,c.POINTER(Event)]),'XDestroyWindow':(I,[P,U]),
          'XFreeColormap':(I,[P,U]),'XFree':(I,[P]),'XCloseDisplay':(I,[P]),
          'XGetImage':(P,[P,U,I,I,c.c_uint,c.c_uint,U,I]),'XGetPixel':(U,[P,I,I]),'XDestroyImage':(I,[P])}
        for n,(r,a) in signatures.items():bind(x,n,r,a)
        g=self.g
        bind(g,'glXChooseVisual',c.POINTER(Visual),[P,I,c.POINTER(I)])
        bind(g,'glXCreateContext',P,[P,c.POINTER(Visual),P,I]);bind(g,'glXMakeCurrent',I,[P,U,P])
        bind(g,'glXSwapBuffers',None,[P,U]);bind(g,'glXDestroyContext',None,[P,P])
        bind(g,'glXGetProcAddressARB',P,[c.c_char_p]);bind(g,'glXQueryExtensionsString',c.c_char_p,[P,I])
        self.display=x.XOpenDisplay(None)
        if not self.display:raise RuntimeError('Open GPU TEST inside the running X11 desktop.')
        self.screen=x.XDefaultScreen(self.display);self.root=x.XRootWindow(self.display,self.screen)
        self.width=self.args.width or min(960,max(480,x.XDisplayWidth(self.display,self.screen)-60))
        self.height=self.args.height or min(720,max(360,x.XDisplayHeight(self.display,self.screen)-80))
        self.visual=g.glXChooseVisual(self.display,self.screen,(I*9)(4,5,12,24,8,8,9,8,0))
        if not self.visual:raise RuntimeError('No double-buffered GLX visual with depth buffer.')
        v=self.visual.contents;self.colormap=x.XCreateColormap(self.display,self.root,v.visual,0)
        attributes=Attributes();attributes.colormap=self.colormap;attributes.event_mask=1|4|(1<<17)
        self.window=x.XCreateWindow(self.display,self.root,20,20,self.width,self.height,0,v.depth,1,v.visual,(1<<13)|(1<<11),c.byref(attributes))
        self.delete=x.XInternAtom(self.display,b'WM_DELETE_WINDOW',0);self.protocols=x.XInternAtom(self.display,b'WM_PROTOCOLS',0)
        atom=U(self.delete);x.XSetWMProtocols(self.display,self.window,c.byref(atom),1)
        x.XStoreName(self.display,self.window,b'Termux Jobs GPU Test');x.XMapWindow(self.display,self.window);x.XSync(self.display,0)
        self.context=g.glXCreateContext(self.display,self.visual,None,1)
        if not self.context or not g.glXMakeCurrent(self.display,self.window,self.context):raise RuntimeError('Cannot create the selected OpenGL context.')
        ext=g.glXQueryExtensionsString(self.display,self.screen) or b''
        address=g.glXGetProcAddressARB(b'glXSwapIntervalEXT') if b'GLX_EXT_swap_control' in ext.split() else None
        if address:self.swap_control=c.CFUNCTYPE(None,P,U,I)(address);self.swap_control(self.display,self.window,0)

    def setup_gl(self):
        specs={'glGetString':(c.c_char_p,[c.c_uint]),'glGetError':(c.c_uint,[]),'glClearColor':(None,[F,F,F,F]),
          'glClear':(None,[c.c_uint]),'glViewport':(None,[I,I,I,I]),'glEnable':(None,[c.c_uint]),'glDisable':(None,[c.c_uint]),
          'glMatrixMode':(None,[c.c_uint]),'glLoadIdentity':(None,[]),'glFrustum':(None,[D,D,D,D,D,D]),'glOrtho':(None,[D,D,D,D,D,D]),
          'glTranslatef':(None,[F,F,F]),'glRotatef':(None,[F,F,F,F]),'glScalef':(None,[F,F,F]),
          'glPushMatrix':(None,[]),'glPopMatrix':(None,[]),'glBegin':(None,[c.c_uint]),'glEnd':(None,[]),
          'glColor3f':(None,[F,F,F]),'glVertex2f':(None,[F,F]),'glVertex3f':(None,[F,F,F]),'glTexCoord2f':(None,[F,F]),
          'glGenTextures':(None,[I,c.POINTER(c.c_uint)]),'glBindTexture':(None,[c.c_uint,c.c_uint]),
          'glTexParameteri':(None,[c.c_uint,c.c_uint,I]),'glTexImage2D':(None,[c.c_uint,I,I,I,I,I,c.c_uint,c.c_uint,P]),
          'glGenLists':(c.c_uint,[I]),'glNewList':(None,[c.c_uint,c.c_uint]),'glEndList':(None,[]),
          'glCallList':(None,[c.c_uint]),'glListBase':(None,[c.c_uint]),'glCallLists':(None,[I,c.c_uint,P]),'glFinish':(None,[])}
        for name,(r,a) in specs.items():bind(self.g,name,r,a)
        self.info={name:(self.g.glGetString(key) or b'unknown').decode('utf-8','replace')[:512] for name,key in [('vendor',0x1F00),('renderer',0x1F01),('opengl',0x1F02)]}
        self.font=self.g.glGenLists(128)
        if not self.font:raise RuntimeError('OpenGL display lists unavailable.')
        for code in range(128):
            self.g.glNewList(self.font+code,0x1300);self.g.glBegin(7)
            for y,row in enumerate(FONT.get(chr(code),FONT['?']).split('/')):
                for x,value in enumerate(row):
                    if value=='1':
                        for a,b in ((x,y),(x+1,y),(x+1,y+1),(x,y+1)):self.g.glVertex2f(a,b)
            self.g.glEnd();self.g.glTranslatef(6,0,0);self.g.glEndList()
        texture=c.c_uint();self.g.glGenTextures(1,c.byref(texture));self.g.glBindTexture(0x0DE1,texture)
        pixels=bytes(value for y in range(64) for x in range(64) for value in ((50,195,170) if ((x//8)^(y//8))&1 else (220,238,245)))
        self.g.glTexImage2D(0x0DE1,0,0x1907,64,64,0,0x1907,0x1401,c.cast(c.c_char_p(pixels),P))
        self.g.glTexParameteri(0x0DE1,0x2801,0x2600);self.g.glTexParameteri(0x0DE1,0x2800,0x2600)
        self.cube=self.g.glGenLists(1);self.g.glNewList(self.cube,0x1300);self.g.glBegin(7)
        faces=[((-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)),((1,-1,-1),(-1,-1,-1),(-1,1,-1),(1,1,-1)),
          ((-1,-1,-1),(-1,-1,1),(-1,1,1),(-1,1,-1)),((1,-1,1),(1,-1,-1),(1,1,-1),(1,1,1)),
          ((-1,1,1),(1,1,1),(1,1,-1),(-1,1,-1)),((-1,-1,-1),(1,-1,-1),(1,-1,1),(-1,-1,1))]
        for i,face in enumerate(faces):
            shade=1-i*.09;self.g.glColor3f(shade,shade,shade)
            for uv,vertex in zip(((0,0),(1,0),(1,1),(0,1)),face):self.g.glTexCoord2f(*uv);self.g.glVertex3f(*vertex)
        self.g.glEnd();self.g.glEndList()

    def rect(self,x,y,w,h,color):
        g=self.g;g.glColor3f(*color);g.glBegin(7)
        for a,b in ((x,y),(x+w,y),(x+w,y+h),(x,y+h)):g.glVertex2f(a,b)
        g.glEnd()

    def text(self,x,y,value,scale=2,color=(.77,.87,.9)):
        data=value.upper().encode('ascii','replace');g=self.g;g.glColor3f(*color);g.glPushMatrix();g.glTranslatef(x,y,0);g.glScalef(scale,scale,1)
        g.glListBase(self.font);g.glCallLists(len(data),0x1401,c.cast(c.c_char_p(data),P));g.glPopMatrix()

    def report(self):
        return {'app':'Jobs GPU Test','version':VERSION,'timeUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
          'graphics':self.info,'surface':[self.width,self.height],'scene':SCENES[self.scene],'requestedSwapInterval':self.interval if self.swap_control else None,
          'frames':self.frames,'durationSeconds':round(time.perf_counter()-self.start,3),
          'recentFrameTimesMs':{'samples':len(self.samples),'median':round(statistics.median(self.samples),3) if self.samples else 0,
             'p95':round(percentile(self.samples,.95),3),'p99':round(percentile(self.samples,.99),3)},
          'note':'Diagnostic scene rates include Python/GLX and UI costs; not comparable to glmark2 scores. No automatic flicker verdict.'}

    def save(self):
        try:
            data=self.report()
            if self.args.report:
                path=Path(self.args.report);write_report(path,data)
            else:
                directory=Path('/sdcard/Download') if Path('/sdcard/Download').is_dir() else Path.home()
                path=write_new_report(directory,data)
            self.message='REPORT SAVED: '+path.name
        except (OSError,ValueError):self.message='REPORT NOT SAVED: CHECK DESTINATION'
        print(self.message,flush=True)

    def events(self):
        event=Event()
        while self.x.XPending(self.display):
            self.x.XNextEvent(self.display,c.byref(event))
            if event.type==22:self.width=max(1,event.configure.width);self.height=max(1,event.configure.height)
            elif event.type==18:self.mapped=False
            elif event.type==19:self.mapped=True
            elif event.type==17:self.running=False;self.window=0
            elif event.type==33 and event.message.message_type==self.protocols and event.message.data.l[0]==self.delete:self.running=False
            elif event.type==2:
                key=self.x.XLookupKeysym(c.byref(event.key),0)
                if key==0xff1b:self.running=False
                elif key in (ord('1'),ord('2'),ord('3')):self.scene=key-ord('1');self.samples.clear()
                elif key==0xffc8:self.fullscreen()
                elif key in (ord('s'),ord('S')):self.save()
            elif event.type==4 and event.key.keycode==1 and self.height-61<=event.key.y<=self.height-17:
                index=int((event.key.x-12)//max(1,(self.width-24)/4))
                if index==0:self.scene=(self.scene+1)%3;self.samples.clear()
                elif index==1 and self.swap_control:self.interval=1-self.interval;self.swap_control(self.display,self.window,self.interval);self.samples.clear()
                elif index==2:self.save()
                elif index==3:self.running=False

    def fullscreen(self):
        event=Event();event.message.type=33;event.message.display=self.display;event.message.window=self.window;event.message.format=32
        event.message.message_type=self.x.XInternAtom(self.display,b'_NET_WM_STATE',0)
        event.message.data.l[0]=2;event.message.data.l[1]=self.x.XInternAtom(self.display,b'_NET_WM_STATE_FULLSCREEN',0);event.message.data.l[3]=1
        self.x.XSendEvent(self.display,self.root,0,(1<<19)|(1<<20),c.byref(event));self.x.XSync(self.display,0)

    def draw(self,elapsed):
        g=self.g;w,h=self.width,self.height;body=max(1,h-195)
        g.glViewport(0,0,w,h);g.glClearColor(.025,.045,.065,1);g.glClear(0x4000|0x0100)
        g.glViewport(0,110,w,body);g.glMatrixMode(0x1701);g.glLoadIdentity();aspect=w/body;g.glFrustum(-aspect,aspect,-1,1,2,80)
        g.glMatrixMode(0x1700);g.glLoadIdentity();g.glEnable(0x0B71);g.glEnable(0x0DE1)
        g.glTranslatef(0,0,-7 if self.scene!=1 else -16)
        if self.scene==1:
            for y in range(-2,3):
                for x in range(-3,4):
                    g.glPushMatrix();g.glTranslatef(x*2.3,y*2.3,0);g.glRotatef(elapsed*37+x*13+y*17,1,1,.4);g.glScalef(.6,.6,.6);g.glCallList(self.cube);g.glPopMatrix()
        else:g.glRotatef(elapsed*35,1,.7,.3);g.glCallList(self.cube)
        g.glDisable(0x0DE1);g.glDisable(0x0B71);g.glViewport(0,0,w,h);g.glMatrixMode(0x1701);g.glLoadIdentity();g.glOrtho(0,w,h,0,-1,1);g.glMatrixMode(0x1700);g.glLoadIdentity()
        if self.scene==2:
            for y in range(100,max(101,h-120),32):
                for x in range(12,w-12,32):self.rect(x,y,30,30,(.1,.14,.18) if ((x//32+y//32)&1) else (.22,.31,.36))
            self.rect(12+(elapsed*90)%max(1,w-50),100,12,max(1,h-220),(.4,.95,.8))
        self.text(18,16,'JOBS GPU TEST',3,(.45,.92,.8));self.text(18,46,SCENES[self.scene])
        self.text(max(18,w-210),20,'%5.0f FPS'%self.fps,2,(1,.9,.45))
        self.text(max(18,w-210),44,'%5.2f MS'%self.ms)
        length=max(1,(w-36)//12);self.text(18,72,self.info['renderer'][:length])
        self.rect(0,h-110,w,110,(.04,.07,.1));self.text(16,h-99,'FRAME TIME  P95: %.2f MS'%self.p95,1)
        if self.chart:
            g.glColor3f(.35,.9,.75);g.glBegin(3)
            for n,value in enumerate(self.chart):g.glVertex2f(w-180+n*1.3,h-78-min(24,value))
            g.glEnd()
        self.text(16,h-80,(self.message or '1-3 SCENE   F11 FULLSCREEN   S REPORT   ESC EXIT')[:max(1,(w-32)//6)],1)
        bw=(w-24)/4
        for index,title in enumerate(('SCENE '+str(self.scene+1),'VSYNC '+(str(self.interval) if self.swap_control else 'N/A'),'REPORT','EXIT')):
            self.rect(12+index*bw,h-61,bw-7,44,(.09,.19,.21));self.text(20+index*bw,h-45,title,2)

    def verify(self):
        self.g.glFinish();self.x.XSync(self.display,0)
        expected=(10,18,26)
        for _ in range(60):
            image=self.x.XGetImage(self.display,self.window,5,self.height-5,1,1,U(-1),2)
            if image:
                pixel=self.x.XGetPixel(image,0,0);self.x.XDestroyImage(image)
                if all(abs(((pixel>>shift)&255)-value)<=1 for shift,value in zip((16,8,0),expected)):return
            time.sleep(.01)
        raise RuntimeError('Window pixel verification failed')

    def run(self):
        self.start=self.last=self.snapshot=time.perf_counter()
        while self.running:
            self.events()
            if not self.running:break
            if not self.mapped:
                if self.args.seconds and time.perf_counter()-self.start>=self.args.seconds:break
                time.sleep(.05);self.last=time.perf_counter();continue
            now=time.perf_counter();self.draw(now-self.start);self.g.glXSwapBuffers(self.display,self.window)
            end=time.perf_counter();duration=(end-self.last)*1000;self.last=end;self.samples.append(duration);self.chart.append(duration);self.frames+=1;self.count+=1
            if end-self.snapshot>=.5:self.fps=self.count/(end-self.snapshot);self.ms=1000/self.fps;self.count=0;self.snapshot=end;self.p95=percentile(self.samples,.95)
            if self.g.glGetError()!=0:raise RuntimeError('OpenGL scene error')
            if (self.args.frames and self.frames>=self.args.frames) or (self.args.seconds and end-self.start>=self.args.seconds):break
        if self.args.verify:self.verify()
        if self.args.report:self.save()
        print(json.dumps(self.report()),flush=True)

    def close(self):
        if self.display:
            if self.context:self.g.glXMakeCurrent(self.display,0,None);self.g.glXDestroyContext(self.display,self.context)
            if self.window:self.x.XDestroyWindow(self.display,self.window)
            if self.colormap:self.x.XFreeColormap(self.display,self.colormap)
            if self.visual:self.x.XFree(self.visual)
            self.x.XCloseDisplay(self.display);self.display=None

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scene',type=int,choices=range(3),default=0);parser.add_argument('--seconds',type=float,default=0)
    parser.add_argument('--frames',type=int,default=0);parser.add_argument('--width',type=int,default=0);parser.add_argument('--height',type=int,default=0)
    parser.add_argument('--report');parser.add_argument('--verify',action='store_true');args=parser.parse_args()
    if not (0<=args.width<=8192 and 0<=args.height<=8192 and args.frames>=0 and args.seconds>=0):parser.error('Invalid window or run limit')
    app=None
    try:
        # Keep the object available for cleanup if context setup fails partway.
        app=App.__new__(App);app.__init__(args);app.run();return 0
    except (OSError,RuntimeError,ValueError,AttributeError) as error:
        print('Jobs GPU Test: '+str(error),file=sys.stderr);return 1
    finally:
        if app is not None and hasattr(app,'display'):app.close()
if __name__=='__main__':sys.exit(main())
