package cit.edu.KlockApp.ui.main.worldClock

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cit.edu.KlockApp.databinding.FragmentAddclockListBinding

// TODO: Customize parameter argument names
const val ARG_ITEM_COUNT = "item_count"

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    AddNewWorldClock.newInstance(30).show(supportFragmentManager, "dialog")
 * </pre>
 */
class AddNewWorldClock : BottomSheetDialogFragment() {

    private var _binding: FragmentAddclockListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddclockListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = TimeZoneAdapter(4) // Adjust item count as needed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}