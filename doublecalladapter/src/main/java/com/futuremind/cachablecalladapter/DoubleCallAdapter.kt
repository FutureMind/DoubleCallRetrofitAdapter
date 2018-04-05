package com.futuremind.cachablecalladapter

import com.futuremind.cachablecalladapter.DoubleCallInterceptor.Companion.CACHE_CALL_TO_BE_SKIPPED
import io.reactivex.Flowable
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.HttpException
import retrofit2.adapter.rxjava2.Result
import java.lang.reflect.Type

class DoubleCallAdapter<R>(private val regularAdapter: CallAdapter<R, *>, private val doubleCallManager: DoubleCallManager, private val shouldDoubleCall: Boolean) : CallAdapter<R, Any> {

    /**
     * [CACHE_CALL_TO_BE_SKIPPED] response just means that there was nothing in the cache and the
     * cachedCall should not return at all
     */
    private val skipRegularCalls = { error: Throwable ->
        when (error is HttpException && error.code() == CACHE_CALL_TO_BE_SKIPPED) {
            true -> Flowable.empty<R>()
            false -> Flowable.error(error)
        }
    }

    private val skipResultCalls = { item: R ->
        when(item is Result<*> && item.response()?.code() == CACHE_CALL_TO_BE_SKIPPED) {
            true -> Flowable.empty<R>()
            false -> Flowable.just(item)
        }
    }

    override fun adapt(originalCall: Call<R>): Any {

        return if (shouldDoubleCall) {

            val cachedCall = originalCall.clone()
            val networkCall = originalCall.clone()
            doubleCallManager.put(originalCall.request())

            Flowable.concat(
                    (regularAdapter.adapt(cachedCall) as Flowable<R>)
                            .flatMap(skipResultCalls)
                            .onErrorResumeNext(skipRegularCalls),
                    regularAdapter.adapt(networkCall) as Flowable<R>
            )

        } else {
            regularAdapter.adapt(originalCall)
        }

    }

    override fun responseType(): Type = regularAdapter.responseType()

}