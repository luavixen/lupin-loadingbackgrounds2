package dev.foxgirl.mc.loadingbackgrounds.impl.async

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.*

/**
 * Provides async/await functionality in Kotlin using coroutines without a
 * coroutine runtime, powered by the [Promise] class.
 *
 * @author Lua MacDougall <lua@foxgirl.dev>
 */
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

    /**
     * Launches a coroutine and returns a [Promise] that will resolve to the coroutine's result.
     *
     * @param context The coroutine context to be used. Defaults to [EmptyCoroutineContext] if not specified.
     * @param coroutine The suspending function to be run as a coroutine.
     * @return A promise representing the result of the asynchronous operation.
     */
    fun <T> go(context: CoroutineContext = EmptyCoroutineContext, coroutine: suspend () -> T): Promise<T> {
        return Promise(CompletableFutureContinuation<T>(context).startCoroutine(coroutine))
    }

    /**
     * Suspends the coroutine until the given [Promise] has completed and returns the value.
     *
     * @param promise The promise to await.
     * @return The resolved value from the promise if successful.
     * @throws Throwable If the promise rejects, the error will be propagated as a coroutine exception.
     */
    suspend fun <T> await(promise: Promise<T>): T {
        return suspendCoroutine { promise.finally { result -> it.resumeWith(result) } }
    }

    /**
     * Suspends the coroutine until all the given [Promise]s have completed.
     * Returns a list of all the values in the same order.
     *
     * @param promises The promises to await as a variable number of arguments.
     * @return A list of the resolved values from the promises if successful.
     * @throws Throwable If one or more promises reject, the error(s) will be propagated as a coroutine exception.
     */
    suspend fun <T> await(vararg promises: Promise<T>): List<T> = await(promises.toList())
    /**
     * Suspends the coroutine until all the given [Promise]s have completed.
     * Returns a list of all the values in the same order.
     *
     * @param promises The promises to await as a collection.
     * @return A list of the resolved values from the promises if successful.
     * @throws Throwable If one or more promises reject, the error(s) will be propagated as a coroutine exception.
     */
    suspend fun <T> await(promises: Collection<Promise<T>>): List<T> = await(Promise.all(promises))

}
