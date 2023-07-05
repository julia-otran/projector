#define _POSIX_C_SOURCE 200809L

#include <inttypes.h>
#include <math.h>
#include <stdio.h>

#include "clock.h"
#include "debug.h"

#ifdef _DEBUG
#define LOG_FPS_INTERVAL_MS 1000
#else
#define LOG_FPS_INTERVAL_MS 5000
#endif // _DEBUG

time_measure* create_measure(char* name) {
    time_measure* tm = (time_measure*)calloc(1, sizeof(time_measure));
    tm->name = name;
    return tm;
}
void begin_measure(time_measure* tm) {
    get_time(&tm->current);
}
void end_measure(time_measure* tm) {
    struct timespec now;

    get_time(&now);

    unsigned long long current_ms = get_delta_time_ms(&now, &tm->current);

    tm->count++;
    tm->total_ms += current_ms;

    if (tm->count > 300) {
       // log_debug("Measure %s average duration: %llu\n", tm->name, tm->total_ms / tm->count);
        tm->total_ms = current_ms;
        tm->count = 1;
    }

    unsigned long long average_ms = tm->total_ms / tm->count;

    if (current_ms > average_ms + 15) {
#ifdef _DEBUG
        log_debug("Measure %s greater than avg. avg=%llu took=%llu\n", tm->name, average_ms, current_ms);
#endif
    }
}

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
