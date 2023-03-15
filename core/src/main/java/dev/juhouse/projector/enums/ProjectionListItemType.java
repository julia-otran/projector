package dev.juhouse.projector.enums;

import dev.juhouse.projector.scenes.*;

import java.io.IOException;

public enum ProjectionListItemType {
    MUSIC, VIDEO, IMAGE, WEB_SITE, TEXT, TIMER;

    public ProjectionItemSubScene createSubScene(double width, double height) throws IOException {
        switch (this) {
            case TIMER:
                return TimerSubScene.Companion.createTimerScene(width, height);
            case WEB_SITE:
                return BrowserSubScene.createScene(width, height);
            case IMAGE:
                return ImageSubScene.createScene(width, height);
            case VIDEO:
                return PlayerSubScene.createScene(width, height);
            case MUSIC:
                return MusicProjectionScene.createScene(width, height);
            case TEXT:
                return TextSubScene.createScene(width, height);
        }

        return null;
    }
}
