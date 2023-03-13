package dev.juhouse.projector.projection2;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Bridge {
    private static String[] getShaderNames() {
        return new String[] {
                "blend.fragment.shader",
                "blend.vertex.shader",
                "color-corrector.fragment.shader",
                "color-corrector.vertex.shader",
        };
    }

    private void loadShaders() {
        for (String shaderName : getShaderNames()) {
            try {
                this.loadShader(shaderName, IOUtils.resourceToString("/shaders/" + shaderName, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Bridge() {
        loadShaders();
    }

    public native void loadShader(String name, String data);

    public native void initialize();

    public native void shutdown();

    public native void loadConfig(String fileName);

    public native void generateConfig(String fileName);

    public native BridgeRender[] getRenderSettings();

    public native void setTextData(BridgeTextData[] data);

    public native void setVideoBuffer(ByteBuffer buffer, int width, int height, boolean crop);

    public native void setVideoBufferRenderFlag(int flag);

    public native void updateVideoBuffer();

    public native void setImageAsset(int[] data, int width, int height, boolean crop, int renderFlag);

    public native void setWebViewBuffer(ByteBuffer buffer, int width, int height);

    public native void setRenderWebViewBuffer(int renderFlag);

    public native void downloadPreviewData(ByteBuffer buffer);

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
}
