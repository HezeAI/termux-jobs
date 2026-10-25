/* Session-only dispatch for DroidDeck's GPL-3.0 DRM/udev adaptations.
 * All unknown devices, sockets and ioctls retain their original libc behavior.
 * Loaded only into the private Arch KDE process tree, never into terminal/AI VMs.
 */
#define _GNU_SOURCE
#include <dlfcn.h>
#include <errno.h>
#include <stdarg.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
extern int bl_udevmon_stand_in(int fd);
extern int bl_drm_gem_close(int fd,unsigned long request,void *arg,int *rc);
int bind(int fd,const struct sockaddr *addr,socklen_t size){
    if(bl_udevmon_stand_in(fd))return 0;
    int (*real)(int,const struct sockaddr*,socklen_t)=dlsym(RTLD_NEXT,"bind");
    if(!real){errno=ENOSYS;return -1;}return real(fd,addr,size);
}
int ioctl(int fd,unsigned long request,...){
    va_list ap;va_start(ap,request);void *arg=va_arg(ap,void*);va_end(ap);int rc;
    if(bl_drm_gem_close(fd,request,arg,&rc))return rc;
    int (*real)(int,unsigned long,...)=dlsym(RTLD_NEXT,"ioctl");
    if(!real){errno=ENOSYS;return -1;}return real(fd,request,arg);
}
