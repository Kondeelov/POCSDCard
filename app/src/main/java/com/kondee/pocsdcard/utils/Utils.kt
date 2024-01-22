package com.kondee.pocsdcard.utils

import android.content.Context
import android.os.Environment
import java.io.File

object Utils {

    fun sdCardDirectory(context: Context): File? {
        val externalFilesDirs = context.getExternalFilesDirs(null)

        return externalFilesDirs.find { dir ->
            val isSDPresent = Environment.getExternalStorageState(dir) == Environment.MEDIA_MOUNTED
            val isSDSupportedDevice = Environment.isExternalStorageRemovable(dir)
            isSDPresent && isSDSupportedDevice
        }
    }
}