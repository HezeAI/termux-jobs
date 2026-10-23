/* SPDX-License-Identifier: MIT
 * Alpine/musl PRoot desktop compatibility, loaded only by x11-session.sh.
 * Not a diagnostic module. Never loaded into Android or another environment.
 */
#define _GNU_SOURCE
#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>

int faccessat(int dirfd,const char *path,int mode,int flags) {
    int (*real_access)(int,const char *,int,int)=dlsym(RTLD_NEXT,"faccessat");
    if(!real_access){errno=ENOSYS;return -1;}
    int result=real_access(dirfd,path,mode,flags);
    /* F_OK tests existence only: no read/write/effective-UID permission decision
     * is weakened. Preserve no-follow semantics, including dangling symlinks.
     * Do not replace valid calls or errors other than this unsupported flag pair.
     */
    if(result<0&&errno==EINVAL&&mode==F_OK&&flags==(AT_EACCESS|AT_SYMLINK_NOFOLLOW)){
        struct stat st;
        return fstatat(dirfd,path,&st,AT_SYMLINK_NOFOLLOW);
    }
    return result;
}

/* Glycin 2 public enum: Auto=0, Bwrap=1, FlatpakSpawn=2, NotSandboxed=3.
 * PRoot cannot create Bubblewrap's namespaces. This affects only image decoding
 * in the opted-in Alpine desktop; it is not an Android/Firefox sandbox switch.
 */
void *gly_loader_new(void *file) {
    /* GdkPixbuf can itself be loaded RTLD_LOCAL (Python/GI, plugins). RTLD_NEXT
     * from a global preload cannot resolve such a local dependency. Use the
     * already-loaded exact ABI library, without loading a second implementation.
     */
    void *library=dlopen("libglycin-2.so.0",RTLD_LAZY|RTLD_NOLOAD);
    if(!library){errno=ENOSYS;return 0;}
    void *(*real_new)(void *)=dlsym(library,"gly_loader_new");
    void (*selector)(void *,int)=dlsym(library,"gly_loader_set_sandbox_selector");
    if(!real_new||!selector){dlclose(library);errno=ENOSYS;return 0;}
    void *loader=real_new(file);
    if(loader)selector(loader,3);
    dlclose(library);
    return loader;
}

/* Thumbnail generation also ENCodes a PNG into its private cache. The creator
 * uses a separate sandbox selector; changing only the decoder is insufficient.
 * Keep this on the same exact ABI and within the already opted-in Alpine tree.
 */
void *gly_creator_new(const char *mimetype,void **error) {
    void *library=dlopen("libglycin-2.so.0",RTLD_LAZY|RTLD_NOLOAD);
    if(!library){errno=ENOSYS;return 0;}
    void *(*real_new)(const char *,void **)=dlsym(library,"gly_creator_new");
    void (*selector)(void *,int)=dlsym(library,"gly_creator_set_sandbox_selector");
    if(!real_new||!selector){dlclose(library);errno=ENOSYS;return 0;}
    void *creator=real_new(mimetype,error);
    if(creator)selector(creator,3);
    dlclose(library);
    return creator;
}
