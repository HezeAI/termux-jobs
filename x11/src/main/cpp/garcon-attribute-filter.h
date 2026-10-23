/* Test-only conservative duplicate ATTRIBUTE_CHANGED guard. LGPL-2.0-or-later. */
#ifndef JOBS_GARCON_ATTRIBUTE_FILTER_H
#define JOBS_GARCON_ATTRIBUTE_FILTER_H
#include <gio/gio.h>
gboolean jobs_garcon_attribute_unchanged(GObject *consumer, GFile *file, GFileMonitorEvent event);
void jobs_garcon_filter_details(guint64 out[12]);
void jobs_garcon_set_observing(gint on);
void jobs_garcon_filter_stats(guint64 *passed, guint64 *suppressed);
#endif
