package arrow.optics.typeclasses

import arrow.Kind
import arrow.TC
import arrow.core.Predicate
import arrow.core.Tuple2
import arrow.optics.Iso
import arrow.optics.Traversal
import arrow.typeclass
import arrow.typeclasses.Applicative
import arrow.typeclasses.Traverse

/**
 * [FilterIndex] provides a [Traversal] for a structure [S] with all its foci [A] whose index [I] satisfies a predicate.
 *
 * @param S source of [Traversal]
 * @param I index that uniquely identifies every focus of the [Traversal]
 * @param A focus that is supposed to be unique for a given pair [S] and [I]
 */
@typeclass
interface FilterIndex<S, I, A> : TC {

    /**
     * Filter the foci [A] of a [Traversal] with the predicate [p].
     */
    fun filter(p: Predicate<I>): Traversal<S, A>

    companion object {

        /**
         * Lift an instance of [FilterIndex] using an [Iso]
         */
        fun <S, A, I, B> fromIso(FI: FilterIndex<A, I, B>, iso: Iso<S, A>): FilterIndex<S, I, B> = object : FilterIndex<S, I, B> {
            override fun filter(p: Predicate<I>): Traversal<S, B> =
                iso compose FI.filter(p)
        }

        /**
         * Create an instance of [FilterIndex] from a [Traverse] and a function `Kind<S, A>) -> Kind<S, Tuple2<A, Int>>`
         */
        fun <S, A> fromTraverse(zipWithIndex: (Kind<S, A>) -> Kind<S, Tuple2<A, Int>>, traverse: Traverse<S>): FilterIndex<Kind<S, A>, Int, A> = object : FilterIndex<Kind<S, A>, Int, A> {
            override fun filter(p: Predicate<Int>): Traversal<Kind<S, A>, A> = object : Traversal<Kind<S, A>, A> {
                override fun <F> modifyF(FA: Applicative<F>, s: Kind<S, A>, f: (A) -> Kind<F, A>): Kind<F, Kind<S, A>> =
                    traverse.traverse(
                        zipWithIndex(s),
                        { (a, j) ->
                            if (p(j)) f(a) else FA.pure(a)
                        },
                        FA
                    )
            }
        }

        /**
         * Filter the foci [A] of a [Traversal] with the predicate [p] given an instance [FilterIndex] [FI].
         */
        fun <S, I, A> filterIndex(FI: FilterIndex<S, I, A>, p: Predicate<I>): Traversal<S, A> = FI.filter(p)
    }
}

/**
 * Lift an instance of [FilterIndex] using an [Iso]
 */
inline fun <S, reified A, reified I, reified B> FilterIndex.Companion.fromIso(iso: Iso<S, A>, FI: FilterIndex<A, I, B> = filterIndex()): FilterIndex<S, I, B> =
    fromIso(FI, iso)

/**
 * Create an instance of [FilterIndex] from a [Traverse] and a function `Kind<S, A>) -> Kind<S, Tuple2<A, Int>>`
 */
inline fun <reified S, A> FilterIndex.Companion.fromTraverse(traverse: Traverse<S> = arrow.typeclasses.traverse(), noinline zipWithIndex: (Kind<S, A>) -> Kind<S, Tuple2<A, Int>>): FilterIndex<Kind<S, A>, Int, A> =
    fromTraverse(zipWithIndex, traverse)

/**
 * Filter the foci [A] of a [Traversal] with the predicate [p] given an instance [FilterIndex] [FI].
 */
inline fun <reified S, reified I, reified A> FilterIndex.Companion.filterIndex(FI: FilterIndex<S, I, A> = filterIndex(), dummy: Unit = Unit, noinline p: Predicate<I>): Traversal<S, A> = FilterIndex.filterIndex(FI, p)
