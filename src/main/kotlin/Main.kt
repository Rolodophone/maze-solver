import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
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
 * How many seconds to pause for when finding a correct path (0 means as fast as possible; -1 means pause until enter
 * is pressed)
 */
const val PAUSE_SOLUTION: Double = -1.0

/**
 * How many seconds to pause for on each path that is neither correct nor a dead end (0 means as fast as possible; -1
 * means until enter is pressed)
 */
const val PAUSE_SEARCHING: Double = 0.0

/**
 * How many seconds to pause for when finding a dead end (0 means as fast as possible; -1 means until enter is pressed)
 */
const val PAUSE_DEAD_END: Double = 1.0

/**
 * The width of the walls of the maze
 */
const val WALL_WIDTH: Double = 12.0

/**
 * The radius of the circle representing the start position of the maze
 */
const val TERMINAL_RADIUS = 25.0


const val HALF_WALL_WIDTH = WALL_WIDTH / 2


lateinit var pg: Program
var state = State.DRAW_MAZE
val squares = initialiseSquares()
var startX: Int? = null
var startY: Int? = null
var endX: Int? = null
var endY: Int? = null


enum class State {
	DRAW_MAZE, SELECT_START, SELECT_END, RUNNING, PAUSED
}


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
			drawer.fontMap = loadFont("data/fonts/default.otf", 16.0)

			extend {
				//never use stroke
				drawer.stroke = null

				//draw background
				drawer.clear(ColorRGBa.WHITE)

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

				//draw start and end
				drawer.fill = ColorRGBa.BLUE

				val startX = startX
				val startY = startY
				if (startX != null && startY != null) {
					drawer.circle(getCenterOfSquareAtIndex(startX, startY), TERMINAL_RADIUS)
				}

				val endX = endX
				val endY = endY
				if (endX != null && endY != null) {
					val pos = getCenterOfSquareAtIndex(endX, endY) - TERMINAL_RADIUS
					drawer.rectangle(pos, TERMINAL_RADIUS*2, TERMINAL_RADIUS*2)
				}

				drawPreviews()

				Info.draw()
			}

			mouse.buttonDown.listen { onMouseDown(it) }

			keyboard.keyDown.listen { e ->
				if (e.key == KEY_ENTER) {
					//move to next state if waiting for enter to be pressed
					when (state) {
						State.DRAW_MAZE -> state = State.SELECT_START
						State.RUNNING -> state = State.PAUSED
						State.PAUSED -> state = State.RUNNING
						else -> {}
					}
				}
				else if (e.key == KEY_ESCAPE) {
					if (state == State.RUNNING || state == State.PAUSED) {
						state = State.DRAW_MAZE
					}
				}
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
fun findNearestWall(): Vector2? {
	// the mouse position in terms of the grid, i.e. if each square had width and height 1
	val mouseXInGrid = pg.mouse.position.x * (NUM_COLUMNS / pg.width.toDouble())
	val mouseYInGrid = pg.mouse.position.y * (NUM_ROWS / pg.height.toDouble())

	// the mouse position within the grid
	val mouseXInSquare = mouseXInGrid % 1
	val mouseYInSquare = mouseYInGrid % 1

	// the index of the square that the mouse resides in
	val hoveredSquareX = floor(mouseXInGrid)
	val hoveredSquareY = floor(mouseYInGrid)

	val nearestWall =  if (mouseYInSquare > mouseXInSquare) {
		if (mouseYInSquare > 1 - mouseXInSquare) Vector2(hoveredSquareX, hoveredSquareY + 0.5)
		else Vector2(hoveredSquareX - 0.5, hoveredSquareY)
	}
	else {
		if (mouseYInSquare > 1 - mouseXInSquare) Vector2(hoveredSquareX + 0.5, hoveredSquareY)
		else Vector2(hoveredSquareX, hoveredSquareY - 0.5)
	}

	return if (nearestWall.x == -0.5 ||
		nearestWall.x == NUM_COLUMNS - 0.5 ||
		nearestWall.y == -0.5 ||
		nearestWall.y == NUM_ROWS - 0.5) null

	else nearestWall
}

fun drawPreviews() {
	when (state) {
		State.DRAW_MAZE -> {
			//draw preview of wall at mouse position
			val nearestWall = findNearestWall()

			if (nearestWall != null) {
				val wall: Rectangle

				if (nearestWall.x % 1 == 0.5) { //vertical
					val screenPos = getScreenPosAtIndex(nearestWall.x + 0.5, nearestWall.y)
					wall = Rectangle(
						screenPos.x - HALF_WALL_WIDTH,
						screenPos.y + HALF_WALL_WIDTH,
						WALL_WIDTH,
						pg.height / NUM_ROWS.toDouble() - WALL_WIDTH
					)
				}
				else { //horizontal
					val screenPos = getScreenPosAtIndex(nearestWall.x, nearestWall.y + 0.5)
					wall = Rectangle(
						screenPos.x + HALF_WALL_WIDTH,
						screenPos.y - HALF_WALL_WIDTH,
						pg.width / NUM_COLUMNS.toDouble() - WALL_WIDTH,
						WALL_WIDTH
					)
				}

				pg.drawer.fill = ColorRGBa(0.5, 0.5, 0.5, 0.5)
				pg.drawer.rectangle(wall)
			}
		}

		State.SELECT_START -> {
			pg.drawer.fill = ColorRGBa(0.0, 0.0, 1.0, 0.5)
			pg.drawer.circle(getCenterOfSquareAtScreenPos(pg.mouse.position), TERMINAL_RADIUS)
		}

		State.SELECT_END -> {
			val pos = getCenterOfSquareAtScreenPos(pg.mouse.position)
			pg.drawer.fill = ColorRGBa(0.0, 0.0, 1.0, 0.5)
			pg.drawer.rectangle(
				pos.x - TERMINAL_RADIUS, pos.y - TERMINAL_RADIUS, TERMINAL_RADIUS * 2, TERMINAL_RADIUS * 2
			)
		}
	}
}

fun onMouseDown(event: MouseEvent) {
	when (state) {
		State.DRAW_MAZE -> {
			//add/remove walls
			val nearestWall = findNearestWall()

			if (nearestWall != null) {
				if (nearestWall.x % 1 == 0.5) { //vertical
					val left = squares[nearestWall.y.toInt()][nearestWall.x.toInt()]
					val right = squares[nearestWall.y.toInt()][nearestWall.x.toInt() + 1]

					if (left.right == null) {
						left.right = right
						right.left = left
					}
					else {
						left.right = null
						right.left = null
					}
				}

				else { //horizontal
					val up = squares[nearestWall.y.toInt()][nearestWall.x.toInt()]
					val down = squares[nearestWall.y.toInt() + 1][nearestWall.x.toInt()]

					if (up.down == null) {
						up.down = down
						down.up = up
					}
					else {
						up.down = null
						down.up = null
					}
				}
			}
		}
		
		State.SELECT_START -> {
			val startPos = getIndexAtScreenPos(pg.mouse.position)
			startX = startPos.x
			startY = startPos.y

			state = State.SELECT_END
		}

		State.SELECT_END -> {
			val endPos = getIndexAtScreenPos(pg.mouse.position)
			endX = endPos.x
			endY = endPos.y

			state = State.RUNNING
		}
	}
}