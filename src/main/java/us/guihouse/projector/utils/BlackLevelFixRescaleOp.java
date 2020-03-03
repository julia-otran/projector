package us.guihouse.projector.utils;

import org.lwjgl.system.CallbackI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.util.Arrays;

public class BlackLevelFixRescaleOp {
    private float scaleFactor;
    private int offset;

    private int cachePointsWidth = 0;
    private int cachePointsHeight = 0;
    private int[] indexes = new int[0];
    private int[] rgbData = new int[0];

    private Object[] data;
    private int nbands;
    private int dataType;

    public BlackLevelFixRescaleOp(float scale, float offset) {
        this.scaleFactor = scale;
        this.offset = Math.round(offset);
    }

    public void filter(final BufferedImage src, final BufferedImage dst) {
        if (cachePointsWidth != src.getWidth() || cachePointsHeight != src.getHeight()) {
            System.out.println("generating cache points");

            cachePointsWidth = src.getWidth();
            cachePointsHeight = src.getHeight();

            int totalPixels = cachePointsWidth * cachePointsHeight;

            indexes = new int[totalPixels];
            rgbData = new int[totalPixels];
            data = new Object[totalPixels];

            int pos = 0;
            for (int x = 0; x < cachePointsWidth; x++) {
                for (int y = 0; y < cachePointsHeight; y++) {
                    indexes[pos] = ((x << 16) & 0xFFFF0000) + (y & 0xFFFF);
                    pos++;
                }
            }

            int nbands = src.getRaster().getNumBands();
            int dataType = src.getRaster().getDataBuffer().getDataType();

            if (nbands != this.nbands || dataType != this.dataType) {
                this.nbands = nbands;
                this.dataType = dataType;

                for (int i = 0; i < totalPixels; i++) {
                    switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                            data[i] = new byte[nbands];
                            break;
                        case DataBuffer.TYPE_USHORT:
                            data[i] = new short[nbands];
                            break;
                        case DataBuffer.TYPE_INT:
                            data[i] = new int[nbands];
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            data[i] = new float[nbands];
                            break;
                        case DataBuffer.TYPE_DOUBLE:
                            data[i] = new double[nbands];
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown data buffer type: " +
                                    dataType);
                    }
                }
            }
        }

        src.getRGB(0, 0, src.getWidth(), src.getHeight(), rgbData, 0, src.getWidth());

        Arrays.stream(indexes).parallel().forEach(point -> {
            int x = (point >> 16) & 0xFFFF;
            int y = point & 0xFFFF;
            int idx = (y * cachePointsWidth) + x;

            int argb = rgbData[idx]; //0; //src.getColorModel().getRGB(src.getRaster().getDataElements(x, y, data[idx]));

            int alpha = (argb >> 24 & 0xFF);

            int r = Math.min(255, Math.round((argb >> 16 & 0xFF) * scaleFactor) + offset);
            int g = Math.min(255, Math.round((argb >> 8 & 0xFF) * scaleFactor) + offset);
            int b = Math.min(255, Math.round((argb & 0xFF) * scaleFactor) + offset);

            rgbData[idx] = alpha << 24 | r << 16 | g << 8 | b;
        });

        dst.setRGB(0, 0, src.getWidth(), src.getHeight(), rgbData, 0, src.getWidth());
    }
}
