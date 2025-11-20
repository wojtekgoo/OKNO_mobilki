package com.example.okno.ui.theme

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel : ViewModel() {
    private val _forceDark = MutableStateFlow<Boolean?>(false)
    val forceDark = _forceDark.asStateFlow()

    fun setForceDark(value: Boolean?) {
        _forceDark.value = value
    }

    fun toggle() { _forceDark.value = !(_forceDark.value ?: false) }
}