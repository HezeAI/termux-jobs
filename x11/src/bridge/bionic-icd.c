#define _GNU_SOURCE
#define VK_NO_PROTOTYPES
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <dlfcn.h>
#include <hardware/hwvulkan.h>
#include <hybris/common/dlfcn.h>
struct AHardwareBuffer;
struct AHardwareBuffer_Desc;
static pthread_once_t once=PTHREAD_ONCE_INIT;
static hwvulkan_device_t *hal;
static int (*allocate_ahb)(const struct AHardwareBuffer_Desc*,struct AHardwareBuffer**);
static void (*describe_ahb)(const struct AHardwareBuffer*,struct AHardwareBuffer_Desc*);
static void (*release_ahb)(struct AHardwareBuffer*);
static int (*send_ahb)(const struct AHardwareBuffer*,int);
static void initialize(void){
 const char *path=getenv("JOBS_BIONIC_DRIVER");
 if(!path||path[0]!='/'){fprintf(stderr,"Bionic for glibc: explicit driver path required\n");return;}
 void *driver=hybris_dlopen(path,RTLD_NOW);
 if(!driver){fprintf(stderr,"Bionic for glibc: %s\n",hybris_dlerror());return;}
 hwvulkan_module_t *module=hybris_dlsym(driver,"HMI");
 if(!module||!module->common.methods||module->common.methods->open(&module->common,"vk0",(hw_device_t**)&hal)!=0){hal=NULL;fprintf(stderr,"Bionic for glibc: Vulkan HAL unavailable\n");return;}
 void *native=hybris_dlopen("libnativewindow.so",RTLD_NOW);
 if(native){allocate_ahb=hybris_dlsym(native,"AHardwareBuffer_allocate");describe_ahb=hybris_dlsym(native,"AHardwareBuffer_describe");release_ahb=hybris_dlsym(native,"AHardwareBuffer_release");send_ahb=hybris_dlsym(native,"AHardwareBuffer_sendHandleToUnixSocket");}
}
VKAPI_ATTR VkResult VKAPI_CALL vk_icdNegotiateLoaderICDInterfaceVersion(uint32_t *version){if(!version)return VK_ERROR_INCOMPATIBLE_DRIVER;if(*version>5)*version=5;return VK_SUCCESS;}
static VkResult VKAPI_CALL enumerate_version(uint32_t *version){
 pthread_once(&once,initialize);if(!hal)return VK_ERROR_INCOMPATIBLE_DRIVER;
 PFN_vkEnumerateInstanceVersion fn=(PFN_vkEnumerateInstanceVersion)hal->GetInstanceProcAddr(VK_NULL_HANDLE,"vkEnumerateInstanceVersion");
 if(fn)return fn(version);
 *version=VK_API_VERSION_1_0;return VK_SUCCESS;
}
VKAPI_ATTR PFN_vkVoidFunction VKAPI_CALL vk_icdGetInstanceProcAddr(VkInstance instance,const char *name){
 pthread_once(&once,initialize);if(!hal||!name)return NULL;
 if(!strcmp(name,"vkEnumerateInstanceVersion"))return (PFN_vkVoidFunction)enumerate_version;
 if(!strcmp(name,"vkEnumerateInstanceExtensionProperties"))return (PFN_vkVoidFunction)hal->EnumerateInstanceExtensionProperties;
 if(!strcmp(name,"vkCreateInstance"))return (PFN_vkVoidFunction)hal->CreateInstance;
 return hal->GetInstanceProcAddr(instance,name);
}
VKAPI_ATTR PFN_vkVoidFunction VKAPI_CALL vk_icdGetPhysicalDeviceProcAddr(VkInstance instance,const char *name){return vk_icdGetInstanceProcAddr(instance,name);}
void AHardwareBuffer_release(struct AHardwareBuffer *buffer){pthread_once(&once,initialize);if(release_ahb)release_ahb(buffer);}
int AHardwareBuffer_sendHandleToUnixSocket(const struct AHardwareBuffer *buffer,int socket){pthread_once(&once,initialize);return send_ahb?send_ahb(buffer,socket):-ENOSYS;}

int AHardwareBuffer_allocate(const struct AHardwareBuffer_Desc *desc,struct AHardwareBuffer **buffer){pthread_once(&once,initialize);return allocate_ahb?allocate_ahb(desc,buffer):-ENOSYS;}
void AHardwareBuffer_describe(const struct AHardwareBuffer *buffer,struct AHardwareBuffer_Desc *desc){pthread_once(&once,initialize);if(describe_ahb)describe_ahb(buffer,desc);}
