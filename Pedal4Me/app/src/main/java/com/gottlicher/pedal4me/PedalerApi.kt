package com.gottlicher.pedal4me

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class SetParams (val bcmPin:Int, val rpm:Int)
data class Status (val isRunning:Boolean,val bcmPin:Int, val rpm:Int)
data class ApiResponse (val success:Boolean)

interface PedalerApi {
    @POST ("/set")
    fun setAsync(@Body params:SetParams): Deferred<Response<ApiResponse>>

    @POST("/stop")
    fun stopAsync(): Deferred<Response<ApiResponse>>

    @GET("/status")
    fun statusAsync(): Deferred<Response<Status>>
}