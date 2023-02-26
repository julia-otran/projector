#include <stdio.h>
#include <stdlib.h>

#ifndef _DEBUG_H_
#define _DEBUG_H_

#define log(args...) {\
    printf(args);\
    fflush(stdout);\
}

void register_render_frame(int render_id);
void register_monitor_frame();

#endif