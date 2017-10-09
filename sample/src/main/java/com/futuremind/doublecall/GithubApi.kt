package com.futuremind.doublecall

import com.futuremind.cachablecalladapter.DoubleCall
import com.futuremind.cachablecalladapter.DoubleCallInterceptor
import io.reactivex.Flowable
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface GithubApi {

    @GET("/users/square/repos")
    fun getSquareRepos() : Single<List<Repo>>

    @DoubleCall
    @GET("/repos/square/{repo}/issues")
    fun getRepoIssue(
            @Path("repo") repo: String,
            @Header(DoubleCallInterceptor.SHOULD_DOUBLE_CALL) shouldDoubleCall: Boolean
    ) : Flowable<List<Issue>>

}