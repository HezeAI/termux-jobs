#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include <signal.h>
#include <ucontext.h>
#include <execinfo.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/gl.h>
#include <GL/glx.h>
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
static void check(Display *d,Window w,int x,int y,unsigned rgb){
 unsigned got=0;
 for(int attempt=0;attempt<30;attempt++){
  XImage *im=XGetImage(d,w,x,y,1,1,AllPlanes,ZPixmap);assert(im);got=(unsigned)(XGetPixel(im,0,0)&0xffffff);XDestroyImage(im);
  if(got==rgb)return;
  usleep(30000);
 }
 fprintf(stderr,"pixel %d,%d=%06x expected %06x\n",x,y,got,rgb);exit(3);
}
static void frame(Display*d,Window w,int width,int height){
 glDrawBuffer(GL_BACK);glViewport(0,0,width,height);glDisable(GL_SCISSOR_TEST);
 glClearColor(1,0,0,1);glClear(GL_COLOR_BUFFER_BIT);glEnable(GL_SCISSOR_TEST);
 glScissor(0,0,width,height/2);glClearColor(0,0,1,1);glClear(GL_COLOR_BUFFER_BIT);
 glDisable(GL_SCISSOR_TEST);glXSwapBuffers(d,w);glFinish();XSync(d,False);
 assert(glGetError()==GL_NO_ERROR);check(d,w,width/2,2,0xff0000);check(d,w,width/2,height-3,0x0000ff);
}
static void fault(int sig,siginfo_t *info,void *context){
 ucontext_t *u=context;dprintf(2,"FAULT: signal=%d code=%d address=%p pc=%p\n",sig,info->si_code,info->si_addr,(void*)u->uc_mcontext.pc);
 FILE *maps=fopen("/proc/self/maps","r");char line[1024];unsigned long low,high;while(maps&&fgets(line,sizeof(line),maps)){if(sscanf(line,"%lx-%lx",&low,&high)==2&&u->uc_mcontext.pc>=low&&u->uc_mcontext.pc<high)dprintf(2,"PC MAP: %s",line);}if(maps)fclose(maps);
 void *frames[32];int count=backtrace(frames,32);backtrace_symbols_fd(frames,count,2);_exit(128+sig);
}
int main(void){struct sigaction sa={.sa_sigaction=fault,.sa_flags=SA_SIGINFO};sigemptyset(&sa.sa_mask);sigaction(SIGBUS,&sa,NULL);sigaction(SIGSEGV,&sa,NULL);setvbuf(stdout,NULL,_IONBF,0);
 Display*d=XOpenDisplay(NULL);assert(d);int a[]={GLX_RGBA,GLX_DOUBLEBUFFER,GLX_RED_SIZE,8,GLX_GREEN_SIZE,8,GLX_BLUE_SIZE,8,None};
 XVisualInfo*v=glXChooseVisual(d,DefaultScreen(d),a);assert(v);
 XSetWindowAttributes s={.colormap=XCreateColormap(d,RootWindow(d,v->screen),v->visual,AllocNone),.event_mask=StructureNotifyMask};
 Window w=XCreateWindow(d,RootWindow(d,v->screen),0,0,31,23,0,v->depth,InputOutput,v->visual,CWColormap|CWEventMask,&s);
 XMapWindow(d,w);XSync(d,False);GLXContext c=glXCreateContext(d,v,NULL,True);assert(c);assert(glXMakeCurrent(d,w,c));
 printf("renderer: %s\n",glGetString(GL_RENDERER));frame(d,w,31,23);
 XResizeWindow(d,w,49,37);XSync(d,False);frame(d,w,49,37);
 glDrawBuffer(GL_FRONT);glClearColor(0,1,0,1);glClear(GL_COLOR_BUFFER_BIT);glFlush();glFinish();XSync(d,False);check(d,w,10,10,0x00ff00);
 glXMakeCurrent(d,None,NULL);glXDestroyContext(d,c);XDestroyWindow(d,w);XFree(v);XCloseDisplay(d);
 puts("PASS: glibc OpenGL/Zink over Bionic Turnip: X11 pixels, odd widths, orientation, resize and front-buffer flush");return 0;
}
