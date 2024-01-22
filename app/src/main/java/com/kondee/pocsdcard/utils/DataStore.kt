package com.kondee.pocsdcard.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

object DataStore {

    val Context.downloadLocationDataStore: DataStore<Preferences> by preferencesDataStore("download_location")

    val downloadLocationPreferenceKey = intPreferencesKey("location")
}