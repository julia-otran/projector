#include <stdlib.h>
#include <inttypes.h>
#include <math.h>
#include <stdio.h>
#include <time.h>

#include "ogl-loader.h"
#include "debug.h"
#include "render-fader.h"

void render_fader_init(render_fader_instance **instance) {
    (*instance) = (render_fader_instance*) calloc(1, sizeof(render_fader_instance));
}

void render_fader_terminate(render_fader_instance *instance) {
    fade_node *current = (fade_node*) instance->fade_node_list;
    fade_node *dealoc;

    while (current) {
        dealoc = current;
        current = (fade_node*) current->next;
        free(dealoc);
    }

    free(instance);
}

fade_node* find_fade_node(render_fader_instance *instance, int fade_id) {
    fade_node *current = (fade_node*) instance->fade_node_list;

    while (current) {
        if (current->fade_id == fade_id) {
            return current;
        } else {
            current = (fade_node*) current->next;
        }
    }

    return NULL;
}

fade_node* find_or_create_fade_node(render_fader_instance *instance, int fade_id) {
    fade_node *node = find_fade_node(instance, fade_id);

    if (node) {
        return node;
    }

    node = (fade_node*) calloc(1, sizeof(fade_node));
    node->fade_id = fade_id;

    node->next = instance->fade_node_list;
    instance->fade_node_list = node;

    return node;
}

void render_fader_fade_in(render_fader_instance *instance, int fade_id, int duration_ms) {
    fade_node *node = find_or_create_fade_node(instance, fade_id);

    node->mode = RENDER_FADER_MODE_IN;
    node->duration_ms = duration_ms;
    clock_gettime(CLOCK_REALTIME, &node->start_time_spec);
    node->dealocate_on_finish = 0;
}

void render_fader_fade_out(render_fader_instance *instance, int fade_id, int duration_ms) {
    fade_node *node = find_or_create_fade_node(instance, fade_id);

    node->mode = RENDER_FADER_MODE_OUT;
    node->duration_ms = duration_ms;
    clock_gettime(CLOCK_REALTIME, &node->start_time_spec);
    node->dealocate_on_finish = 0;
}

void render_fader_set_alpha(fade_node *node) {
    struct timespec spec;
    clock_gettime(CLOCK_REALTIME, &spec);

    long current_ms = round(spec.tv_nsec / 1.0e6) + (spec.tv_sec * 1000);
    long init_ms = round(node->start_time_spec.tv_nsec / 1.0e6) + (node->start_time_spec.tv_sec * 1000);

    long elapsed_time = current_ms - init_ms;

    if (node->mode == RENDER_FADER_MODE_IN) {
        if (elapsed_time >= node->duration_ms) {
            glColor4f(1.0, 1.0, 1.0, 1.0);
        } else {
            float alpha = elapsed_time / (float)node->duration_ms;
            glColor4f(1.0, 1.0, 1.0, alpha);
        }
    } else if (node->mode == RENDER_FADER_MODE_OUT) {
        if (elapsed_time >= node->duration_ms) {
            glColor4f(1.0, 1.0, 1.0, 0.0);
        } else {
            long remaiming_time = node->duration_ms - elapsed_time;
            float alpha = remaiming_time / (float)node->duration_ms;
            glColor4f(1.0, 1.0, 1.0, alpha);
        }
    }
}

int render_fader_is_hidden(fade_node *node) {
    if (node->mode == RENDER_FADER_MODE_OUT) {
        struct timespec spec;
        clock_gettime(CLOCK_REALTIME, &spec);

        long current_ms = round(spec.tv_nsec / 1.0e6) + (spec.tv_sec * 1000);
        long init_ms = round(node->start_time_spec.tv_nsec / 1.0e6) + (node->start_time_spec.tv_sec * 1000);

        long elapsed_time = current_ms - init_ms;

        if (elapsed_time >= node->duration_ms) {
            return 1;
        }
    }

    return 0;
}

void render_fader_cleanup(render_fader_instance *instance) {
    fade_node **previous_ptr = &instance->fade_node_list;
    fade_node *current = (*previous_ptr);

    while (current) {
        if (render_fader_is_hidden(current)) {
            (*previous_ptr) = (fade_node *) current->next;
            free(current);
        } else {
            previous_ptr = (fade_node **) &current->next;
        }

        current = (*previous_ptr);
    }
}
