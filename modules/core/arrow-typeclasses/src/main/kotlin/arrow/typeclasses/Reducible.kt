package arrow.typeclasses

import arrow.*
import arrow.core.*

/**
 * Data structures that can be reduced to a summary value.
 *
 * Reducible is like a non-empty Foldable. In addition to the fold methods it provides reduce
 * methods which do not require an initial value.
 *
 * In addition to the methods needed by `Foldable`, `Reducible` is implemented in terms of two methods:
 *
 *  - reduceLeftTo(fa)(f)(g) eagerly reduces with an additional mapping function
 *  - reduceRightTo(fa)(f)(g) lazily reduces with an additional mapping function
 */
@typeclass
interface Reducible<F> : Foldable<F>, TC {

    /**
     * Left-associative reduction on F using the function f.
     *
     * Implementations should override this method when possible.
     */
    fun <A> reduceLeft(fa: Kind<F, A>, f: (A, A) -> A): A = reduceLeftTo(fa, { a -> a }, f)

    /**
     * Right-associative reduction on F using the function f.
     */
    fun <A> reduceRight(fa: Kind<F, A>, f: (A, Eval<A>) -> Eval<A>): Eval<A> = reduceRightTo(fa, { a -> a }, f)

    /**
     * Apply f to the "initial element" of fa and combine it with every other value using
     * the given function g.
     */
    fun <A, B> reduceLeftTo(fa: Kind<F, A>, f: (A) -> B, g: (B, A) -> B): B

    override fun <A, B> reduceLeftToOption(fa: Kind<F, A>, f: (A) -> B, g: (B, A) -> B): Option<B> = Some(reduceLeftTo(fa, f, g))

    /**
     * Apply f to the "initial element" of fa and lazily combine it with every other value using the
     * given function g.
     */
    fun <A, B> reduceRightTo(fa: Kind<F, A>, f: (A) -> B, g: (A, Eval<B>) -> Eval<B>): Eval<B>

    override fun <A, B> reduceRightToOption(fa: Kind<F, A>, f: (A) -> B, g: (A, Eval<B>) -> Eval<B>): Eval<Option<B>> =
        reduceRightTo(fa, f, g).map({ Some(it) })

    override fun <A> isEmpty(fa: Kind<F, A>): Boolean = false

    override fun <A> nonEmpty(fa: Kind<F, A>): Boolean = true
}

/**
 * Reduce a F<A> value using the given Semigroup<A>.
 */
inline fun <F, reified A> Reducible<F>.reduce(fa: Kind<F, A>, SA: Semigroup<A> = semigroup()): A = reduceLeft(fa, { a, b -> SA.combine(a, b) })

/**
 * Reduce a F<G<A>> value using SemigroupK<G>, a universal semigroup for G<_>.
 *
 * This method is a generalization of reduce.
 */
inline fun <F, reified G, A> Reducible<F>.reduceK(fga: Kind<F, Kind<G, A>>, SGKG: SemigroupK<G> = semigroupK()): Kind<G, A> = reduce(fga, SGKG.algebra())

/**
 * Apply f to each element of fa and combine them using the given Semigroup<B>.
 */
inline fun <F, A, reified B> Reducible<F>.reduceMap(fa: Kind<F, A>, noinline f: (A) -> B, SB: Semigroup<B> = semigroup()): B =
    reduceLeftTo(fa, f, { b, a -> SB.combine(b, f(a)) })

/**
 * This class defines a Reducible<F> in terms of a Foldable<G> together with a split method, F<A> -> (A, G<A>).
 *
 * This class can be used on any type where the first value (A) and the "rest" of the values (G<A>) can be easily found.
 */
abstract class NonEmptyReducible<F, G> : Reducible<F> {

    abstract fun FG(): Foldable<G>

    abstract fun <A> split(fa: Kind<F, A>): Tuple2<A, Kind<G, A>>

    override fun <A, B> foldLeft(fa: Kind<F, A>, b: B, f: (B, A) -> B): B {
        val (a, ga) = split(fa)
        return FG().foldLeft(ga, f(b, a), f)
    }

    override fun <A, B> foldRight(fa: Kind<F, A>, lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
        Eval.Always({ split(fa) }).flatMap { (a, ga) -> f(a, FG().foldRight(ga, lb, f)) }

    override fun <A, B> reduceLeftTo(fa: Kind<F, A>, f: (A) -> B, g: (B, A) -> B): B {
        val (a, ga) = split(fa)
        return FG().foldLeft(ga, f(a), { bb, aa -> g(bb, aa) })
    }

    override fun <A, B> reduceRightTo(fa: Kind<F, A>, f: (A) -> B, g: (A, Eval<B>) -> Eval<B>): Eval<B> =
        Eval.Always({ split(fa) }).flatMap { (a, ga) ->
            FG().reduceRightToOption(ga, f, g).flatMap { option ->
                when (option) {
                    is Some<B> -> g(a, Eval.Now(option.t))
                    is None -> Eval.Later({ f(a) })
                }
            }
        }

    override fun <A> fold(ma: Monoid<A>, fa: Kind<F, A>): A {
        val (a, ga) = split(fa)
        return ma.combine(a, FG().fold(ma, ga))
    }

    override fun <A> find(fa: Kind<F, A>, f: (A) -> Boolean): Option<A> {
        val (a, ga) = split(fa)
        return if (f(a)) Some(a) else FG().find(ga, f)
    }

    override fun <A> exists(fa: Kind<F, A>, p: (A) -> Boolean): Boolean {
        val (a, ga) = split(fa)
        return p(a) || FG().exists(ga, p)
    }

    override fun <A> forall(fa: Kind<F, A>, p: (A) -> Boolean): Boolean {
        val (a, ga) = split(fa)
        return p(a) && FG().forall(ga, p)
    }
}

inline fun <reified F, reified G, A> NonEmptyReducible<F, G>.size(MB: Monoid<Long> = monoid(), fa: Kind<F, A>): Long {
    val (_, tail) = split(fa)
    return 1 + FG().size(MB, tail)
}

fun <F, G, A> NonEmptyReducible<F, G>.get(fa: Kind<F, A>, idx: Long): Option<A> = if (idx == 0L) Some(split(fa).a) else FG().get(split(fa).b, idx - 1L)

inline fun <F, reified G, A, B> NonEmptyReducible<F, G>.foldM(fa: Kind<F, A>, z: B, crossinline f: (B, A) -> Kind<G, B>, MG: Monad<G> = monad()): Kind<G, B> {
    val (a, ga) = split(fa)
    return MG.flatMap(f(z, a), { FG().foldM(ga, it, f) })
}
