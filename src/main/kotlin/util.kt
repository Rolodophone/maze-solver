import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import kotlin.random.Random.Default.nextInt

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
	val hoveredSquareX = kotlin.math.floor(mouseXInGrid)
	val hoveredSquareY = kotlin.math.floor(mouseYInGrid)

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


fun randomChance(chance: Int): Boolean {
	return nextInt(chance) == 0
}