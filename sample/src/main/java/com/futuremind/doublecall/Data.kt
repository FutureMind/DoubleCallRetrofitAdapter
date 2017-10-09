package com.futuremind.doublecall

import com.squareup.moshi.Json

data class Repo(val id: Long, val name: String, val url: String, val owner: User)
data class User(val id: Long, val login: String, @Json(name = "avatar_url") val avatarUrl: String?, val url: String)
data class Issue(val id: Long, val title: String)