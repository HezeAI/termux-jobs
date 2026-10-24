#define _GNU_SOURCE
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/gl.h>
#include <GL/glx.h>
#include <GL/glxext.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <assert.h>
#include <signal.h>
#include <ucontext.h>
#include <execinfo.h>
#include <unistd.h>
static double seconds(void){struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return t.tv_sec+t.tv_nsec/1e9;}
static void phase(Display*d,Window w,const char*name,int interval){
 PFNGLXSWAPINTERVALEXTPROC set=(PFNGLXSWAPINTERVALEXTPROC)glXGetProcAddressARB((const GLubyte*)"glXSwapIntervalEXT");
 const char*ext=glXQueryExtensionsString(d,DefaultScreen(d));
 unsigned actual=999;
 if(set&&ext&&strstr(ext,"GLX_EXT_swap_control")){set(d,w,interval);glXQueryDrawable(d,w,GLX_SWAP_INTERVAL_EXT,&actual);}
 glFinish();XSync(d,False);unsigned n=0;double start=seconds();
 do{glClearColor(.125,.75,.25,1);glClear(GL_COLOR_BUFFER_BIT);glXSwapBuffers(d,w);n++;}while(seconds()-start<1.0&&n<10000);
 glFinish();XSync(d,False);double elapsed=seconds()-start;
 printf("PHASE %s requested=%d reported=%u frames=%u seconds=%.6f fps=%.2f\n",name,interval,actual,n,elapsed,n/elapsed);
}
static void fault(int sig,siginfo_t *info,void *context){
 ucontext_t *u=context;dprintf(2,"FAULT signal=%d address=%p pc=%p\n",sig,info->si_addr,(void*)u->uc_mcontext.pc);
 FILE *maps=fopen("/proc/self/maps","r");char line[1024];unsigned long low,high;
 while(maps&&fgets(line,sizeof(line),maps))if(sscanf(line,"%lx-%lx",&low,&high)==2&&u->uc_mcontext.pc>=low&&u->uc_mcontext.pc<high)dprintf(2,"PC MAP %s",line);
 if(maps){fclose(maps);}
 void *frames[32];int count=backtrace(frames,32);backtrace_symbols_fd(frames,count,2);_exit(128+sig);
}
int main(void){struct sigaction sa={.sa_sigaction=fault,.sa_flags=SA_SIGINFO};sigemptyset(&sa.sa_mask);sigaction(SIGSEGV,&sa,NULL);sigaction(SIGBUS,&sa,NULL);setvbuf(stdout,NULL,_IONBF,0);Display*d=XOpenDisplay(NULL);assert(d);int a[]={GLX_RGBA,GLX_DOUBLEBUFFER,GLX_RED_SIZE,8,GLX_GREEN_SIZE,8,GLX_BLUE_SIZE,8,None};XVisualInfo*v=glXChooseVisual(d,DefaultScreen(d),a);assert(v);
 XSetWindowAttributes attrs={.colormap=XCreateColormap(d,RootWindow(d,v->screen),v->visual,AllocNone)};
 Window w=XCreateWindow(d,RootWindow(d,v->screen),0,0,800,600,0,v->depth,InputOutput,v->visual,CWColormap,&attrs);XMapWindow(d,w);XSync(d,False);
 GLXContext c=glXCreateContext(d,v,NULL,True);assert(c);assert(glXMakeCurrent(d,w,c));printf("RENDERER %s\n",glGetString(GL_RENDERER));glViewport(0,0,800,600);
 phase(d,w,"visible-vsync",getenv("JOBS_START_ZERO")?0:1);phase(d,w,"visible-zero",0);
 Window cover=XCreateSimpleWindow(d,RootWindow(d,v->screen),0,0,800,600,0,0,0);XMapRaised(d,cover);XSync(d,False);phase(d,w,"covered-zero",0);XImage *covered=XGetImage(d,cover,100,100,1,1,AllPlanes,ZPixmap);assert(covered);assert((XGetPixel(covered,0,0)&0xffffff)==0);XDestroyImage(covered);puts("PASS covered window stays black");XDestroyWindow(d,cover);XSync(d,False);phase(d,w,"visible-again-zero",0);phase(d,w,"visible-vsync-again",1);
 XImage*im=XGetImage(d,w,100,100,1,1,AllPlanes,ZPixmap);assert(im);unsigned pixel=XGetPixel(im,0,0)&0xffffff;XDestroyImage(im);printf("PIXEL %06x\n",pixel);assert(((pixel>>8)&255)>=189&&((pixel>>8)&255)<=193);assert(((pixel>>16)&255)>=30&&((pixel>>16)&255)<=34);assert((pixel&255)>=62&&(pixel&255)<=66);assert(glGetError()==GL_NO_ERROR);
 glXMakeCurrent(d,None,NULL);glXDestroyContext(d,c);XDestroyWindow(d,w);XFreeColormap(d,attrs.colormap);XFree(v);XCloseDisplay(d);return 0;
}
