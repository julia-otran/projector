package us.guihouse.projector.projection.glfw

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import kotlin.system.exitProcess

class GLFWColorCorrection constructor(private val texId: Int){
    companion object {
        const val VERTEX_SHADER_SRC = """
            #version 150 core
            in vec4 in_Position;
            in vec2 in_Uv;
            
            out vec2 frag_Uv;
            
            void main(void) {
                gl_Position = in_Position;
                frag_Uv = in_Uv;
            }
        """

        const val FRAGMENT_SHADER_SRC = """
            #version 150 core
            
            in vec2 frag_Uv;
            uniform sampler2D image;
            
            uniform vec4 brightAdjust;
            uniform vec4 exposureAdjust;
 
            uniform vec4 midAdjust;
            
            void main(void) {
                vec4 texel = texture2D(image, frag_Uv);
                
                gl_FragColor = texel * exposureAdjust + brightAdjust;
            }
        """
    }

    // Quad variables
    private var vaoId = 0
    private var vboId = 0
    private var uvGlBuffer = 0

    // Shader variables
    private var vsId = 0
    private var fsId = 0
    private var pId = 0
    private var textureUniform = 0

    // Uniforms
    private var brightAdjustUniform = 0
    private var exposureAdjustUniform = 0
    private var midAdjustUniform = 0

    fun init() {
        setupQuad()
        setupShaders()
        setupUniforms()
    }

    private fun setupQuad() {
        // Vertices, the order is not important. XYZW instead of XYZ
        val vertices = floatArrayOf(
            -1f, 1f, 0f, 1f,
            -1f, -1f, 0f, 1f,
            1f, -1f, 0f, 1f,
            1f, 1f, 0f, 1f
        )

        val verticesBuffer = BufferUtils.createFloatBuffer(vertices.size)
        verticesBuffer.put(vertices)
        verticesBuffer.flip()

        // Create a new Vertex Array Object in memory and select it (bind)
        vaoId = GL30.glGenVertexArrays()
        GL30.glBindVertexArray(vaoId)

        // Create a new Vertex Buffer Object in memory and select it (bind) - VERTICES
        vboId = GL15.glGenBuffers()
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW)
        GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, 0, 0)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

        val uv = floatArrayOf(
            0f, 1f,
            0f, 0f,
            1f, 0f,
            1f, 1f
        )

        val uvBuffer = BufferUtils.createFloatBuffer(uv.size)
        uvBuffer.put(uv)
        uvBuffer.flip()

        uvGlBuffer = GL15.glGenBuffers()
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, uvGlBuffer)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, uvBuffer, GL15.GL_STATIC_DRAW)
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

        // Deselect (bind to 0) the VAO
        GL30.glBindVertexArray(0)
    }

    private fun setupShaders() {
        // Load the vertex shader
        vsId = loadShader(VERTEX_SHADER_SRC, GL20.GL_VERTEX_SHADER)
        // Load the fragment shader
        fsId = loadShader(FRAGMENT_SHADER_SRC, GL20.GL_FRAGMENT_SHADER)

        // Create a new shader program that links both shaders
        pId = GL20.glCreateProgram()
        GL20.glAttachShader(pId, vsId)
        GL20.glAttachShader(pId, fsId)

        GL20.glBindAttribLocation(pId, 0, "in_Position")
        GL20.glBindAttribLocation(pId, 1, "in_Uv")

        GL20.glLinkProgram(pId)
        GL20.glValidateProgram(pId)
        val errorCheckValue = GL11.glGetError()

        if (errorCheckValue != GL11.GL_NO_ERROR) {
            println("ERROR - Could not create the shaders: $errorCheckValue")
            exitProcess(-1)
        }

        textureUniform = GL20.glGetUniformLocation(pId, "image")
        brightAdjustUniform = GL20.glGetUniformLocation(pId, "brightAdjust")
        exposureAdjustUniform = GL20.glGetUniformLocation(pId, "exposureAdjust")
        midAdjustUniform = GL20.glGetUniformLocation(pId, "midAdjust")

        GL20.glUseProgram(0)
    }

    private fun setupUniforms() {
        GL20.glUseProgram(pId)
        GL20.glUniform4f(brightAdjustUniform, 0.0f, 0.0f, 0.0f, 0.0f)
        GL20.glUniform4f(exposureAdjustUniform, 1.3f, 1.0f, 1.0f, 1.0f)
        GL20.glUniform4f(midAdjustUniform, 0.5f, 1.0f, 1.0f, 1.0f)
        GL20.glUseProgram(0)
    }

    private fun loadShader(shaderSource: String, type: Int): Int {
        val shaderID = GL20.glCreateShader(type)
        GL20.glShaderSource(shaderID, shaderSource)
        GL20.glCompileShader(shaderID)
        return shaderID
    }

    fun loopCycle() {
        GL20.glUseProgram(pId)

        GL20.glUniform1i(textureUniform, 0)

        GL20.glActiveTexture(GL20.GL_TEXTURE0)
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, texId)

        // Bind to the VAO that has all the information about the vertices
        GL30.glBindVertexArray(vaoId)
        GL20.glEnableVertexAttribArray(0)
        GL20.glEnableVertexAttribArray(1)

        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4)

        GL20.glActiveTexture(0)
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
        GL20.glDisableVertexAttribArray(0)
        GL20.glDisableVertexAttribArray(1)
        GL30.glBindVertexArray(0)
        GL20.glUseProgram(0)
    }


    fun shutdown() {
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
        GL15.glDeleteBuffers(uvGlBuffer)

        // Delete the VAO
        GL30.glBindVertexArray(0)
        GL30.glDeleteVertexArrays(vaoId)
    }
}