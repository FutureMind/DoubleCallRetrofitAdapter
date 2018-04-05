package com.futuremind.cachablecalladapter

import com.futuremind.cachablecalladapter.DoubleCallManager.CallStrategy.*
import okhttp3.Request
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

/**
 * Responsible for communication between [DoubleCallAdapterFactory] and [DoubleCallInterceptor].
 * When [DoubleCallAdapter] encounters a [DoubleCall] marked call, it [put]s it here.
 * Every time [rotateCallStrategy] is called by [DoubleCallInterceptor] a [CallStrategy] for this specific
 * call is established.
 */
class DoubleCallManager private constructor(internal val logging: Boolean) {

    private val cacheStrategies = mutableMapOf<Int, CallStrategy>()

    lateinit var adapterFactory: DoubleCallAdapterFactory internal set
    lateinit var interceptor: DoubleCallInterceptor internal set

    internal fun put(request: Request) {
        cacheStrategies[request.key()] = FORCE_CACHE
    }

    internal fun rotateCallStrategy(request: Request, hackHeader: String?): CallStrategy {
        val originalCallStrategy = cacheStrategies[request.key()]
        val realCallStrategy = evaluateRealCacheStrategy(originalCallStrategy, hackHeader)
        return when (realCallStrategy) {
            NONE -> NONE
            SKIPPED_CACHE_CALL -> {
                cacheStrategies.put(request.key(), NONE)
                SKIPPED_CACHE_CALL
            }
            FORCE_CACHE -> {
                cacheStrategies.put(request.key(), FORCE_NETWORK)
                FORCE_CACHE
            }
            FORCE_NETWORK -> {
                cacheStrategies.remove(request.key())
                FORCE_NETWORK
            }
        }
    }

    /**
     * The [hackHeader] can disable the original cache strategy, if it's set to false,
     * the first call will have [SKIPPED_CACHE_CALL] strategy and the second [NONE]
     */
    private fun evaluateRealCacheStrategy(originalCallStrategy: CallStrategy?, hackHeader: String?): CallStrategy = when {
        hackHeader == null -> originalCallStrategy ?: NONE
        hackHeader.toBoolean() -> originalCallStrategy ?: NONE
        else -> when {
            originalCallStrategy != CallStrategy.NONE -> SKIPPED_CACHE_CALL
            else -> NONE
        }
    }

    private fun Request.key() = (this.url().toString() + this.method().toString()).hashCode()

    enum class CallStrategy {

        /** There will only be a single call, so no CallStrategy is needed */
        NONE,

        /** In case where user defined [DoubleCall] annotation but then disabled it with
         *  [DoubleCallInterceptor.SHOULD_DOUBLE_CALL]=false, the first call will be
         *  (a little bit hackishly) skipped*/
        SKIPPED_CACHE_CALL,

        /** This is the first from two calls - it will be forced from cache
         * (might be an ignored call when cache is empty) */
        FORCE_CACHE,

        /** This is the second from two calls - it will be forced from network */
        FORCE_NETWORK
    }

    class Builder{

        private var logging = false

        fun setLogging(logging: Boolean) : Builder{
            this.logging = logging
            return this
        }

        fun build() : DoubleCallManager {
            val manager = DoubleCallManager(logging)
            manager.adapterFactory = DoubleCallAdapterFactory(RxJava2CallAdapterFactory.create(), manager)
            manager.interceptor = DoubleCallInterceptor(manager)
            return manager
        }

    }

}