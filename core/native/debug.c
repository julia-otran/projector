#define _POSIX_C_SOURCE 200809L

#include <inttypes.h>
#include <math.h>
#include <stdio.h>
#include <time.h>

#include "debug.h"

#define LOG_FPS_INTERVAL_SECS 3

static int render_frame_count = 0;
static long render_last_time_ms = 0;
static long render_last_time_sec = 0;

void register_render_frame() {
    struct timespec spec;
    clock_gettime(CLOCK_REALTIME, &spec);

    render_frame_count++;

    long sec_delta = spec.tv_sec - render_last_time_sec;

    if (sec_delta > LOG_FPS_INTERVAL_SECS) {
        long new_ms = round(spec.tv_nsec / 1.0e6);
        long ms_delta = (sec_delta * 1000) + (new_ms - render_last_time_ms);

        double fps = render_frame_count / (ms_delta / 1000.0);

        log_debug("Main Render FPS: %lf\n", fps);

        render_last_time_ms = new_ms;
        render_last_time_sec = spec.tv_sec;

        render_frame_count = 0;
    }
}

static int monitor_frame_count = 0;
static long monitor_last_time_ms = 0;
static long monitor_last_time_sec = 0;

void register_monitor_frame() {
    struct timespec spec;
    clock_gettime(CLOCK_REALTIME, &spec);

    monitor_frame_count++;

    long sec_delta = spec.tv_sec - monitor_last_time_sec;

    if (sec_delta > LOG_FPS_INTERVAL_SECS) {
        long new_ms = round(spec.tv_nsec / 1.0e6);
        long ms_delta = (sec_delta * 1000) + (new_ms - monitor_last_time_ms);

        double fps = monitor_frame_count / (ms_delta / 1000.0);

        log_debug("Monitors FPS: %lf\n", fps);

        monitor_last_time_ms = new_ms;
        monitor_last_time_sec = spec.tv_sec;

        monitor_frame_count = 0;
    }
}
