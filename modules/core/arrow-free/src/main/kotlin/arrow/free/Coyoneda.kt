package arrow.free

import arrow.*
import arrow.typeclasses.Functor

private typealias AnyFunc = (Any?) -> Any?

@higherkind data class Coyoneda<F, P, A>(val pivot: Kind<F, P>, internal val ks: List<AnyFunc>) : CoyonedaOf<F, P, A>, CoyonedaKindedJ<F, P, A> {

    @Suppress("UNCHECKED_CAST")
    private val transform: (P) -> A = {
        var curr: Any? = it
        ks.forEach { curr = it(curr) }
        curr as A
    }

    fun lower(FF: Functor<F>): Kind<F, A> = FF.map(pivot, transform)

    @Suppress("UNCHECKED_CAST")
    fun <B> map(f: (A) -> B): Coyoneda<F, P, B> = Coyoneda(pivot, ks + f as AnyFunc)

    fun toYoneda(FF: Functor<F>): Yoneda<F, A> =
            object : Yoneda<F, A>() {
                override operator fun <B> invoke(f: (A) -> B): Kind<F, B> =
                        this@Coyoneda.map(f).lower(FF)
            }

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified U, A, B> invoke(fa: Kind<U, A>, noinline f: (A) -> B): Coyoneda<U, A, B> = unsafeApply(fa, listOf(f as AnyFunc))

        inline fun <reified U, A, B> unsafeApply(fa: Kind<U, A>, f: List<AnyFunc>): Coyoneda<U, A, B> = Coyoneda(fa, f)
    }
}
