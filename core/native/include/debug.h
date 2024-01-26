#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#ifndef _DEBUG_H_
#define _DEBUG_H_

#define log_debug(...) {\
    printf(__VA_ARGS__);\
    fflush(stdout);\
}

typedef struct {
    char* name;
    int count;
    struct timespec current;
    unsigned long long total_ms;
} time_measure;

time_measure* create_measure(char *name);
void begin_measure(time_measure* tm);
void end_measure(time_measure* tm);

void register_render_frame();
void register_monitor_frame();
void register_stream_frame();

#endif