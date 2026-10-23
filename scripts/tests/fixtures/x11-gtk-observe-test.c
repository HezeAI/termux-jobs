#define _POSIX_C_SOURCE 200809L
#include <stdarg.h>
#include <assert.h>
#include "../../../x11/src/main/cpp/x11-gtk-observe.c"
static unsigned connected,unrefs;
static int marked;
static Type type(const char *name){return !strcmp(name,"GtkMenu")?1:!strcmp(name,"GtkMenuItem")?2:!strcmp(name,"GarconGtkMenu")?4:0;}
static int isa(void *object,Type t){return ((uintptr_t)object==1&&(t==1||t==4))||((uintptr_t)object==2&&t==2);}
static void *value(const void *v){return (void *)v;}
static void *parent(void *w){return (uintptr_t)w==2?(void *)1:NULL;}
static void *window(void *w){(void)w;return (void *)20;}
static unsigned long xid(void *w){(void)w;return 123;}
static void get(void *w,const char *key,...){assert(w==(void *)1);assert(!strcmp(key,"menu"));va_list args;va_start(args,key);void **out=va_arg(args,void **);*out=(void *)10;va_end(args);}
static void unref(void *w){assert(w==(void *)10);unrefs++;}
static void *getdata(void *w,unsigned q){assert(w==(void *)10);assert(q==1);return marked?(void *)1:NULL;}
static void setdata(void *w,unsigned q,void *v){assert(w==(void *)10&&q==1&&v);marked=1;}
static unsigned long connect_signal(void *w,const char *signal,void (*cb)(void *,void *),void *data,void *destroy,unsigned flags){assert(w==(void *)10&&!strcmp(signal,"reload-required")&&cb==core_reload&&!data&&!destroy&&!flags);connected++;return 1;}
static void control(int n,const char *mode){FILE *f=fopen(JOBS_GTK_CONTROL,"w");assert(f);fprintf(f,"%s %d %s\n",identity,n,mode);fclose(f);}
int main(void){
    source="panel";strcpy(identity,"a11a11a1-1111-4111-8111-111111111111");
    p_g_type_from_name=type;p_g_type_check_instance_is_a=isa;p_g_value_get_object=value;p_gtk_widget_get_parent=parent;p_gtk_widget_get_window=window;p_gdk_x11_window_get_xid=xid;
    p_g_object_get=get;p_g_object_unref=unref;p_g_object_get_qdata=getdata;p_g_object_set_qdata=setdata;p_g_signal_connect_data=connect_signal;core_quark=1;
    emission(NULL,1,(void *)1,(void *)(uintptr_t)MAP);emission(NULL,1,(void *)1,(void *)(uintptr_t)ADD);
    assert(connected==1&&unrefs==2&&counts[MAP]==0&&counts[ADD]==0);
    control(1,"on");tick(NULL);assert(active&&serial==1);
    emission(NULL,1,(void *)1,(void *)(uintptr_t)DRAW);emission(NULL,1,(void *)1,(void *)(uintptr_t)ADD);emission(NULL,1,(void *)1,(void *)(uintptr_t)REMOVE);
    emission(NULL,1,(void *)2,(void *)(uintptr_t)ENTER);emission(NULL,1,(void *)2,(void *)(uintptr_t)MOTION);emission(NULL,1,(void *)3,(void *)(uintptr_t)DRAW);core_reload(NULL,NULL);
    assert(counts[DRAW]==1&&counts[ADD]==1&&counts[REMOVE]==1&&counts[ENTER]==1&&counts[MOTION]==1&&counts[RELOAD]==1&&last_menu==123);
    tick(NULL);control(0,"off");tick(NULL);assert(active&&serial==1);
    FILE *f=fopen(JOBS_GTK_CONTROL,"w");assert(f);fputs("wrong-token 2 off\n",f);fclose(f);tick(NULL);assert(active);
    f=fopen(JOBS_GTK_CONTROL,"w");assert(f);fprintf(f,"%s +2 off\n",identity);fclose(f);tick(NULL);assert(active);
    control(2,"off");tick(NULL);assert(!active&&serial==2);core_reload(NULL,NULL);emission(NULL,1,(void *)1,(void *)(uintptr_t)DRAW);assert(counts[RELOAD]==0&&counts[DRAW]==0);
    control(3,"on");tick(NULL);assert(active&&serial==3&&counts[DRAW]==0);
    unlink(JOBS_GTK_CONTROL);assert(!symlink("/do-not-follow",JOBS_GTK_CONTROL));int next,on;assert(!read_control(&next,&on));unlink(JOBS_GTK_CONTROL);
    return 0;
}
