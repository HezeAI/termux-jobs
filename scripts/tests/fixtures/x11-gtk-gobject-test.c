#define _POSIX_C_SOURCE 200809L
#include <assert.h>
#include <dlfcn.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
typedef unsigned long Type;
typedef struct {Type type;const char *name;unsigned class_size,instance_size;} TypeQuery;
static Type widget_type,container_type,menu_type;
static void *menu,*core;
static void *(*ref_object)(void *);
static unsigned drawn;
/* Only these GTK-facing functions are simulated. Real GLib performs type,
 * GValue, signal query, emission hooks, NO_HOOKS connection and timer dispatch. */
Type gtk_widget_get_type(void){return widget_type;}
Type gtk_container_get_type(void){return container_type;}
Type gtk_menu_get_type(void){return menu_type;}
void *gtk_widget_get_window(void *w){(void)w;return NULL;}
void *gtk_widget_get_parent(void *w){(void)w;return NULL;}
void g_object_get(void *w,const char *key,...){assert(w==menu&&!strcmp(key,"menu"));va_list a;va_start(a,key);void **out=va_arg(a,void **);*out=ref_object(core);va_end(a);}
static void drawn_callback(void *w,void *data){assert(w==menu&&!data);drawn++;}
#define LOAD(ret,name,args) ret (*name) args=(ret (*) args)dlsym(glib,#name);assert(name)
int main(int argc,char **argv){
 assert(argc==3);void *gio=dlopen("libgio-2.0.so.0",RTLD_NOW|RTLD_GLOBAL);assert(gio);void *glib=dlopen("libgobject-2.0.so.0",RTLD_NOW|RTLD_GLOBAL);assert(glib);
 LOAD(Type,g_type_from_name,(const char *));LOAD(void,g_type_query,(Type,TypeQuery *));LOAD(Type,g_type_register_static_simple,(Type,const char *,unsigned,void *,unsigned,void *,unsigned));
 LOAD(void,g_set_prgname,(const char *));LOAD(unsigned,g_signal_newv,(const char *,Type,unsigned,void *,void *,void *,void *,Type,unsigned,Type *));
 LOAD(void *,g_object_new,(Type,const char *,...));LOAD(void,g_object_unref,(void *));LOAD(void,g_signal_emit_by_name,(void *,const char *,...));LOAD(int,g_main_context_iteration,(void *,int));
 LOAD(unsigned long,g_signal_connect_data,(void *,const char *,void (*)(void *,void *),void *,void *,unsigned));
 ref_object=(void *(*)(void *))dlsym(glib,"g_object_ref");assert(ref_object);
 Type root=g_type_from_name("GObject");TypeQuery size;g_type_query(root,&size);
 widget_type=g_type_register_static_simple(root,"GtkWidget",size.class_size,NULL,size.instance_size,NULL,0);
 container_type=g_type_register_static_simple(widget_type,"GtkContainer",size.class_size,NULL,size.instance_size,NULL,0);
 menu_type=g_type_register_static_simple(container_type,"GtkMenu",size.class_size,NULL,size.instance_size,NULL,0);
 Type garcon_gtk=g_type_register_static_simple(menu_type,"GarconGtkMenu",size.class_size,NULL,size.instance_size,NULL,0);
 Type garcon_core=g_type_register_static_simple(root,"GarconMenu",size.class_size,NULL,size.instance_size,NULL,0);
 const char *signals[]={"draw","map","unmap","style-updated","enter-notify-event","leave-notify-event","motion-notify-event"};
 for(unsigned i=0;i<7;i++)assert(g_signal_newv(signals[i],widget_type,2,NULL,NULL,NULL,NULL,4,0,NULL));
 assert(g_signal_newv("add",container_type,2,NULL,NULL,NULL,NULL,4,0,NULL));assert(g_signal_newv("remove",container_type,2,NULL,NULL,NULL,NULL,4,0,NULL));
 assert(g_signal_newv("reload-required",garcon_core,2|64,NULL,NULL,NULL,NULL,4,0,NULL));
 menu=g_object_new(garcon_gtk,NULL);core=g_object_new(garcon_core,NULL);g_set_prgname("xfce4-panel");
 assert(!setenv("JOBS_X11_GTK_OBSERVE","1",1));assert(!setenv("JOBS_X11_FOCUS_TOKEN","a11a11a1-1111-4111-8111-111111111111",1));
 FILE *file=fopen(argv[2],"w");assert(file);fputs("a11a11a1-1111-4111-8111-111111111111 1 on\n",file);fclose(file);
 void *module=dlopen(argv[1],RTLD_NOW|RTLD_LOCAL);assert(module);void (*init)(int *,char ***)=(void (*)(int *,char ***))dlsym(module,"gtk_module_init");assert(init);init(NULL,NULL);
 /* Real GIO monitor + real GValue enum/object marshalling. Emitting metadata
  * notifications here does not change any file or touch a live desktop. */
 void *(*new_file)(const char *)=(void *(*)(const char *))dlsym(gio,"g_file_new_for_path");
 void *(*monitor_file)(void *,unsigned,void *,void **)=(void *(*)(void *,unsigned,void *,void **))dlsym(gio,"g_file_monitor_file");
 int (*cancel_monitor)(void *)=(int (*)(void *))dlsym(gio,"g_file_monitor_cancel");
 LOAD(unsigned,g_quark_from_static_string,(const char *));LOAD(void,g_object_set_qdata,(void *,unsigned,void *));
 assert(new_file&&monitor_file&&cancel_monitor);
 void *menu_file=new_file("/nonexistent/SECRET.menu"),*item_file=new_file("/nonexistent/PRIVATE.desktop");
 void *monitor=monitor_file(menu_file,0,NULL,NULL);assert(monitor);
 g_object_set_qdata(monitor,g_quark_from_static_string("garcon-menu-file-quark"),menu_file);
 g_signal_emit_by_name(monitor,"changed",menu_file,NULL,4);g_signal_emit_by_name(monitor,"changed",item_file,NULL,0);
 assert(g_signal_connect_data(menu,"draw",drawn_callback,NULL,NULL,0));
 g_signal_emit_by_name(menu,"map");g_signal_emit_by_name(menu,"draw");g_signal_emit_by_name(menu,"draw");g_signal_emit_by_name(menu,"add");g_signal_emit_by_name(menu,"remove");g_signal_emit_by_name(core,"reload-required");assert(drawn==2);
 for(unsigned i=0;i<220;i++){g_main_context_iteration(NULL,0);struct timespec t={0,10000000};nanosleep(&t,NULL);}
 cancel_monitor(monitor);g_object_unref(monitor);g_object_unref(menu_file);g_object_unref(item_file);g_object_unref(menu);g_object_unref(core);return 0;
}
