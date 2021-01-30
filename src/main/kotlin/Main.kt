import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.math.IntVector2
import org.openrndr.shape.Rectangle


/**
 * Number of columns in the maze (width)
 */
const val NUM_COLUMNS: Int = 32

/**
 * Number of rows in the maze (height)
 */
const val NUM_ROWS: Int = 18

/**
 * How many milliseconds to pause for when finding a correct path (0 means as fast as possible; -1 means pause until
 * enter is pressed)
 */
const val PAUSE_SOLUTION: Long = -1

/**
 * How many milliseconds to pause for on each path that is neither correct nor a dead end (0 means as fast as possible;
 * -1 means until enter is pressed).
 */
const val PAUSE_SEARCHING: Long = 10

/**
 * How many milliseconds to pause for when finding a dead end (0 means as fast as possible; -1 means until enter is
 * pressed).
 */
const val PAUSE_DEAD_END: Long = PAUSE_SEARCHING

/**
 * The colour the path changes to when a solution is found.
 */
val COLOUR_SOLUTION: ColorRGBa = ColorRGBa.GREEN

/**
 * The normal colour of the path
 */
val COLOUR_SEARCHING: ColorRGBa = ColorRGBa.RED

/**
 * The colour the path changes to when a dead end is found.
 */
val COLOUR_DEAD_END: ColorRGBa = COLOUR_SEARCHING

/**
 * The width of the walls of the maze.
 */
const val WALL_WIDTH: Double = 8.0

/**
 * The radius of the circle representing the start position of the maze..
 */
const val TERMINAL_RADIUS: Double = 12.0

/**
 * The width of the line representing the current path.
 */
const val PATH_WIDTH: Double = 8.0

/**
 * The chance that each square creates a wall next to it when the walls are randomised.
 */
const val WALL_CHANCE: Int = 4


const val HALF_WALL_WIDTH = WALL_WIDTH / 2


lateinit var pg: Program

var state = State.DRAW_MAZE
lateinit var squares: List<List<Square>>
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
			width = 1280
			height = 720
			windowResizable = true
			title = "Maze Solver"
		}
		program {
			pg = this
			drawer.fontMap = loadFont("data/fonts/default.otf", 16.0)
			initialiseSquares()

			extend {
				//disable stroke
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

				drawStuff()

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

				Info.draw()
			}

			mouse.buttonDown.listen { onMouseDown(it) }

			keyboard.keyDown.listen { e ->
				if (e.key == KEY_ENTER) {
					when (state) {
						State.DRAW_MAZE -> state = State.SELECT_START
						State.RUNNING -> pauseSolving()
						State.PAUSED -> resumeSolving()
						State.SELECT_START -> {
							if (startX != null && startY != null) {
								state = State.SELECT_END
							}
						}
						State.SELECT_END -> {
							if (endX != null && endY != null) {
								startSolving()
							}
						}
					}
				}
				else if (e.key == KEY_ESCAPE) {
					if (state == State.RUNNING || state == State.PAUSED) {
						stopSolving()
					}
				}
				else if (e.name == "r") {
					if (state == State.DRAW_MAZE) {
						randomiseSquares()
					}
				}
			}
		}
	}
}

fun initialiseSquares() {
	squares = MutableList(NUM_ROWS) { y ->
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
}

fun randomiseSquares() {
	//remove all walls
	initialiseSquares()

	//add walls randomly
	for (row in squares) {
		for (square in row) {
			square.left.let {
				if (it != null && randomChance(WALL_CHANCE)) {
					it.right = null
					square.left = null
				}
			}
			square.up.let {
				if (it != null && randomChance(WALL_CHANCE)) {
					it.down = null
					square.up = null
				}
			}
			square.right.let {
				if (it != null && randomChance(WALL_CHANCE)) {
					it.left = null
					square.right = null
				}
			}
			square.down.let {
				if (it != null && randomChance(WALL_CHANCE)) {
					it.up = null
					square.down = null
				}
			}
		}
	}
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
			//prevent concurrent modification
			val currentPath = currentPath
			val currentPathType = currentPathType

			pg.drawer.stroke = when (currentPathType!!) {
				PathType.DEAD_END -> COLOUR_DEAD_END
				PathType.JOURNEY -> COLOUR_SEARCHING
				PathType.SOLUTION -> COLOUR_SOLUTION
			}
			pg.drawer.strokeWeight = PATH_WIDTH

			pg.drawer.lineStrip(currentPath.map { getCenterOfSquareAtIndex(it.x, it.y) })

			pg.drawer.stroke = null
			pg.drawer.strokeWeight = 0.0

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
}

fun stopSolving() {
	solvingJob?.cancel()
	state = State.DRAW_MAZE
}

fun pauseSolving() {
	state = State.PAUSED
}

fun resumeSolving() {
	pauseChannel.offer(Unit)
	state = State.RUNNING
}