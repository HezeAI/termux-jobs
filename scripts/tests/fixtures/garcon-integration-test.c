#include "garcon/garcon.h"
#include <assert.h>
#include "garcon-attribute-filter.h"
#include <sys/stat.h>
static unsigned reloads;
static void reloaded(GarconMenu *m,void *u){(void)m;(void)u;reloads++;}
int main(int argc,char **argv){
 assert(argc==2);g_setenv("JOBS_X11_GARCON_FILTER","1",TRUE);
 gchar *apps=g_build_filename(argv[1],"apps",NULL);assert(!g_mkdir_with_parents(apps,0700));
 gchar *entry=g_build_filename(apps,"test.desktop",NULL);GError *error=NULL;
 assert(g_file_set_contents(entry,"[Desktop Entry]\nType=Application\nName=Alpha\nExec=/bin/true\n",-1,&error));
 gchar *menu_path=g_build_filename(argv[1],"test.menu",NULL);gchar *xml=g_strdup_printf("<Menu><Name>Applications</Name><AppDir>%s</AppDir><Include><All/></Include></Menu>",apps);
 assert(g_file_set_contents(menu_path,xml,-1,&error));GarconMenu *menu=garcon_menu_new_for_path(menu_path);assert(menu);assert(garcon_menu_load(menu,NULL,&error));
 GList *items=garcon_menu_get_items(menu);assert(g_list_length(items)==1);assert(!g_strcmp0(garcon_menu_item_get_name(items->data),"Alpha"));g_list_free(items);
 g_signal_connect(menu,"reload-required",G_CALLBACK(reloaded),NULL);
 /* Deliver real inotify callbacks by using chmod on our temporary file only. */
 for(unsigned turn=0;turn<3;turn++){
  assert(!chmod(entry,0600));gint64 until=g_get_monotonic_time()+500000;
  while(g_get_monotonic_time()<until){g_main_context_iteration(NULL,FALSE);g_usleep(1000);}
 }
 guint64 passed,suppressed;jobs_garcon_filter_stats(&passed,&suppressed);assert(passed>0&&suppressed>0);
 assert(g_file_set_contents(entry,"[Desktop Entry]\nType=Application\nName=Bravo\nExec=/bin/true\n",-1,&error));
 gint64 until=g_get_monotonic_time()+500000;while(g_get_monotonic_time()<until){g_main_context_iteration(NULL,FALSE);g_usleep(1000);}
 assert(garcon_menu_load(menu,NULL,&error));items=garcon_menu_get_items(menu);assert(g_list_length(items)==1);assert(!g_strcmp0(garcon_menu_item_get_name(items->data),"Bravo"));g_list_free(items);
 g_print("real-menu-read=true real-inotify-filtered=true content-change-applied=true passed=%lu filtered=%lu\n",(unsigned long)passed,(unsigned long)suppressed);
 g_object_unref(menu);g_free(apps);g_free(entry);g_free(menu_path);g_free(xml);return 0;
}