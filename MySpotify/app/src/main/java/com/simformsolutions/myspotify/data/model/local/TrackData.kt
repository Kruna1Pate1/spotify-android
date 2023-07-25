package com.simformsolutions.myspotify.data.model.local

import android.os.Parcelable
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.simformsolutions.myspotify.BR
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrackItem(
    val id: String,
    val image: String?,
    val title: String,
    val type: String,
    val artists: String,
    val previewUrl: String? = null,
    var description: String = "",
    private var _isLiked: Boolean = false
) : BaseObservable(), Parcelable {

    @IgnoredOnParcel
    @get:Bindable
    var isLiked: Boolean = _isLiked
        set(value) {
            _isLiked = value
            field = value
            notifyPropertyChanged(BR.liked)
        }
}