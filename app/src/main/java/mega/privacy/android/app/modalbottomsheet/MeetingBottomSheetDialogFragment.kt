package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetMeetingBinding
import mega.privacy.android.app.databinding.BottomSheetMeetingSimpleBinding
import mega.privacy.android.app.interfaces.MeetingBottomSheetDialogActionListener
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

@AndroidEntryPoint
class MeetingBottomSheetDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {

    companion object {
        const val TAG = "MeetingBottomSheetDialog"
        private const val SHOW_SIMPLE_LIST = "SHOW_SIMPLE_LIST"

        @JvmStatic
        fun newInstance(showSimpleList: Boolean): MeetingBottomSheetDialogFragment =
            MeetingBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(SHOW_SIMPLE_LIST, showSimpleList)
                }
            }
    }

    @Inject
    lateinit var getFeatureFlagUseCase: GetFeatureFlagValueUseCase

    private val showSimpleList by lazy { arguments?.getBoolean(SHOW_SIMPLE_LIST) ?: false }
    private var listener: MeetingBottomSheetDialogActionListener? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        if (showSimpleList) {
            val binding =
                BottomSheetMeetingSimpleBinding.inflate(LayoutInflater.from(context), null, false)
            binding.btnStartMeeting.setOnClickListener(this)
            binding.btnJoinMeeting.setOnClickListener(this)
            binding.dividerSchedule.isVisible = true
            binding.btnScheduleMeeting.isVisible = true
            binding.btnScheduleMeeting.setOnClickListener(this)

            dialog.setContentView(binding.root)
        } else {
            val binding = BottomSheetMeetingBinding.inflate(LayoutInflater.from(context), null, false)
            binding.ivStartMeeting.setOnClickListener(this)
            binding.ivJoinMeeting.setOnClickListener(this)
            dialog.setContentView(binding.root)
        }
    }

    override fun onStart() {
        super.onStart()
        if (showSimpleList) {
            val dialog = dialog ?: return
            BottomSheetBehavior.from(dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)).state =
                BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_start_meeting, R.id.btn_start_meeting -> {
                listener?.onCreateMeeting()
                dismiss()
            }
            R.id.iv_join_meeting, R.id.btn_join_meeting -> {
                listener?.onJoinMeeting()
                dismiss()
            }
            R.id.btn_schedule_meeting -> {
                listener?.onScheduleMeeting()
                dismiss()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as MeetingBottomSheetDialogActionListener
    }
}
