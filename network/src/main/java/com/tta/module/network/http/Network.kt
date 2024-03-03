package com.tta.module.network.http

import com.google.gson.GsonBuilder
import com.tta.module.network.adapter.GsonAnyAdapter
import com.tta.module.network.adapter.GsonListAdapter
import com.tta.module.network.bean.HttpResult
import com.tta.module.network.factory.LiveDataCallAdapterFactory
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import io.reactivex.functions.Function
import java.util.concurrent.Executors


abstract class Network(var mBaseUrl: String, var isAddOkHttpClient: Boolean = true) {

    var retrofit: Retrofit? = null
    private var retrofitHashMap = HashMap<String, Retrofit>()

    private fun getRetrofit(service: Class<*>): Retrofit {
        retrofit = retrofitHashMap[mBaseUrl + service.name]
        if (retrofit != null) {
            return retrofit!!
        }
        val gson = GsonBuilder()
            .registerTypeAdapter(List::class.java, GsonListAdapter())//处理后台有时一个接口data结构不同的情况
            .registerTypeAdapter(Any::class.java, GsonAnyAdapter())
            .create()
        val b = Retrofit.Builder().baseUrl(mBaseUrl)
        if (isAddOkHttpClient) {
            b.client(getOkHttpClient())  //请求的网络框架
                .addConverterFactory(GsonConverterFactory.create(gson)) //解析数据格式
                .addCallAdapterFactory(LiveDataCallAdapterFactory()) // 使用LiveData作为回调适配器
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // 使用LiveData作为回调适配器
        } else {
            b.callbackExecutor(Executors.newSingleThreadExecutor())
        }
        retrofit = b.build()
        retrofitHashMap[mBaseUrl + service.name] = retrofit!!
        return retrofit!!
    }

    //abstract fun <T> getService(service: Class<T>): T

    fun <T> getService(service: Class<T>): T {
        return getRetrofit(service).create(service)
    }

    fun <T> getService(baseUrl: String, service: Class<T>): T {
        mBaseUrl = baseUrl
        return getRetrofit(service).create(service)
    }

    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor {
            }.setLevel(HttpLoggingInterceptor.Level.BODY))
            .addInterceptor(getRequestInterceptor())
            .addInterceptor(getResponseInterceptor())
            .build()
    }

    fun <T> applySchedulers(observer: Observer<T>): ObservableTransformer<T, T> {
        return ObservableTransformer<T, T> {
            val observable =
                it.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .map(getAppErrorHandler())
                    .onErrorResumeNext(Function<Throwable, Observable<T>> { t ->
                        //统一处理http异常
                        Observable.error(t)
                    })
            observable.subscribe(observer)
            observable
        }
    }

    private fun <T> getAppErrorHandler(): Function<T, T> {
        return Function<T, T> { t ->
            if (t is HttpResult<*>) {
                if (t.code != HttpResult.API_SUCCESS) {
                    //统一处理其他错误码
                    println("错误码${t.code}")
                }
            }
            t
        }
    }

    abstract fun getRequestInterceptor(): Interceptor
    abstract fun getResponseInterceptor(): Interceptor
}