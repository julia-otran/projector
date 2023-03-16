Projector Window Configuration
=====

# Sample 1

This sample describes a configuration composed by:

- One video display output, connected to a 2x2 HDMI Wall Controller
- The display is fractionated in a 2x2, so we transform the Wall Controller outputs on 4 independent outputs
- Top Left part of display is connected to the left projector
- Top Right part of display is connected to the right projector
- Bottom Left part of display is connected to the Stage Monitors
- Bottom Right part of display is sent to live broadcast as a green screen

So, we have 3 renders
- The main, sized 1820x540 (see about blending projector)
- A secondary, sized 960x540 used to stage monitors
- A secondary, sized 960x540 used to live broadcast

## About blending projectors

When blending projectors, we got something like:

```
  +-------+-------+-------+
  | #1    | 1 + 2 | #2    |
  +-------+-------+-------+
```

- The middle of the screen is composed by overlapping both projectors.

- The render width should be the sum of the projector width, minus one time the size of overlapping area
    In this example, we considered that the projectors will use 960x540 resolution. 
    (the HDMI wall ctrl may upscale it to 1920x1080, or maybe 1280x720, however what really matters in such case is the aspect ratio,
    so using 960x540 makes calcs easy. You might have a 4k output and just multiply these values by 2)
    So, 960+960 = 1920, however we had a 100px overlapping area, Which results into a 1820 total width
    As result, the main render will be sized 1820x540

- The input bound width of the virtual screens #1 #2 should have 960 pixels: This gives a width of ~0.527472 (960/1820)
- The input bound height of the virtual screens will be 1.
- The input bound x of virtual screen #1 will be at 0
- The input bound x of virtual screen #2 will be at x = ~0.472527 (1 - 0.527472)
- The input bound y of virtual screen #1 and #2 will be 0

- You should set a blend from left-to-right on virtual screen #1
- You should set a blend from right-to-left on virtual screen #2 
