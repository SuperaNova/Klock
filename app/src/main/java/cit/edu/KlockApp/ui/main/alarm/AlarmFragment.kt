package cit.edu.KlockApp.ui.main.alarm

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cit.edu.KlockApp.R
import cit.edu.KlockApp.databinding.FragmentAlarmBinding
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlarmFragment : Fragment() {
    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!
    private lateinit var alarmViewModel: AlarmViewModel
    private lateinit var adapter: AlarmRecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        alarmViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
        ).get(AlarmViewModel::class.java)

        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setup RecyclerView
        val recycler = view.findViewById<RecyclerView>(R.id.alarmRecycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = AlarmRecyclerAdapter(
            alarms = alarmViewModel.alarms.value?.toMutableList() ?: mutableListOf(),
            onItemClick = { alarm ->
                // When clicking an alarm, pass a copy of it to avoid reference issues
                val intent = Intent(requireContext(), AlarmEditActivity::class.java).apply {
                    putExtra("alarm", alarm.copy())  // Ensure you're passing a copy
                }
                updateAlarmLauncher.launch(intent)
            },
            onEnabledChange = { alarm ->
                // Update alarm when the enabled status is changed
                alarmViewModel.updateAlarm(alarm.copy())  // Ensure copy here too
            }
        )
        recycler.adapter = adapter

        // Swipe-to-delete
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val paint = Paint().apply {
                color = ContextCompat.getColor(requireContext(), R.color.red)
            }
            private val icon = ContextCompat.getDrawable(requireContext(), R.drawable.delete_24px)
            private val iconW = icon?.intrinsicWidth ?: 0
            private val iconH = icon?.intrinsicHeight ?: 0

            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.adapterPosition
                // Cancel the scheduled system alarm
                val deleted = adapter.removeAt(pos)
                val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val cancelIntent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                    putExtra("ALARM_LABEL", deleted.label)
                }
                val pending = PendingIntent.getBroadcast(
                    requireContext(),
                    deleted.id,
                    cancelIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pending)

                // Remove from ViewModel and notify user
                alarmViewModel.deleteAlarm(deleted)
                Toast.makeText(requireContext(), "Alarm deleted", Toast.LENGTH_SHORT).show()
            }

            val swipeThreshold = 100f // Define a threshold for when to show the icon
            val maxIconWidth = iconW.toFloat() // The full width of the icon

            override fun onChildDraw(
                c: Canvas,
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isActive: Boolean
            ) {
                val item = vh.itemView
                val rect = RectF(
                    item.right + dX, item.top.toFloat(), item.right.toFloat(), item.bottom.toFloat()
                )

                // Always show the red background when swiping left, regardless of threshold
                if (dX < 0) {
                    c.drawRect(rect, paint)
                }

                // Calculate how much of the trash icon should be shown
                val iconWidth = when {
                    dX < -swipeThreshold -> maxIconWidth // Show full icon if swipe is beyond threshold
                    dX < 0 -> maxIconWidth * (-dX / swipeThreshold) // Show partial icon based on the swipe amount
                    else -> 0f // No icon if swiped back to the right
                }

                // Ensure that the icon does not go past the edge of the item
                val iconLeft = item.right - (item.height - iconH) / 2 - iconWidth
                val iconRight = item.right - (item.height - iconH) / 2

                // If the icon exceeds the bounds of the item, adjust it to stay within the right edge
                val adjustedLeft = Math.max(iconLeft, item.right - (item.height - iconH) / 2 - maxIconWidth)
                val adjustedRight = Math.min(iconRight, item.right - (item.height - iconH) / 2)

                // Only draw the icon if there's any portion to show
                if (iconWidth > 0) {
                    val top = item.top + (item.height - iconH) / 2
                    val bottom = top + iconH
                    icon?.setBounds(adjustedLeft.toInt(), top.toInt(), adjustedRight.toInt(), bottom.toInt())
                    icon?.draw(c)
                }

                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }

        })
        itemTouchHelper.attachToRecyclerView(recycler)

        // Observe LiveData with post to avoid layout conflicts
        alarmViewModel.alarms.observe(viewLifecycleOwner) { alarms ->
            recycler.post {
                adapter.updateList(alarms)
            }
        }

        return view
    }

    private val updateAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<Alarm>("updatedAlarm")?.let { updated ->
                // Pass a copy of the alarm to ensure the ViewModel works with a new instance
                alarmViewModel.updateAlarm(updated.copy())  // Using .copy() to avoid reference issues
                val fmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
                Toast.makeText(
                    requireContext(),
                    "Alarm updated: ${updated.time.format(fmt)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val addAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<Alarm>("updatedAlarm")?.let { newAlarm ->
                // Ensure you're adding a copy of the alarm to avoid reference issues
                alarmViewModel.addAlarm(newAlarm.copy())
            }
        }
    }

    fun launchAddAlarm() {
        val intent = Intent(requireContext(), AlarmAddActivity::class.java)
        addAlarmLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
