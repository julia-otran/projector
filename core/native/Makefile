BUILDDIR = output

RENDER_SOURCEDIR = render
RENDER_SOURCES = $(filter-out render/lib-render.c, $(wildcard $(RENDER_SOURCEDIR)/*.c))

RENDER_LINUX_SOURCEDIR = render/linux
RENDER_LINUX_SOURCES = $(filter-out render/lib-render.c, $(wildcard $(RENDER_LINUX_SOURCEDIR)/*.c))

CJSON_SOURCEDIR = cJSON
CJSON_SOURCES = cJSON/cJSON.c cJSON/cJSON_Utils.c

TINYCTHREAD_SOURCEDIR = tinycthread/source
TINYCTHREAD_SOURCES = tinycthread/source/tinycthread.c

TRIANGLE_SOURCEDIR = triangle
TRIANGLE_SOURCES = triangle/triangle.c

triangle_objs = $(patsubst $(TRIANGLE_SOURCEDIR)/%.c,$(BUILDDIR)/%.o,$(TRIANGLE_SOURCES))

CFLAGS = -Iinclude -IcJSON -Itinycthread/source -Itriangle -fPIC -DENABLE_LOCALES -DTRILIBRARY -DANSI_DECLARATORS -I/usr/include

LDFLAGS = -L/usr/lib/x86_64-linux-gnu -L/usr/lib -lGLEW -lGL -lX11 -lvlc -lglfw -lm -m64 -lturbojpeg

EXECUTABLE = app-render

OBJECTS = output/app-render

CC=gcc

all: dir $(BUILDDIR)/$(EXECUTABLE)

dir:
	mkdir -p $(BUILDDIR)

$(OBJECTS):
	$(CC) $(CFLAGS) -o output/app-render $(RENDER_SOURCES) $(RENDER_LINUX_SOURCES) $(CJSON_SOURCES) $(TINYCTHREAD_SOURCES) $(TRIANGLE_SOURCES) $(LDFLAGS)

clean:
	rm -f output/app-render
