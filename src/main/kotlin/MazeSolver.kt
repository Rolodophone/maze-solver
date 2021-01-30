import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import org.openrndr.math.IntVector2

var discoveredSquares = mutableListOf<Square>()
var currentPath = mutableListOf<Square>()
var currentPathType: PathType? = null

enum class PathType {
	DEAD_END, SOLUTION, JOURNEY
}

val pauseChannel = Channel<Unit>(0)

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
	currentPathType = PathType.JOURNEY
	state = State.RUNNING
	val solutions = maze[startPos.y][startPos.x].visit(maze, maze[endPos.y][endPos.x])
	state = State.DRAW_MAZE
	return solutions
}

suspend fun Square.visit(maze: List<List<Square>>, endSquare: Square): List<List<Square>> {
	currentPath.add(this)
	discoveredSquares.add(this)

	if (this == endSquare) {
		currentPathType = PathType.SOLUTION
		delayOrPause(PAUSE_SOLUTION)
		currentPathType = PathType.JOURNEY
		currentPath.removeLast()
		return listOf(currentPath)
	}

	delayOrPause(PAUSE_SEARCHING)

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
		delayOrPause(PAUSE_DEAD_END)
		currentPathType = PathType.JOURNEY
	}
	else {
		delayOrPause(PAUSE_SEARCHING)
	}

	currentPath.removeLast()
	return solutions
}

suspend fun delayOrPause(timeMillis: Long) {
	//pause automatically
	if (timeMillis == -1L) {
		state = State.PAUSED
	}
	else {
		delay(timeMillis)
	}

	//pause either automatically or manually
	if (state == State.PAUSED) {
		pauseChannel.receive()
	}
}