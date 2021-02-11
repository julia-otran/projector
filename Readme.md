Projector
=========

Software for media (lyrics, images, videos) projection

Contribute
==========

Just open a PR or a Issue. If you notice any bug, please, let me know.

Build
=====
- Use `maven` and at least `java 13`

To build, just run this command to the top level project folder:

```
mvn package
```

The Windows `jar` should be created at `windows/target/projector-windows-x.x-jar-with-dependencies.jar`
The other platforms (`linux` and `macOS`) should be with same name, inside `target` folder for the platform desired.

Install
==========

## Windows

1. Install Java 13
2. Install o VLC Player (versão 3.x)
   ### Beware
   - You probably installed JDK 64 bits. So you must install VLC 64 bits. Pay attention that VLC website by default directs to the 32 bits version. Be sure to select the 64 bits distribution.
   - You may install VLC preferably in `C:\VLC`. If you do this, also set and environment variable `VLC_PLUGIN_PATH` with value `C:\VLC\plugins`, otherwise program won't start.


Window Configurations
=====================

If the default window configuration doesn't work for you, you can create a template and costomize it.

After the program started, go to `Configurações` -> `Telas` -> `Criar Preset` and type the name for the preset.

The created presets are in `~/Projector/Window Configs/*.json`, or the equivalent on Windows `C:\Users\your-user\Projector\Window Configs\*.json`

## Example of a complete window config preset


```
[
  {
    "displayId": ":0.1",
    "virtualScreenId": "main",
    "displayBounds": {
      "x": 0,
      "y": 0,
      "width": 1920,
      "height": 1080
    },
    "project": true,
    "x": 0,
    "y": 0,
    "width": 1920,
    "height": 1080,
    "helpLines": [{ "X1": 0, "X2": 1920, "Y1": 0, "Y2": 0, "lineWidth": 3.0 }],
    "blends": [{ "x": 0, "y": 0, "width": 1920, "height": 200, "direction": 2, "id": 1, "useCurve": true }],
    "blackLevelAdjust": {
      "points": [{ "x": 0, "y": 20 }, { "x": 1000, "y": 100 }, { "x": 600, "y": 600 }, { "x": 100, "y": 500 }],
      "offset": 100
    },
    "whiteBalance": {
      "bright": {
        "r": 0,
        "g": 0,
        "b": 0
      },
      "exposure": {
        "r": 1.0,
        "g": 1.0,
        "b": 1.0
      }
    },
    "colorBalance": {
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
      "preserveLuminosity": true
    }
  }
]

```

1. `displayId`: [Required] Internal identity of the monitor.

If there's no `displayBounds` set, `displayId` will be used to assign the window to the monitor. However, this id is not trustworthy.

*Don't change this value. If experience issues with window showing in wrong monitor, please, check displayBounds*

2. `virtualScreenId`: [Optional, defaults to `main`] virtual screen identity:

  A virtual screen is where content is rendered, and then a virtual screen is rendered to the monitor. This allows using monitors from different sizes (and using `x`, `y`, `width`, `height` to choose which area should be displayed in the monitor), and also allows monitors to be concatenated (and setting an overlap area too).

  The virtual screen size is calculated by the `max(x + width)` and `max(y + height)` of all screens with the same `virtualScreenId`.

  It is also possible to have more than one virtual screen. If you have different sizes screens, you can also create two or more virtual screens with different sizes.

  Another possibility, is to have a `chroma` virtual screen, which is screen with green background, so you can use with a Chroma Key filter to legend videos.

  ### Possible values to `virtualScreenId`

  - `main`: The main virtual screen
  - `chroma`: The chroma virtual screen
  - `main-chroma`: If there is no main virtual screen, and you just want the chroma to be generated, use this value, so the main virtual screen becomes a chroma screen.
  - qualquer outro valor: cria uma tela virtual com o id nomeado.

3. `displayBounds` [Optional, if absent will fallback to `displayId`]:
  Used to assign the window to the monitor. As you can see, we do this via monitor coordinates and resolution.

  You should not change this values. If you change a monitor resolution, or a monitor position (in the Display settings of your Operational System) these values won't work anymore. You can try to ajust it, but I will suggest that you create a new window config template and copy settings from the old to the new file.

  *Do not change these values unless you change a monitor coordinate or resolution*

4. project: Whether should be projected to this screen

5. `x` and `y`: [Required. Value `0` means no customization] Coordinates where virtual screen will be rendered to the monitor.

  *Useful if you want to merge two or more projectors. See explanation below.*

  Suppose your big screen is conposed by two projectors, Both with 1280x720 resolution, in the following disposition:
  ```
  +----------------+----------------+
  | Projector 1    | Projetcor 2    |
  +----------------+----------------+
  ```
  You shoud use `x = 0; y = 0` for projector 1 and `x = 1280; y = 0` for projector 2.

  This way, a virtual screen with size 2560x720 will be created and it will be divided into the two projectors.

  Also, you might want set an overlap (blend) in the middle so the union of they become smooth. Something like this:
```
  +-------+-------+-------+
  | #1    | #1 #2 | #2    |
  +-------+-------+-------+
```
  In this way, you should first measure the `blend` size. You can do this following intructions under the `helpLines` option.

  Suppose we had a 100px blend. You should configure `x` and `y` following way:

```
#1: x = 0; y = 0;
#2 x = 1180; y = 0;
```
  *Tip: use help lines to measure the blend size, when setting up for the first time.*
  *Don't worry about the union beign more brighter with bright colors. You should fix this using `blends` (See item 8). There's also support for fixing the black level (See item 9).*

6. `width` and `height` [Required. The default value is equal to `displayBounds.width` and `displayBounds.height`, so, the monitor resolution]: They are used to calculate the virtual screen size.

  *You may not want change these values*
  *They are useful if you are using monitors of different sizes, in such case, set both monitors to the same value to scale the rendering of the virtual screen to the monitor*

7. `helpLines` [Required. Can be an empty array `[]`]: You can create lines to help you align the projectors.

  *Suppose projectors resolution are 1280x720 and blending area of 100px*

  - Measuring `blend` size when using two projectors:
    1. In the projector #1 create a line to the right: `X1 = 1280, X2 = 1280, Y1 = 0, Y2 = 720`
    2. In the projetcor #2 create a line to the left: `X1 = 0, X2 = 0, Y1 = 0, Y2 = 720`
    3. Using the line created on step 2, grow the values of X1 and X2 until the line of #2 and #1 become just one thin line
    4. The X1 or X2 (should be same value. Adjust projectors keystone if it are crossed) of projector #2 will be the blending size.

    *Be sure the projectors keystone are paralell. Otherwise lines should cross instead of beign one. Adjust the keystone in projector if that's the case*

  - Align two projectors (Use this every time that the blending are becomes blurry)
    1. In the projector #1 create a line to the right `X1 = 1280, X2 = 1280, Y1 = 0, Y2 = 720`
    2. In the projector #1 create another line to the right (given a blend area of `100px -> X1=1180, X2=1180, Y1=0, Y2=720`)
    3. In the projector #2 create a line to the left (`X1 = 0, X2 = 0, Y1 = 0, Y2 = 720`)
    4. In the projector #2 create another line to the left (given a blend area of 100px -> `X1=100, X2=100, Y1=0, Y2=720`)
    5. Align both projectors so the 4 lines becomes just 2 thin lines

  - Align edges (for each of the projectors, you can do even if you have only one projector)
    1. Create a line to the top `X1=0, X2=1280, Y1=0, Y2=0`
    2. Create a line to the bottom `X1=0, X2=1280, Y1=719, Y2=719`
    3. Create a line to the left: `X1 = 0, X2 = 0, Y1 = 0, Y2 = 720`
    4. Create a line to the right: `X1 = 1280, X2 = 1280, Y1 = 0, Y2 = 720`
    5. Align corners so lines stay inside the screen white area.

  *Tip: if you cannot see the line, or the line is too weak, try to increase the `lineWidth`.*

8. `blends`:

  When using 2 projectors with a blend area, you notice that the union is more bighter for brighter colors. To fix this, use these blends.

  The main idea is to create a gradient from lightest to darkest on right of projector #1 and from darkest to lightest for projector #2, so the bright becomes constant and we got a fade betwwen the projectors, which turns the union smooth.

  *Given a blend area of 100px*

  1. In the projector #1, create a `blend` with `x=1180, y=0, width=100, height=720, direction=0`
  2. In the projector #2, create a `blend` with `x=0, y=0, width=100, height=720, direction=1`

9. `blackLevelAdjust`: If you use two projectors, you may notice that there is a lighter "black" part in the union.

  So, for each of the projectors you should create a quad (or another form with more vertices) and lighten the darkest part so it become equal to the union.

  The `points` for projector #1 should be something like
  ```
  [{ "x": 0, "y": 0 }, { "x": 0, "y": 720 }, { "x": 1000, "y": 720 }, { "x": 950, "y": 0 }]
  ```

  Adjust the `x` of the last two points to match the lightest part of the union. Do the same for projector #2.

  You should equalize the value of `offset` so the black keeps constant on entire screen. Values can be different for different projectors.

10. `whiteBalance`: Adjust the white balance for each of the projectors if there white is not equal.

  You can set a bright and a exposure value for each of the red (`r`), green (`g`) and blue (`b`) channels.

  If you increase or decrease both RGB values the same quantity, you can control the bright and exposure.

  - The `bright` values can go from `-1.0` (darkest) to `0.0` (neutral) to `1.0` lightest
  - The `exposure` values can go from `0.0` (darkest) to `1.0` (neutral) to `+∞`

  *Useful when the white is different between the projectors*
  *Tip: to equalize the white, don't increase R, G or B values. Always use values from -1.0 and 0.0 to bright and 0.0 to 1.0 for exposure. This is because it is not possible to increase a white color to be more lighter. So, instead,
  equalize the color reducing R, G, B values*

11. `colorBalance`: Color balance

  Color balance values can go from `-1.0` (darkest) to `0.0` (neutral) to `1.0` (lightest)

  *Useful when need to correct a projector color balance. Mainly when there are multiple projectors and it color is different even after matching the white. Notice that color balance won't affect white balance*
