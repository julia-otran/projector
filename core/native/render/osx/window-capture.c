#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "debug.h"
#include "window-capture.h"

#import "Projector-Swift.h"
#import "ScreenCaptureKit/ScreenCaptureKit.h"

typedef struct {
    SCWindow *window;
    SCStreamConfiguration *stream_configuration;
    SCStream *stream;
    SCContentFilter *filter;
    WindowCaptureDelegate *delegate;
} window_capture_extra_data;

window_capture_list_callback callback_function;
SCShareableContent *shareable_content;
dispatch_queue_t window_capture_dispatch_queue;

void window_capture_init(window_capture_list_callback fn) {
    callback_function = fn;
}

void window_capture_get_window_list() {
    [SCShareableContent getShareableContentExcludingDesktopWindows:false onScreenWindowsOnly:true completionHandler:^(SCShareableContent * _Nullable shareableContent, NSError * _Nullable error)
     {
     shareable_content = shareableContent;
     
     if (shareableContent != NULL) {
        NSArray<SCWindow *> *windows = [shareableContent windows];
        char** return_char_arr = calloc([windows count], sizeof(char*));
        int count = 0;
     
        for (SCWindow *window in windows) {
     return_char_arr[count] = (char*) [[window title] UTF8String];
     count++;
        }
     
     callback_function(return_char_arr, count);
     free(return_char_arr);
     }
     }
    ];
}

void* window_capture_get_handler(char *window_name) {
    SCWindow *foundWindow = NULL;
    
    if (shareable_content != NULL) {
        NSString *nameString = [NSString stringWithUTF8String:window_name];
        
        NSArray<SCWindow *> *windows = [shareable_content windows];
     
        for (SCWindow *window in windows) {
            if ([nameString isEqualToString:[window title]]) {
                foundWindow = window;
            }
        }
    }
    
    window_capture_extra_data *extra_data = calloc(1, sizeof(window_capture_extra_data));
    extra_data->window = foundWindow;
    
    SCContentFilter *filter = [[SCContentFilter alloc] initWithDesktopIndependentWindow:foundWindow];
    extra_data->filter = filter;
    
    SCStreamConfiguration *config = [[SCStreamConfiguration alloc] init];
    [config setWidth:[foundWindow frame].size.width];
    [config setHeight:[foundWindow frame].size.height];
    
    CMTime time;
    time.value = 1;
    time.timescale = 50;
    
    [config setMinimumFrameInterval: time];
    [config setQueueDepth:1];
    
    extra_data->stream_configuration = config;
    
    WindowCaptureDelegate *delegate = [[WindowCaptureDelegate alloc] init];
    
    window_capture_dispatch_queue = dispatch_queue_create("WindowCaptureQueue", DISPATCH_QUEUE_SERIAL);
    
    SCStream *stream = [[SCStream alloc] initWithFilter:filter configuration:config delegate:delegate];
    NSError *error;
    
    [stream addStreamOutput:delegate type:SCStreamOutputTypeScreen sampleHandlerQueue:window_capture_dispatch_queue error:&error];
    
    extra_data->stream = stream;
    extra_data->delegate = delegate;
    
    dispatch_async(window_capture_dispatch_queue, ^{
        [stream startCaptureWithCompletionHandler:^(NSError * _Nullable error) {
         NSError *error2 = error;
         }];
    });
    
    return (void*) extra_data;
}

void window_capture_free_handler(void *handler) {
    window_capture_extra_data* handler_data = (window_capture_extra_data*) handler;
    
    SCStream *stream = handler_data->stream;
    
    [stream stopCaptureWithCompletionHandler:^(NSError * _Nullable error) {}];
    
    free(handler);
}

void window_capture_get_window_size(void *handler, int *out_width, int *out_height) {
    window_capture_extra_data* handler_data = (window_capture_extra_data*) handler;
    SCWindow *window = handler_data->window;
    SCStreamConfiguration *config = handler_data->stream_configuration;
    SCStream *stream = handler_data->stream;
    
    (*out_width) = [window frame].size.width;
    (*out_height) = [window frame].size.height;
    
    if ([config width] != [window frame].size.width || [config height] != [window frame].size.height) {
        [config setWidth:[window frame].size.width];
        [config setHeight:[window frame].size.height];
        
        [stream updateConfiguration:config completionHandler:^(NSError * _Nullable error) {}];
    }
}

void window_capture_get_image(void *handler, int width, int height, void *out_data) {
    window_capture_extra_data* handler_data = (window_capture_extra_data*) handler;
    WindowCaptureDelegate *delegate = handler_data->delegate;
    
    [delegate printScreen:out_data width:width height:height];
}

void window_capture_terminate() {
}
