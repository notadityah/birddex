package com.birddex.app.data.network

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import retrofit2.Response

/** Outcome of a BirdDex API call (non-auth endpoints; auth has its own AuthResult). */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

sealed interface ApiError {
    /** No connection, timeout, DNS failure… */
    data object Network : ApiError
    /** Signed out or session expired. UnauthorizedInterceptor has already signed the app out. */
    data object Unauthorized : ApiError
    /** Any other HTTP error; [message] is the API's own `error`/`message` text when it sent one. */
    data class Http(val code: Int, val message: String?) : ApiError
    /** The response couldn't be read (wrong shape, HTML page, …). */
    data class Unexpected(val detail: String?) : ApiError
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}

/** Runs one API call that returns its body directly and turns every outcome into an [ApiResult]. */
suspend fun <T> apiCall(json: Json, request: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(request())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    ApiResult.Failure(httpError(e.response(), json))
} catch (e: UnexpectedHtmlResponseException) {
    ApiResult.Failure(ApiError.Unexpected(e.message))
} catch (e: IOException) {
    ApiResult.Failure(ApiError.Network)
} catch (e: SerializationException) {
    ApiResult.Failure(ApiError.Unexpected(e.message))
} catch (e: IllegalArgumentException) {
    ApiResult.Failure(ApiError.Unexpected(e.message))
}

/** The API answers errors as `{"error": "..."}` (Hono handlers) or `{"message": "..."}`. */
fun httpError(response: Response<*>?, json: Json): ApiError {
    val code = response?.code() ?: return ApiError.Unexpected("No response")
    if (code == 401) return ApiError.Unauthorized
    val message = response.errorBody()?.string()?.let { body ->
        runCatching {
            val obj = json.parseToJsonElement(body) as? JsonObject
            (obj?.get("error") ?: obj?.get("message"))?.jsonPrimitive?.content
        }.getOrNull()
    }
    return ApiError.Http(code, message)
}
