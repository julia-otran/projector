package dev.juhouse.projector.projection.glfw

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import dev.juhouse.projector.models.WindowConfig
import dev.juhouse.projector.projection.models.VirtualScreen
import java.awt.Rectangle
import kotlin.system.exitProcess

class GLFWColorCorrection(private val bounds: Rectangle) {
    companion object {
        const val VERTEX_SHADER_SRC = """
            attribute vec4 in_Position;
            attribute vec2 in_Uv;
            
            varying vec2 frag_Uv;
            
            void main(void) {
                gl_Position = in_Position;
                frag_Uv = in_Uv;
            }
        """

        const val FRAGMENT_SHADER_SRC = """
            varying vec2 frag_Uv;
            uniform sampler2D image;
            
            uniform vec4 brightAdjust;
            uniform vec4 exposureAdjust;
 
            uniform vec4 lowAdjust;
            uniform vec4 midAdjust;
            uniform vec4 highAdjust;
            
            uniform float preserveLuminosity;
            
            vec4 rgbToHsl(vec4 rgbColor) {
                float minVal = min(rgbColor.r, min(rgbColor.g, rgbColor.b));
                float maxVal = max(rgbColor.r, max(rgbColor.g, rgbColor.b));
                
                float lum = (minVal + maxVal) / 2.0;
                float sat = 0.0;
                float hue = 0.0;
                
                float delta = maxVal - minVal; 
                
                if (lum > 0.5) {
                    sat = delta / (2.0 - delta);
                } else {
                    sat = delta / (maxVal + minVal);
                }
                
                if (delta == 0.0) {
                    delta = 1.0;
                }
                    
                if (rgbColor.r == maxVal) {
                    hue = (rgbColor.g - rgbColor.b) / delta; 
                }
                
                if (rgbColor.g == maxVal) {
                    hue = 2.0 + ((rgbColor.b - rgbColor.r) / delta);
                }
                
                if (rgbColor.b == maxVal) {
                    hue = 4.0 + ((rgbColor.r - rgbColor.g) / delta);
                }          
                
                hue = hue / 6.0;
                
                if (hue < 0.0) {
                    hue = 1.0 + hue;
                }
                
                return(vec4(hue, sat, lum, rgbColor.a));
            }
            
            float convertColorPart(float n1, float n2, float hue) {
                if (hue > 6.0) {
                    hue = hue - 6.0;
                }
                if (hue < 0.0) {
                    hue = hue + 6.0;
                }
                if (hue < 1.0) {
                    return(n1 + (n2 - n1) * hue);
                }
                if (hue < 3.0) {
                    return(n2);
                }
                if (hue < 4.0) {
                    return(n1 + (n2 - n1) * (4.0 - hue));
                }
                
                return(n1);
            }
            
            vec4 hslToRgb(vec4 hsl) {
                float r = 0.0;
                float g = 0.0;
                float b = 0.0;
                
                float x = 0.0;
                float y = 0.0;
                
                if (hsl.g > 0.0) {
                    if (hsl.b <= 0.5) {
                        y = hsl.b * (1.0 + hsl.g);
                    } else {
                        y = hsl.g + hsl.b - (hsl.g * hsl.b);
                    }
                    
                    x = (2.0 * hsl.b) - y;

                    r = convertColorPart(x, y, hsl.r * 6.0 + 2.0);
                    g = convertColorPart(x, y, hsl.r * 6.0);
                    b = convertColorPart(x, y, hsl.r * 6.0 - 2.0);
                } else {
                    r = hsl.b;
                    g = hsl.b;
                    b = hsl.b;
                }
                
                return(vec4(r, g, b, hsl.a));
            }
            
            void main(void) {
                vec4 texel = texture2D(image, frag_Uv);                
                vec4 hsl = rgbToHsl(texel);
                
                float lum = hsl.b;
                
                float a = 0.25;
                float scale = 0.7;
                float b = 1.0 - scale;
                
                float shadowsLum = lum - b;
                float highlightsLum = lum + b -1.0;
                
                float shadowsMultiply = clamp((shadowsLum / (-1.0 * a)) + 0.5, 0.0, 1.0) * scale;
                float highlightsMultiply = clamp((highlightsLum / a) + 0.5, 0.0, 1.0) * scale;
                
                float midtonesMultiply0 = clamp((shadowsLum / a) + 0.5, 0.0, 1.0);
                float midtonesMultiply1 = clamp((highlightsLum / (-1.0 * a)) + 0.5, 0.0, 1.0);
                float midtonesMultiply = midtonesMultiply0 * midtonesMultiply1 * scale;
                
                vec4 shadows = shadowsMultiply * lowAdjust;
                vec4 mids = midtonesMultiply * midAdjust;
                vec4 highs = highlightsMultiply * highAdjust;
                
                vec4 colorCorrected = texel + shadows + mids + highs;
                colorCorrected = clamp(colorCorrected, 0.0, 1.0);
                vec4 colorCorrectedHsl;
                
                if (preserveLuminosity > 0.0) {
                    colorCorrectedHsl = rgbToHsl(colorCorrected);
                    colorCorrectedHsl.b = hsl.b;
                    colorCorrected = hslToRgb(colorCorrectedHsl);
                }
                
                vec4 result = colorCorrected * exposureAdjust + brightAdjust;
                
                gl_FragColor = vec4(result.rgb, 1.0);
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
    private var lowAdjustUniform = 0
    private var midAdjustUniform = 0
    private var highAdjustUniform = 0
    private var preserveLumUniform = 0

    fun init(windowConfig: WindowConfig, virtualScreen: VirtualScreen) {
        setupQuad(windowConfig, virtualScreen)
        setupShaders()
        initUniforms()
    }

    private fun setupQuad(windowConfig: WindowConfig, virtualScreen: VirtualScreen) {
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

        val texWidth = windowConfig.width / virtualScreen.width.toFloat()
        val texHeight = windowConfig.height / virtualScreen.height.toFloat()

        val translateX = windowConfig.x / virtualScreen.width.toFloat()
        val translateY = windowConfig.y / virtualScreen.height.toFloat()

        val uv = floatArrayOf(
            translateX, translateY,
            translateX, translateY + texHeight,
            translateX + texWidth, translateY + texHeight,
            translateX + texWidth, translateY
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
        vsId = loadShader(VERTEX_SHADER_SRC, GL20.GL_VERTEX_SHADER, "Color Correction Vertex Shader")
        // Load the fragment shader
        fsId = loadShader(FRAGMENT_SHADER_SRC, GL20.GL_FRAGMENT_SHADER, "Color Correction Fragment Shader")

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

        lowAdjustUniform = GL20.glGetUniformLocation(pId, "lowAdjust")
        midAdjustUniform = GL20.glGetUniformLocation(pId, "midAdjust")
        highAdjustUniform = GL20.glGetUniformLocation(pId, "highAdjust")
        preserveLumUniform = GL20.glGetUniformLocation(pId, "preserveLuminosity")

        GL20.glUseProgram(0)
    }

    private fun initUniforms() {
        GL20.glUseProgram(pId)
        GL20.glUniform4f(brightAdjustUniform, 0.0f, 0.0f, 0.0f, 0.0f)
        GL20.glUniform4f(exposureAdjustUniform, 1.0f, 1.0f, 1.0f, 1.0f)

        GL20.glUniform4f(lowAdjustUniform, 0f, 0f, 0f, 0f)
        GL20.glUniform4f(midAdjustUniform, 0f, 0f, 0f, 0f)
        GL20.glUniform4f(highAdjustUniform, 0f, 0f, 0f, 0f)
        GL20.glUniform1f(preserveLumUniform, 1f)

        GL20.glUseProgram(0)
    }

    fun setWindowConfig(windowConfig: WindowConfig) {
        initUniforms()

        GL20.glUseProgram(pId)

        windowConfig.whiteBalance?.let { whiteBalance ->
            whiteBalance.bright?.let {
                GL20.glUniform4f(brightAdjustUniform, it.r, it.g, it.b, 0.0f)
            }

            whiteBalance.exposure?.let {
                GL20.glUniform4f(exposureAdjustUniform, it.r, it.g, it.b, 1.0f)
            }
        }

        windowConfig.colorBalance?.let { colorBalance ->
            colorBalance.shadows?.let {
                GL20.glUniform4f(lowAdjustUniform, it.r, it.g, it.b, 0f)
            }

            colorBalance.midtones?.let {
                GL20.glUniform4f(midAdjustUniform, it.r, it.g, it.b, 0f)
            }

            colorBalance.highlights?.let {
                GL20.glUniform4f(highAdjustUniform, it.r, it.g, it.b, 0f)
            }

            if (colorBalance.isPreserveLuminosity) {
                GL20.glUniform1f(preserveLumUniform, 1.0f)
            } else {
                GL20.glUniform1f(preserveLumUniform, 0f)
            }
        }

        GL20.glUseProgram(0)
    }

    private fun loadShader(shaderSource: String, type: Int, name: String): Int {
        val shaderID = GL20.glCreateShader(type)
        GL20.glShaderSource(shaderID, shaderSource)
        GL20.glCompileShader(shaderID)
        val compileStatus = GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS)
        if (compileStatus == 0) {
            print("Failed compiling shader $name")
            println(GL20.glGetShaderInfoLog(shaderID))
        }
        return shaderID
    }

    fun loopCycle(texId: Int) {
        GL20.glUseProgram(pId)

        GL20.glUniform1i(textureUniform, 0)

        GL20.glActiveTexture(GL20.GL_TEXTURE0)
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, texId)

        // Bind to the VAO that has all the information about the vertices
        GL30.glBindVertexArray(vaoId)
        GL20.glEnableVertexAttribArray(0)
        GL20.glEnableVertexAttribArray(1)

        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4)

        GL20.glBindTexture(GL20.GL_TEXTURE_2D, 0)
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
