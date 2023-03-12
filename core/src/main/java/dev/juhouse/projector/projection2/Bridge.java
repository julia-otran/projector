package dev.juhouse.projector.projection2;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Bridge {
    private static String[] getShaderNames() {
        return new String[] {
                "bicubic-filter.fragment.shader",
                "bicubic-filter.vertex.shader",
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

    public native int getTextRenderAreaWidth();

    public native int getTextRenderAreaHeight();

    public native int getRenderAreaWidth();

    public native int getRenderAreaHeight();

    public native BridgeRender[] getRenderSettings();

    public native void setTextImage(int[] data, int textHeight);

    public native void setVideoBuffer(ByteBuffer buffer, int width, int height, boolean crop);

    public native void setVideoBufferRenderFlag(int flag);

    public native void updateVideoBuffer();

    public native void setImageAsset(int[] data, int width, int height, boolean crop);

    public native void setWebViewBuffer(ByteBuffer buffer, int width, int height);

    public native void setRenderWebViewBuffer(boolean render);

    public native void downloadPreviewData(ByteBuffer buffer);
}
