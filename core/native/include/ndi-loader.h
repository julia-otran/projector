#ifndef _NDI_LOADER_H_
#define _NDI_LOADER_H_

#include "Processing.NDI.Lib.h"

#ifdef _WIN32

#pragma warning(suppress : 4996)

#ifdef _WIN64
#pragma comment(lib, "Processing.NDI.Lib.x64.lib")
#else // _WIN64
#pragma comment(lib, "Processing.NDI.Lib.x86.lib")
#endif // _WIN64
#endif

#endif // !_NDI_LOADER_H_