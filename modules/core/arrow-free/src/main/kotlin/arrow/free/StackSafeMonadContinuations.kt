package arrow.free

import arrow.Kind
import arrow.typeclasses.Monad
import arrow.typeclasses.stackLabels
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn

@RestrictsSuspension
open class StackSafeMonadContinuation<F, A>(M: Monad<F>, override val context: CoroutineContext = EmptyCoroutineContext) :
    Continuation<Free<F, A>>, Monad<F> by M {

    override fun resume(value: Free<F, A>) {
        returnedMonad = value
    }

    override fun resumeWithException(exception: Throwable) {
        throw exception
    }

    protected lateinit var returnedMonad: Free<F, A>

    internal fun returnedMonad(): Free<F, A> = returnedMonad

    suspend fun <B> Kind<F, B>.bind(): B = bind { Free.liftF(this) }

    suspend fun <B> Free<F, B>.bind(): B = bind { this }

    suspend fun <B> bind(m: () -> Free<F, B>): B = suspendCoroutineOrReturn { c ->
        val labelHere = c.stackLabels // save the whole coroutine stack labels
        returnedMonad = m().flatMap { z ->
            c.stackLabels = labelHere
            c.resume(z)
            returnedMonad
        }
        COROUTINE_SUSPENDED
    }

    @Deprecated("Yielding in comprehensions isn't required anymore", ReplaceWith("b"))
    infix fun <B> yields(b: B): B = b

    @Deprecated("Yielding in comprehensions isn't required anymore", ReplaceWith("b()"))
    infix fun <B> yields(b: () -> B): B = b()
}

/**
 * Entry point for monad bindings which enables for comprehension. The underlying impl is based on coroutines.
 * A coroutine is initiated and inside [StackSafeMonadContinuation] suspended yielding to [flatMap]. Once all the flatMap binds are completed
 * the underlying monad is returned from the act of executing the coroutine.
 *
 * This combinator ultimately returns computations lifting to [Free] to automatically for comprehend in a stack-safe way
 * over any stack-unsafe monads.
 */
fun <F, B> Monad<F>.bindingStackSafe(c: suspend StackSafeMonadContinuation<F, *>.() -> B):
    Free<F, B> {
        val continuation = StackSafeMonadContinuation<F, B>(this)
        val wrapReturn: suspend StackSafeMonadContinuation<F, *>.() -> Free<F, B> = { Free.pure(c()) }
        wrapReturn.startCoroutine(continuation, continuation)
        return continuation.returnedMonad()
    }
