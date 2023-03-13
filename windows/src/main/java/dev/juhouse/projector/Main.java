package dev.juhouse.projector;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import java.io.File;

import static dev.juhouse.projector.utils.ResourceManager.unpackResource;

public class Main {

    private static void loadNativeLib() {
        File glfwLib = unpackResource("/glfw3.dll", "glfw3.dll");
        System.load(glfwLib.toString());

        unpackResource("/LibRender.exp", "LibRender.exp");
        unpackResource("/LibRender.pdb", "LibRender.pdb");
        unpackResource("/LibRender.lib", "LibRender.lib");

        File libExportFile = unpackResource("/LibRender.dll", "LibRender.dll");
        System.load(libExportFile.toString());
    }

    public static void main(String[] args) {
        loadNativeLib();
        Projector.main(args);
    }
}
