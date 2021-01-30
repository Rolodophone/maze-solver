import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.math.IntVector2
import org.openrndr.shape.Rectangle


/**
 * Number of columns in the maze (width)
 */
const val NUM_COLUMNS: Int = 6

/**
 * Number of rows in the maze (height)
 */
const val NUM_ROWS: Int = 4

/**
 * How many milliseconds to pause for when finding a correct path (0 means as fast as possible; -1 means pause until enter
 * is pressed)
 */
const val PAUSE_SOLUTION: Long = -1

/**
 * How many milliseconds to pause for on each path that is neither correct nor a dead end (0 means as fast as possible; -1
 * means until enter is pressed)
 */
const val PAUSE_SEARCHING: Long = 0

/**
 * How many milliseconds to pause for when finding a dead end (0 means as fast as possible; -1 means until enter is pressed)
 */
const val PAUSE_DEAD_END: Long = 1000

/**
 * The width of the walls of the maze
 */
const val WALL_WIDTH: Double = 12.0

/**
 * The radius of the circle representing the start position of the maze
 */
const val TERMINAL_RADIUS = 25.0

/**
 * The width of the line representing the current path
 */
const val PATH_WIDTH = 10.0


const val HALF_WALL_WIDTH = WALL_WIDTH / 2


lateinit var pg: Program

var state = State.DRAW_MAZE
val squares = initialiseSquares()
var startX: Int? = null
var startY: Int? = null
var endX: Int? = null
var endY: Int? = null

var solvingJob: Job? = null


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

				drawStuff()

				Info.draw()
			}

			mouse.buttonDown.listen { onMouseDown(it) }

			keyboard.keyDown.listen { e ->
				if (e.key == KEY_ENTER) {
					when (state) {
						State.DRAW_MAZE -> state = State.SELECT_START
						State.RUNNING -> pauseSolving()
						State.PAUSED -> resumeSolving()
						else -> {}
					}
				}
				else if (e.key == KEY_ESCAPE) {
					if (state == State.RUNNING || state == State.PAUSED) {
						stopSolving()
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

fun drawStuff() {
	when (state) {
		//draw preview of wall at mouse position
		State.DRAW_MAZE -> {
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
		
		//draw start pos preview
		State.SELECT_START -> {
			pg.drawer.fill = ColorRGBa(0.0, 0.0, 1.0, 0.5)
			pg.drawer.circle(getCenterOfSquareAtScreenPos(pg.mouse.position), TERMINAL_RADIUS)
		}

		//draw end pos preview
		State.SELECT_END -> {
			val pos = getCenterOfSquareAtScreenPos(pg.mouse.position)
			pg.drawer.fill = ColorRGBa(0.0, 0.0, 1.0, 0.5)
			pg.drawer.rectangle(
				pos.x - TERMINAL_RADIUS, pos.y - TERMINAL_RADIUS, TERMINAL_RADIUS * 2, TERMINAL_RADIUS * 2
			)
		}
		
		// draw path
		State.PAUSED, State.RUNNING -> {
			pg.drawer.stroke = when (currentPathType!!) {
				PathType.DEAD_END -> ColorRGBa.RED
				PathType.JOURNEY -> ColorRGBa.YELLOW
				PathType.SOLUTION -> ColorRGBa.GREEN
			}
			pg.drawer.strokeWeight = PATH_WIDTH
			pg.drawer.lineStrip(currentPath.map { getCenterOfSquareAtIndex(it.x, it.y) })
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
			val startPos = getIndexAtScreenPos(event.position)
			startX = startPos.x
			startY = startPos.y

			state = State.SELECT_END
		}

		State.SELECT_END -> {
			val endPos = getIndexAtScreenPos(event.position)
			endX = endPos.x
			endY = endPos.y

			startSolving()
		}

		else -> {}
	}
}

fun startSolving() {
	solvingJob = GlobalScope.launch {
		solveMaze(squares, IntVector2(startX!!, startY!!), IntVector2(endX!!, endY!!))
	}

	state = State.RUNNING
}

fun stopSolving() {
	solvingJob?.cancel()
	state = State.DRAW_MAZE
}

fun pauseSolving() {
	//TODO
	state = State.PAUSED
}

fun resumeSolving() {
	//TODO
	state = State.RUNNING
}