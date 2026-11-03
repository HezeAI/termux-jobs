#define _GNU_SOURCE
/* Test-only seccomp supervisor. Socket descriptors are created here and injected
 * into the filtered tree. connect/bind are executed on these retained descriptors,
 * with copied, checked addresses: no CONTINUE or sockaddr TOCTOU exception.
 * Only listeners bound by this tree can be reached. No host firewall changes. */
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <dirent.h>
#include <unistd.h>
#include <fcntl.h>
#include <signal.h>
#include <poll.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/uio.h>
#include <linux/filter.h>
#include <linux/seccomp.h>
#include <linux/audit.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#if defined(__aarch64__)
#define GUARD_ARCH AUDIT_ARCH_AARCH64
#elif defined(__x86_64__)
#define GUARD_ARCH AUDIT_ARCH_X86_64
#else
#error Unsupported audit architecture
#endif
struct entry {int fd;unsigned long inode;};
struct listener {struct sockaddr_storage address;socklen_t length;};
static struct entry sockets[8192];static size_t socket_count;
static struct listener endpoints[1024];static size_t endpoint_count;
static FILE *logfile;
static void event(const char *name,int number,int error){fprintf(logfile,"{\"event\":\"%s\",\"syscall\":%d,\"errno\":%d}\n",name,number,error);fflush(logfile);}
static void address_event(const char *name,int number,const struct sockaddr_storage *a){
 char ip[INET6_ADDRSTRLEN]="unix";unsigned int port=0;
 if(a->ss_family==AF_INET){inet_ntop(AF_INET,&((const struct sockaddr_in*)a)->sin_addr,ip,sizeof(ip));port=ntohs(((const struct sockaddr_in*)a)->sin_port);}
 else if(a->ss_family==AF_INET6){inet_ntop(AF_INET6,&((const struct sockaddr_in6*)a)->sin6_addr,ip,sizeof(ip));port=ntohs(((const struct sockaddr_in6*)a)->sin6_port);}
 fprintf(logfile,"{\"event\":\"%s\",\"syscall\":%d,\"family\":%u,\"address\":\"%s\",\"port\":%u}\n",name,number,(unsigned int)a->ss_family,ip,port);fflush(logfile);
}
static void sendfd(int socket,int fd){char data='x',space[CMSG_SPACE(sizeof(int))]={0};struct iovec io={&data,1};struct msghdr msg={.msg_iov=&io,.msg_iovlen=1,.msg_control=space,.msg_controllen=sizeof(space)};struct cmsghdr *c=CMSG_FIRSTHDR(&msg);c->cmsg_level=SOL_SOCKET;c->cmsg_type=SCM_RIGHTS;c->cmsg_len=CMSG_LEN(sizeof(int));memcpy(CMSG_DATA(c),&fd,sizeof(fd));if(sendmsg(socket,&msg,0)!=1)_exit(125);}
static int recvfd(int socket){char data,space[CMSG_SPACE(sizeof(int))]={0};struct iovec io={&data,1};struct msghdr msg={.msg_iov=&io,.msg_iovlen=1,.msg_control=space,.msg_controllen=sizeof(space)};if(recvmsg(socket,&msg,0)!=1)return -1;struct cmsghdr *c=CMSG_FIRSTHDR(&msg);if(!c||c->cmsg_level!=SOL_SOCKET||c->cmsg_type!=SCM_RIGHTS)return -1;int fd;memcpy(&fd,CMSG_DATA(c),sizeof(fd));return fd;}
static int known_socket(pid_t pid,int fd){char name[80],link[80];snprintf(name,sizeof(name),"/proc/%d/fd/%d",pid,fd);ssize_t n=readlink(name,link,sizeof(link)-1);if(n<0)return -1;link[n]=0;unsigned long inode;if(sscanf(link,"socket:[%lu]",&inode)!=1)return -1;for(size_t i=0;i<socket_count;i++)if(sockets[i].inode==inode)return sockets[i].fd;return -1;}
static int copy_address(struct seccomp_notif *q,struct sockaddr_storage *address,socklen_t *length){
 uint64_t n=q->data.args[2];if(n<sizeof(sa_family_t)||n>sizeof(*address))return -1;memset(address,0,sizeof(*address));struct iovec local={address,n},remote={(void*)(uintptr_t)q->data.args[1],n};if(process_vm_readv(q->pid,&local,1,&remote,1,0)!=(ssize_t)n)return -1;*length=(socklen_t)n;return 0;
}
static int loopback(const struct sockaddr_storage *a,socklen_t n){
 if(a->ss_family==AF_INET&&n>=sizeof(struct sockaddr_in))return ((const struct sockaddr_in*)a)->sin_addr.s_addr==htonl(INADDR_LOOPBACK);
 if(a->ss_family==AF_INET6&&n>=sizeof(struct sockaddr_in6))return IN6_IS_ADDR_LOOPBACK(&((const struct sockaddr_in6*)a)->sin6_addr);
 return a->ss_family==AF_UNIX&&n>offsetof(struct sockaddr_un,sun_path)+1&&((const struct sockaddr_un*)a)->sun_path[0]==0;
}
static int equal_address(const struct listener *p,const struct sockaddr_storage *a,socklen_t n){
 if(p->address.ss_family!=a->ss_family)return 0;
 if(a->ss_family==AF_INET)return ((const struct sockaddr_in*)&p->address)->sin_port==((const struct sockaddr_in*)a)->sin_port;
 if(a->ss_family==AF_INET6)return ((const struct sockaddr_in6*)&p->address)->sin6_port==((const struct sockaddr_in6*)a)->sin6_port;
 return p->length==n&&!memcmp(&p->address,a,n);
}
static int install(void){
 #define TRAP(n) BPF_JUMP(BPF_JMP|BPF_JEQ|BPF_K,n,0,1),BPF_STMT(BPF_RET|BPF_K,SECCOMP_RET_USER_NOTIF)
 #define DENY(n) BPF_JUMP(BPF_JMP|BPF_JEQ|BPF_K,n,0,1),BPF_STMT(BPF_RET|BPF_K,SECCOMP_RET_ERRNO|EPERM)
 struct sock_filter filter[]={
 BPF_STMT(BPF_LD|BPF_W|BPF_ABS,offsetof(struct seccomp_data,arch)),BPF_JUMP(BPF_JMP|BPF_JEQ|BPF_K,GUARD_ARCH,1,0),BPF_STMT(BPF_RET|BPF_K,SECCOMP_RET_KILL_PROCESS),
 BPF_STMT(BPF_LD|BPF_W|BPF_ABS,offsetof(struct seccomp_data,nr)),TRAP(__NR_socket),TRAP(__NR_connect),TRAP(__NR_bind),TRAP(__NR_socketpair),TRAP(__NR_setsockopt),
 DENY(__NR_io_uring_setup),DENY(__NR_bpf),
 BPF_STMT(BPF_RET|BPF_K,SECCOMP_RET_ALLOW)};
 struct sock_fprog p={.len=sizeof(filter)/sizeof(filter[0]),.filter=filter};if(prctl(PR_SET_NO_NEW_PRIVS,1,0,0,0))return -1;return syscall(__NR_seccomp,SECCOMP_SET_MODE_FILTER,SECCOMP_FILTER_FLAG_NEW_LISTENER,&p);
}
static void handle(int listener,struct seccomp_notif *q){
 if(ioctl(listener,SECCOMP_IOCTL_NOTIF_ID_VALID,&q->id))return;
 struct seccomp_notif_resp r={.id=q->id,.error=-EPERM};int nr=q->data.nr;
 if(nr==__NR_socket){
  int family=q->data.args[0],type=q->data.args[1],protocol=q->data.args[2],base=type&~(SOCK_CLOEXEC|SOCK_NONBLOCK);
  if((family==AF_INET||family==AF_INET6||family==AF_UNIX)&&base==SOCK_STREAM&&socket_count<8192){
   int fd=socket(family,type,protocol);if(fd<0){r.error=-errno;goto answer;}
   struct stat st;if(fstat(fd,&st)){close(fd);goto answer;}
   struct seccomp_notif_addfd add={.id=q->id,.flags=SECCOMP_ADDFD_FLAG_SEND,.srcfd=fd,.newfd_flags=type&SOCK_CLOEXEC?O_CLOEXEC:0};int childfd=ioctl(listener,SECCOMP_IOCTL_NOTIF_ADDFD,&add);
   if(childfd>=0){sockets[socket_count++]=(struct entry){fd,st.st_ino};return;}close(fd);r.error=-EPERM;
  }
  event("socket-denied",nr,-r.error);
 }else if(nr==__NR_socketpair){
  int base=(int)q->data.args[1]&~(SOCK_CLOEXEC|SOCK_NONBLOCK);
  /* AF_UNIX socketpairs only connect this tree's already-owned descriptors. */
  if(q->data.args[0]==AF_UNIX&&base==SOCK_STREAM){r.error=0;r.flags=SECCOMP_USER_NOTIF_FLAG_CONTINUE;}else event("socketpair-denied",nr,EPERM);
 }else if(nr==__NR_setsockopt){
  /* Only immutable numeric arguments decide this exception; never route options. */
  int level=q->data.args[1],option=q->data.args[2];
  if(level!=IPPROTO_IP&&(level!=IPPROTO_IPV6||option==IPV6_V6ONLY)){r.error=0;r.flags=SECCOMP_USER_NOTIF_FLAG_CONTINUE;}else event("routing-option-denied",nr,EPERM);
 }else{
  struct sockaddr_storage address;socklen_t length;int fd=known_socket(q->pid,(int)q->data.args[0]);
  if(fd<0||copy_address(q,&address,&length)||!loopback(&address,length)){event("address-denied",nr,EPERM);goto answer;}
  if(nr==__NR_bind){
   if(endpoint_count>=1024)goto answer;
   /* Only fresh ephemeral TCP listeners; Unix names must be unique to this run. */
   if((address.ss_family==AF_INET&&((struct sockaddr_in*)&address)->sin_port)||(address.ss_family==AF_INET6&&((struct sockaddr_in6*)&address)->sin6_port))goto answer;
   int result=bind(fd,(struct sockaddr*)&address,length);if(result<0){r.error=-errno;goto answer;}
   struct listener e={.length=sizeof(e.address)};if(getsockname(fd,(struct sockaddr*)&e.address,&e.length))goto answer;endpoints[endpoint_count++]=e;r.error=0;r.val=0;address_event("listener-registered",nr,&e.address);
  }else{
   int allowed=0;for(size_t i=0;i<endpoint_count;i++)if(equal_address(&endpoints[i],&address,length)){allowed=1;break;}
   if(!allowed){address_event("unregistered-destination-denied",nr,&address);goto answer;}
   int result=connect(fd,(struct sockaddr*)&address,length);r.error=result<0?-errno:0;r.val=result;event("local-connect",nr,-r.error);
  }
 }
answer:
 /* Never continue connect/bind: only the checked copy above reaches the kernel. */
 if(ioctl(listener,SECCOMP_IOCTL_NOTIF_SEND,&r)&&errno!=ENOENT)event("notification-response-error",nr,errno);
}
int main(int argc,char **argv){
 if(argc<4||strcmp(argv[2],"--")){fprintf(stderr,"usage: local-network-only LOG -- COMMAND [ARGS]\n");return 125;}
 /* Do not inherit preconnected sockets outside the controlled tree. */
 for(int fd=0;fd<3;fd++){struct stat st;if(!fstat(fd,&st)&&S_ISSOCK(st.st_mode)){fprintf(stderr,"refuse inherited network stdio\n");return 125;}}
 DIR *directory=opendir("/proc/self/fd");if(!directory){perror("owned descriptors");return 125;}int directory_fd=dirfd(directory);struct dirent *item;while((item=readdir(directory))){char *end;long fd=strtol(item->d_name,&end,10);if(!*end&&fd>=3&&fd!=directory_fd)close((int)fd);}closedir(directory);
 logfile=fopen(argv[1],"w");if(!logfile){perror("guard log");return 125;}setvbuf(logfile,NULL,_IOLBF,0);
 int pair[2];if(socketpair(AF_UNIX,SOCK_STREAM|SOCK_CLOEXEC,0,pair)){perror("bootstrap socketpair");return 125;}
 pid_t child=fork();if(child<0)return 125;
 if(!child){if(prctl(PR_SET_PDEATHSIG,SIGTERM))_exit(125);close(pair[0]);fclose(logfile);int listener=install();if(listener<0){perror("seccomp listener");_exit(125);}sendfd(pair[1],listener);close(listener);close(pair[1]);execvp(argv[3],argv+3);perror("exec");_exit(125);}
 close(pair[1]);int listener=recvfd(pair[0]);close(pair[0]);if(listener<0){int status;waitpid(child,&status,0);return 125;}
 int status=0,ended=0;struct pollfd poller={.fd=listener,.events=POLLIN};
 while(!ended){int ready=poll(&poller,1,100);if(ready>0&&(poller.revents&POLLIN)){struct seccomp_notif q={0};if(!ioctl(listener,SECCOMP_IOCTL_NOTIF_RECV,&q))handle(listener,&q);else if(errno!=ENOENT&&errno!=EINTR)event("notification-read-error",-1,errno);}pid_t p=waitpid(child,&status,WNOHANG);if(p==child)ended=1;}
 close(listener);for(size_t i=0;i<socket_count;i++)close(sockets[i].fd);fclose(logfile);return WIFEXITED(status)?WEXITSTATUS(status):128+WTERMSIG(status);
}
