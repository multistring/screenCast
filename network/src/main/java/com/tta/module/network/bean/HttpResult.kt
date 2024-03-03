package com.tta.module.network.bean

class HttpResult<T> {

    companion object {
        const val API_SUCCESS = "0"
        const val API_FAIL = "-10"
        const val API_ERROR = "-20"
    }

    var code: String? = null
    var data: T? = null
    var msg: String? = null
    var result: Boolean = false

    constructor()
    constructor(code: String?, data: T?, message: String?) {
        this.code = code
        this.data = data
        this.msg = message
    }

    constructor(code: String?) {
        this.code = code
    }
}