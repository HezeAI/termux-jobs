#define _GNU_SOURCE
#include <sys/socket.h>
#include <sys/un.h>
#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <dlfcn.h>
#include <errno.h>
/* Private test harness only: redirect just this test display, no other sockets. */
int connect(int fd,const struct sockaddr *address,socklen_t length){
 int (*next)(int,const struct sockaddr*,socklen_t)=dlsym(RTLD_NEXT,"connect");if(!next){errno=ENOSYS;return -1;}
 const char *display=getenv("JOBS_X11_DISPLAY"),*socketPath=getenv("JOBS_X11_SOCKET");
 if(address&&address->sa_family==AF_UNIX&&display&&socketPath&&length>offsetof(struct sockaddr_un,sun_path)){
  const struct sockaddr_un *a=(const struct sockaddr_un*)address;size_t bytes=length-offsetof(struct sockaddr_un,sun_path);if(bytes>sizeof(a->sun_path))bytes=sizeof(a->sun_path);
  const char *path=a->sun_path;if(bytes&&!*path){path++;bytes--;}if(bytes&&path[bytes-1]==0)bytes--;
  char expected[80];int n=snprintf(expected,sizeof(expected),"/tmp/.X11-unix/X%s",display);
  if(n>0&&(size_t)n==bytes&&!memcmp(path,expected,bytes)&&strlen(socketPath)+1<sizeof(a->sun_path)){
   struct sockaddr_un target={.sun_family=AF_UNIX};memcpy(target.sun_path+1,socketPath,strlen(socketPath));return next(fd,(const struct sockaddr*)&target,offsetof(struct sockaddr_un,sun_path)+1+strlen(socketPath));
  }
 }
 return next(fd,address,length);
}
