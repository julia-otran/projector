#include "ogl-loader.h"
#include "monitor.h"
#include "render.h"

#ifndef _LOOP_H_
#define _LOOP_H_

void main_loop_start();
void main_loop_schedule_config_reload(projection_config *config);
void main_loop_terminate();

#endif