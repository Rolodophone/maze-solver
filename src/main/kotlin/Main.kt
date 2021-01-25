import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.shape.LineSegment


/**
 * Number of columns in the maze (width)
 */
const val NUM_COLUMNS: Int = 5

/**
 * Number of rows in the maze (height)
 */
const val NUM_ROWS: Int = 5

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
                val lines = mutableListOf<LineSegment>()
                val squareWidth = width / NUM_COLUMNS
                val squareHeight = height / NUM_ROWS

                for (i in 1 until NUM_COLUMNS) {
                    val x = squareWidth * i
                    lines.add(LineSegment(x, 0, x, height))
                }

                for (i in 1 until NUM_ROWS) {
                    val y = squareHeight * i
                    lines.add(LineSegment(0, y, width, y))
                }

                //draw lines
                drawer.stroke = ColorRGBa.GRAY
                drawer.lineSegments(lines)
            }
        }
    }
}