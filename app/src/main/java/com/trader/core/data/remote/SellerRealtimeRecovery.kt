package com.trader.core.data.remote

import android.util.Log
import com.google.firebase.database.DatabaseError
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen

/** Terminal SDK callback, retaining its code without exposing remote messages to UI/logs. */
class SellerRealtimeException(val databaseCode: Int, cause: Throwable? = null) :
    RuntimeException("Seller realtime listener failed (code=$databaseCode)", cause)

/**
 * Applied inside each merchant's collectLatest boundary. Retry transient SDK failures
 * twice (500ms,1000ms); never retry denied credentials or fabricate an empty snapshot.
 * On exhaustion, stop this listener while keeping the merchant observer alive. This
 * is bounded client recovery, not server authorization or a user-visible sync-status API.
 */
internal fun <T> Flow<T>.recoverSellerRealtime(): Flow<T> = retryWhen { failure, attempt ->
    val code = (failure as? SellerRealtimeException)?.databaseCode
    val transient = code == DatabaseError.DISCONNECTED || code == DatabaseError.NETWORK_ERROR ||
        code == DatabaseError.UNAVAILABLE || (failure is FirebaseFirestoreException && failure.code in setOf(
            FirebaseFirestoreException.Code.UNAVAILABLE, FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED, FirebaseFirestoreException.Code.ABORTED))
    if (transient && attempt < 2) {
        delay(500L shl attempt.toInt())
        true
    } else false
}.catch { failure ->
    if (failure is CancellationException) throw failure
    val code = when (failure) {
        is SellerRealtimeException -> failure.databaseCode.toString()
        is FirebaseFirestoreException -> failure.code.name
        else -> throw failure
    }
    Log.w("SellerRealtime", "Realtime listener stopped (code=$code); next merchant session can resubscribe")
}
