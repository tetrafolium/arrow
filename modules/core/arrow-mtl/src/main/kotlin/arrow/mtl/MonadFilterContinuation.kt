package arrow.mtl

import arrow.Kind
import arrow.typeclasses.MonadContinuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.RestrictsSuspension

@RestrictsSuspension
open class MonadFilterContinuation<F, A>(val MF: MonadFilter<F>, override val context: CoroutineContext = EmptyCoroutineContext) :
    MonadContinuation<F, A>(MF) {

    /**
     * marker exception that interrupts the coroutine flow and gets captured
     * to provide the monad empty value
     */
    private object PredicateInterrupted : RuntimeException()

    override fun resumeWithException(exception: Throwable) {
        when (exception) {
            is PredicateInterrupted -> returnedMonad = MF.empty()
            else -> super.resumeWithException(exception)
        }
    }

    /**
     * Short circuits monadic bind if `predicate == false` return the
     * monad `empty` value.
     */
    fun continueIf(predicate: Boolean) {
        if (!predicate) throw PredicateInterrupted
    }

    /**
     * Binds only if the given predicate matches the inner value otherwise binds into the Monad `empty()` value
     * on `MonadFilter` instances
     */
    suspend fun <B> Kind<F, B>.bindWithFilter(f: (B) -> Boolean): B {
        val b: B = bind { this }
        return if (f(b)) b else bind { MF.empty<B>() }
    }
}
