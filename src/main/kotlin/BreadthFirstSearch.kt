import org.openrndr.math.IntVector2

var currentBFSPath = BFSTree(0, 0, mutableListOf())

class SquareQueueItem(val square: Square, val prevBFSTree: BFSTree?)

suspend fun breadthFirstSearch(maze: List<List<Square>>, startPos: IntVector2, endPos: IntVector2) {
	discoveredSquares = mutableListOf()
	currentPathType = PathType.JOURNEY
	val endSquare = maze[endPos.y][endPos.x]
	val squareQueue = mutableListOf(SquareQueueItem(maze[startPos.y][startPos.x], null))

	state = State.BFS

	while (squareQueue.isNotEmpty()) {
		val currentSquare = squareQueue.first().square
		val currentBFSTree = BFSTree(currentSquare.x, currentSquare.y, mutableListOf())
		val prevBFSTree = squareQueue.first().prevBFSTree

		synchronized(solvingLock) {
			if (prevBFSTree == null) {
				currentBFSPath = currentBFSTree
			}
			else {
				prevBFSTree.children.add(currentBFSTree)
			}
		}
		discoveredSquares.add(currentSquare)

		if (currentSquare == endSquare) {
			currentPathType = PathType.SOLUTION
			delayOrPause(PAUSE_SOLUTION)
			currentPathType = PathType.JOURNEY
		}

		delayOrPause(PAUSE_SEARCHING)

		for (adjacentSquare in currentSquare) {
			if (adjacentSquare !in discoveredSquares) {
				squareQueue.add(SquareQueueItem(adjacentSquare, currentBFSTree))
			}
		}

		squareQueue.removeFirst()
	}

	state = State.DRAW_MAZE
}