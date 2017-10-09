package com.futuremind.cachablecalladapter

import kotlin.annotation.AnnotationRetention.RUNTIME

@Target(AnnotationTarget.FUNCTION)
@Retention(RUNTIME)
annotation class DoubleCall