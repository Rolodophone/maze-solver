import org.openrndr.color.ColorRGBa

object Info {
	var text = "Draw the maze. Press enter when done."

	fun draw() {
		text = when (state) {
			State.DRAW_MAZE -> "Draw the maze. Press enter when done."
			State.SELECT_START -> "Select a start location."
			State.SELECT_END -> "Select an end location."
			State.RUNNING -> "Running. Press enter to pause or press escape to stop."
			State.PAUSED -> "Paused. Press enter to continue or press escape to stop."
		}

		//draw a white background
		if (text != "") {
			pg.drawer.fill = ColorRGBa.WHITE
			pg.drawer.rectangle(10.0, 6.0, 300.0, 20.0)
		}

		pg.drawer.fill = ColorRGBa.BLACK
		pg.drawer.text(text, 10.0, 20.0)
	}
}