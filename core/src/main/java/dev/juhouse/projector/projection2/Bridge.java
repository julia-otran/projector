package dev.juhouse.projector.projection2;

import com.sun.jna.Pointer;
import org.apache.commons.io.IOUtils;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Bridge {
    private boolean shadersLoaded = false;
    private static String[] getShaderNames() {
        return new String[] {
                "blend.fragment.shader",
                "blend.vertex.shader",
                "blur.fragment.shader",
                "blur.vertex.shader",
                "color-corrector.fragment.shader",
                "color-corrector.vertex.shader",
                "direct.fragment.shader",
                "direct.vertex.shader",
        };
    }

    public void loadShaders() {
        if (shadersLoaded) {
            return;
        }

        shadersLoaded = true;

        for (String shaderName : getShaderNames()) {
            try {
                this.loadShader(shaderName, IOUtils.resourceToString("/shaders/" + shaderName, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private native void loadShader(String name, String data);

    public native void initialize();

    public native void shutdown();

    public native void runOnMainThreadLoop();

    public native void loadConfig(String fileName);

    public native void generateConfig(String fileName);

    public native BridgeRender[] getRenderSettings();

    public native void setTextData(BridgeTextData[] data);

    public void attachPlayer(MediaPlayer player) {
        attachPlayerPtr(Pointer.nativeValue(player.mediaPlayerInstance().getPointer()));
    }

    private native void attachPlayerPtr(long player);

    public native void reload();

    public static class VideoPreviewOutputBufferTooSmall extends Exception {}

    public static class VideoPreviewNoOutputData extends Exception {}

    public BridgeVideoPreviewSize downloadPlayerPreview(MediaPlayer player, ByteBuffer buffer) throws VideoPreviewOutputBufferTooSmall {
        BridgeVideoPreviewSize result = downloadPlayerPreviewPtr(
                Pointer.nativeValue(player.mediaPlayerInstance().getPointer()),
                buffer
        );

        if (result.getWidth() * result.getHeight() * 4 > buffer.capacity()) {
            throw new VideoPreviewOutputBufferTooSmall();
        }

        return result;
    }

    private native BridgeVideoPreviewSize downloadPlayerPreviewPtr(long player, ByteBuffer buffer);

    public void setVideoRenderFlag(MediaPlayer player, boolean crop, int flag) {
        if (player == null) {
            setVideoRenderFlagPtr(0, crop, flag);
        } else {
            setVideoRenderFlagPtr(
                    Pointer.nativeValue(player.mediaPlayerInstance().getPointer()),
                    crop,
                    flag
            );
        }
    }

    private native void setVideoRenderFlagPtr(long player, boolean crop, int flag);

    public native void setMultiImageAsset(int[] data, int width, int height, int renderId, boolean crop);

    public native void setWebViewBuffer(ByteBuffer buffer, int width, int height);

    public native void setRenderWebViewBuffer(int renderFlag);

    public native void downloadPreviewData(int renderId, ByteBuffer buffer);

    public native void getWindowList(BridgeWindowCaptureCallbacks callback);

    public native void setWindowCaptureWindowName(String name);

    public native void setWindowCaptureRender(int renderFlag);

    public native void setWindowCaptureCrop(boolean newVal);

    public int getRenderAreaWidth() {
        return Arrays.stream(getRenderSettings()).filter(config -> config.getRenderMode() == 1).findFirst().map(BridgeRender::getWidth).orElse(1280);
    }

    public int getRenderAreaHeight() {
        return Arrays.stream(getRenderSettings()).filter(config -> config.getRenderMode() == 1).findFirst().map(BridgeRender::getHeight).orElse(720);
    }

    public int getTextAreaWidth() {
        return Arrays.stream(getRenderSettings()).filter(config -> config.getRenderMode() == 1).findFirst().map(BridgeRender::getTextAreaWidth).orElse(1280);
    }

    public int getTextAreaHeight() {
        return Arrays.stream(getRenderSettings()).filter(config -> config.getRenderMode() == 1).findFirst().map(BridgeRender::getTextAreaHeight).orElse(720);
    }

    public native BridgeCaptureDevice[] getVideoCaptureDevices();

    public native void setVideoCaptureDevice(String deviceName, int width, int height);

    public native void downloadVideoCapturePreview(ByteBuffer buffer);

    public native void setVideoCaptureEnabled(boolean enabled);

    public native void setVideoCaptureRender(int render);

    public native void setVideoCaptureCrop(boolean crop);

    public native void addNDIDeviceFindCallback(BridgeNDIDeviceFindCallback callback);

    public native void removeNDIDeviceFindCallback(BridgeNDIDeviceFindCallback callback);

    public native void searchNDIDevices();

    public native void connectNDIDevice(String name);

    public native void downloadNDIPreview(ByteBuffer buffer, BridgeNDIPreviewInfo previewInfo);

    public native void setNDIEnabled(boolean enabled);

    public native void setNDIRender(int render);

    public native void setNDICrop(boolean crop);
}
