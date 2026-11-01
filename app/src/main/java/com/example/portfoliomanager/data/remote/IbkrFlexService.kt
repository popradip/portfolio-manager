package com.example.portfoliomanager.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url

interface IbkrFlexService {

    @Headers("User-Agent: Java/1.8.0")
    @GET("AccountManagement/FlexWebService/SendRequest")
    suspend fun sendRequest(
        @Query("t") token: String,
        @Query("q") queryId: String,
        @Query("v") version: String = "3"
    ): Response<ResponseBody>

    @Headers("User-Agent: Java/1.8.0")
    @GET("AccountManagement/FlexWebService/GetStatement")
    suspend fun getStatement(
        @Query("q") referenceCode: String,
        @Query("t") token: String,
        @Query("v") version: String = "3"
    ): Response<ResponseBody>

    @Headers("User-Agent: Java/1.8.0")
    @GET("Universal/servlet/FlexStatementService.SendRequest")
    suspend fun sendRequestLegacy(
        @Query("t") token: String,
        @Query("q") queryId: String,
        @Query("v") version: String = "3"
    ): Response<ResponseBody>

    @Headers("User-Agent: Java/1.8.0")
    @GET("Universal/servlet/FlexStatementService.GetStatement")
    suspend fun getStatementLegacy(
        @Query("q") referenceCode: String,
        @Query("t") token: String,
        @Query("v") version: String = "3"
    ): Response<ResponseBody>

    @Headers("User-Agent: Java/1.8.0")
    @GET
    suspend fun getStatementByUrl(
        @Url url: String,
        @Query("q") referenceCode: String,
        @Query("t") token: String,
        @Query("v") version: String = "3"
    ): Response<ResponseBody>
}
