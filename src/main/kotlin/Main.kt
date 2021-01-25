import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.olive.oliveProgram
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


fun main() {
    application {
        configure {
            width = 1080
            height = 720
            windowResizable = true
            title = "Maze Solver"
        }
        oliveProgram {
            val squares = initialiseSquares()

            extend {
                //draw background
                drawer.clear(ColorRGBa.WHITE)

                //calculate grid
                val points = mutableListOf<Rectangle>()

                val squareWidth = width / NUM_COLUMNS.toDouble()
                val squareHeight = height / NUM_ROWS.toDouble()

                for (xi in 0..NUM_COLUMNS) {
                    for (yi in 0..NUM_ROWS) {

                        val x = xi * squareWidth
                        val y = yi * squareHeight

                        points.add(Rectangle(
                            x - HALF_WALL_WIDTH,
                            y - HALF_WALL_WIDTH,
                            WALL_WIDTH,
                            WALL_WIDTH
                        ))
                    }
                }

                //draw points
                drawer.fill = ColorRGBa.BLACK
                drawer.rectangles(points)
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
