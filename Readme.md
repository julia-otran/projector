Projector
=========

Software for media (lyrics, images, videos) projection

Contribute
==========

Just open a PR or a Issue. If you notice any bug, please, let me know.

Build
=====
- Java 19 recommended

## Building Windows

1. If you don't have git configured to create symlinks, you have to delete the 
    `windows\native\LibRender\LibRender\src` file and instead create a symlink. You can do this by opening a 
    Command Prompt with admin permissions and running: 
    ```
   cd windows\native\LibRender\LibRender
   del src
   mklink /d src ..\..\..\..\core\native
   ```
    
2. First, generate the librender JNI headers: (You may use the `Generate Headers` IntelliJ Run Config)
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
There's no support to OSX right now. Feel free to setup a XCode project so building librender could be possible

Install
==========

## Windows

1. Install Java 19
2. Install VLC Player (version 3.x)
   ### Beware
   - You must install VLC 64 bits. Pay attention that VLC website by default directs to the 32 bits version. Be sure to select the 64 bits distribution.
   - Install VLC Player on the default location. Other locations won't work with this software

## Linux

1. Install Java 19
2. Install Vlc Player from Ubuntu Software, or from your package manager

## Window Configurations

See [Window Configuration Docs](https://github.com/julia-otran/projector/tree/master/docs/window-configurations)
