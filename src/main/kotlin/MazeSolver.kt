import kotlinx.coroutines.delay
import org.openrndr.math.IntVector2

var discoveredSquares = mutableListOf<Square>()
var currentPath = mutableListOf<Square>()
var currentPathType: PathType? = null

enum class PathType {
	DEAD_END, SOLUTION, JOURNEY
}

/**
 * Solves a maze (a 2D list of [Square]s).
 *
 * This function runs in another coroutine so it is non-blocking. While it runs, it updates the [currentPath] variable
 * with the path that it is currently checking, and the [currentPathType] with the type of path it is (dead-end,
 * solution, or neither)
 *
 * @return A list of the solutions to the maze. Each solution is a list of adjacent squares in the order that they
 * should be traversed to get from the start to the finish.
 */
suspend fun solveMaze(maze: List<List<Square>>, startPos: IntVector2, endPos: IntVector2): List<List<Square>> {
	discoveredSquares = mutableListOf()
	currentPath = mutableListOf()
	currentPathType = null
	return maze[startPos.y][startPos.x].visit(maze, maze[endPos.y][endPos.x])
}

suspend fun Square.visit(maze: List<List<Square>>, endSquare: Square): List<List<Square>> {
	currentPath.add(this)
	discoveredSquares.add(this)

	if (this == endSquare) {
		currentPathType = PathType.SOLUTION
		delay(PAUSE_SOLUTION)
		return listOf(currentPath)
	}

	delay(PAUSE_SEARCHING)

	val solutions = mutableListOf<List<Square>>()
	var deadEnd = true

	for (adjacentSquare in this) {
		if (adjacentSquare !in discoveredSquares) {
			solutions.addAll(adjacentSquare.visit(maze, endSquare))
			deadEnd = false
		}
	}

	if (deadEnd) {
		currentPathType = PathType.DEAD_END
		delay(PAUSE_DEAD_END)
		currentPathType = PathType.JOURNEY
	}
	else {
		delay(PAUSE_SEARCHING)
	}

	currentPath.removeLast()
	return solutions
}