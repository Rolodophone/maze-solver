import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.shape.LineSegment

/**
 *  This is a template for a live program.
 *
 *  It uses oliveProgram {} instead of program {}. All code inside the
 *  oliveProgram {} can be changed while the program is running.
 */

const val NUM_COLUMNS = 5
const val NUM_ROWS = 5

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