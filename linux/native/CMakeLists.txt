cmake_minimum_required(VERSION 3.5.0)
project(projector VERSION 0.1.0 LANGUAGES C)

add_library(projector
src/cJSON/cJSON.c
src/cJSON/cJSON_Utils.c
src/tinycthread/source/tinycthread.c
src/triangle/triangle.c
src/render/clock.c
src/render/config.c
src/render/config-debug.c
src/render/config-parse.c
src/render/config-serialize.c
src/render/debug.c
src/render/lib-render.c
src/render/loop.c
src/render/monitor.c
src/render/ogl-loader.c
src/render/render.c
src/render/render-fader.c
src/render/render-image.c
src/render/render-pixel-unpack-buffer.c
src/render/render-preview.c
src/render/render-tex-blur.c
src/render/render-text.c
src/render/render-video.c
src/render/render-video-capture.c
src/render/render-web-view.c
src/render/render-window-capture.c
src/render/virtual-screen.c
src/render/vs-black-level-adjust.c
src/render/vs-blend.c
src/render/vs-color-corrector.c
src/render/vs-help-lines.c
)

target_include_directories(projector PRIVATE src/include src/cJSON src/tinycthread/source src/triangle)
