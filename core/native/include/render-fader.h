#define _POSIX_C_SOURCE 200809L

#include <inttypes.h>
#include <math.h>
#include <stdio.h>
#include <time.h>

#ifndef _RENDER_TEX_FADER_H_
#define _RENDER_TEX_FADER_H_

#define RENDER_FADER_MODE_IN 1
#define RENDER_FADER_MODE_OUT 2

#define RENDER_FADER_DEFAULT_TIME_MS 300

typedef struct {
    int fade_id;
    void *extra_data;

    int mode;
    int duration_ms;

    struct timespec start_time_spec;

    void *next;
} fade_node;

typedef struct {
    fade_node *fade_node_list;
} render_fader_instance;

void render_fader_init(render_fader_instance **instance);

void render_fader_fade_in(render_fader_instance *instance, int fade_id, int duration_ms);
void render_fader_fade_in_data(render_fader_instance *instance, int fade_id, int duration_ms, void *extra_data);

void render_fader_fade_out(render_fader_instance *instance, int fade_id, int duration_ms);

void render_fader_fade_in_out(render_fader_instance *instance, int fade_id, int duration_ms);
void render_fader_fade_in_out_data(render_fader_instance *instance, int fade_id, int duration_ms, void *extra_data);

int render_fader_is_hidden(fade_node *node);
void render_fader_cleanup(render_fader_instance *instance);

void render_fader_terminate(render_fader_instance *instance);

float render_fader_get_alpha(fade_node *node);

#define render_fader_for_each(INSTANCE) \
    for (fade_node *node = INSTANCE->fade_node_list; node != NULL; node = node->next)

#define min(A, B) (A < B ? A : B)

#endif