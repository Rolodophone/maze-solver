import org.openrndr.shape.LineSegment

fun LineSegment(x0: Int, y0: Int, x1: Int, y1: Int): LineSegment {
    return LineSegment(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
}