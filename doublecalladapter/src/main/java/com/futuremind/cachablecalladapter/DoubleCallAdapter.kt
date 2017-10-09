package com.futuremind.cachablecalladapter

import io.reactivex.Flowable
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.HttpException
import java.lang.reflect.Type

class DoubleCallAdapter<R>(private val regularAdapter: CallAdapter<R, *>, private val doubleCallManager: DoubleCallManager, private val shouldDoubleCall: Boolean) : CallAdapter<R, Any> {

    /**
     * 504 response just means that there was nothing in the cache and the
     * cachedCall should not return at all
     */
    private val cachedCallErrorHandler = { error: Throwable ->
        when(error is HttpException && error.code()==504) {
            true -> Flowable.empty<R>()
            false -> Flowable.error(error)
        }
    }

    override fun adapt(originalCall: Call<R>): Any {

        return if (shouldDoubleCall) {

            val cachedCall = originalCall.clone()
            val networkCall = originalCall.clone()
            doubleCallManager.put(originalCall.request())

            Flowable.concat(
                    (regularAdapter.adapt(cachedCall) as Flowable<R>).onErrorResumeNext(cachedCallErrorHandler),
                    regularAdapter.adapt(networkCall) as Flowable<R>
            )

        } else {
            regularAdapter.adapt(originalCall)
        }

    }

    override fun responseType(): Type = regularAdapter.responseType()

}