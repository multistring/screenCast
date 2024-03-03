package com.tta.module.network.bean

class BaseChildResponseList<T, E> : BaseResponseList<T>() {
    var extData: E? = null
}