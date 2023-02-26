package dev.juhouse.projector.projection2;

import java.nio.ByteBuffer;

public class Bridge {
    public native void initialize();

    public native void shutdown();

    public native void loadConfig(String fileName);

    public native int getTextRenderAreaWidth();

    public native int getTextRenderAreaHeight();

    public native int getRenderAreaWidth();

    public native int getRenderAreaHeight();

    public native void setTextImage(int[] data, int textHeight);

    public native void setVideoBuffer(ByteBuffer buffer, int width, int height, boolean crop);

    public native void setRenderVideoBuffer(boolean render);

    public native void setImageAsset(int[] data, int width, int height, boolean crop);

    public native void setImageBackgroundAsset(int[] data, int width, int height, boolean crop);
}
