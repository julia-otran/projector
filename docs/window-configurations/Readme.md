Window Configurations
=====================

If the default window configuration doesn't work for you, you can create a template and customize it.

After the program started, go to `Configurações` -> `Telas` -> `Criar Preset` and type the name for the preset.

The created presets are in `~/Projector/Window Configs/*.json`, or the equivalent on Windows `C:\Users\your-user\Projector\Window Configs\*.json`

## About these docs

There are some examples of various types of configs. You should generate a new preset inside the software
and adjust it according to a sample or by creating your own config.

## Examples Index
- [Sample 1](https://github.com/julia-otran/projector/tree/master/docs/window-configurations/sample-1)

## Example of a complete window config preset

```
{
  "renders": [
    {
      "render_id": 1,
      "render_name": "Tela",
      "w": 2560,
      "h": 1080,
      "text_area": {
        "x": 30,
        "y": 30,
        "w": 2500,
        "h": 1020
      },
      "enable_render_background_assets": 1,
      "enable_render_image": 1,
      "enable_render_video": 1,
      "render_mode": 1,
      "background_clear_color": {
        "r": 0,
        "g": 0,
        "b": 0
      },
      "text_color": {
        "r": 1,
        "g": 1,
        "b": 0
      }
    }
  ],
  "display": [
    {
      "monitor_bounds": {
        "x": -2560,
        "y": 0,
        "w": 2560,
        "h": 1080
      },
      "projection_enabled": 1,
      "virtual_screens": [
        {
          "source_render_id": 1,
          "input_bounds": {
            "x": 0,
            "y": 0,
            "w": 1,
            "h": 1
          },
          "output_bounds": {
            "x": 0,
            "y": 0,
            "w": 2560,
            "h": 1080
          },
          "color_balance": {
            "shadows": {
              "r": 0,
              "g": 0,
              "b": 0
            },
            "midtones": {
              "r": 0,
              "g": 0,
              "b": 0
            },
            "highlights": {
              "r": 0,
              "g": 0,
              "b": 0
            },
            "shadows_luminance": 0.0,
            "midtones_luminance": 0.0,
            "highlights_luminance": 0.0
          },
          "white_balance": {
            "bright": {
              "r": 0,
              "g": 0,
              "b": 0
            },
            "exposure": {
              "r": 1,
              "g": 1,
              "b": 1
            }
          },
          "blends": [
            {
                "position: { "x": 0, "y": 0, "w": 100, "h": 1080 },
                "direction": 0,
            }
          ],
          "help_lines": [
            {
                "x1": 0,
                "x2": 2560,
                "y1: 0,
                "y2": 0,
                "line_width": 2.0
            }
          ],
          "black_level_adjusts": [
            {
                "x1": 0,
                "x2": 1000,
                "y1": 0,
                "y2": 1000,
                "color": { "r": 0.0, "g": 0.1, "b": 0.0 },
                "alpha": 0.1,
            }
          ]
        }
      ]
    }
  ]
}
```

- `renders`: [Required]

This defines a place where the media will be painted. You may have how many you want.

There are some use cases that you can have a dedicated render:
- The main output
- Stage outputs
- Green screen for live broadcast

There isn't a limit of renders, however be sure that your video card will be capable of processing many pixels as
you get with the renders quantity and size

1. `renders[].renderId`: Unique integer identifier for the render. May be 1, 2, 3, 4...
2. `renders[].renderName`: Use this field to identify the render, Like `Main`, `Stage Monitors`, `Live Broadcast`
3. `renders[].w`: Total width for this render.
   *Notice that a render may be split between monitors or virtual screens*
   Such case, you should set w as the total screen width
4. `renders[].h`: Total height for this render.
5. `renders[].text_area`: Bounds of the area that can be used to render the lyrics
6. `renders[].enable_render_background_assets`: If set to 1, background videos and background image will be rendered in this render.
   *Set to 0 if this is a live broadcast render*
7. `renders[].enable_render_image`: If set to 1, images will be rendered to this render.
8. `renders[].enable_render_video`: If set to 1, videos and webview will be rendered to this layer.
9. `renders[].render_mode`: Set to 1 if this is the main render, otherwise, set 2.
   *Be sure to only have one render with render_mode 1*
10. `renders[].background_clear_color`: Background color for the render. You can set to green for a live broadcast render.
11. `renders[].text_color`: Text color.

- `display`

This sections defines where the renderers output will be placed on one or multiple screens.

1. `display[].monitor_bounds`: This field is used to identify the monitor.
   *If you change a monitor resolution or position on the operating system config, you should update this field so it
   matches the new configs*

2. `display[].projection_enabled`: If a projection window should be displayed in this monitor.

3. `display[].virtual_screen`: This is like a mapping of a render area and a monitor area.
    - You can also create blends (useful when projecting to one larger screen composed by multiple projectors)
    - Black level adjusts (useful when projecting to one larger screen composed by multiple projectors)
    - Help lines (useful for aligning one or multiple projectors)
    - White Balance
    - Color Balance

4. `display[].virtual_screen[].source_render_id`: Specify the id of the render which will be displayed in this virtual screen

5. `display[].virtual_screen[].input_bounds`: Specify the input area of the render that will be used.
   *These values should be between 0 (means the left-bottom side of the render) and 1 (means the right-top side of the render)*

6. `display[].virtual_screen[].output_bounds`: Specify the area of the monitor where the virtual screen will be displayed.

7. `display[].virtual_screen[].blends[].position`
    - `display[].virtual_screen[].blends[].position.x`
    - `display[].virtual_screen[].blends[].position.y`
    - `display[].virtual_screen[].blends[].position.w`
    - `display[].virtual_screen[].blends[].position.h`
      This defines the blend position.

8. `display[].virtual_screen[].blends[].direction`:
    - `0` Left to Right
    - `1` Right to Left
    - `2` Top to Bottom
    - `3` Bottom to Top
