#include "debug.h"
#include "video-capture.h"

#import <AVFoundation/AVFoundation.h>
#import "Projector-Swift.h"

AVCaptureDevice *avDevice;
AVCaptureDeviceFormat *format;
AVCaptureSession *session;
AVCaptureVideoDataOutput *output;
VideoCaptureBuffer *bufferDelegate;
dispatch_queue_t dispatchQueue;
AVCaptureInputPort *inputVideoPort;
AVCaptureConnection *connection;


int width, height;

bool captureAuthorized;

void video_capture_init() {
    
}

void video_capture_set_device(char* name, int in_width, int in_height) {
    NSArray<AVCaptureDeviceType> *deviceTypes = [
        [NSArray alloc]
        initWithObjects:AVCaptureDeviceTypeExternalUnknown, AVCaptureDeviceTypeBuiltInWideAngleCamera, nil];
    
    AVCaptureDeviceDiscoverySession *ds = [AVCaptureDeviceDiscoverySession discoverySessionWithDeviceTypes:deviceTypes mediaType:AVMediaTypeVideo position: AVCaptureDevicePositionUnspecified];
    
    NSString *nameString = [NSString stringWithUTF8String:name];
    
    avDevice = NULL;
    format = NULL;
    
    for (AVCaptureDevice *dev in ds.devices) {
        if ([nameString isEqualToString:dev.localizedName]) {
            avDevice = dev;
            width = in_width;
            height = in_height;
        }
    }
}

void video_capture_capture_authorized() {
    if (avDevice == NULL) {
        return;
    }
    
    bufferDelegate = NULL;
    
    NSError *inputDeviceError;
    AVCaptureDeviceInput *input = [[AVCaptureDeviceInput alloc] initWithDevice:avDevice error:&inputDeviceError];
    
    if (inputDeviceError != NULL || input == NULL) {
        return;
    }
    
    for (AVCaptureInputPort *p in [input ports]) {
        AVMediaType mediaType = [p mediaType];
        
        if ([AVMediaTypeVideo isEqualToString:mediaType]) {
            inputVideoPort = p;
            [p setEnabled:true];
        } else {
            [p setEnabled:false];
        }
    }
    
    NSError *lockError;
    [avDevice lockForConfiguration:&lockError];
    
    if (lockError == NULL) {
        for (AVCaptureDeviceFormat *fmt in [avDevice formats]) {
            CMVideoDimensions dimensions = CMVideoFormatDescriptionGetDimensions(fmt.formatDescription);
            
            if (dimensions.width == width && dimensions.height == height) {
                format = fmt;
            }
        }
        
        [avDevice setActiveFormat:format];
        [avDevice unlockForConfiguration];
    }
    
    dispatchQueue = dispatch_queue_create("VideoCaptureQueue", DISPATCH_QUEUE_SERIAL);
    dispatch_async(dispatchQueue, ^{
        session = [[AVCaptureSession alloc] init];
        [session beginConfiguration];
        
        output = [[AVCaptureVideoDataOutput alloc] init];
        [output setAlwaysDiscardsLateVideoFrames:true];
        [output setVideoSettings:[[NSMutableDictionary alloc] init]];
    
        bufferDelegate = [[VideoCaptureBuffer alloc] init];
        [output setSampleBufferDelegate:bufferDelegate queue:dispatchQueue];
    
        NSArray<AVCaptureInputPort*> *inputPorts = [[NSMutableArray alloc] initWithObjects:inputVideoPort, nil];
        connection = [[AVCaptureConnection alloc] initWithInputPorts:inputPorts output:output];
        [connection setEnabled:true];
        
        if ([session canAddInput:input] && lockError == NULL) {
            [session addOutputWithNoConnections:output];
            [session addInputWithNoConnections:input];
            [session addConnection:connection];
            [session commitConfiguration];
            [session startRunning];
        } else {
            if (inputDeviceError == NULL) {
                [avDevice unlockForConfiguration];
            }
            session = NULL;
        }
    });
    
}

void video_capture_open_device() {
    AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    
    if (authStatus == AVAuthorizationStatusNotDetermined) {
        [AVCaptureDevice requestAccessForMediaType:AVMediaTypeMuxed completionHandler:^(BOOL granted) {
         
         if (granted) {
           captureAuthorized = true;
           video_capture_capture_authorized();
         }
        }];
    } else if (authStatus == AVAuthorizationStatusAuthorized) {
        captureAuthorized = true;
        video_capture_capture_authorized();
    } else {
        captureAuthorized = false;
    }
}

void video_capture_preview_frame(void* buffer) {
    if (bufferDelegate != NULL) {
        [bufferDelegate printOutputPreview:buffer];
    }
}

void video_capture_print_frame(void* buffer) {
    if (bufferDelegate != NULL) {
        [bufferDelegate printOutput:buffer];
    }
}

void video_capture_close() {
    if (session != NULL) {
        [session stopRunning];
        session = NULL;
    }
    bufferDelegate = NULL;
    output = NULL;
    dispatchQueue = NULL;
    format = NULL;
    avDevice = NULL;
    
    if (connection != NULL) {
        [connection setEnabled:false];
        connection = NULL;
    }
    
    if (inputVideoPort != NULL) {
        [inputVideoPort setEnabled:false];
        inputVideoPort = NULL;
    }
}

void video_capture_terminate() {
}
