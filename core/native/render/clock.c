#include <time.h>

#include "clock.h"

#ifdef _WIN32

#define BILLION                             (1E9)
#include "windows.h"

static BOOL g_first_time = 1;
static LARGE_INTEGER g_counts_per_sec;

void get_time(struct timespec* ct)
{
    LARGE_INTEGER count;

    if (g_first_time)
    {
        g_first_time = 0;

        if (0 == QueryPerformanceFrequency(&g_counts_per_sec))
        {
            g_counts_per_sec.QuadPart = 0;
        }
    }

    if ((NULL == ct) || (g_counts_per_sec.QuadPart <= 0) ||
        (0 == QueryPerformanceCounter(&count)))
    {
        return -1;
    }

    ct->tv_sec = count.QuadPart / g_counts_per_sec.QuadPart;
    ct->tv_nsec = ((count.QuadPart % g_counts_per_sec.QuadPart) * BILLION) / g_counts_per_sec.QuadPart;

    return 0;
}
#endif

#ifndef _WIN32

void get_time(struct timespec* ct) {
    clock_gettime(CLOCK_REALTIME, ct);
}

#endif // !_WIN32

unsigned long long get_delta_time_ms(struct timespec* last, struct timespec* before) {
    unsigned long sec_delta = last->tv_sec - before->tv_sec;
    unsigned long new_ms = (unsigned long) (last->tv_nsec / 1.0e6);
    unsigned long old_ms = (unsigned long) (before->tv_nsec / 1.0e6);
    unsigned long long ms_delta = (sec_delta * 1000) + (new_ms - old_ms);

    return ms_delta;
}

void copy_time(struct timespec* destination, struct timespec* source) {
    destination->tv_sec = source->tv_sec;
    destination->tv_nsec = source->tv_nsec;
}