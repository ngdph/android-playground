package co.iostream.apps.android.core

import kotlinx.serialization.Serializable

@Serializable
open class IOItem() {
    protected var _order: Int = 0
    var order: Int
        get() = _order
        set(value) {
            _order = value
        }

    protected var _isSelected: Boolean = false
    var isSelected: Boolean
        get() = _isSelected
        set(value) {
            _isSelected = value
        }

    protected var _visibility: Boolean = true
    var visibility: Boolean
        get() = _visibility
        set(value) {
            _visibility = value
        }

    protected var _isEnabled: Boolean = true
    var isEnabled: Boolean
        get() = _isEnabled
        set(value) {
            _isEnabled = value
        }

    protected var _autoNotify = false

    constructor(autoNotify: Boolean = true) : this() {
        _autoNotify = autoNotify
    }
}