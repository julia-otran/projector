package us.guihouse.projector.projection.glfw;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;

public class RGBImageCopy {
    private static byte toByte(int num) {
        if (num >= 255) {
            num = num & 0xFF;
        }

        if (num <= Byte.MAX_VALUE && num >= 0) {
            return (byte) num;
        }

        num = num - 256;

        if (num >= Byte.MIN_VALUE && num <= Byte.MAX_VALUE) {
            return (byte) num;
        }

        return 0;
    }

    private static void copyPixel(int argb, ByteBuffer buffer) {
        byte num = toByte((argb >> 16) & 0xFF);
        buffer.put(num);
        num = toByte((argb >> 8) & 0xFF);
        buffer.put(num);
        num = toByte(argb & 0xFF);
        buffer.put(num);
    }

    public static void copyImageToBuffer(BufferedImage img, ByteBuffer buffer) {
        buffer.clear();

        int[] pixelsSrc = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();

        for (int argb : pixelsSrc) {
            copyPixel(argb, buffer);
        }

        buffer.flip();
    }
}
