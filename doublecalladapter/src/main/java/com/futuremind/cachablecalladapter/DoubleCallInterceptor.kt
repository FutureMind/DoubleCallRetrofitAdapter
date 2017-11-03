package com.futuremind.cachablecalladapter

import okhttp3.*
import okhttp3.Interceptor.Chain
import okhttp3.Protocol.HTTP_1_0

class DoubleCallInterceptor(private val doubleCallManager: DoubleCallManager) : Interceptor {

    companion object {
        const val SHOULD_DOUBLE_CALL = "DoubleCallHackHeader"
        private const val HEADER_FORCE_CACHE = "only-if-cached, max-stale=" + Integer.MAX_VALUE
        private const val HEADER_FORCE_NETWORK = "no-cache"
    }

    override fun intercept(chain: Chain): Response {
        var request = chain.request()
        val hackHeader = request.header(SHOULD_DOUBLE_CALL)
        if (hackHeader != null)   request = request.newBuilder().removeHeader(SHOULD_DOUBLE_CALL).build()
        val cacheStrategy = doubleCallManager.rotateCallStrategy(request, hackHeader)
        return when (cacheStrategy) {
            DoubleCallManager.CallStrategy.NONE                -> chain.proceed(request)
            DoubleCallManager.CallStrategy.IGNORE_CACHE_CALL   -> prepareIgnoredResponse(request)
            DoubleCallManager.CallStrategy.FORCE_CACHE         -> chain.proceed(request.newBuilder().addHeader("Cache-Control", HEADER_FORCE_CACHE).build())
            DoubleCallManager.CallStrategy.FORCE_NETWORK       -> chain.proceed(request.newBuilder().addHeader("Cache-Control", HEADER_FORCE_NETWORK).build())
        }
    }

    private fun prepareIgnoredResponse(request: Request): Response = Response.Builder()
            .request(request)
            .protocol(HTTP_1_0)
            .code(504)
            .message("Ignored cached call")
            .body(ResponseBody.create(null, ""))
            .build()


}