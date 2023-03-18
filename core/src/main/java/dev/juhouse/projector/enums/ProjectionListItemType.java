package dev.juhouse.projector.enums;

import dev.juhouse.projector.scenes.*;

import java.io.IOException;

public enum ProjectionListItemType {
    MUSIC, VIDEO, IMAGE, WEB_SITE, TEXT, TIMER, WINDOW_CAPTURE;

    public ProjectionItemSubScene createSubScene(double width, double height) throws IOException {
        return switch (this) {
            case WINDOW_CAPTURE -> WindowCaptureSubScene.Companion.createWindowCaptureScene(width, height);
            case TIMER -> TimerSubScene.Companion.createTimerScene(width, height);
            case WEB_SITE -> BrowserSubScene.createScene(width, height);
            case IMAGE -> ImageSubScene.createScene(width, height);
            case VIDEO -> PlayerSubScene.createScene(width, height);
            case MUSIC -> MusicProjectionScene.createScene(width, height);
            case TEXT -> TextSubScene.createScene(width, height);
        };
    }
}
