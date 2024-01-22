package com.kondee.pocsdcard

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.kondee.pocsdcard.databinding.ActivityMainBinding
import com.kondee.pocsdcard.enums.DownloadLocation
import com.kondee.pocsdcard.enums.FileStatus
import com.kondee.pocsdcard.utils.Constant
import com.kondee.pocsdcard.worker.DownloadWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()
        observe()
    }

    override fun onResume() {
        super.onResume()

//        viewModel.getSDCardStatus()
    }

    private fun initView() {

        binding.buttonDownload.setOnClickListener {
            WorkManager.getInstance(this@MainActivity)
                .enqueue(DownloadWorker.getWorkRequest(Constant.FILE_URL))
        }

        binding.buttonDelete.setOnClickListener {
            viewModel.deleteFile()
        }

        binding.toggleGroupDownloadLocation.addOnButtonCheckedListener(onButtonCheckedListener)
    }

    private fun observe() {

        lifecycleScope.launch {
            viewModel.sdCardAvailableFlow.collect { sdCardAvailable ->

                binding.labelDownloadLocation.isVisible = sdCardAvailable
                binding.toggleGroupDownloadLocation.isVisible = sdCardAvailable

                binding.textViewSdCardStatus.text = if (sdCardAvailable) {
                    "SD Card Available!"
                } else {
                    "No SD Card!"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.fileStatusFlow.collect { fileStatus ->

                binding.textViewFileStatus.text = fileStatus.status

                binding.progress.isVisible = fileStatus is FileStatus.Downloading
                binding.textViewProgress.isVisible = fileStatus is FileStatus.Downloading

                when (fileStatus) {
                    is FileStatus.Downloaded -> {
                        binding.textViewFilePath.text = fileStatus.fileLocation
                    }
                    is FileStatus.Downloading -> {
                        binding.textViewFilePath.text = "-"
                        binding.progress.progress = fileStatus.progress
                        binding.textViewProgress.text = "${fileStatus.progress}%"
                    }
                    is FileStatus.Error -> {
                        binding.textViewFilePath.text = "-"
                    }
                    FileStatus.Moving -> {
                        binding.textViewFilePath.text = "-"
                    }
                    FileStatus.Idle -> {
                        binding.textViewFilePath.text = "-"
                    }
                }
            }
        }

        lifecycleScope.launch {
            WorkManager.getInstance(this@MainActivity)
                .getWorkInfosByTagFlow(Constant.FILE_URL).collect { workInfos ->
                    viewModel.handleDownloadStates(workInfos)
                }
        }

        lifecycleScope.launch {
            viewModel.downloadLocationFlow.collect {
                displayedDownloadLocation(it)
            }
        }
    }

    private fun displayedDownloadLocation(it: DownloadLocation) {
        binding.toggleGroupDownloadLocation.removeOnButtonCheckedListener(onButtonCheckedListener)

        binding.toggleGroupDownloadLocation.check(
            when (it) {
                DownloadLocation.Internal -> {
                    binding.buttonInternal.id
                }
                DownloadLocation.SDCard -> {
                    binding.buttonSdCard.id
                }
            }
        )

        binding.toggleGroupDownloadLocation.addOnButtonCheckedListener(onButtonCheckedListener)
    }

    fun showDialogMoveFiles(destinationLocation: DownloadLocation) {

        val destination = when (destinationLocation) {
            DownloadLocation.Internal -> {
                "พื้นที่เครื่อง"
            }
            DownloadLocation.SDCard -> {
                "SD Card"
            }
        }
        val dialog = AlertDialog.Builder(this@MainActivity)
            .setTitle("ปรับปรุงการจัดเก็บไฟล์")
            .setMessage(
                String.format(
                    "คุณต้องการย้ายไฟล์ที่ดาวน์โหลดแล้ว ไปเก็บไว้ใน %s ด้วยหรือไม่\n" +
                        "หากไม่ย้าย ไฟล์ในเครื่องจะถูกลบ", destination
                )
            )
            .setPositiveButton("ย้ายเลย") { _, _ ->
                viewModel.setDownloadLocation(destinationLocation, true)
            }
            .setNegativeButton("ไม่ย้าย") { _, _ ->
                viewModel.setDownloadLocation(destinationLocation)
            }
            .setOnDismissListener {
                resetDisplayedDownloadLocation()
            }
            .create()

        dialog.show()
    }

    private fun resetDisplayedDownloadLocation() {

        lifecycleScope.launch {
            val downloadLocation = viewModel.downloadLocationFlow.first()
            displayedDownloadLocation(downloadLocation)
        }
    }

    /**
     * Listener
     */

    private val onButtonCheckedListener = MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked ->
        if (isChecked) {
            when (checkedId) {
                binding.buttonInternal.id -> {
                    showDialogMoveFiles(DownloadLocation.Internal)
                }
                binding.buttonSdCard.id -> {
                    showDialogMoveFiles(DownloadLocation.SDCard)
                }
            }
        }
    }
}