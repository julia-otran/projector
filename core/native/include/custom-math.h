#ifndef _CUSTOM_MATH_H_
#define _CUSTOM_MATH_H_

#include <math.h>

#ifndef MAX
#define MAX(a,b) ((a) > (b) ? a : b)
#endif

#ifndef MIN
#define MIN(a,b) ((a) < (b) ? a : b)
#endif

#ifndef CLAMP
#define CLAMP(n,vmin,vmax) MAX(MIN(n, vmax), vmin)
#endif

#endif
