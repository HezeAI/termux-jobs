#define _POSIX_C_SOURCE 200809L
#include "garcon-attribute-filter.h"
#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>

/* A fixed ceiling; never cache contents, retain GFiles, or follow symlinks.
 * Unknown, unreadable, oversized, racing and first-seen files always pass.
 * ctime participates in the stable-read check, not semantic equality:
 * repeated chmod to the same mode may change ctime without changing meaning.
 * atime is deliberately ignored. No monitor signal/handler is modified. */
#define MAX_ENTRIES 512
#define MAX_FILE (256 * 1024)
typedef struct { struct stat st; char digest[65]; } Snapshot;
static GMutex lock;
static GHashTable *cache;
static guint64 passed_count,suppressed_count,details[12];
static gint observing;
static GQuark consumer_quark;
static gboolean remove_consumer(gpointer key,gpointer value,gpointer prefix){(void)value;return g_str_has_prefix(key,prefix);}
static void consumer_gone(gpointer unused,GObject *where){
 (void)unused;char *prefix=g_strdup_printf("%p:",(void *)where);
 g_mutex_lock(&lock);if(cache)g_hash_table_foreach_remove(cache,remove_consumer,prefix);g_mutex_unlock(&lock);g_free(prefix);
}
static gboolean same_stat(const struct stat *a,const struct stat *b,gboolean ctime){
 return a->st_dev==b->st_dev&&a->st_ino==b->st_ino&&a->st_mode==b->st_mode&&a->st_uid==b->st_uid&&a->st_gid==b->st_gid&&a->st_size==b->st_size&&a->st_mtim.tv_sec==b->st_mtim.tv_sec&&a->st_mtim.tv_nsec==b->st_mtim.tv_nsec&&(!ctime||(a->st_ctim.tv_sec==b->st_ctim.tv_sec&&a->st_ctim.tv_nsec==b->st_ctim.tv_nsec));
}
static gboolean snapshot(const char *path,Snapshot *out){
 int fd=open(path,O_RDONLY|O_NOFOLLOW|O_NONBLOCK|O_CLOEXEC);if(fd<0)return FALSE;
 struct stat after;gboolean ok=FALSE;GChecksum *sum=NULL;
 if(fstat(fd,&out->st)||!S_ISREG(out->st.st_mode)||out->st.st_size<0||out->st.st_size>MAX_FILE)goto done;
 sum=g_checksum_new(G_CHECKSUM_SHA256);char data[8192];ssize_t n;gsize total=0;
 for(;;){n=read(fd,data,sizeof(data));if(n<0&&errno==EINTR)continue;if(n<0)goto done;if(n==0)break;total+=(gsize)n;if(total>MAX_FILE)goto done;g_checksum_update(sum,(const guchar *)data,(gsize)n);}
 if(fstat(fd,&after)||total!=(gsize)out->st.st_size||!same_stat(&out->st,&after,TRUE))goto done;
 memcpy(out->digest,g_checksum_get_string(sum),65);ok=TRUE;
 done:if(sum)g_checksum_free(sum);close(fd);return ok;
}
gboolean jobs_garcon_attribute_unchanged(GObject *consumer,GFile *file,GFileMonitorEvent event){
 gboolean enabled=!g_strcmp0(g_getenv("JOBS_X11_GARCON_FILTER"),"1");
 /* Off is a true opt-out, including diagnostics: no path lookup/stat/hash. */
 if(!enabled)return FALSE;
 if(!G_IS_OBJECT(consumer))return FALSE;
 char *path=g_file_get_path(file);if(!path)return FALSE;
 char *key=g_strdup_printf("%p:%s",(void *)consumer,path);
 gboolean suppress=FALSE;g_mutex_lock(&lock);
 if(!cache)cache=g_hash_table_new_full(g_str_hash,g_str_equal,g_free,g_free);
 if(!consumer_quark)consumer_quark=g_quark_from_static_string("termux-jobs-private-garcon-consumer-v1");
 if(!g_object_get_qdata(consumer,consumer_quark)){
  g_object_weak_ref(consumer,consumer_gone,NULL);g_object_set_qdata(consumer,consumer_quark,GINT_TO_POINTER(1));
 }
 if(event!=G_FILE_MONITOR_EVENT_ATTRIBUTE_CHANGED||!g_str_has_suffix(path,".desktop")){
  details[9]++;g_hash_table_remove(cache,key);goto done;
 }
 details[11]++;guint64 id=G_GUINT64_CONSTANT(14695981039346656037);for(const guchar *c=(const guchar *)path;*c;c++){id^=*c;id*=G_GUINT64_CONSTANT(1099511628211);}details[10]=id;
 Snapshot *old=g_hash_table_lookup(cache,key),next;struct stat st;
 if(lstat(path,&st)||!S_ISREG(st.st_mode)||st.st_size<0||st.st_size>MAX_FILE){details[8]++;g_hash_table_remove(cache,key);goto done;}
 if(old&&same_stat(&old->st,&st,TRUE))details[1]++;
 /* Always hash: Android/proot can expose coarse ctime, so equal metadata
  * cannot prove equal contents even for same-size writes with restored mtime. */
 if(!snapshot(path,&next)){details[8]++;g_hash_table_remove(cache,key);goto done;}
 if(!old)details[0]++;
 else{
  if(strcmp(old->digest,next.digest))details[3]++;
  if(old->st.st_mode!=next.st.st_mode)details[4]++;
  if(old->st.st_uid!=next.st.st_uid||old->st.st_gid!=next.st.st_gid)details[5]++;
  if(old->st.st_dev!=next.st.st_dev||old->st.st_ino!=next.st.st_ino)details[6]++;
  if(old->st.st_mtim.tv_sec!=next.st.st_mtim.tv_sec||old->st.st_mtim.tv_nsec!=next.st.st_mtim.tv_nsec)details[7]++;
 }
 suppress=old&&same_stat(&old->st,&next.st,FALSE)&&!strcmp(old->digest,next.digest);
 if(suppress)details[2]++;
 if(!old&&g_hash_table_size(cache)>=MAX_ENTRIES){/* pass through once at capacity; bounded reset */g_hash_table_remove_all(cache);}
 Snapshot *copy=g_new(Snapshot,1);*copy=next;g_hash_table_replace(cache,g_strdup(key),copy);
 done:if(suppress&&enabled)suppressed_count++;else passed_count++;g_mutex_unlock(&lock);g_free(key);g_free(path);return suppress&&enabled;
}
void jobs_garcon_filter_stats(guint64 *passed,guint64 *suppressed){g_mutex_lock(&lock);*passed=passed_count;*suppressed=suppressed_count;g_mutex_unlock(&lock);}

void jobs_garcon_filter_details(guint64 out[12]){g_mutex_lock(&lock);memcpy(out,details,sizeof(details));g_mutex_unlock(&lock);}
void jobs_garcon_set_observing(gint on){g_atomic_int_set(&observing,on!=0);}
