/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.other;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author guilherme
 */
public class GraphicsFinder {

    public static class Device {

        private final GraphicsDevice device;
        private final boolean isProjectionDevice;

        private Device(GraphicsDevice device, boolean isProjectionDevice) {
            this.device = device;
            this.isProjectionDevice = isProjectionDevice;
        }

        public boolean isProjectionDevice() {
            return isProjectionDevice;
        }

        public GraphicsDevice getDevice() {
            return device;
        }
    }

    public static List<Device> getAvailableDevices() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();
        List<String> preferred = getPreferredProjectionDeviceID();

        return Arrays.stream(devices)
                .filter(GraphicsDevice::isFullScreenSupported)
                .map(dev -> new Device(dev, preferred.contains(dev.getIDstring())))
                .collect(Collectors.toList());

    }

    public static Device getDefaultDevice() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice dev = ge.getDefaultScreenDevice();

        return new Device(dev, false);
    }

    private static List<String> getPreferredProjectionDeviceID() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();
        GraphicsDevice main = ge.getDefaultScreenDevice();

        if (main == null) {
            return Collections.emptyList();
        }

        if (devices.length < 2) {
            return Collections.emptyList();
        }

        String mainId = main.getIDstring();

        return Arrays.stream(devices)
                .filter(GraphicsDevice::isFullScreenSupported)
                .map(GraphicsDevice::getIDstring)
                .filter(dev -> !dev.equals(mainId))
                .collect(Collectors.toList());
    }

    private static String getDeviceName(GraphicsDevice dev) {
        StringBuilder builder = new StringBuilder();

        int width = dev.getDisplayMode().getWidth();
        int height = dev.getDisplayMode().getHeight();

        return builder.append(dev.getIDstring())
                .append(" (")
                .append(width)
                .append("x")
                .append(height)
                .append(")")
                .toString();
    }
}
