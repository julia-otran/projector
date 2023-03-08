#include <stdio.h>
#include <stdlib.h>

#ifndef _DEBUG_H_
#define _DEBUG_H_

#define log_debug(...) {\
    printf(__VA_ARGS__);\
    fflush(stdout);\
}

void register_render_frame();
void register_monitor_frame();

#endif