package com.example.jetdictionary.core

import android.util.Log
import com.example.jetdictionary.domain.model.BaseResponse
import com.example.jetdictionary.presenter.base.ViewModelState
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import retrofit2.Response
import java.io.IOException

typealias NetworkAPIInvoke<T> = suspend () -> Response<T>


suspend fun <T> performSafeNetworkApiCall(
    messageInCaseOfError: String = "Network error",
    allowRetries: Boolean = true,
    numberOfRetries: Int = 2,
    networkApiCall: NetworkAPIInvoke<T>,
    networkHelper: NetworkHelper
): Flow<IOResult<T>> {
    var delayDuration = 1000L
    val delayFactor = 2
    return flow {
        if (!networkHelper.isNetworkConnected()) {
            emit(IOResult.OnFailed(Errors.NoInternetError()))
            return@flow
        }
        val response = networkApiCall()
        if (response.isSuccessful) {
            response.body()?.let {
                Log.e("NetworkApiCall", response.body().toString())
                emit(IOResult.OnSuccess(it))
            }
                ?: emit(IOResult.OnFailed(Errors.NetworkError(message = "API call successful but empty response body")))
            return@flow
        }
        emit(
            IOResult.OnFailed(
                Errors.NetworkError(
                    statusCode = response.code(),
                    message = response.errorBody()?.string()
                        ?.fromJson(type = BaseResponse::class.java)?.message ?: messageInCaseOfError
                )
            )
        )
        return@flow
    }.catch { e ->
        emit(IOResult.OnFailed(Errors.NetworkError(message = "Exception during network API call: ${e.message}")))
        return@catch
    }.retryWhen { cause, attempt ->
        if (!allowRetries || attempt > numberOfRetries || cause !is IOException) return@retryWhen false
        delay(delayDuration)
        delayDuration *= delayFactor
        return@retryWhen true
    }.flowOn(Dispatchers.IO)
}

suspend fun <T : Any> getViewStateFlowForNetworkCall(ioOperation: suspend () -> Flow<IOResult<T>>) =

    flow {
        emit(ViewModelState.LoadingState<T, Throwable>(refreshing = true))
        ioOperation().map {
            when (it) {
                is IOResult.OnSuccess -> ViewModelState.LoadedState(content = it.data)
                is IOResult.OnFailed -> ViewModelState.ErrorState<T, Throwable>(error = it.error)
            }
        }.collect {
            emit(it)
        }

    }.flowOn(Dispatchers.IO)

fun <A> String.fromJson(type: Class<A>): A {
    return Gson().fromJson(this, type)
}

fun <A> A.toJson(): String? {
    return Gson().toJson(this)
}