Projector
=========

Software for media (lyrics, images, videos, countdown, window capture, NDI®) projection

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
- NDI® SDK required

## Building Windows

1. If you don't have git configured to create symlinks, you have to delete the 
    `windows\native\LibRender\LibRender\src` file and instead create a symlink. You can do this by opening a 
    Command Prompt with admin permissions and running: 
    ```
   cd windows\native\LibRender\LibRender
   del src
   mklink /d src ..\..\..\..\core\native
   ```
    
2. Install the NDI ® SDK (currently Version 6) in the default location. (You may change the default location, 
however if changed, adjusts should be made to the Visual Studio solution) 

3. Generate the lib render JNI headers: (You may use the `Generate Headers` IntelliJ Run Config)
    ```
    mvn compile -pl core
    ```
4. Open `windows\native\LibRender\LibRender.sln` on Visual Studio
5. On Visual Studio, compile LibRender dynamic library
6. Package the Windows jar (You may run the `Package Windows` IntelliJ Run Config)
    ```
    mvn clean package -pl core,windows
    ```

    The Windows `jar` should be created at `windows/target/projector-windows-x.x-jar-with-dependencies.jar`

## Building Linux

*Be sure to have installed: *
- gcc 
- opengl library development headers

1. Unpack the NDI ® SDK inside `linux/native/LibNDI` directory.

Just run (Or, use the `Package Linux` IntelliJ Run Config)

```
mvn clean package -pl core,linux
```

## Building OSX
1. Install VLC Player App
2. Install GLFW via Brew
   ```
   brew install glfw
   ```
3. Generate the lib render JNI headers: (You may use the `Generate Headers` IntelliJ Run Config)
    ```
    mvn compile -pl core
    ```
4. Open the XCode project and compile the libProjector
5. Package the OSx jar (You may run the `Package MacOs` IntelliJ Run Config)
    ```
    mvn clean package -pl core,osx
    ```

   The OSx `jar` should be created at `osx/target/projector-osx-x.x-jar-with-dependencies.jar`


_Note: The OSx project is not done yet. There are missing platform dependent features: Video Device capture and Window capture_

Minimum Requirements
====================

To execute projector software you will need:

- GPU with OpenGL 3.2 and GLSL 1.0 support (you can even use mesa, however performance may be degraded)
- 2GB free ram for running the software

Preparing the system to run the built jar directly
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

- Support for NDI® Inputs and also Outputs
- Support for blending projectors (Blending, advanced color matching, black level adjust)
- Support for live broadcast lyrics output (with green background)
- Support for stage monitors
- Full automatic and output independent lyrics line breaking (it can break lines for main output while keeping non-broken lines for live broadcast output) 
- Splitting screen support (you may use a 2x2 HDMI Wall Controller and split 1 video output into 4 independent outputs)

See: [Window Configuration Docs](https://github.com/julia-otran/projector/tree/master/docs/window-configurations)

Also See [NDI®](https://ndi.video)
