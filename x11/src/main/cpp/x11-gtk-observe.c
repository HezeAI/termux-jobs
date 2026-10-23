/* Private GTK3 metadata module. No GTK headers/link dependency, settings writes,
 * input payload inspection, injected input or retained widget references.
 * Public GLib/GTK ABI declarations checked against GLib gsignal.h and GTK3.
 */
#define _POSIX_C_SOURCE 200809L
#include <dlfcn.h>
#ifndef RTLD_DEFAULT
#define RTLD_DEFAULT ((void *)0)
#endif
#include <stdatomic.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <time.h>
#include <limits.h>
#ifndef JOBS_GTK_CONTROL
#define JOBS_GTK_CONTROL "/tmp/termux-jobs-x11/x11-observation-control"
#endif

typedef unsigned long Type;
typedef struct { unsigned id; const char *name; Type owner; unsigned flags; Type result; unsigned n; const Type *params; } SignalQuery;
typedef int (*Hook)(void *,unsigned,const void *,void *);
#define FN(ret,name,args) static ret (*p_##name) args
FN(const char *,g_get_prgname,(void));
FN(Type,g_type_from_name,(const char *));
FN(void *,g_type_class_ref,(Type));
FN(void,g_type_class_unref,(void *));
FN(int,g_type_check_instance_is_a,(void *,Type));
FN(void *,g_value_get_object,(const void *));
/* Public GValue ABI: GType + two 64-bit union slots (glib gvalue.h). */
typedef struct { Type type; union { long long number; double real; void *pointer; } data[2]; } Value;
FN(int,g_value_get_enum,(const void *));
FN(Type,g_file_monitor_get_type,(void));
FN(char *,g_file_get_path,(void *));
FN(void,g_free,(void *));
FN(unsigned,g_quark_try_string,(const char *));
FN(unsigned,g_signal_lookup,(const char *,Type));
FN(void,g_signal_query,(unsigned,SignalQuery *));
FN(unsigned long,g_signal_add_emission_hook,(unsigned,unsigned,Hook,void *,void *));
FN(unsigned long,g_signal_connect_data,(void *,const char *,void (*)(void *,void *),void *,void *,unsigned));
FN(unsigned,g_quark_from_static_string,(const char *));
FN(void *,g_object_get_qdata,(void *,unsigned));
FN(void,g_object_set_qdata,(void *,unsigned,void *));
FN(void,g_object_get,(void *,const char *,...));
FN(void,g_object_unref,(void *));
FN(Type,gtk_widget_get_type,(void));
FN(Type,gtk_container_get_type,(void));
FN(Type,gtk_menu_get_type,(void));
FN(void *,gtk_widget_get_window,(void *));
FN(void *,gtk_widget_get_parent,(void *));
FN(unsigned long,gdk_x11_window_get_xid,(void *));
FN(unsigned,g_timeout_add_seconds,(unsigned,int (*)(void *),void *));

enum { DRAW, ADD, REMOVE, MAP, UNMAP, STYLE, ENTER, LEAVE, MOTION, RELOAD, NCOUNTS };
static const char *const names[]={"draw","add","remove","map","unmap","style-updated","enter-notify-event","leave-notify-event","motion-notify-event"};
static _Atomic unsigned long long counts[NCOUNTS];
static unsigned long long last_counts[NCOUNTS];
static unsigned hooks,core_quark,core_hooks;
static unsigned long last_menu;
static const char *source;
static char identity[37];
static _Atomic int active;
static int serial=-1,initialized;
static long long capture_start,last_emit;
static void (*guard_details)(uint64_t *);
static void (*guard_observing)(int);
static unsigned file_hook;
static void (*filter_stats)(uint64_t *,uint64_t *);
static _Atomic unsigned long long file_events[11],file_types[4],file_tagged,file_poll,file_inotify;
static unsigned long long last_file_total;
static _Atomic uint64_t last_file_id;
static unsigned file_category(const char *path){
    size_t n=path?strlen(path):0;
    if(n>=5&&!strcmp(path+n-5,".menu"))return 0;
    if(n>=8&&!strcmp(path+n-8,".desktop"))return 1;
    if(n>=10&&!strcmp(path+n-10,".directory"))return 2;
    return 3;
}
static int file_emission(void *hint,unsigned n,const void *values,void *data){
    (void)hint;(void)data;
    if(!active||n!=4||!values)return 1;
    const Value *v=values;void *monitor=p_g_value_get_object(v),*file=p_g_value_get_object(v+1);
    int event=p_g_value_get_enum(v+3);if(!monitor||!file||event<0||event>10)return 1;
    file_events[event]++;
    Type poll=p_g_type_from_name("GPollFileMonitor"),inotify=p_g_type_from_name("GInotifyFileMonitor");
    if(poll&&p_g_type_check_instance_is_a(monitor,poll))file_poll++;
    if(inotify&&p_g_type_check_instance_is_a(monitor,inotify))file_inotify++;
    unsigned quark=p_g_quark_try_string("garcon-menu-file-quark");
    if(quark&&p_g_object_get_qdata(monitor,quark))file_tagged++;
    char *path=p_g_file_get_path(file);file_types[file_category(path)]++;
    uint64_t hash=UINT64_C(14695981039346656037);
    if(path){for(const unsigned char *c=(const unsigned char *)path;*c;c++){hash^=*c;hash*=UINT64_C(1099511628211);}p_g_free(path);last_file_id=hash;}
    return 1; /* observing all monitors in panel/desktop; never suppress events */
}
static unsigned long long file_total(void){unsigned long long n=0;for(unsigned i=0;i<11;i++)n+=file_events[i];return n;}
static void emit_files(void){
    if(!file_hook)return;
    char line[700];int n=snprintf(line,sizeof(line),"\nJOBS_X11_GTK source=%s capture=%d phase=files file-hook=%u file-events=%llu changed=%llu done=%llu deleted=%llu created=%llu attribute=%llu pre-unmount=%llu unmounted=%llu moved=%llu renamed=%llu moved-in=%llu moved-out=%llu garcon-tagged=%llu poll-monitor=%llu inotify-monitor=%llu menu=%llu desktop=%llu directory=%llu other=%llu last-file-id=%llx\n",source,serial,file_hook,file_total(),file_events[0],file_events[1],file_events[2],file_events[3],file_events[4],file_events[5],file_events[6],file_events[7],file_events[8],file_events[9],file_events[10],file_tagged,file_poll,file_inotify,file_types[0],file_types[1],file_types[2],file_types[3],(unsigned long long)last_file_id);
    if(n>0&&(size_t)n<sizeof(line)){ssize_t written=write(STDOUT_FILENO,line,(size_t)n);(void)written;}
    last_file_total=file_total();
}
static long long now_ms(void){struct timespec t;if(clock_gettime(CLOCK_MONOTONIC,&t))return 0;return t.tv_sec*1000LL+t.tv_nsec/1000000;}

/* Missing/replaced controls are ignored, matching the independent observer.
 * Never accept an arbitrary command/PID/path or follow a symbolic link. */
static int read_control(int *next,int *enabled){
    int fd=open(JOBS_GTK_CONTROL,O_RDONLY|O_NOFOLLOW|O_CLOEXEC|O_NONBLOCK);
    if(fd<0)return 0;
    char text[258],token[37],digits[11],mode[4],extra;unsigned long number;
    ssize_t n=read(fd,text,257);close(fd);
    if(n<0||n>256)return 0;
    text[n]=0;
    if(memchr(text,0,(size_t)n)||sscanf(text,"%36s %10s %3s %c",token,digits,mode,&extra)!=3||strcmp(token,identity))return 0;
    if(strspn(digits,"0123456789")!=strlen(digits))return 0;
    number=strtoul(digits,NULL,10);if(number>INT_MAX)return 0;
    if(strcmp(mode,"on")&&strcmp(mode,"off"))return 0;
    *next=(int)number;*enabled=!strcmp(mode,"on");return 1;
}
static void emit(const char *phase){
    unsigned long long v[NCOUNTS];for(unsigned i=0;i<NCOUNTS;i++)v[i]=counts[i];
    uint64_t guard_passed=0,guard_filtered=0;if(filter_stats)filter_stats(&guard_passed,&guard_filtered);
    char line[700];int n=snprintf(line,sizeof(line),"\nJOBS_X11_GTK source=%s capture=%d phase=%s elapsed-ms=%lld hooks=%u core-hooks=%u last-menu=%lx draw=%llu add=%llu remove=%llu map=%llu unmap=%llu style=%llu enter=%llu leave=%llu motion=%llu reload=%llu guard-passed-total=%llu guard-filtered-total=%llu\n",source,serial,phase,now_ms()-capture_start,hooks,core_hooks,last_menu,v[DRAW],v[ADD],v[REMOVE],v[MAP],v[UNMAP],v[STYLE],v[ENTER],v[LEAVE],v[MOTION],v[RELOAD],(unsigned long long)guard_passed,(unsigned long long)guard_filtered);
    if(n>0&&(size_t)n<sizeof(line)){ssize_t written=write(STDOUT_FILENO,line,(size_t)n);(void)written;}
    for(unsigned i=0;i<NCOUNTS;i++)last_counts[i]=v[i];
    last_emit=now_ms();emit_files();
    if(guard_details){
        uint64_t d[12];guard_details(d);
        int length=snprintf(line,sizeof(line),"\nJOBS_X11_GTK source=%s capture=%d phase=attributes totals-since-process-start=true new=%llu same-stat=%llu same-content=%llu content-change=%llu mode-change=%llu owner-change=%llu identity-change=%llu mtime-change=%llu unreadable-or-unsupported=%llu other-event=%llu last-file-id=%llx attribute-events=%llu\n",source,serial,(unsigned long long)d[0],(unsigned long long)d[1],(unsigned long long)d[2],(unsigned long long)d[3],(unsigned long long)d[4],(unsigned long long)d[5],(unsigned long long)d[6],(unsigned long long)d[7],(unsigned long long)d[8],(unsigned long long)d[9],(unsigned long long)d[10],(unsigned long long)d[11]);
        if(length>0&&(size_t)length<sizeof(line)){ssize_t written=write(STDOUT_FILENO,line,(size_t)length);(void)written;}
    }
}
static int tick(void *unused){
    (void)unused;int next,on;
    if(read_control(&next,&on)&&next>serial){
        if(active)emit("finished");
        serial=next;active=on;if(guard_observing)guard_observing(on);last_menu=0;for(unsigned i=0;i<NCOUNTS;i++){counts[i]=0;last_counts[i]=0;}for(unsigned i=0;i<11;i++)file_events[i]=0;for(unsigned i=0;i<4;i++)file_types[i]=0;file_tagged=file_poll=file_inotify=last_file_total=last_file_id=0;capture_start=now_ms();
        if(active)emit("started");
    }
    int changed=file_total()!=last_file_total;for(unsigned i=0;i<NCOUNTS;i++)if(counts[i]!=last_counts[i])changed=1;
    if(active&&(changed||now_ms()-last_emit>=30000))emit("sample");
    return 1;
}
static void core_reload(void *object,void *unused){(void)object;(void)unused;if(active)counts[RELOAD]++;}
static void observe_core(void *widget){
    Type type=p_g_type_from_name("GarconGtkMenu");
    if(!type||!p_g_type_check_instance_is_a(widget,type))return;
    void *core=NULL;p_g_object_get(widget,"menu",&core,NULL);
    if(core){
        if(!p_g_object_get_qdata(core,core_quark)){
            unsigned long handler=p_g_signal_connect_data(core,"reload-required",core_reload,NULL,NULL,0);
            if(handler){p_g_object_set_qdata(core,core_quark,(void *)(uintptr_t)handler);core_hooks++;}
        }
        p_g_object_unref(core);
    }
}
static int emission(void *hint,unsigned n,const void *values,void *data){
    (void)hint;if(!n)return 1;
    unsigned kind=(unsigned)(uintptr_t)data;if(kind>=RELOAD)return 1;
    if(!active&&kind!=MAP&&kind!=ADD)return 1;
    void *widget=p_g_value_get_object(values);if(!widget)return 1;
    Type menu_type=p_g_type_from_name("GtkMenu");
    int menu=menu_type&&p_g_type_check_instance_is_a(widget,menu_type);
    if(menu&&(kind==MAP||kind==ADD))observe_core(widget);
    if(!active)return 1;
    if(!menu&&kind>=ENTER){
        Type item=p_g_type_from_name("GtkMenuItem");
        if(item&&p_g_type_check_instance_is_a(widget,item)){
            void *parent=p_gtk_widget_get_parent(widget);
            menu=parent&&menu_type&&p_g_type_check_instance_is_a(parent,menu_type);
        }
    }
    if(!menu)return 1;
    counts[kind]++;
    if(p_gdk_x11_window_get_xid){void *window=p_gtk_widget_get_window(widget);if(window)last_menu=p_gdk_x11_window_get_xid(window);}
    return 1; /* keep hook; no signal return/propagation is changed */
}
/* GTK can drop a module reference after a settings change. Registered hooks and
 * the GLib source must not become dangling code pointers. Only this small module
 * is made resident; callbacks still check the manual recording state. */
__attribute__((visibility("default"))) const char *g_module_check_init(void *module){
    void (*resident)(void *)=(void (*)(void *))dlsym(RTLD_DEFAULT,"g_module_make_resident");
    if(!resident)return "GTK observation requires GModule residency";
    resident(module);return NULL;
}
__attribute__((visibility("default"))) void gtk_module_init(int *argc,char ***argv){
    (void)argc;(void)argv;if(initialized)return;
    const char *enabled=getenv("JOBS_X11_GTK_OBSERVE");if(!enabled||strcmp(enabled,"1"))return;
#define LOAD(name) do { p_##name=(__typeof__(p_##name))dlsym(RTLD_DEFAULT,#name);if(!p_##name)return; } while(0)
    LOAD(g_get_prgname);
    const char *program=p_g_get_prgname();
    if(program&&!strcmp(program,"xfce4-panel"))source="panel";
    else if(program&&!strcmp(program,"xfdesktop"))source="desktop";
    else return; /* unrelated GTK programs receive no hooks/timer */
    const char *token=getenv("JOBS_X11_FOCUS_TOKEN");
    if(!token||strlen(token)!=36||strspn(token,"abcdef0123456789-")!=36)return;
    memcpy(identity,token,37);
    LOAD(g_type_from_name);LOAD(g_type_class_ref);LOAD(g_type_class_unref);LOAD(g_type_check_instance_is_a);LOAD(g_value_get_object);
    LOAD(g_signal_lookup);LOAD(g_signal_query);LOAD(g_signal_add_emission_hook);LOAD(g_signal_connect_data);
    LOAD(g_quark_from_static_string);LOAD(g_object_get_qdata);LOAD(g_object_set_qdata);LOAD(g_object_get);LOAD(g_object_unref);
    LOAD(gtk_widget_get_type);LOAD(gtk_container_get_type);LOAD(gtk_menu_get_type);LOAD(gtk_widget_get_window);LOAD(gtk_widget_get_parent);LOAD(g_timeout_add_seconds);
    p_gdk_x11_window_get_xid=(__typeof__(p_gdk_x11_window_get_xid))dlsym(RTLD_DEFAULT,"gdk_x11_window_get_xid");
    Type types[]={p_gtk_widget_get_type(),p_gtk_container_get_type(),p_gtk_menu_get_type()};
    if(!types[0]||!types[1]||!types[2])return;
    void *classes[3];for(unsigned i=0;i<3;i++)classes[i]=p_g_type_class_ref(types[i]);
    core_quark=p_g_quark_from_static_string("termux-jobs-private-gtk-observation-v1");
    for(unsigned i=0;i<RELOAD;i++){
        unsigned id=p_g_signal_lookup(names[i],types[i==ADD||i==REMOVE?1:0]);SignalQuery query={0};
        if(!id)continue;
        p_g_signal_query(id,&query);
        if(!(query.flags&(1U<<6))&&p_g_signal_add_emission_hook(id,0,emission,(void *)(uintptr_t)i,NULL))hooks|=1U<<i;
    }
    for(unsigned i=0;i<3;i++)p_g_type_class_unref(classes[i]);
    /* GIO is already loaded by GTK; optional instrumentation cannot prevent
     * normal menus from starting. Force type init before querying its signal. */
#define OPTIONAL(name) p_##name=(__typeof__(p_##name))dlsym(RTLD_DEFAULT,#name)
    OPTIONAL(g_value_get_enum);OPTIONAL(g_file_monitor_get_type);OPTIONAL(g_file_get_path);OPTIONAL(g_free);OPTIONAL(g_quark_try_string);
    if(p_g_value_get_enum&&p_g_file_monitor_get_type&&p_g_file_get_path&&p_g_free&&p_g_quark_try_string){
        Type type=p_g_file_monitor_get_type();void *klass=p_g_type_class_ref(type);
        unsigned id=p_g_signal_lookup("changed",type);SignalQuery query={0};if(id)p_g_signal_query(id,&query);
        if(id&&query.n==3&&!(query.flags&(1U<<6)))file_hook=p_g_signal_add_emission_hook(id,0,file_emission,NULL,NULL)!=0;
        if(klass)p_g_type_class_unref(klass);
    }
    guard_observing=(__typeof__(guard_observing))dlsym(RTLD_DEFAULT,"jobs_garcon_set_observing");
    guard_details=(__typeof__(guard_details))dlsym(RTLD_DEFAULT,"jobs_garcon_filter_details");
    filter_stats=(__typeof__(filter_stats))dlsym(RTLD_DEFAULT,"jobs_garcon_filter_stats");
    initialized=1;p_g_timeout_add_seconds(1,tick,NULL);
    tick(NULL);
}
