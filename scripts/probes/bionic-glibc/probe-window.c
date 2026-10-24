#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include <signal.h>
#include <ucontext.h>
#include <execinfo.h>
#define _GNU_SOURCE
#define VK_NO_PROTOTYPES
#define VK_USE_PLATFORM_XCB_KHR
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <unistd.h>
#include <dlfcn.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <xcb/xcb.h>
#include <vulkan/vulkan.h>
#define NEED(x) do{if(!(x)){fprintf(stderr,"FAILED: %s at %d\n",#x,__LINE__);return 2;}}while(0)
#define OK(x) do{VkResult result=(x);if(result!=VK_SUCCESS){fprintf(stderr,"FAILED: %s -> %d at %d\n",#x,result,__LINE__);return 3;}}while(0)
#define INST(n) PFN_##n n=(PFN_##n)gipa(instance,#n);NEED(n)
#define DEV(n) PFN_##n n=(PFN_##n)vkGetDeviceProcAddr(device,#n);NEED(n)
static void fault(int sig,siginfo_t *info,void *context){
 ucontext_t *u=context;dprintf(2,"FAULT: signal=%d code=%d address=%p pc=%p\n",sig,info->si_code,info->si_addr,(void*)u->uc_mcontext.pc);
 FILE *maps=fopen("/proc/self/maps","r");char line[1024];unsigned long low,high;while(maps&&fgets(line,sizeof(line),maps)){if(sscanf(line,"%lx-%lx",&low,&high)==2&&u->uc_mcontext.pc>=low&&u->uc_mcontext.pc<high)dprintf(2,"PC MAP: %s",line);}if(maps)fclose(maps);
 void *frames[32];int count=backtrace(frames,32);backtrace_symbols_fd(frames,count,2);_exit(128+sig);
}
int main(void){struct sigaction sa={.sa_sigaction=fault,.sa_flags=SA_SIGINFO};sigemptyset(&sa.sa_mask);sigaction(SIGBUS,&sa,NULL);sigaction(SIGSEGV,&sa,NULL);
 setvbuf(stdout,NULL,_IONBF,0);puts("STAGE: connect private X11");
 const char *address=getenv("JOBS_X11_SOCKET"),*cookiePath=getenv("JOBS_X11_COOKIE_PATH");NEED(address&&cookiePath);
 char cookie[16];FILE *f=fopen(cookiePath,"rb");NEED(f&&fread(cookie,1,sizeof(cookie),f)==sizeof(cookie));fclose(f);
 int fd=socket(AF_UNIX,SOCK_STREAM,0);NEED(fd>=0);struct sockaddr_un target={.sun_family=AF_UNIX};NEED(strlen(address)+1<sizeof(target.sun_path));memcpy(target.sun_path+1,address,strlen(address));NEED(connect(fd,(struct sockaddr*)&target,offsetof(struct sockaddr_un,sun_path)+1+strlen(address))==0);
 xcb_auth_info_t auth={.namelen=18,.name="MIT-MAGIC-COOKIE-1",.datalen=16,.data=cookie};xcb_connection_t *c=xcb_connect_to_fd(fd,&auth);NEED(c&&!xcb_connection_has_error(c));
 xcb_screen_t *screen=xcb_setup_roots_iterator(xcb_get_setup(c)).data;NEED(screen);xcb_window_t window=xcb_generate_id(c);uint32_t black=0;
 xcb_create_window(c,XCB_COPY_FROM_PARENT,window,screen->root,10,10,256,256,0,XCB_WINDOW_CLASS_INPUT_OUTPUT,screen->root_visual,XCB_CW_BACK_PIXEL,&black);xcb_map_window(c,window);xcb_flush(c);
 void *loader=dlopen("libvulkan.so.1",RTLD_NOW|RTLD_GLOBAL);NEED(loader);PFN_vkGetInstanceProcAddr gipa=dlsym(loader,"vkGetInstanceProcAddr");NEED(gipa);
 PFN_vkCreateInstance create=(PFN_vkCreateInstance)gipa(NULL,"vkCreateInstance");NEED(create);
 const char *extensions[]={VK_KHR_SURFACE_EXTENSION_NAME,VK_KHR_XCB_SURFACE_EXTENSION_NAME,VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME};
 VkApplicationInfo app={.sType=VK_STRUCTURE_TYPE_APPLICATION_INFO,.pApplicationName="Termux Jobs Bionic for glibc private X11 probe",.apiVersion=VK_API_VERSION_1_1};VkInstanceCreateInfo ii={.sType=VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO,.pApplicationInfo=&app,.enabledExtensionCount=3,.ppEnabledExtensionNames=extensions};VkInstance instance;
 puts("STAGE: Vulkan instance and XCB surface");OK(create(&ii,NULL,&instance));
 INST(vkEnumeratePhysicalDevices);INST(vkGetPhysicalDeviceProperties);INST(vkGetPhysicalDeviceQueueFamilyProperties);INST(vkCreateXcbSurfaceKHR);INST(vkGetPhysicalDeviceSurfaceSupportKHR);INST(vkGetPhysicalDeviceSurfaceCapabilitiesKHR);INST(vkGetPhysicalDeviceSurfaceFormatsKHR);INST(vkCreateDevice);INST(vkGetDeviceProcAddr);INST(vkDestroySurfaceKHR);INST(vkDestroyInstance);
 uint32_t count=4;VkPhysicalDevice physical[4];OK(vkEnumeratePhysicalDevices(instance,&count,physical));NEED(count>0);VkPhysicalDeviceProperties props;vkGetPhysicalDeviceProperties(physical[0],&props);printf("GPU: %s\n",props.deviceName);
 VkXcbSurfaceCreateInfoKHR si={.sType=VK_STRUCTURE_TYPE_XCB_SURFACE_CREATE_INFO_KHR,.connection=c,.window=window};VkSurfaceKHR surface;OK(vkCreateXcbSurfaceKHR(instance,&si,NULL,&surface));
 uint32_t qcount=0;vkGetPhysicalDeviceQueueFamilyProperties(physical[0],&qcount,NULL);NEED(qcount>0&&qcount<64);VkQueueFamilyProperties families[64];vkGetPhysicalDeviceQueueFamilyProperties(physical[0],&qcount,families);uint32_t family=0;
 for(;family<qcount;family++){VkBool32 supported=0;OK(vkGetPhysicalDeviceSurfaceSupportKHR(physical[0],family,surface,&supported));if((families[family].queueFlags&VK_QUEUE_GRAPHICS_BIT)&&supported)break;}NEED(family<qcount);
 VkSurfaceCapabilitiesKHR caps;OK(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physical[0],surface,&caps));count=0;OK(vkGetPhysicalDeviceSurfaceFormatsKHR(physical[0],surface,&count,NULL));NEED(count>0&&count<64);VkSurfaceFormatKHR formats[64];OK(vkGetPhysicalDeviceSurfaceFormatsKHR(physical[0],surface,&count,formats));
 printf("Surface formats:");for(uint32_t i=0;i<count;i++)printf(" %u",formats[i].format);puts("");
 VkSurfaceFormatKHR format=formats[0];for(uint32_t i=0;i<count;i++){if(formats[i].format==VK_FORMAT_R8G8B8A8_UNORM)format=formats[i];}printf("Surface format: %u\n",format.format);
 float priority=1;VkDeviceQueueCreateInfo qi={.sType=VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO,.queueFamilyIndex=family,.queueCount=1,.pQueuePriorities=&priority};const char *deviceExtensions[]={VK_KHR_SWAPCHAIN_EXTENSION_NAME};VkDeviceCreateInfo di={.sType=VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO,.queueCreateInfoCount=1,.pQueueCreateInfos=&qi,.enabledExtensionCount=1,.ppEnabledExtensionNames=deviceExtensions};VkDevice device;
 puts("STAGE: create Vulkan device");OK(vkCreateDevice(physical[0],&di,NULL,&device));
 DEV(vkResetFences);DEV(vkResetCommandPool);DEV(vkGetDeviceQueue);DEV(vkCreateSwapchainKHR);DEV(vkGetSwapchainImagesKHR);DEV(vkCreateFence);DEV(vkWaitForFences);DEV(vkAcquireNextImageKHR);DEV(vkCreateCommandPool);DEV(vkAllocateCommandBuffers);DEV(vkBeginCommandBuffer);DEV(vkCmdPipelineBarrier);DEV(vkCmdClearColorImage);DEV(vkEndCommandBuffer);DEV(vkQueueSubmit);DEV(vkQueueWaitIdle);DEV(vkQueuePresentKHR);DEV(vkDestroyFence);DEV(vkDestroyCommandPool);DEV(vkDestroySwapchainKHR);DEV(vkDestroyDevice);
 VkQueue queue;vkGetDeviceQueue(device,family,0,&queue);VkExtent2D extent=caps.currentExtent;if(extent.width==UINT32_MAX)extent=(VkExtent2D){256,256};uint32_t imageCount=caps.minImageCount>2?caps.minImageCount:2;if(caps.maxImageCount&&imageCount>caps.maxImageCount)imageCount=caps.maxImageCount;
 VkSwapchainCreateInfoKHR sci={.sType=VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR,.surface=surface,.minImageCount=imageCount,.imageFormat=format.format,.imageColorSpace=format.colorSpace,.imageExtent=extent,.imageArrayLayers=1,.imageUsage=VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT|VK_IMAGE_USAGE_TRANSFER_DST_BIT,.imageSharingMode=VK_SHARING_MODE_EXCLUSIVE,.preTransform=caps.currentTransform,.compositeAlpha=VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR,.presentMode=VK_PRESENT_MODE_FIFO_KHR,.clipped=VK_TRUE};VkSwapchainKHR swapchain;
 puts("STAGE: create Android-buffer X11 swapchain");VkResult swap_result=vkCreateSwapchainKHR(device,&sci,NULL,&swapchain);
 const char *expected_error=getenv("JOBS_EXPECT_SWAPCHAIN_ERROR");if(expected_error){NEED(swap_result==(VkResult)strtol(expected_error,NULL,10));vkDestroyDevice(device,NULL);vkDestroySurfaceKHR(instance,surface,NULL);vkDestroyInstance(instance,NULL);xcb_destroy_window(c,window);xcb_disconnect(c);puts("PASS: expected swapchain failure and orderly cleanup");return 0;}OK(swap_result);count=16;VkImage images[16];OK(vkGetSwapchainImagesKHR(device,swapchain,&count,images));NEED(count>0&&count<=16);
 VkFence fence;VkFenceCreateInfo fi={.sType=VK_STRUCTURE_TYPE_FENCE_CREATE_INFO};OK(vkCreateFence(device,&fi,NULL,&fence));
 VkCommandPool pool;VkCommandPoolCreateInfo pi={.sType=VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO,.queueFamilyIndex=family};OK(vkCreateCommandPool(device,&pi,NULL,&pool));unsigned repeats=getenv("JOBS_REUSE_FRAMES")?(unsigned)strtoul(getenv("JOBS_REUSE_FRAMES"),NULL,10):1;NEED(repeats>=1&&repeats<=1000);
 for(unsigned frame=0;frame<repeats;frame++){
 OK(vkResetFences(device,1,&fence));OK(vkResetCommandPool(device,pool,0));uint32_t imageIndex;OK(vkAcquireNextImageKHR(device,swapchain,5000000000ULL,VK_NULL_HANDLE,fence,&imageIndex));OK(vkWaitForFences(device,1,&fence,VK_TRUE,5000000000ULL));VkCommandBuffer command;VkCommandBufferAllocateInfo ci={.sType=VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO,.commandPool=pool,.level=VK_COMMAND_BUFFER_LEVEL_PRIMARY,.commandBufferCount=1};OK(vkAllocateCommandBuffers(device,&ci,&command));VkCommandBufferBeginInfo begin={.sType=VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO,.flags=VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT};OK(vkBeginCommandBuffer(command,&begin));
 VkImageSubresourceRange range={.aspectMask=VK_IMAGE_ASPECT_COLOR_BIT,.levelCount=1,.layerCount=1};VkImageMemoryBarrier barrier={.sType=VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER,.dstAccessMask=VK_ACCESS_TRANSFER_WRITE_BIT,.oldLayout=VK_IMAGE_LAYOUT_UNDEFINED,.newLayout=VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,.srcQueueFamilyIndex=VK_QUEUE_FAMILY_IGNORED,.dstQueueFamilyIndex=VK_QUEUE_FAMILY_IGNORED,.image=images[imageIndex],.subresourceRange=range};vkCmdPipelineBarrier(command,VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,VK_PIPELINE_STAGE_TRANSFER_BIT,0,0,NULL,0,NULL,1,&barrier);
 VkClearColorValue color={.float32={.125f,.75f,.25f,1.f}};vkCmdClearColorImage(command,images[imageIndex],VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,&color,1,&range);barrier.srcAccessMask=VK_ACCESS_TRANSFER_WRITE_BIT;barrier.dstAccessMask=0;barrier.oldLayout=VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;barrier.newLayout=VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;vkCmdPipelineBarrier(command,VK_PIPELINE_STAGE_TRANSFER_BIT,VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,0,0,NULL,0,NULL,1,&barrier);OK(vkEndCommandBuffer(command));
 VkSubmitInfo submit={.sType=VK_STRUCTURE_TYPE_SUBMIT_INFO,.commandBufferCount=1,.pCommandBuffers=&command};OK(vkQueueSubmit(queue,1,&submit,VK_NULL_HANDLE));OK(vkQueueWaitIdle(queue));VkPresentInfoKHR present={.sType=VK_STRUCTURE_TYPE_PRESENT_INFO_KHR,.swapchainCount=1,.pSwapchains=&swapchain,.pImageIndices=&imageIndex};puts("STAGE: present GPU image into X11 window");OK(vkQueuePresentKHR(queue,&present));
 uint32_t pixel=0;for(unsigned i=0;i<30;i++){usleep(100000);xcb_get_image_reply_t *reply=xcb_get_image_reply(c,xcb_get_image(c,XCB_IMAGE_FORMAT_Z_PIXMAP,window,128,128,1,1,~0u),NULL);if(reply&&xcb_get_image_data_length(reply)>=4)memcpy(&pixel,xcb_get_image_data(reply),4);free(reply);if(((pixel>>8)&255)>150)break;}
 printf("X11 pixel: 0x%08x\n",pixel);NEED(((pixel>>8)&255)>=189&&((pixel>>8)&255)<=193);NEED(((pixel>>16)&255)>=30&&((pixel>>16)&255)<=34);NEED((pixel&255)>=62&&(pixel&255)<=66);}
 printf("PASS: %u frames reused one swapchain without OUT_OF_DATE\n",repeats);puts("STAGE: orderly cleanup");OK(vkQueueWaitIdle(queue));vkDestroyFence(device,fence,NULL);vkDestroyCommandPool(device,pool,NULL);vkDestroySwapchainKHR(device,swapchain,NULL);vkDestroyDevice(device,NULL);vkDestroySurfaceKHR(instance,surface,NULL);vkDestroyInstance(instance,NULL);xcb_destroy_window(c,window);xcb_disconnect(c);puts("PASS: Imported Bionic driver rendered a verified pixel into X11 through glibc");return 0;
}
