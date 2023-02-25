package dev.juhouse.projector.projection2;

public class Bridge {
    public native void initialize();

    public native void shutdown();

    public native void load_config(String fileName);
}
