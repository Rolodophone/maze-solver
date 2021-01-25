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