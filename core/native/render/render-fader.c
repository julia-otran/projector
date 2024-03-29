#include <stdlib.h>
#include <inttypes.h>
#include <stdio.h>
#include <string.h>

#include "custom-math.h"
#include "clock.h"
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
    get_time(&node->start_time_spec);
    node->mode = 0;

    node->next = instance->fade_node_list;
    instance->fade_node_list = node;

    return node;
}

long timespec_to_ms(struct timespec *spec) {
    return round(spec->tv_nsec / 1.0e6) + (spec->tv_sec * 1000);
}

void render_fader_fade_in_data(render_fader_instance *instance, int fade_id, int duration_ms, void *extra_data) {
    struct timespec spec;
    get_time(&spec);

    fade_node *node = find_or_create_fade_node(instance, fade_id);

    long current_ms = timespec_to_ms(&spec);
    long init_ms = timespec_to_ms(&node->start_time_spec);

    if (node->mode == RENDER_FADER_MODE_OUT) {
        node->mode = RENDER_FADER_MODE_IN;
        node->duration_ms = MIN((current_ms - init_ms), duration_ms);
        memcpy(&node->start_time_spec, &spec, sizeof(struct timespec));
    }

    if (node->mode == 0) {
        node->mode = RENDER_FADER_MODE_IN;
        node->duration_ms = duration_ms;
    }

    node->extra_data = extra_data;
}

void render_fader_fade_in(render_fader_instance *instance, int fade_id, int duration_ms) {
    render_fader_fade_in_data(instance, fade_id, duration_ms, NULL);
}

void render_fader_fade_out(render_fader_instance *instance, int fade_id, int duration_ms) {
    fade_node *node = find_fade_node(instance, fade_id);
    struct timespec spec;

    if (node && node->mode == RENDER_FADER_MODE_IN) {
        get_time(&spec);

        long current_ms = timespec_to_ms(&spec);
        long init_ms = timespec_to_ms(&node->start_time_spec);

        node->mode = RENDER_FADER_MODE_OUT;
        node->duration_ms = min((current_ms - init_ms), duration_ms);
        memcpy(&node->start_time_spec, &spec, sizeof(struct timespec));
    }
}

void render_fader_fade_in_out_data(render_fader_instance *instance, int fade_id, int duration_ms, void *data) {
    render_fader_fade_in_data(instance, fade_id, duration_ms, data);

    render_fader_for_each(instance) {
        if (node->fade_id != fade_id) {
            render_fader_fade_out(instance, node->fade_id, duration_ms);
        }
    }
}

void render_fader_fade_in_out(render_fader_instance *instance, int fade_id, int duration_ms) {
    render_fader_fade_in_out_data(instance, fade_id, duration_ms, NULL);
}

float render_fader_get_alpha_with_time(fade_node* node, struct timespec* spec) {
    long current_ms = timespec_to_ms(spec);
    long init_ms = timespec_to_ms(&node->start_time_spec);

    long elapsed_time = current_ms - init_ms;

    if (node->mode == RENDER_FADER_MODE_IN) {
        if (elapsed_time >= node->duration_ms) {
            return 1.0;
        }
        else {
            return elapsed_time / (float)node->duration_ms;
        }
    }
    else if (node->mode == RENDER_FADER_MODE_OUT) {
        if (elapsed_time >= node->duration_ms) {
            return 0.0;
        }
        else {
            long remaiming_time = node->duration_ms - elapsed_time;
            return remaiming_time / (float)node->duration_ms;
        }
    }

    // may never happen?
    return 1.0;
}

float render_fader_get_alpha(fade_node *node) {
    struct timespec spec;
    get_time(&spec);
    return render_fader_get_alpha_with_time(node, &spec);
}

int render_fader_is_hidden(fade_node *node) {
    if (node->mode == RENDER_FADER_MODE_OUT) {
        struct timespec spec;
        get_time(&spec);

        long current_ms = timespec_to_ms(&spec);
        long init_ms = timespec_to_ms(&node->start_time_spec);

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
        if (current->mode == RENDER_FADER_MODE_DELETE) {
            (*previous_ptr) = (fade_node *) current->next;
            free(current);
        } else {
            previous_ptr = (fade_node **) &current->next;
        }

        current = (*previous_ptr);
    }
}
