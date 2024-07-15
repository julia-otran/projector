#ifdef __APPLE_CC__
#include <Foundation/Foundation.h>
#endif

#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <stdint.h>
#include <string.h>
#include <sys/types.h>
#include <stddef.h>

#ifdef _WIN32

#include <basetsd.h>
#include <windows.h>

#endif

#ifndef _INT_VLC_LOADER_H_

#define _INT_VLC_LOADER_H_

#ifdef _WIN32
#define ssize_t SSIZE_T
#endif

#ifdef __gnu_linux__
#define ssize_t size_t
#endif

#ifdef __APPLE_CC__
#include "VLCKit/VLCKit.h"
#endif

#include "vlc/vlc.h"

#endif
