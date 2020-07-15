package java_util

import arrow.Kind
import arrow.core.toT
import arrow.data.ListK
import arrow.data.k
import arrow.data.traverse
import arrow.optics.Optional
import arrow.optics.POptional
import arrow.optics.Traversal
import arrow.optics.typeclasses.Each
import arrow.optics.typeclasses.FilterIndex
import arrow.optics.typeclasses.Index
import arrow.syntax.either.left
import arrow.syntax.either.right
import arrow.typeclasses.Applicative

interface ListEachInstance<A> : Each<List<A>, A> {
    override fun each() = object : Traversal<List<A>, A> {
        override fun <F> modifyF(FA: Applicative<F>, s: List<A>, f: (A) -> Kind<F, A>): Kind<F, List<A>> =
                ListK.traverse().traverse(s.k(), f, FA).let {
                    FA.map(it) {
                        it.list
                    }
                }
    }
}

object ListEachInstanceImplicits {
    @JvmStatic
    fun <A> instance(): Each<List<A>, A> = object : ListEachInstance<A> {}
}

interface ListFilterIndexInstance<A> : FilterIndex<List<A>, Int, A> {
    override fun filter(p: (Int) -> Boolean): Traversal<List<A>, A> = object : Traversal<List<A>, A> {
        override fun <F> modifyF(FA: Applicative<F>, s: List<A>, f: (A) -> Kind<F, A>): Kind<F, List<A>> =
                ListK.traverse().traverse(s.mapIndexed { index, a -> a toT index }.k(), { (a, j) ->
                    if (p(j)) f(a) else FA.pure(a)
                }, FA).let {
                    FA.map(it) {
                        it.list
                    }
                }
    }
}

object ListFilterIndexInstanceImplicits {
    @JvmStatic
    fun <A> instance(): FilterIndex<List<A>, Int, A> = object : ListFilterIndexInstance<A> {}
}

interface ListIndexInstance<A> : Index<List<A>, Int, A> {
    override fun index(i: Int): Optional<List<A>, A> = POptional(
            getOrModify = { it.getOrNull(i)?.right() ?: it.left() },
            set = { a -> { l -> l.mapIndexed { index: Int, aa: A -> if (index == i) a else aa } } }
    )
}

object ListIndexInstanceImplicits {
    @JvmStatic
    fun <A> instance(): Index<List<A>, Int, A> = object : ListIndexInstance<A> {}
}
