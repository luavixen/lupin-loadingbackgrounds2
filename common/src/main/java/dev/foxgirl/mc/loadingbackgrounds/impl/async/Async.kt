/**
 *
 */

package dev.foxgirl.mc.loadingbackgrounds.impl.async

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.*

object Async {

    private class CompletableFutureContinuation<T>(override val context: CoroutineContext) : CompletableFuture<T>(), Continuation<T> {

        fun startCoroutine(coroutine: suspend () -> T): CompletableFutureContinuation<T> {
            coroutine.startCoroutine(this)
            return this
        }

        override fun resumeWith(result: Result<T>) {
            result.fold(::complete, ::completeExceptionally)
        }

    }

    fun <T> go(context: CoroutineContext = EmptyCoroutineContext, coroutine: suspend () -> T): Promise<T> {
        return Promise(CompletableFutureContinuation<T>(context).startCoroutine(coroutine))
    }

    suspend fun <T> await(promise: Promise<T>): T {
        return suspendCoroutine { promise.finally { result -> it.resumeWith(result) } }
    }

    suspend fun <T> await(vararg promises: Promise<T>): List<T> = await(promises.toList())
    suspend fun <T> await(promises: Collection<Promise<T>>): List<T> = await(Promise.all(promises))

}
