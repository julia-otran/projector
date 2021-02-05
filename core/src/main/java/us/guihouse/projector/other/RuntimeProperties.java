package us.guihouse.projector.other;

import lombok.Setter;

public class RuntimeProperties {
    @Setter
    private static boolean logFPS;

    public static boolean isLogFPS() {
        return logFPS;
    }

    private static final String LOG_FPS_ARG = "--log-fps";

    public static void init(String[] args) {
        for (String arg : args) {
            if (LOG_FPS_ARG.equals(arg)) {
                setLogFPS(true);
            }
        }
    }
}
