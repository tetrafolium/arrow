package arrow.typeclasses

import arrow.Kind
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn

interface BindingInContextContinuation<in T> : Continuation<T> {
    fun await(): Throwable?
}

@RestrictsSuspension
open class MonadContinuation<F, A>(M: Monad<F>, override val context: CoroutineContext = EmptyCoroutineContext) :
    Continuation<Kind<F, A>>, Monad<F> by M {

    override fun resume(value: Kind<F, A>) {
        returnedMonad = value
    }

    override fun resumeWithException(exception: Throwable) {
        throw exception
    }

    protected fun bindingInContextContinuation(context: CoroutineContext): BindingInContextContinuation<Kind<F, A>> =
        object : BindingInContextContinuation<Kind<F, A>> {
            val latch: CountDownLatch = CountDownLatch(1)

            var error: Throwable? = null

            override fun await() = latch.await().let { error }

            override val context: CoroutineContext = context

            override fun resume(value: Kind<F, A>) {
                returnedMonad = value
                latch.countDown()
            }

            override fun resumeWithException(exception: Throwable) {
                error = exception
                latch.countDown()
            }
        }

    protected lateinit var returnedMonad: Kind<F, A>

    open fun returnedMonad(): Kind<F, A> = returnedMonad

    suspend fun <B> Kind<F, B>.bind(): B = bind { this }

    suspend fun <B> (() -> B).bindIn(context: CoroutineContext): B =
        bindIn(context, this)

    open suspend fun <B> bind(m: () -> Kind<F, B>): B = suspendCoroutineOrReturn { c ->
        val labelHere = c.stackLabels // save the whole coroutine stack labels
        returnedMonad = flatMap(
            m(),
            { x: B ->
                c.stackLabels = labelHere
                c.resume(x)
                returnedMonad
            }
        )
        COROUTINE_SUSPENDED
    }

    open suspend fun <B> bindIn(context: CoroutineContext, m: () -> B): B = suspendCoroutineOrReturn { c ->
        val labelHere = c.stackLabels // save the whole coroutine stack labels
        val monadCreation: suspend () -> Kind<F, A> = {
            flatMap(
                pure(m()),
                { xx: B ->
                    c.stackLabels = labelHere
                    c.resume(xx)
                    returnedMonad
                }
            )
        }
        val completion = bindingInContextContinuation(context)
        returnedMonad = flatMap(
            pure(Unit),
            {
                monadCreation.startCoroutine(completion)
                val error = completion.await()
                if (error != null) {
                    throw error
                }
                returnedMonad
            }
        )
        COROUTINE_SUSPENDED
    }

    @Deprecated("Yielding in comprehensions isn't required anymore", ReplaceWith("b"))
    fun <B> yields(b: B): B = b

    @Deprecated("Yielding in comprehensions isn't required anymore", ReplaceWith("b()"))
    fun <B> yields(b: () -> B): B = b()
}
