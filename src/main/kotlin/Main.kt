import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.floor


/**
 * Number of columns in the maze (width)
 */
const val NUM_COLUMNS: Int = 6

/**
 * Number of rows in the maze (height)
 */
const val NUM_ROWS: Int = 4

/**
 * How many seconds to pause for when finding a correct path (0 means as fast as possible)
 */
const val PAUSE_SOLUTION: Double = 3.0

/**
 * How many seconds to pause for on each path that is neither correct nor a dead end (0 means as fast as possible)
 */
const val PAUSE_SEARCHING: Double = 0.0

/**
 * How many seconds to pause for when finding a dead end (0 means as fast as possible)
 */
const val PAUSE_DEAD_END: Double = 1.0

/**
 * The width of the walls of the maze
 */
const val WALL_WIDTH: Double = 12.0


const val HALF_WALL_WIDTH = WALL_WIDTH / 2


lateinit var pg: Program


fun main() {
	application {
		configure {
			width = 1080
			height = 720
			windowResizable = true
			title = "Maze Solver"
		}
		oliveProgram {
			pg = this
			val squares = initialiseSquares()

			extend {
				//never use stroke
				drawer.stroke = null

				//draw background
				drawer.clear(ColorRGBa.WHITE)

				//draw preview of wall at mouse position
				val nearestWall = findNearestWall()

				val wall = if (abs(nearestWall.x % 1) == 0.5) { //vertical
					Rectangle(
						(nearestWall.x + 0.5) * (width/NUM_COLUMNS.toDouble()) - HALF_WALL_WIDTH,
						(nearestWall.y)       * (height/NUM_ROWS.toDouble())   + HALF_WALL_WIDTH,
						WALL_WIDTH,
						height/NUM_ROWS.toDouble() - WALL_WIDTH
					)
				}
				else { //horizontal
					Rectangle(
						(nearestWall.x)			* (width/NUM_COLUMNS.toDouble())	+ HALF_WALL_WIDTH,
						(nearestWall.y + 0.5)	* (height/NUM_ROWS.toDouble())		- HALF_WALL_WIDTH,
						width/NUM_COLUMNS.toDouble() - WALL_WIDTH,
						WALL_WIDTH
					)
				}

				drawer.fill = ColorRGBa.GRAY
				drawer.rectangle(wall)

				//for walls and grid
				val walls = mutableListOf<Rectangle>()

				//calculate grid
				val squareWidth = width / NUM_COLUMNS.toDouble()
				val squareHeight = height / NUM_ROWS.toDouble()

				for (xi in 0..NUM_COLUMNS) {
					for (yi in 0..NUM_ROWS) {

						val x = xi * squareWidth
						val y = yi * squareHeight

						walls.add(Rectangle(
							x - HALF_WALL_WIDTH,
							y - HALF_WALL_WIDTH,
							WALL_WIDTH,
							WALL_WIDTH
						))
					}
				}

				//calculate walls
				for (row in squares) {
					for (square in row) {
						if (square.left == null) {
							walls.add(Rectangle(
								square.x * squareWidth,
								square.y * squareHeight + HALF_WALL_WIDTH,
								HALF_WALL_WIDTH,
								squareHeight - WALL_WIDTH
							))
						}
						if (square.up == null) {
							walls.add(Rectangle(
								square.x * squareWidth + HALF_WALL_WIDTH,
								square.y * squareHeight,
								squareWidth - WALL_WIDTH,
								HALF_WALL_WIDTH
							))
						}
						if (square.right == null) {
							walls.add(Rectangle(
								square.x * squareWidth + squareWidth - HALF_WALL_WIDTH,
								square.y * squareHeight + HALF_WALL_WIDTH,
								HALF_WALL_WIDTH,
								squareHeight - WALL_WIDTH
							))
						}
						if (square.down == null) {
							walls.add(Rectangle(
								square.x * squareWidth + HALF_WALL_WIDTH,
								square.y * squareHeight + squareHeight - HALF_WALL_WIDTH,
								squareWidth - WALL_WIDTH,
								HALF_WALL_WIDTH
							))
						}
					}
				}

				//draw walls
				drawer.fill = ColorRGBa.BLACK
				drawer.rectangles(walls)
			}
		}
	}
}

fun initialiseSquares(): List<List<Square>> {
	val squares = MutableList(NUM_ROWS) { y ->
		MutableList(NUM_COLUMNS) { x ->
			Square(x, y, null, null, null, null)
		}
	}

	//make the squares point to each other
	for (row in squares) {
		for (square in row) {
			if (square.x != 0) {
				square.left = squares[square.y][square.x - 1]
			}
			if (square.y != 0) {
				square.up = squares[square.y - 1][square.x]
			}
			if (square.x != NUM_COLUMNS - 1) {
				square.right = squares[square.y][square.x + 1]
			}
			if (square.y != NUM_ROWS - 1) {
				square.down = squares[square.y + 1][square.x]
			}
		}
	}

	return squares
}

/**
 * Finds the nearest wall to the mouse position.
 *
 * @return The coordinates of the wall so that the wall between (1, 2) and (1, 3) would be (1, 2.5)
 */
fun findNearestWall(): Vector2 {
	// the mouse position in terms of the grid, i.e. if each square had width and height 1
	val mouseXInGrid = pg.mouse.position.x * (NUM_COLUMNS / pg.width.toDouble())
	val mouseYInGrid = pg.mouse.position.y * (NUM_ROWS / pg.height.toDouble())

	// the mouse position within the grid
	val mouseXInSquare = mouseXInGrid % 1
	val mouseYInSquare = mouseYInGrid % 1

	// the index of the square that the mouse resides in
	val hoveredSquareX = floor(mouseXInGrid)
	val hoveredSquareY = floor(mouseYInGrid)

	return if (mouseYInSquare > mouseXInSquare) {
		if (mouseYInSquare > 1 - mouseXInSquare) Vector2(hoveredSquareX, hoveredSquareY + 0.5)
		else Vector2(hoveredSquareX - 0.5, hoveredSquareY)
	}
	else {
		if (mouseYInSquare > 1 - mouseXInSquare) Vector2(hoveredSquareX + 0.5, hoveredSquareY)
		else Vector2(hoveredSquareX, hoveredSquareY - 0.5)
	}
}
