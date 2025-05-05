package cit.edu.KlockApp.ui.util

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * A simple ItemTouchHelper.Callback for handling drag & drop.
 *
 * @param listener The listener to notify when an item is moved.
 */
class SimpleItemTouchHelperCallback(
    private val listener: OnItemMoveListener
) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean {
        return false // Drag will be started manually
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false // Disable swipe gestures for now
    }

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // Prevent dragging the last item if it's a different view type (e.g., the Add button)
        val adapter = recyclerView.adapter
        if (adapter != null && viewHolder.bindingAdapterPosition == adapter.itemCount - 1) {
             // Assuming the last item is non-draggable (like an 'Add' button)
             // Check based on position might be fragile. A better check might involve itemViewType
             // if the adapter exposes it or if ViewHolder has a specific property.
             // For now, simply disabling drag for the last item.
            return makeMovementFlags(0, 0) // No drag, no swipe
        }
        
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = 0 // No swipe
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        // Prevent dropping onto the last item position if it's the Add button
        val adapter = recyclerView.adapter
        if (adapter != null && target.bindingAdapterPosition == adapter.itemCount - 1) {
             // Prevent dropping onto the last position (assumed Add button)
             return false
        }
        
        // Notify the listener about the move
        listener.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true // Indicate that the move was handled
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Not used since isItemViewSwipeEnabled is false
    }

    // Optional: Highlight the item being dragged
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        // Prevent visual change for the last item (Add button)
        val adapter = viewHolder?.bindingAdapter // Get adapter from ViewHolder
        val position = viewHolder?.bindingAdapterPosition // Get position from ViewHolder
        
        if (adapter != null && position != null && position != RecyclerView.NO_POSITION && position == adapter.itemCount - 1) {
             // Don't apply visual effect to the last item
             return
        }
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            viewHolder?.itemView?.alpha = 0.7f
        }
    }

    // Optional: Restore item appearance when drag is finished
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        // Prevent visual change for the last item (Add button)
        val adapter = recyclerView.adapter
        if (adapter != null && viewHolder.bindingAdapterPosition == adapter.itemCount - 1) {
             // Don't restore visual effect for the last item
             return
        }
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.alpha = 1.0f // Restore full opacity
    }
} 