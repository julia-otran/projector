# makefile for programs foo and bar.

targets = app-render

TOCLEAN = $(TARGETS)  # this variable stores what should be erased on clean target.  Here we store the programs to erase.

SOURCEDIR += render
SOURCEDIR += cJSON
SOURCEDIR += tinycthread/source
SOURCES = $(wildcard $(SOURCEDIR)/*.c) triangle/triangle.c

BUILDDIR = output

app-render_objs = $(patsubst $(SOURCEDIR)/%.c,$(BUILDDIR)/%.o,$(SOURCES))
TOCLEAN += $(app-render_objs)  # here, we add the objects of foo

CFLAGS = -Iinclude -IcJSON -Itinycthread/source -Itriangle

app-render: $(app-render_objs)
	$(CC) $(LDFLAGS) -o $@ $(app-render_objs)

clean:
	rm -f $(TOCLEAN)
