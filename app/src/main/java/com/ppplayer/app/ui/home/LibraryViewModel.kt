package com.ppplayer.app.ui.home

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ppplayer.app.data.MediaFile
import com.ppplayer.app.data.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryState(
    val hasPermission: Boolean = false,
    val loading: Boolean = true,
    val videos: List<MediaFile> = emptyList(),
    val audio: List<MediaFile> = emptyList(),
)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MediaRepository(app)
    private val _state = MutableStateFlow(LibraryState(hasPermission = MediaRepository.hasPermission(app)))
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) = refresh(debounce = true)
    }

    init {
        val resolver = app.contentResolver
        resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
        resolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        refresh()
    }

    /** Re-checks the permission and reloads the library. */
    fun refresh(debounce: Boolean = false) {
        val granted = MediaRepository.hasPermission(getApplication())
        if (!granted) {
            _state.value = LibraryState(hasPermission = false, loading = false)
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (debounce) delay(400)
            _state.value = _state.value.copy(hasPermission = true, loading = _state.value.videos.isEmpty())
            val videos = repository.loadVideos()
            val audio = repository.loadAudio()
            _state.value = LibraryState(hasPermission = true, loading = false, videos = videos, audio = audio)
        }
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(observer)
    }
}
