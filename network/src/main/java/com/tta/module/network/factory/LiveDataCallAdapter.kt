package com.tta.module.network.factory

import androidx.lifecycle.LiveData
import com.tta.module.network.bean.HttpResult
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean

class LiveDataCallAdapter<R>(private val responseType: Type) : CallAdapter<R, LiveData<HttpResult<*>>> {
    override fun responseType(): Type {
        return responseType
    }

    override fun adapt(call: Call<R>): LiveData<HttpResult<*>> {
        return object : LiveData<HttpResult<*>>() {
            var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                   call.enqueue(object : Callback<R> {
                        override fun onResponse(call: Call<R>, response: Response<R>) {
                            var responseData: HttpResult<*> = HttpResult<Any?>()
                            if (response.isSuccessful) {
                                responseData = response.body() as HttpResult<*>
                            } else {
                                responseData.apply {
                                    try {
                                        msg = response.errorBody()!!.string()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                    data = null
                                    code = response.code().toString()
                                }
                            }
                            postValue(responseData)
                        }

                        override fun onFailure(call: Call<R>, throwable: Throwable) {
                            val responseData: HttpResult<*> = HttpResult<Any?>().apply {
                                code = HttpResult.API_FAIL
                                data = null
                                msg = throwable.message
                            }
                            postValue(responseData)
                        }
                    })
                }
            }
        }
    }

}