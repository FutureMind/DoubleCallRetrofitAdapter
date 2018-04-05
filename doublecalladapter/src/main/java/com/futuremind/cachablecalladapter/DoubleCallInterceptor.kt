package com.futuremind.cachablecalladapter

import okhttp3.*
import okhttp3.Interceptor.Chain
import okhttp3.Protocol.HTTP_1_0

class DoubleCallInterceptor(private val doubleCallManager: DoubleCallManager) : Interceptor {

    companion object {
        const val SHOULD_DOUBLE_CALL = "DoubleCallHackHeader"
        const val CACHE_CALL_TO_BE_SKIPPED = 504 //Retrofit uses 504 Unsatisfiable request when cache is empty and we force cache
        private const val HEADER_FORCE_CACHE = "only-if-cached, max-stale=" + Integer.MAX_VALUE
        private const val HEADER_FORCE_NETWORK = "no-cache"
    }

    override fun intercept(chain: Chain): Response {
        var request = chain.request()
        val hackHeader = request.header(SHOULD_DOUBLE_CALL)
        if (hackHeader != null)   request = request.newBuilder().removeHeader(SHOULD_DOUBLE_CALL).build()
        val cacheStrategy = doubleCallManager.rotateCallStrategy(request, hackHeader)
        if(doubleCallManager.logging) System.out.println("$cacheStrategy cache strategy for call $request")
        return when (cacheStrategy) {
            DoubleCallManager.CallStrategy.NONE                -> chain.proceed(request)
            DoubleCallManager.CallStrategy.SKIPPED_CACHE_CALL  -> prepareSkippedResponse(request)
            DoubleCallManager.CallStrategy.FORCE_CACHE         -> chain.proceed(request.newBuilder().addHeader("Cache-Control", HEADER_FORCE_CACHE).build())
            DoubleCallManager.CallStrategy.FORCE_NETWORK       -> chain.proceed(request.newBuilder().addHeader("Cache-Control", HEADER_FORCE_NETWORK).build())
        }
    }

    private fun prepareSkippedResponse(request: Request): Response = Response.Builder()
            .request(request)
            .protocol(HTTP_1_0)
            .code(CACHE_CALL_TO_BE_SKIPPED)
            .message("Skipped cached call (internal DoubleCall stuff)")
            .body(ResponseBody.create(null, ""))
            .build()


}