Projector
=========

Software for media (lyrics, images, videos, countdown, window capture) projection

Usage Tutorials
===============

- PT-BR: https://drive.google.com/drive/folders/1nfW4n2-hX5BixKBA01zzeReR9PcODZPa?usp=share_link

Contribute
==========

Just open a PR or an issue. If you notice any bug, please, let me know.

I18n
=====

There's only support to pt-BR right now.

Build
=====
- Java 21 required

## Building Windows

1. If you don't have git configured to create symlinks, you have to delete the 
    `windows\native\LibRender\LibRender\src` file and instead create a symlink. You can do this by opening a 
    Command Prompt with admin permissions and running: 
    ```
   cd windows\native\LibRender\LibRender
   del src
   mklink /d src ..\..\..\..\core\native
   ```
    
2. First, generate the lib render JNI headers: (You may use the `Generate Headers` IntelliJ Run Config)
    ```
    mvn compile -pl core
    ```
3. Open `windows\native\LibRender\LibRender.sln` on Visual Studio
4. On Visual Studio, compile LibRender dynamic library
5. Package the Windows jar (You may run the `Package Windows` IntelliJ Run Config)
    ```
    mvn clean package -pl core,windows
    ```

    The Windows `jar` should be created at `windows/target/projector-windows-x.x-jar-with-dependencies.jar`

## Building Linux

*Be sure to have gcc installed and also the opengl library development headers*

Just run (Or, use the `Package Linux` IntelliJ Run Config)

```
mvn clean package -pl core,linux
```

## Building OSX
There's no support to OSX right now. Feel free to create a XCode project so building lib render could be possible

Minimum Requirements
====================

To execute projector software you will need:

- GPU with OpenGL 3.2 and GLSL 1.0 support (you can even use mesa, however performance may be degraded)
- 2GB free ram for running the software

## Windows

No special requirements

## Linux

- This software only works with X11. 

Install
==========

## Windows

1. Install Java 21
2. Install Microsoft Visual C++ Redistributable
3. Install VLC Player (version 3.x)
   ### Beware
   - You must install VLC 64 bits. Pay attention that VLC website by default directs to the 32 bits version. Be sure to select the 64 bits distribution.
   - Install VLC Player on the default location. Other locations won't work with this software

## Linux

1. Install Java 21
2. Install Vlc Player from Ubuntu Software, or from your package manager

Window Configurations
=====================

The default window config will create a projection window at the first non-primary monitor.

This can be changed, and there's also a wonderful world of possibilities:

- Support for blending projectors (Blending, advanced color matching, black level adjust)
- Support for live broadcast lyrics output (with green background)
- Support for stage monitors
- Full automatic and output independent lyrics line breaking (it can break lines for main output while keeping non-broken lines for live broadcast output) 
- Splitting screen support (you may use a 2x2 HDMI Wall Controller and split 1 video output into 4 independent outputs)

See: [Window Configuration Docs](https://github.com/julia-otran/projector/tree/master/docs/window-configurations)
