package us.guihouse.projector

import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL20

import java.io.IOException

import java.io.FileReader

import java.io.BufferedReader

import org.lwjgl.opengl.GL15

import org.lwjgl.BufferUtils
import java.nio.ByteBuffer


class Test internal constructor() {
    private var window: Long

    fun finish() {
        GLFW.glfwDestroyWindow(window)
    }

    companion object {
        const val VERTEX_SHADER = """
            #version 150 core
 
            in vec4 in_Position;
             
            void main(void) {
                gl_Position = in_Position;
            }
        """

        const val FRAGMENT_SHADER = """
            #version 150 core
             
            void main(void) {
                gl_FragColor = vec4(1.0, 0.0, 0.0, 0.0);
            }
        """

        @JvmStatic
        fun main(args: Array<String>) {
            val thread = Thread { Test() }
            thread.start()
            thread.join()
        }
    }

    // Quad variables
    private var vaoId = 0
    private var vboId = 0
    private var vbocId = 0
    private var vboiId = 0
    private var indicesCount = 0

    // Shader variables
    private var vsId = 0
    private var fsId = 0
    private var pId = 0


    init {
        GLFW.glfwInit()
        window = GLFW.glfwCreateWindow(1920, 1080, "Title", GLFW.glfwGetPrimaryMonitor(), 0)
        GLFW.glfwMakeContextCurrent(window)
        GL.createCapabilities()
        GLFW.glfwShowWindow(window)

        GL11.glClearColor(0.4f, 0.6f, 0.9f, 0f);

        GL11.glViewport(0, 0, 500, 500)

        GL30.glClear(GL30.GL_COLOR_BUFFER_BIT)
        this.setupQuad()
        this.setupShaders()

        while (!GLFW.glfwWindowShouldClose(window)) {
            // Do a single loop (logic/render)
            this.loopCycle()
            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()
        }

        destroyOpenGL()
        finish()
    }

    fun setupQuad() {
        // Vertices, the order is not important. XYZW instead of XYZ
        val vertices = floatArrayOf(
            -0.5f, 0.5f, 0f, 1f,
            -0.5f, -0.5f, 0f, 1f,
            0.5f, -0.5f, 0f, 1f,
            0.5f, 0.5f, 0f, 1f
        )
        val verticesBuffer = BufferUtils.createFloatBuffer(vertices.size)
        verticesBuffer.put(vertices)
        verticesBuffer.flip()
        val colors = floatArrayOf(
            1f, 0f, 0f, 1f,
            0f, 1f, 0f, 1f,
            0f, 0f, 1f, 1f,
            1f, 1f, 1f, 1f
        )
        val colorsBuffer = BufferUtils.createFloatBuffer(colors.size)
        colorsBuffer.put(colors)
        colorsBuffer.flip()

        // OpenGL expects to draw vertices in counter clockwise order by default
        val indices = byteArrayOf(
            0, 1, 2,
            2, 3, 0
        )
        indicesCount = indices.size
        val indicesBuffer: ByteBuffer = BufferUtils.createByteBuffer(indicesCount)
        indicesBuffer.put(indices)
        indicesBuffer.flip()

        // Create a new Vertex Array Object in memory and select it (bind)
        vaoId = GL30.glGenVertexArrays()
        GL30.glBindVertexArray(vaoId)

        // Create a new Vertex Buffer Object in memory and select it (bind) - VERTICES
        vboId = GL15.glGenBuffers()
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW)
        GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, 0, 0)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

        // Create a new VBO for the indices and select it (bind) - COLORS
        vbocId = GL15.glGenBuffers()
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbocId)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, colorsBuffer, GL15.GL_STATIC_DRAW)
//        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 0, 0)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

        // Deselect (bind to 0) the VAO
        GL30.glBindVertexArray(0)

        // Create a new VBO for the indices and select it (bind) - INDICES
//        vboiId = GL15.glGenBuffers()
//        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId)
//        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW)
//        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun setupShaders() {
        var errorCheckValue = GL11.glGetError()

        // Load the vertex shader
        vsId = loadShader(VERTEX_SHADER, GL20.GL_VERTEX_SHADER)
        // Load the fragment shader
        fsId = loadShader(FRAGMENT_SHADER, GL20.GL_FRAGMENT_SHADER)

        // Create a new shader program that links both shaders
        pId = GL20.glCreateProgram()
        GL20.glAttachShader(pId, vsId)
        GL20.glAttachShader(pId, fsId)

        // Position information will be attribute 0
        GL20.glBindAttribLocation(pId, 0, "in_Position")
        // Color information will be attribute 1
//        GL20.glBindAttribLocation(pId, 1, "in_Color")
        GL20.glLinkProgram(pId)
        GL20.glValidateProgram(pId)
        errorCheckValue = GL11.glGetError()
        if (errorCheckValue != GL11.GL_NO_ERROR) {
            System.out.println("ERROR - Could not create the shaders:" + errorCheckValue)
            System.exit(-1)
        }
    }

    fun loopCycle() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
        GL20.glUseProgram(pId)

        // Bind to the VAO that has all the information about the vertices
        GL30.glBindVertexArray(vaoId)
        GL20.glEnableVertexAttribArray(0)
//        GL20.glEnableVertexAttribArray(1)

        // Bind to the index VBO that has all the information about the order of the vertices
        //GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId)

        // Draw the vertices
        //GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0)
        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4)

        // Put everything back to default (deselect)
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
        GL20.glDisableVertexAttribArray(0)
//        GL20.glDisableVertexAttribArray(1)
        GL30.glBindVertexArray(0)
        GL20.glUseProgram(0)
    }

    fun destroyOpenGL() {
        // Delete the shaders
        GL20.glUseProgram(0)
        GL20.glDetachShader(pId, vsId)
        GL20.glDetachShader(pId, fsId)
        GL20.glDeleteShader(vsId)
        GL20.glDeleteShader(fsId)
        GL20.glDeleteProgram(pId)

        // Select the VAO
        GL30.glBindVertexArray(vaoId)

        // Disable the VBO index from the VAO attributes list
        GL20.glDisableVertexAttribArray(0)
        GL20.glDisableVertexAttribArray(1)

        // Delete the vertex VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        GL15.glDeleteBuffers(vboId)

        // Delete the color VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        GL15.glDeleteBuffers(vbocId)

        // Delete the index VBO
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
        GL15.glDeleteBuffers(vboiId)

        // Delete the VAO
        GL30.glBindVertexArray(0)
        GL30.glDeleteVertexArrays(vaoId)
    }

    fun loadShader(shaderSource: String, type: Int): Int {
        var shaderID = 0

        shaderID = GL20.glCreateShader(type)
        GL20.glShaderSource(shaderID, shaderSource)
        GL20.glCompileShader(shaderID)
        return shaderID
    }
}