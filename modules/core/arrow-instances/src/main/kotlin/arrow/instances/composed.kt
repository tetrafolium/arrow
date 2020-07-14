package arrow.instances

import arrow.*
import arrow.core.Eval
import arrow.typeclasses.*

interface ComposedFoldable<F, G> :
    Foldable<Nested<F, G>> {

    fun FF(): Foldable<F>

    fun GF(): Foldable<G>

    override fun <A, B> foldLeft(fa: Kind<Nested<F, G>, A>, b: B, f: (B, A) -> B): B =
        FF().foldLeft(fa.unnest(), b, { bb, aa -> GF().foldLeft(aa, bb, f) })

    fun <A, B> foldLC(fa: Kind<F, Kind<G, A>>, b: B, f: (B, A) -> B): B = foldLeft(fa.nest(), b, f)

    override fun <A, B> foldRight(fa: Kind<Nested<F, G>, A>, lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
        FF().foldRight(fa.unnest(), lb, { laa, lbb -> GF().foldRight(laa, lbb, f) })

    fun <A, B> foldRC(fa: Kind<F, Kind<G, A>>, lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> = foldRight(fa.nest(), lb, f)

    companion object {
        operator fun <F, G> invoke(FF: Foldable<F>, GF: Foldable<G>): ComposedFoldable<F, G> =
            object : ComposedFoldable<F, G> {
                override fun FF(): Foldable<F> = FF

                override fun GF(): Foldable<G> = GF
            }
    }
}

inline fun <F, reified G> Foldable<F>.compose(GT: Foldable<G> = foldable<G>()): ComposedFoldable<F, G> = object :
    ComposedFoldable<F, G> {
    override fun FF(): Foldable<F> = this@compose

    override fun GF(): Foldable<G> = GT
}

interface ComposedTraverse<F, G> :
    Traverse<Nested<F, G>>,
    ComposedFoldable<F, G> {

    fun FT(): Traverse<F>

    fun GT(): Traverse<G>

    fun GA(): Applicative<G>

    override fun FF(): Foldable<F> = FT()

    override fun GF(): Foldable<G> = GT()

    override fun <H, A, B> traverse(fa: Kind<Nested<F, G>, A>, f: (A) -> Kind<H, B>, HA: Applicative<H>): Kind<H, Kind<Nested<F, G>, B>> =
        HA.map(FT().traverse(fa.unnest(), { ga -> GT().traverse(ga, f, HA) }, HA), { it.nest() })

    fun <H, A, B> traverseC(fa: Kind<F, Kind<G, A>>, f: (A) -> Kind<H, B>, HA: Applicative<H>): Kind<H, Kind<Nested<F, G>, B>> = traverse(fa.nest(), f, HA)

    companion object {
        operator fun <F, G> invoke(
            FF: Traverse<F>,
            GF: Traverse<G>,
            GA: Applicative<G>
        ): ComposedTraverse<F, G> =
            object : ComposedTraverse<F, G> {
                override fun FT(): Traverse<F> = FF

                override fun GT(): Traverse<G> = GF

                override fun GA(): Applicative<G> = GA
            }
    }
}

inline fun <reified F, reified G> Traverse<F>.compose(GT: Traverse<G> = traverse<G>(), GA: Applicative<G> = applicative<G>()): Traverse<Nested<F, G>> =
    object :
        ComposedTraverse<F, G> {
        override fun FT(): Traverse<F> = this@compose

        override fun GT(): Traverse<G> = GT

        override fun GA(): Applicative<G> = GA
    }

interface ComposedSemigroupK<F, G> : SemigroupK<Nested<F, G>> {

    fun F(): SemigroupK<F>

    override fun <A> combineK(x: Kind<Nested<F, G>, A>, y: Kind<Nested<F, G>, A>): Kind<Nested<F, G>, A> = F().combineK(x.unnest(), y.unnest()).nest()

    fun <A> combineKC(x: Kind<F, Kind<G, A>>, y: Kind<F, Kind<G, A>>): Kind<Nested<F, G>, A> = combineK(x.nest(), y.nest())

    companion object {
        operator fun <F, G> invoke(SF: SemigroupK<F>): SemigroupK<Nested<F, G>> =
            object : ComposedSemigroupK<F, G> {
                override fun F(): SemigroupK<F> = SF
            }
    }
}

inline fun <F, G> SemigroupK<F>.compose(): SemigroupK<Nested<F, G>> = object : ComposedSemigroupK<F, G> {
    override fun F(): SemigroupK<F> = this@compose
}

interface ComposedMonoidK<F, G> : MonoidK<Nested<F, G>>, ComposedSemigroupK<F, G> {

    override fun F(): MonoidK<F>

    override fun <A> empty(): Kind<Nested<F, G>, A> = F().empty<Kind<G, A>>().nest()

    fun <A> emptyC(): Kind<F, Kind<G, A>> = empty<A>().unnest()

    companion object {
        operator fun <F, G> invoke(MK: MonoidK<F>): MonoidK<Nested<F, G>> =
            object : ComposedMonoidK<F, G> {
                override fun F(): MonoidK<F> = MK
            }
    }
}

fun <F, G> MonoidK<F>.compose(): MonoidK<Nested<F, G>> = object : ComposedMonoidK<F, G> {
    override fun F(): MonoidK<F> = this@compose
}

interface ComposedFunctor<F, G> : Functor<Nested<F, G>> {
    fun F(): Functor<F>

    fun G(): Functor<G>

    override fun <A, B> map(fa: Kind<Nested<F, G>, A>, f: (A) -> B): Kind<Nested<F, G>, B> = F().map(fa.unnest(), { G().map(it, f) }).nest()

    fun <A, B> mapC(fa: Kind<F, Kind<G, A>>, f: (A) -> B): Kind<F, Kind<G, B>> = map(fa.nest(), f).unnest()

    companion object {
        operator fun <F, G> invoke(FF: Functor<F>, GF: Functor<G>): Functor<Nested<F, G>> =
            object : ComposedFunctor<F, G> {
                override fun F(): Functor<F> = FF

                override fun G(): Functor<G> = GF
            }
    }
}

inline fun <reified F, reified G> Functor<F>.compose(GF: Functor<G>): Functor<Nested<F, G>> = ComposedFunctor(this, GF)

interface ComposedApplicative<F, G> : Applicative<Nested<F, G>>, ComposedFunctor<F, G> {
    override fun F(): Applicative<F>

    override fun G(): Applicative<G>

    override fun <A, B> map(fa: Kind<Nested<F, G>, A>, f: (A) -> B): Kind<Nested<F, G>, B> = ap(fa, pure(f))

    override fun <A> pure(a: A): Kind<Nested<F, G>, A> = F().pure(G().pure(a)).nest()

    override fun <A, B> ap(fa: Kind<Nested<F, G>, A>, ff: Kind<Nested<F, G>, (A) -> B>):
        Kind<Nested<F, G>, B> = F().ap(fa.unnest(), F().map(ff.unnest(), { gfa: Kind<G, (A) -> B> -> { ga: Kind<G, A> -> G().ap(ga, gfa) } })).nest()

    fun <A, B> apC(fa: Kind<F, Kind<G, A>>, ff: Kind<F, Kind<G, (A) -> B>>): Kind<F, Kind<G, B>> = ap(fa.nest(), ff.nest()).unnest()

    companion object {
        operator fun <F, G> invoke(FF: Applicative<F>, GF: Applicative<G>):
            Applicative<Nested<F, G>> =
                object : ComposedApplicative<F, G> {
                    override fun F(): Applicative<F> = FF

                    override fun G(): Applicative<G> = GF
                }
    }
}

inline fun <reified F, reified G> Applicative<F>.compose(GA: Applicative<G> = applicative<G>()): Applicative<Nested<F, G>> = ComposedApplicative(this, GA)

interface ComposedAlternative<F, G> : Alternative<Nested<F, G>>, ComposedApplicative<F, G>, ComposedMonoidK<F, G> {
    override fun F(): Alternative<F>

    companion object {
        operator fun <F, G> invoke(AF: Alternative<F>, AG: Applicative<G>):
            Alternative<Nested<F, G>> =
                object : ComposedAlternative<F, G> {
                    override fun F(): Alternative<F> = AF

                    override fun G(): Applicative<G> = AG
                }
    }
}

inline fun <reified F, reified G> Alternative<F>.compose(GA: Applicative<G> = applicative<G>()): Alternative<Nested<F, G>> = ComposedAlternative(this, GA)

interface ComposedBifoldable<F, G> : Bifoldable<Nested<F, G>> {
    fun F(): Bifoldable<F>

    fun G(): Bifoldable<G>

    override fun <A, B, C> bifoldLeft(fab: Kind2<Nested<F, G>, A, B>, c: C, f: (C, A) -> C, g: (C, B) -> C): C =
        F().bifoldLeft(
            fab.biunnest(), c,
            { cc: C, gab: Kind2<G, A, B> -> G().bifoldLeft(gab, cc, f, g) },
            { cc: C, gab: Kind2<G, A, B> -> G().bifoldLeft(gab, cc, f, g) }
        )

    override fun <A, B, C> bifoldRight(fab: Kind2<Nested<F, G>, A, B>, c: Eval<C>, f: (A, Eval<C>) -> Eval<C>, g: (B, Eval<C>) -> Eval<C>): Eval<C> =
        F().bifoldRight(
            fab.biunnest(), c,
            { gab: Kind2<G, A, B>, cc: Eval<C> -> G().bifoldRight(gab, cc, f, g) },
            { gab: Kind2<G, A, B>, cc: Eval<C> -> G().bifoldRight(gab, cc, f, g) }
        )

    fun <A, B, C> bifoldLeftC(fab: Kind2<F, Kind2<G, A, B>, Kind2<G, A, B>>, c: C, f: (C, A) -> C, g: (C, B) -> C): C =
        bifoldLeft(fab.binest(), c, f, g)

    fun <A, B, C> bifoldRightC(fab: Kind2<F, Kind2<G, A, B>, Kind2<G, A, B>>, c: Eval<C>, f: (A, Eval<C>) -> Eval<C>, g: (B, Eval<C>) -> Eval<C>): Eval<C> =
        bifoldRight(fab.binest(), c, f, g)

    companion object {
        operator fun <F, G> invoke(BF: Bifoldable<F>, BG: Bifoldable<G>): ComposedBifoldable<F, G> =
            object : ComposedBifoldable<F, G> {
                override fun F(): Bifoldable<F> = BF

                override fun G(): Bifoldable<G> = BG
            }
    }
}

inline fun <reified F, reified G> Bifoldable<F>.compose(BG: Bifoldable<G> = bifoldable()): Bifoldable<Nested<F, G>> = ComposedBifoldable(this, BG)
