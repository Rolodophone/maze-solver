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
			State.DRAW_MAZE -> "Draw the maze. Press R to randomise. Press ENTER when done."
			State.SELECT_START -> "Select a start location. Press ENTER to keep."
			State.SELECT_END -> "Select an end location. Press ENTER to keep."
			State.SELECT_TYPE -> "Press: D for DFS; B for BFS; S for SPF."
			State.DFS -> "Running DFS. Press ENTER to pause or press ESC to stop."
			State.BFS -> "Running BFS. Press ENTER to pause or press ESC to stop."
			State.SPF -> "Running SPF. Press ENTER to pause or press ESC to stop."
			State.PAUSED -> "Paused. Press ENTER to continue or press ESC to stop."
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