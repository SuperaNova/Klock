package cit.edu.KlockApp.util

/**
 * Interface to notify a listener that an item has been moved in a RecyclerView.
 */
interface OnItemMoveListener {
    /**
     * Called when an item has been dragged and dropped.
     *
     * @param fromPosition The starting position of the item.
     * @param toPosition   The ending position of the item.
     */
    fun onItemMove(fromPosition: Int, toPosition: Int)
} 