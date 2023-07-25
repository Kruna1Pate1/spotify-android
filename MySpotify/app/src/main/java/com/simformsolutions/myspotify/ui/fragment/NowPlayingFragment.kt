package com.simformsolutions.myspotify.ui.fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.IBinder
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.simformsolutions.myspotify.R
import com.simformsolutions.myspotify.data.model.local.ItemType
import com.simformsolutions.myspotify.data.model.local.TrackItem
import com.simformsolutions.myspotify.databinding.FragmentNowPlayingBinding
import com.simformsolutions.myspotify.ui.base.BaseFragment
import com.simformsolutions.myspotify.ui.service.NowPlayingService
import com.simformsolutions.myspotify.ui.viewmodel.MainViewModel
import com.simformsolutions.myspotify.ui.viewmodel.NowPlayingViewModel
import com.simformsolutions.myspotify.utils.IntentData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NowPlayingFragment : BaseFragment<FragmentNowPlayingBinding, NowPlayingViewModel>() {

    private val args: NowPlayingFragmentArgs by navArgs()
    private val activityViewModel: MainViewModel by activityViewModels()
    private var nowPlayingService: NowPlayingService? = null
    private val receiver = NowPlayingReceiver()
    private var isBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            nowPlayingService = (binder as? NowPlayingService.MyBinder)?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override val viewModel: NowPlayingViewModel by viewModels()

    override fun getLayoutResId(): Int = R.layout.fragment_now_playing

    override fun initialize() {
        super.initialize()
        requireContext().registerReceiver(receiver, IntentFilter().apply {
            addAction(IntentData.ACTION_RESUME_PLAYBACK)
            addAction(IntentData.ACTION_PAUSE_PLAYBACK)
            addAction(IntentData.ACTION_NEXT_TRACK)
            addAction(IntentData.ACTION_PREVIOUS_TRACK)
        })
        setupUI()
    }

    override fun initializeObservers() {
        super.initializeObservers()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.name.collectLatest { name ->
                        activityViewModel.setSubtitle(name)
                    }
                }
                launch {
                    viewModel.track.collectLatest { track ->
                        track?.let { nowPlayingService?.startPlayback(it) }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(requireContext(), NowPlayingService::class.java).also { intent ->
            requireContext().bindService(intent, serviceConnection, 0)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            requireContext().unbindService(serviceConnection)
        }
    }

    override fun onDestroyView() {
        activityViewModel.setAppBarScrollingEnabled(true)
        activityViewModel.setSubtitle("")
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        requireContext().unregisterReceiver(receiver)
        super.onDestroyView()
    }


    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupUI() {
        activityViewModel.setAppBarScrollingEnabled(false)
        setupTitle()
        startNowPlayingService()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        viewModel.setupPlayingQueue(args.trackId, args.id, args.type)
        setupListener()
    }

    private fun setupTitle() {
        val type = if (args.type == ItemType.TRACK) {
            ItemType.ALBUM
        } else {
            args.type
        }
        requireActivity().title =
            getString(R.string.playing_from, type.getLocalizedName(requireContext()))
    }

    private fun setupListener() {
        binding.btnPlayPause.setOnClickListener {
            if (binding.btnPlayPause.isChecked) {
                nowPlayingService?.resumePlayback()
            } else {
                nowPlayingService?.pausePlayback()
            }
        }
    }

    private fun startNowPlayingService(trackItem: TrackItem? = null) {
        val intent = Intent(requireContext(), NowPlayingService::class.java).apply {
            putExtra(IntentData.TRACK_ITEM, trackItem)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private inner class NowPlayingReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                IntentData.ACTION_RESUME_PLAYBACK -> {
                    binding.btnPlayPause.isChecked = true
                }
                IntentData.ACTION_PAUSE_PLAYBACK -> {
                    binding.btnPlayPause.isChecked = false
                }
                IntentData.ACTION_PREVIOUS_TRACK -> {
                    viewModel.previousTrack()
                }
                IntentData.ACTION_NEXT_TRACK -> {
                    viewModel.nextTrack()
                }
            }
        }
    }
}