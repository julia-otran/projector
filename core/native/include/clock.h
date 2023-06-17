#include <time.h>

#ifndef _CLOCK_H_
#define _CLOCK_H_

void get_time(struct timespec *ts);
unsigned long long get_delta_time_ms(struct timespec* last, struct timespec* before);
void copy_time(struct timespec* destination, struct timespec* source);
#endif // !_CLOCK_H_

