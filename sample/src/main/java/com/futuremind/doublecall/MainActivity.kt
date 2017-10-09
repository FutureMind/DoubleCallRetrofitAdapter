package com.futuremind.doublecall

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.futuremind.cachablecalladapter.DoubleCallAdapterFactory
import com.futuremind.cachablecalladapter.DoubleCallInterceptor
import com.futuremind.cachablecalladapter.DoubleCallManager
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS

        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        val cacheFile = File(application.cacheDir, "apiResponses")
        val doubleCallManager = DoubleCallManager()

        val apiClient = OkHttpClient.Builder()
                .addInterceptor(DoubleCallInterceptor(doubleCallManager))
                .addInterceptor(loggingInterceptor)
                .cache(Cache(cacheFile, 5 * 1024 * 1024))
                .build()

        val api = Retrofit.Builder()
                .addCallAdapterFactory(DoubleCallAdapterFactory(RxJava2CallAdapterFactory.create(), doubleCallManager))
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl("https://api.github.com/")
                .client(apiClient)
                .build()
                .create(GithubApi::class.java)

        api.getSquareRepos()
                .flatMapPublisher { Flowable.fromIterable(it) }
                .flatMap { repo ->
                    api.getRepoIssue(repo.name, Random().nextBoolean())
                }
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()

    }
}
