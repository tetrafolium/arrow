package arrow.optics

import arrow.core.None
import arrow.core.Option
import arrow.data.Const
import arrow.typeclasses.Monoid

@PublishedApi internal object AndMonoid : Monoid<Boolean> {
    override fun combine(a: Boolean, b: Boolean): Boolean = a && b
    override fun empty(): Boolean = true
}

internal sealed class First
internal sealed class Last

@PublishedApi internal fun <A> firstOptionMonoid() = object : Monoid<Const<Option<A>, First>> {

    override fun empty() = Const<Option<A>, First>(None)

    override fun combine(a: Const<Option<A>, First>, b: Const<Option<A>, First>) =
            if (a.value.fold({ false }, { true })) a else b
}

internal fun <A> lastOptionMonoid() = object : Monoid<Const<Option<A>, Last>> {

    override fun empty() = Const<Option<A>, Last>(None)

    override fun combine(a: Const<Option<A>, Last>, b: Const<Option<A>, Last>) =
            if (b.value.fold({ false }, { true })) b else a
}
