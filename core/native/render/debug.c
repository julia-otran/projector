#define _POSIX_C_SOURCE 200809L

#include <inttypes.h>
#include <math.h>
#include <stdio.h>

#include "clock.h"
#include "debug.h"

#define LOG_FPS_INTERVAL_MS 1000

static int stream_frame_count = 0;
static long stream_last_time_ms = 0;
static long stream_last_time_sec = 0;

void register_stream_frame() {
    struct timespec spec;
    get_time(&spec);

    stream_frame_count++;

    long sec_delta = spec.tv_sec - stream_last_time_sec;
    long new_ms = round(spec.tv_nsec / 1.0e6);
    long ms_delta = (sec_delta * 1000) + (new_ms - stream_last_time_ms);

    if (ms_delta > LOG_FPS_INTERVAL_MS) {
        double fps = stream_frame_count / (ms_delta / 1000.0);

        log_debug("Stream FPS: %lf\n", fps);

        stream_last_time_ms = new_ms;
        stream_last_time_sec = spec.tv_sec;

        stream_frame_count = 0;
    }
}

static int monitor_frame_count = 0;
static long monitor_last_time_ms = 0;
static long monitor_last_time_sec = 0;

void register_monitor_frame() {
    struct timespec spec;
    get_time(&spec);

    monitor_frame_count++;

    long sec_delta = spec.tv_sec - monitor_last_time_sec;
    long new_ms = round(spec.tv_nsec / 1.0e6);
    long ms_delta = (sec_delta * 1000) + (new_ms - monitor_last_time_ms);

    if (ms_delta > LOG_FPS_INTERVAL_MS) {
        double fps = monitor_frame_count / (ms_delta / 1000.0);

        log_debug("Monitors FPS: %lf\n", fps);

        monitor_last_time_ms = new_ms;
        monitor_last_time_sec = spec.tv_sec;

        monitor_frame_count = 0;
    }
}
