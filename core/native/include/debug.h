#include <stdio.h>
#include <stdlib.h>

#ifndef _DEBUG_H_
#define _DEBUG_H_

#define log(args...) {\
    printf(args);\
    fflush(stdout);\
}

#endif