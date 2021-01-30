import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Rectangle


object Info {
	private var text = ""
	private val bounds = Rectangle(6.0, 6.0, 435.0, 20.0)

	fun draw() {
		//don't draw when mouse is hovering over
		if (pg.mouse.position in bounds) {
			return
		}

		text = when (state) {
			State.DRAW_MAZE -> "Draw the maze. Press r to randomise. Press enter when done."
			State.SELECT_START -> "Select a start location. Press enter to keep."
			State.SELECT_END -> "Select an end location. Press enter to keep."
			State.RUNNING -> "Running. Press enter to pause or press escape to stop."
			State.PAUSED -> "Paused. Press enter to continue or press escape to stop."
		}

		//draw a white background
		if (text != "") {
			pg.drawer.fill = ColorRGBa.WHITE
			pg.drawer.rectangle(bounds)
		}

		pg.drawer.fill = ColorRGBa.BLACK
		pg.drawer.text(text, 10.0, 20.0)
	}
}