package dev.juhouse.projector.forms.controllers.projection

import dev.juhouse.projector.projection2.ProjectionManager
import dev.juhouse.projector.projection2.image.ProjectionSolidColor
import javafx.beans.value.ChangeListener
import javafx.fxml.FXML
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.layout.Pane
import java.awt.Color
import java.net.URL
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class SolidColorController : ProjectionController(), ProjectionBarControlCallbacks {

    @FXML
    private lateinit var redTextField: TextField
    @FXML
    private lateinit var greenTextField: TextField
    @FXML
    private lateinit var blueTextField: TextField
    @FXML
    private lateinit var hueTextField: TextField
    @FXML
    private lateinit var satTextField: TextField
    @FXML
    private lateinit var lumaTextField: TextField

    @FXML
    private lateinit var redSlider: Slider
    @FXML
    private lateinit var greenSlider: Slider
    @FXML
    private lateinit var blueSlider: Slider
    @FXML
    private lateinit var hueSlider: Slider
    @FXML
    private lateinit var satSlider: Slider
    @FXML
    private lateinit var lumaSlider: Slider

    @FXML
    private lateinit var projectionControlPane: Pane

    private val controlBar = ProjectionBarControl()

    private lateinit var projectable: ProjectionSolidColor

    private val rgbTextChangeListener: ChangeListener<String> = ChangeListener { observableValue, t, t2 ->
        val r = redTextField.text.toDouble()
        val g = greenTextField.text.toDouble()
        val b = blueTextField.text.toDouble()
        setRGB(doubleArrayOf(r, g, b))
    }

    private val rgbSliderChangeListener: ChangeListener<Number> = ChangeListener { observableValue, t, t2 ->
        val r = redSlider.value
        val g = greenSlider.value
        val b = blueSlider.value
        setRGB(doubleArrayOf(r, g, b))
    }

    private val hslTextChangeListener: ChangeListener<String> = ChangeListener { observableValue, t, t2 ->
        val h = hueTextField.text.toDouble()
        val s = satTextField.text.toDouble()
        val l = lumaTextField.text.toDouble()
        setHSL(doubleArrayOf(h, s, l))
    }

    private val hslSliderChangeListener: ChangeListener<Number> = ChangeListener { observableValue, t, t2 ->
        val h = hueSlider.value
        val s = satSlider.value
        val l = lumaSlider.value
        setHSL(doubleArrayOf(h, s, l))
    }
    override fun initialize(p0: URL?, p1: ResourceBundle?) {

    }
    override fun initWithProjectionManager(projectionManager: ProjectionManager) {
        super.initWithProjectionManager(projectionManager)

        projectable = projectionManager.createSolidColor()

        controlBar.projectable = projectable
        controlBar.callback = this
        controlBar.manager = projectionManager
        controlBar.attach(projectionControlPane)

        redTextField.textProperty().addListener(rgbTextChangeListener)
        greenTextField.textProperty().addListener(rgbTextChangeListener)
        blueTextField.textProperty().addListener(rgbTextChangeListener)

        redSlider.valueProperty().addListener(rgbSliderChangeListener)
        greenSlider.valueProperty().addListener(rgbSliderChangeListener)
        blueSlider.valueProperty().addListener(rgbSliderChangeListener)

        hueTextField.textProperty().addListener(hslTextChangeListener)
        satTextField.textProperty().addListener(hslTextChangeListener)
        lumaTextField.textProperty().addListener(hslTextChangeListener)

        hueSlider.valueProperty().addListener(hslSliderChangeListener)
        satSlider.valueProperty().addListener(hslSliderChangeListener)
        lumaSlider.valueProperty().addListener(hslSliderChangeListener)
    }

    private fun setRGB(rgb: DoubleArray) {
        val hsl = RGBtoHSL(rgb)

        redTextField.text = rgb[0].toString()
        redSlider.value = rgb[0]

        greenTextField.text = rgb[1].toString()
        greenSlider.value = rgb[1]

        blueTextField.text = rgb[2].toString()
        blueSlider.value = rgb[2]

        hueTextField.text = hsl[0].toString()
        hueSlider.value = hsl[0]

        satTextField.text = hsl[1].toString()
        satSlider.value = hsl[1]

        lumaTextField.text = hsl[2].toString()
        lumaSlider.value = hsl[2]

        projectable.setColor(rgb)
    }

    private fun setHSL(hsl: DoubleArray) {
        val rgb = HSLtoRGB(hsl)

        redTextField.text = rgb[0].toString()
        redSlider.value = rgb[0]

        greenTextField.text = rgb[1].toString()
        greenSlider.value = rgb[1]

        blueTextField.text = rgb[2].toString()
        blueSlider.value = rgb[2]

        hueTextField.text = hsl[0].toString()
        hueSlider.value = hsl[0]

        satTextField.text = hsl[1].toString()
        satSlider.value = hsl[1]

        lumaTextField.text = hsl[2].toString()
        lumaSlider.value = hsl[2]

        projectable.setColor(rgb)
    }

    private fun clamp(v: Double): Double {
        return if (v < 0.0) {
            0.0
        } else if (v > 1.0) {
            1.0
        } else v
    }

    private fun RGBtoHSL(rgb: DoubleArray): DoubleArray {
        val r = rgb[0]
	    val g = rgb[1]
	    val b = rgb[2]

	    val cMin = r.coerceAtMost(g.coerceAtMost(b))
	    val cMax = r.coerceAtLeast(g.coerceAtLeast(b))

        var h = 0.0
	    var s = 0.0
	    val l = (cMax + cMin) / 2.0

	    if (cMax > cMin) {
		    val cDelta = cMax - cMin

		    s = if (l > 0.0) cDelta / (1.0 - abs((2.0 * l) - 1.0)) else 0.0

            h = if (r == cMax) {
                (g - b) / cDelta
            } else if (g == cMax) {
                2.0 + (b - r) / cDelta
            } else {
                4.0 + (r - g) / cDelta
            }

            if (h < 0.0) {
                h += 6.0
            }

            h /= 6.0
	    }

	    return doubleArrayOf(h, s, l)
    }

    private fun HSLtoRGB(hsl: DoubleArray): DoubleArray {
        val rgb = DoubleArray(3)

        rgb[0] = clamp(abs((((hsl[0] * 6.0f) + 0.0f) % 6.0f) - 3.0f) - 1.0)
        rgb[1] = clamp(abs((((hsl[0] * 6.0f) + 4.0f) % 6.0f) - 3.0f) - 1.0)
        rgb[2] = clamp(abs((((hsl[0] * 6.0f) + 4.0f) % 6.0f) - 3.0f) - 1.0)

        rgb[0] = (hsl[1] + hsl[2]) * (rgb[0] - 0.5f) * (1f - Math.abs((2f * hsl[2]) - 1f))
        rgb[1] = (hsl[1] + hsl[2]) * (rgb[1] - 0.5f) * (1f - Math.abs((2f * hsl[2]) - 1f))
        rgb[2] = (hsl[1] + hsl[2]) * (rgb[2] - 0.5f) * (1f - Math.abs((2f * hsl[2]) - 1f))

        return rgb
    }

    override fun onEscapeKeyPressed() {
        onProjectionEnd()
    }

    override fun stop() {
        onProjectionEnd()
        getProjectionManager().stop(projectable)
    }

    override fun onProjectionBegin() {
        getProjectionManager().setProjectable(projectable)
    }

    override fun onProjectionEnd() {
        getProjectionManager().setProjectable(null)
    }
}