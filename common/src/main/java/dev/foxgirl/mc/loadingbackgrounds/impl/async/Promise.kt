package dev.foxgirl.mc.loadingbackgrounds.impl.async

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.function.BiConsumer
import java.util.function.BiFunction

/**
 * Represents an abstraction over a [CompletableFuture] that simplifies the interface
 * while providing additional utilities for asynchronous composition and result handling.
 *
 * @param T The type of result that this promise will eventually provide.
 * @author Lua MacDougall <lua@foxgirl.dev>
 */
class Promise<T>(private val future: CompletableFuture<T>) : Future<T> by future {

    /**
     * Constructs a [Promise] that has already resolved with the provided value.
     *
     * @param value The value to wrap as a promise, marking it as already completed.
     */
    constructor(value: T) : this(CompletableFuture.completedFuture(value))

    /**
     * Constructs a [Promise] that has already rejected with the provided cause.
     *
     * @param cause The [Throwable] to wrap as a promise, marking it as already completed.
     */
    constructor(cause: Throwable) : this(CompletableFuture.failedFuture(cause))

    /**
     * Constructs a new [Promise] with the provided supplier function.
     *
     * @param executor The executor to run the supplier function.
     * @param supplier The function that provides the value to resolve the promise.
     */
    constructor(executor: Executor, supplier: () -> T) : this(evaluatePromiseSupplier(executor, supplier))

    /**
     * Constructs a new [Promise], initializing it with a consumer function that
     * accepts a [CompletableFuture] which should be used to resolve the promise.
     *
     * @param consumer
     *   A function that takes a [CompletableFuture] and defines how the promise should be resolved or rejected.
     *   If the consumer throws an exception, the promise will be rejected with that exception.
     */
    constructor(consumer: (CompletableFuture<T>) -> Unit) : this(evaluatePromiseConsumer(consumer))

    /**
     * Indicates whether the promise has completed, either resolved or rejected.
     */
    val completed: Boolean get() = future.isDone

    /**
     * If this promise has completed, this is a [Result] containing the resolved value or rejected error.
     * Otherwise, this variable will be `null`.
     */
    val result: Result<T>? get() {
        return if (completed) {
            try {
                Result.success(get())
            } catch (cause: Throwable) {
                Result.failure(cause)
            }
        } else {
            null
        }
    }

    /**
     * Executes a function with the resolved value of the promise.
     *
     * If an executor is provided, the function will execute asynchronously using it.
     * Otherwise, the function execution will be done synchronously.
     *
     * @param executor The executor to use for asynchronous execution. Defaults to `null`.
     * @param function The function to apply to the current promise's value.
     * @return A new promise that resolves to the result of the applied function.
     */
    fun <U> then(executor: Executor? = null, function: (T) -> U): Promise<U> {
        return if (executor != null) {
            Promise(future.thenApplyAsync(function, executor))
        } else {
            Promise(future.thenApply(function))
        }
    }

    /**
     * Executes a function that returns a new promise, composing it with the current promise.
     *
     * If an executor is provided, the function will execute asynchronously using it.
     * Otherwise, the function execution will be performed synchronously.
     *
     * @param executor The executor to use for asynchronous execution. Defaults to `null`.
     * @param function The function to apply to the current promise's value. This function returns a new promise.
     * @return A new promise that resolves to the resolved value of the promise returned by the applied function.
     */
    fun <U> thenCompose(executor: Executor? = null, function: (T) -> Promise<U>): Promise<U> {
        return if (executor != null) {
            Promise(future.thenComposeAsync({ function(it).future }, executor))
        } else {
            Promise(future.thenCompose { function(it).future })
        }
    }

    /**
     * Combines the result of this promise with another promise using the supplied function.
     *
     * If an executor is provided, the combination function will execute asynchronously using it.
     * Otherwise, the combination execution will be performed synchronously.
     *
     * @param other The other promise to combine with.
     * @param executor The executor to use for asynchronous execution. Defaults to `null`.
     * @param function The function that takes the value of this promise and the value of the other promise and produces a combined result.
     * @return A new promise that resolves to the combined result of this promise and the other promise.
     */
    fun <U, V> thenCombine(other: Promise<U>, executor: Executor? = null, function: (T, U) -> V): Promise<V> {
        return if (executor != null) {
            Promise(future.thenCombineAsync(other.future, function, executor))
        } else {
            Promise(future.thenCombine(other.future, function))
        }
    }

    /**
     * Handles exceptions that occur during promise execution by providing a recovery function.
     *
     * If an executor is provided, the recovery function will execute asynchronously using it.
     * Otherwise, the recovery function execution will be performed synchronously.
     *
     * @param executor The executor to use for asynchronous execution. Defaults to `null`.
     * @param function
     *   The function to apply when an exception occurs.
     *   This function receives the exception and should return a value of type [T] to recover from the error.
     * @return
     *   A new promise that resolves to either the original result (if no exception occurred)
     *   or the result of the recovery function (if an exception was handled).
     */
    fun exceptionally(executor: Executor? = null, function: (Throwable) -> T): Promise<T> {
        return if (executor != null) {
            Promise(future.exceptionallyAsync(function, executor))
        } else {
            Promise(future.exceptionally(function))
        }
    }

    /**
     * Executes a function when the promise completes, regardless of whether it resolved or rejected.
     * This is useful for cleanup operations or logging.
     *
     * If an executor is provided, the function will execute asynchronously using it.
     * Otherwise, the function execution will be performed synchronously.
     *
     * @param executor The executor to use for asynchronous execution. Defaults to `null`.
     * @param function
     *   The function to execute when the promise completes.
     *   This function receives a [Result] containing either the resolved value or rejected error.
     * @return A new promise that completes with the same result as the original promise.
     */
    fun finally(executor: Executor? = null, function: (Result<T>) -> Unit): Promise<T> {
        val handler = BiConsumer<T?, Throwable?> { value, cause -> function(createResult(value, cause)) }
        return if (executor != null) {
            Promise(future.whenCompleteAsync(handler, executor))
        } else {
            Promise(future.whenComplete(handler))
        }
    }

    /**
     * Transforms the result of the promise (whether resolved or rejected) into a new value.
     *
     * If an executor is provided, the transformation function will execute asynchronously using it.
     * Otherwise, the transformation execution will be performed synchronously.
     *
     * @param executor The executor to use for asynchronous execution. Defaults to `null`.
     * @param function
     *   The function to apply to the promise result.
     *   This function receives a [Result] containing either the resulting value or error, and returns a new value of type [U].
     * @return A new promise that completes with the result of the transformation function.
     */
    fun <U> handle(executor: Executor? = null, function: (Result<T>) -> U): Promise<U> {
        val handler = BiFunction<T?, Throwable?, U> { value, cause -> function(createResult(value, cause)) }
        return if (executor != null) {
            Promise(future.handleAsync(handler, executor))
        } else {
            Promise(future.handle(handler))
        }
    }

    companion object {

        private fun <T> evaluatePromiseSupplier(executor: Executor, supplier: () -> T): CompletableFuture<T> {
            return CompletableFuture.supplyAsync(supplier, executor)
        }

        private fun <T> evaluatePromiseConsumer(consumer: (CompletableFuture<T>) -> Unit): CompletableFuture<T> {
            return CompletableFuture<T>().also { future ->
                try {
                    consumer(future)
                } catch (cause: Throwable) {
                    future.completeExceptionally(cause)
                }
            }
        }

        private fun <T> createResult(value: T?, cause: Throwable?): Result<T> {
            return if (cause != null) { Result.failure(cause) } else { Result.success(value) } as Result<T>
        }

        private class AwaitingCompletableFuture<T>(promises: Array<out Promise<T>>) : CompletableFuture<List<Result<T>>>() {
            private val results = arrayOfNulls<Result<T>>(promises.size)
            private var count = 0
            init {
                for ((i, promise) in promises.withIndex()) {
                    promise.finally { result ->
                        val complete = synchronized(this@AwaitingCompletableFuture) {
                            results[i] = result
                            count++
                            count >= results.size
                        }
                        if (complete) {
                            complete(results.asList() as List<Result<T>>)
                        }
                    }
                }
            }
        }

        private fun <T> waitForResults(promises: Array<out Promise<T>>): Promise<List<Result<T>>> {
            return Promise(AwaitingCompletableFuture(promises))
        }

        private fun <T> flattenResults(promise: Promise<List<Result<T>>>): Promise<List<T>> {
            return promise.then { results ->
                val values = ArrayList<T>(results.size)
                val iterator = results.iterator()
                while (iterator.hasNext()) {
                    val value = iterator.next().getOrElse { causeFirst ->
                        throw if (iterator.hasNext()) {
                            RuntimeException(causeFirst).also {
                                do {
                                    val causeNext = iterator.next().exceptionOrNull()
                                    if (causeNext != null) it.addSuppressed(causeNext)
                                } while (iterator.hasNext())
                            }
                        } else {
                            causeFirst
                        }
                    }
                    values.add(value)
                }
                values
            }
        }

        /**
         * Waits for all provided promises to complete and returns their results, regardless of success or failure.
         * This function will not reject even if some promises fail - instead, it collects all results
         * (both successful values and exceptions) and returns them as a list of [Result] objects.
         *
         * @param promises The collection of promises to wait for completion.
         * @return A new promise that resolves to a list of [Result] objects, one for each input promise.
         */
        fun <T> settled(promises: Collection<Promise<T>>) = waitForResults(promises.toTypedArray())

        /**
         * Waits for all provided promises to complete successfully and returns their values.
         * If any promise fails, this function will reject with an exception. If multiple promises fail,
         * the first exception encountered will be thrown with additional failures added as suppressed exceptions.
         *
         * @param promises The collection of promises that must all succeed.
         * @return A new promise that resolves to a list of values from all successful promises, or rejects if any promise fails.
         */
        fun <T> all(promises: Collection<Promise<T>>) = flattenResults(waitForResults(promises.toTypedArray()))

    }

}
