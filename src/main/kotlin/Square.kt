class Square(val x: Int, val y: Int, var left: Square?, var up: Square?, var right: Square?, var down: Square?): Iterable<Square> {
    override fun iterator(): Iterator<Square> {
        val tempList = mutableListOf(right, down, up, left) //prefer going to to the bottom right
        tempList.removeAll { it == null }

        val list = tempList.map { it!! }

        return list.iterator()
    }
}