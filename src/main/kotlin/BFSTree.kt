import org.openrndr.shape.LineSegment

class BFSTree(val x: Int, val y: Int, val children: MutableList<BFSTree>) {

	fun calcLines(): List<LineSegment> {

		val lines = mutableListOf<LineSegment>()

		for (child in children) {
			lines.add(LineSegment(getCenterOfSquareAtIndex(x, y), getCenterOfSquareAtIndex(child.x, child.y)))
			lines.addAll(child.calcLines())
		}

		return lines
	}
}