#define _POSIX_C_SOURCE 200809L

#include <inttypes.h>
#include <math.h>
#include <stdio.h>

#include "clock.h"
#include "debug.h"

#define LOG_FPS_INTERVAL_MS 1000

static int stream_frame_count = 0;
static struct timespec stream_last_time = {
    .tv_nsec = 0,
    .tv_sec = 0,
};

void register_stream_frame() {
    struct timespec spec;
    get_time(&spec);

    stream_frame_count++;

    long ms_delta = get_delta_time_ms(&spec, &stream_last_time);

    if (ms_delta > LOG_FPS_INTERVAL_MS) {
        double fps = stream_frame_count / (ms_delta / 1000.0);

        log_debug("Stream FPS: %lf\n", fps);
        copy_time(&stream_last_time, &spec);

        stream_frame_count = 0;
    }
}

static int monitor_frame_count = 0;
static struct timespec monitor_last_time = {
    .tv_nsec = 0,
    .tv_sec = 0
};

void register_monitor_frame() {
    struct timespec spec;
    get_time(&spec);

    monitor_frame_count++;

    unsigned long long ms_delta = get_delta_time_ms(&spec, &monitor_last_time);

    if (ms_delta > LOG_FPS_INTERVAL_MS) {
        double fps = monitor_frame_count / (ms_delta / 1000.0);

        log_debug("Monitors FPS: %lf\n", fps);
        copy_time(&monitor_last_time, &spec);

        monitor_frame_count = 0;
    }
}
