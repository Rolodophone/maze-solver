import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2

fun floor(num: Double, precision: Double): Double {
	return kotlin.math.floor(num / precision) * precision
}

fun getScreenPosAtIndex(x: Double, y: Double): Vector2 {
	return Vector2(
		x * (pg.width / NUM_COLUMNS.toDouble()),
		y * (pg.height / NUM_ROWS.toDouble())
	)
}
fun getScreenPosAtIndex(x: Int, y: Int) = getScreenPosAtIndex(x.toDouble(), y.toDouble())
fun getScreenPosAtIndex(index: IntVector2) = getScreenPosAtIndex(index.x, index.y)
fun getScreenPosAtIndex(index: Vector2) = getScreenPosAtIndex(index.x, index.y)

fun getIndexAtScreenPos(x: Double, y: Double): IntVector2 {
	return IntVector2(
		(x * (NUM_COLUMNS / pg.width.toDouble())).toInt(),
		(y * (NUM_ROWS / pg.height.toDouble())).toInt()
	)
}
fun getIndexAtScreenPos(pos: Vector2) = getIndexAtScreenPos(pos.x, pos.y)

fun getCenterOfSquareAtIndex(x: Int, y: Int): Vector2 {
	return getScreenPosAtIndex(x + 0.5, y + 0.5)
}
fun getCenterOfSquareAtIndex(index: IntVector2) = getCenterOfSquareAtIndex(index.x, index.y)

fun getCenterOfSquareAtScreenPos(x: Double, y: Double): Vector2 {
	return getCenterOfSquareAtIndex(getIndexAtScreenPos(x, y))
}
fun getCenterOfSquareAtScreenPos(pos: Vector2) = getCenterOfSquareAtScreenPos(pos.x, pos.y)