package com.futuremind.cachablecalladapter

import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.lang.reflect.Type

class DoubleCallAdapterFactory(private val regularFactory: RxJava2CallAdapterFactory, private val cacheNegotiator: DoubleCallManager) : CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (annotations.find { it.annotationClass == DoubleCall::class } != null) {
            //TODO only accept Flowable
            return DoubleCallAdapter(regularFactory.get(returnType, annotations, retrofit)!!, cacheNegotiator, true)
        }
        return regularFactory.get(returnType, annotations, retrofit)
    }

}