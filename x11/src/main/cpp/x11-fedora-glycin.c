/* SPDX-License-Identifier: MIT
 * Fedora/glibc PRoot image compatibility. No filesystem permission overrides.
 */
#define _GNU_SOURCE
#include <dlfcn.h>
#include <errno.h>

/* Glycin 2 public enum: Auto=0, Bwrap=1, FlatpakSpawn=2, NotSandboxed=3.
 * PRoot cannot create Bubblewrap's namespaces. This affects only image decoding
 * in the Fedora PRoot desktop; it is not an Android/Firefox sandbox switch.
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
 * Keep this on the same exact ABI and within the already Fedora desktop tree.
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
