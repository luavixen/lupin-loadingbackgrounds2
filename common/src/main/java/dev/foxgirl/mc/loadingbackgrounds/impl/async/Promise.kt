package dev.foxgirl.mc.loadingbackgrounds.impl.async

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.function.BiConsumer
import java.util.function.BiFunction

class Promise<T>(private val future: CompletableFuture<T>) : Future<T> by future {

    constructor(value: T) : this(CompletableFuture.completedFuture(value))
    constructor(cause: Throwable) : this(CompletableFuture.failedFuture(cause))

    constructor(executor: Executor? = null, supplier: () -> T) : this(evaluatePromiseSupplier(executor, supplier))

    val result: Result<T>? get() {
        return if (isDone) {
            try {
                Result.success(get())
            } catch (cause: Throwable) {
                Result.failure(cause)
            }
        } else {
            null
        }
    }

    fun <U> then(executor: Executor? = null, function: (T) -> U): Promise<U> {
        return if (executor != null) {
            Promise(future.thenApplyAsync(function, executor))
        } else {
            Promise(future.thenApply(function))
        }
    }
    fun <U> thenCompose(executor: Executor? = null, function: (T) -> Promise<U>): Promise<U> {
        return if (executor != null) {
            Promise(future.thenComposeAsync({ function(it).future }, executor))
        } else {
            Promise(future.thenCompose { function(it).future })
        }
    }
    fun <U, V> thenCombine(other: Promise<U>, executor: Executor? = null, function: (T, U) -> V): Promise<V> {
        return if (executor != null) {
            Promise(future.thenCombineAsync(other.future, function, executor))
        } else {
            Promise(future.thenCombine(other.future, function))
        }
    }

    fun exceptionally(executor: Executor? = null, function: (Throwable) -> T): Promise<T> {
        return if (executor != null) {
            Promise(future.exceptionallyAsync(function, executor))
        } else {
            Promise(future.exceptionally(function))
        }
    }

    fun finally(executor: Executor? = null, function: (Result<T>) -> Unit): Promise<T> {
        val handler = BiConsumer<T?, Throwable?> { value, cause -> function(createResult(value, cause)) }
        return if (executor != null) {
            Promise(future.whenCompleteAsync(handler, executor))
        } else {
            Promise(future.whenComplete(handler))
        }
    }
    fun <U> handle(executor: Executor? = null, function: (Result<T>) -> U): Promise<U> {
        val handler = BiFunction<T?, Throwable?, U> { value, cause -> function(createResult(value, cause)) }
        return if (executor != null) {
            Promise(future.handleAsync(handler, executor))
        } else {
            Promise(future.handle(handler))
        }
    }

    companion object {

        private fun <T> evaluatePromiseSupplier(executor: Executor?, supplier: () -> T): CompletableFuture<T> {
            return if (executor != null) {
                CompletableFuture.supplyAsync(supplier, executor)
            } else {
                try {
                    CompletableFuture.completedFuture(supplier())
                } catch (cause: Throwable) {
                    CompletableFuture.failedFuture(cause)
                }
            }
        }

        private fun <T> createResult(value: T?, cause: Throwable?): Result<T> {
            return if (cause != null) { Result.failure(cause) } else { Result.success(value) } as Result<T>
        }

        private class Waiter<T>(promises: Array<out Promise<T>>) : CompletableFuture<List<Result<T>>>() {
            private val results = arrayOfNulls<Result<T>>(promises.size)
            private var count = 0
            init {
                for ((i, promise) in promises.withIndex()) {
                    promise.finally { result ->
                        val complete = synchronized(this@Waiter) {
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
            return Promise(Waiter(promises))
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

        fun <T> settled(vararg promises: Promise<T>) = waitForResults(promises)
        fun <T> settled(promises: Collection<Promise<T>>) = waitForResults(promises.toTypedArray())

        fun <T> all(vararg promises: Promise<T>) = flattenResults(waitForResults(promises))
        fun <T> all(promises: Collection<Promise<T>>) = flattenResults(waitForResults(promises.toTypedArray()))

    }

}
