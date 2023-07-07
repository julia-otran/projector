#pragma comment(lib, "mf.lib")
#pragma comment(lib, "mfplat.lib")
#pragma comment(lib, "mfreadwrite.lib")
#pragma comment(lib, "mfuuid.lib")

#define SafeRelease(ptr) if (ptr) { ptr->lpVtbl->Release(ptr); ptr = NULL; }

#include <windows.h>
#include <stringapiset.h>
#include <combaseapi.h>
#include <dshow.h>
#include <uuids.h>
#include <mfapi.h>
#include <mfobjects.h>
#include <mfidl.h>
#include <mfreadwrite.h>
#include <Mferror.h>
#include <CameraUIControl.h>
#include <mftransform.h>

#include "turbojpeg.h"
#include "debug.h"
#include "video-capture.h"

#define WideToCharArr(wide, char_out_arr) \
    int internal_len = WideCharToMultiByte(1252, 0, wide, -1, NULL, 0, 0, 0); \
	char_out_arr = malloc(internal_len + 1); \
	WideCharToMultiByte(1252, 0, wide, -1, char_out_arr, internal_len, 0, 0); \
	char_out_arr[internal_len] = 0; \

static IMFSourceReader* source_reader;
static IMFMediaSource* media_source;
static IMFMediaType* media_type;
static DWORD streamIndex;

static tjhandle turbo_jpeg;

static char* device_name;
static int width, height;

int FindDeviceIndexByName(IMFActivate** ppDevices, char* name, UINT count)
{
    int index = -1;

    for (DWORD i = 0; i < count; i++)
    {
        HRESULT hr = S_OK;
        WCHAR* szFriendlyName = NULL;

        // Try to get the display name.
        UINT32 cchName;
        hr = ppDevices[i]->lpVtbl->GetAllocatedString(ppDevices[i],
            &MF_DEVSOURCE_ATTRIBUTE_FRIENDLY_NAME,
            &szFriendlyName, &cchName);

        if (SUCCEEDED(hr))
        {
            char* cmp;
            WideToCharArr(szFriendlyName, cmp)
            if (strcmp(cmp, name) == 0)
            {
                index = i;
            }
            free(cmp);
        }
        CoTaskMemFree(szFriendlyName);
    }

    return index;
}

HRESULT CreateVideoDeviceSource(IMFMediaSource** ppSource, char* device_name)
{
    *ppSource = NULL;

    IMFMediaSource* pSource = NULL;
    IMFAttributes* pAttributes = NULL;
    IMFActivate** ppDevices = NULL;

    // Create an attribute store to specify the enumeration parameters.
    HRESULT hr = MFCreateAttributes(&pAttributes, 1);
    if (FAILED(hr))
    {
        goto done;
    }

    // Source type: video capture devices
    hr = pAttributes->lpVtbl->SetGUID(
        pAttributes,
        &MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE,
        &MF_DEVSOURCE_ATTRIBUTE_SOURCE_TYPE_VIDCAP_GUID
    );

    if (FAILED(hr))
    {
        goto done;
    }

    // Enumerate devices.
    UINT32 count;
    hr = MFEnumDeviceSources(pAttributes, &ppDevices, &count);
    if (FAILED(hr))
    {
        goto done;
    }

    if (count == 0)
    {
        hr = E_FAIL;
        goto done;
    }

    int device_index = FindDeviceIndexByName(ppDevices, device_name, count);

    if (device_index == -1)
    {
        hr = E_FAIL;
        goto done;
    }

    // Create the media source object.
    hr = ppDevices[device_index]->lpVtbl->ActivateObject(ppDevices[device_index], &IID_IMFMediaSource, &pSource);

    if (FAILED(hr))
    {
        goto done;
    }

    *ppSource = pSource;
    (*ppSource)->lpVtbl->AddRef((*ppSource));

done:
    SafeRelease(pAttributes);

    for (DWORD i = 0; i < count; i++)
    {
        SafeRelease(ppDevices[i]);
    }
    CoTaskMemFree(ppDevices);
    SafeRelease(pSource);
    return hr;
}

HRESULT CreateSourceReader(
    IMFMediaSource* pSource,
    IMFSourceReader** ppReader)
{
    HRESULT hr = S_OK;
    IMFAttributes* pAttributes = NULL;

    hr = MFCreateAttributes(&pAttributes, 1);
    if (FAILED(hr))
    {
        goto done;
    }

    hr = MFCreateSourceReaderFromMediaSource(pSource, pAttributes, ppReader);

done:
    SafeRelease(pAttributes);
    return hr;
}

HRESULT EnumerateTypesForStream(IMFSourceReader* pReader, DWORD dwStreamIndex, IMFMediaType** ppMediaType, int width, int height)
{
    HRESULT hr = S_OK;
    DWORD dwMediaTypeIndex = 0;

    while (SUCCEEDED(hr))
    {
        IMFMediaType* pType = NULL;

        hr = pReader->lpVtbl->GetNativeMediaType(pReader, dwStreamIndex, dwMediaTypeIndex, &pType);
        
        if (hr == MF_E_NO_MORE_TYPES)
        {
            hr = S_FALSE;
            break;
        }
        else if (SUCCEEDED(hr))
        {
            AM_MEDIA_TYPE* amMediaType;
            GUID subType;

            pType->lpVtbl->GetGUID(pType, &MF_MT_SUBTYPE, &subType);

            if (IsEqualGUID(&subType, &MFVideoFormat_MJPG) == 0) 
            {
                pType->lpVtbl->Release(pType);
            }
            else
            {

                pType->lpVtbl->GetRepresentation(pType, FORMAT_VideoInfo, &amMediaType);

                VIDEOINFOHEADER* info_header = (VIDEOINFOHEADER*)amMediaType->pbFormat;

                if (info_header->bmiHeader.biWidth == width && info_header->bmiHeader.biHeight == height) {
                    (*ppMediaType) = pType;

                    pType->lpVtbl->FreeRepresentation(pType, FORMAT_VideoInfo, amMediaType);

                    return S_OK;
                }
                else
                {
                    pType->lpVtbl->FreeRepresentation(pType, FORMAT_VideoInfo, amMediaType);
                    pType->lpVtbl->Release(pType);
                }
            }
        }

        ++dwMediaTypeIndex;
    }

    return hr;
}

HRESULT EnumerateMediaTypes(IMFSourceReader* pReader, DWORD* pdwStreamIndex, IMFMediaType** ppMediaType, int width, int height)
{
    HRESULT hr = S_OK;
    DWORD dwStreamIndex = 0;

    while (SUCCEEDED(hr))
    {
        hr = EnumerateTypesForStream(pReader, dwStreamIndex, ppMediaType, width, height);

        if (hr == S_OK) {
            (*pdwStreamIndex) = dwStreamIndex;
            return S_OK;
        }

        if (hr == MF_E_INVALIDSTREAMNUMBER)
        {
            hr = S_OK;
            break;
        }

        ++dwStreamIndex;
    }

    return S_FALSE;
}

void SetCaptureDeviceOutputFormat(IMFSourceReader* pReader, DWORD dwStreamIndex, IMFMediaType* pMediaType)
{
    HRESULT hr = pReader->lpVtbl->SetCurrentMediaType(pReader, dwStreamIndex, NULL, pMediaType);

    if (FAILED(hr)) {
        log_debug("Failed to set media type\n");
    }
}

void video_capture_init() {
    MFStartup(MF_VERSION, 0);
    turbo_jpeg = tjInitDecompress();
}

void video_capture_set_device(char* name, int in_width, int in_height) {
    device_name = name;
    width = in_width;
    height = in_height;
}

void video_capture_open_device() {
    CreateVideoDeviceSource(&media_source, device_name);
    CreateSourceReader(media_source, &source_reader);

    HRESULT hr = EnumerateMediaTypes(source_reader, &streamIndex, &media_type, width, height);

    if (SUCCEEDED(hr) && hr != S_FALSE)
    {
        SetCaptureDeviceOutputFormat(source_reader, streamIndex, media_type);
    }
    else
    {
        log_debug("No media type found!\n");
    }
}

void video_capture_print_frame_int(void* dstBuffer, enum TJPF color) {
    DWORD stream_index;
    DWORD stream_flags;
    LONGLONG timestamp;

    IMFSample* sample;

    source_reader->lpVtbl->ReadSample(source_reader, MF_SOURCE_READER_FIRST_VIDEO_STREAM, 0, streamIndex, &stream_flags, &timestamp, &sample);

    if (sample == NULL) {
        return;
    }

    IMFMediaBuffer* srcBuffer;

    sample->lpVtbl->ConvertToContiguousBuffer(sample, &srcBuffer);

    BYTE* jpegData;
    DWORD maxLenght;
    DWORD currentLenght;

    srcBuffer->lpVtbl->Lock(srcBuffer, &jpegData, &maxLenght, &currentLenght);
    
    tjDecompress2(turbo_jpeg, jpegData, currentLenght, dstBuffer, width, width * 4, height, color, TJFLAG_FASTDCT | TJFLAG_FASTUPSAMPLE | TJFLAG_NOREALLOC);

    srcBuffer->lpVtbl->Unlock(srcBuffer);

    srcBuffer->lpVtbl->Release(srcBuffer);
    sample->lpVtbl->Release(sample);
    
}

void video_capture_preview_frame(void* buffer) {
    video_capture_print_frame_int(buffer, TJPF_BGRA);
}

void video_capture_print_frame(void* buffer) {
    video_capture_print_frame_int(buffer, TJPF_RGBA);
}

void video_capture_close() {
    source_reader->lpVtbl->Release(source_reader);
    media_source->lpVtbl->Release(media_source);
    media_type->lpVtbl->Release(media_type);
}

void video_capture_terminate() {
    MFShutdown();
    tjDestroy(turbo_jpeg);
}
