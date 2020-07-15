package arrow.syntax.foldable

import arrow.*
import arrow.core.Eval
import arrow.core.Option
import arrow.typeclasses.*

inline fun <reified F, A, B> Kind<F, A>.foldLeft(FT: Foldable<F> = foldable(), b: B, noinline f: (B, A) -> B): B = FT.foldLeft(this, b, f)

inline fun <reified F, A, B> Kind<F, A>.foldRight(FT: Foldable<F> = foldable(), b: Eval<B>, noinline f: (A, Eval<B>) -> Eval<B>): Eval<B> = FT.foldRight(this, b, f)

inline fun <reified F, reified A> Kind<F, A>.fold(FT: Foldable<F> = foldable(), MA: Monoid<A> = monoid()): A = FT.fold(MA, this)

inline fun <reified F, reified A> Kind<F, A>.combineAll(FT: Foldable<F> = foldable(), MA: Monoid<A> = monoid()): A = FT.combineAll(MA, this)

inline fun <reified F, A, reified B> Kind<F, A>.foldMap(FT: Foldable<F> = foldable(), MB: Monoid<B> = monoid(), noinline f: (A) -> B): B =
        FT.foldMap(MB, this, f)

inline fun <reified F, reified G, A, B> Kind<F, A>.traverse_(FT: Foldable<F> = foldable(), AG: Applicative<G> = applicative(), noinline f: (A) -> Kind<G, B>):
        Kind<G, Unit> = FT.traverse_(AG, this, f)

inline fun <reified F, reified G, A> Kind<F, Kind<G, A>>.sequence_(FT: Foldable<F> = foldable(), AG: Applicative<G> = applicative()):
        Kind<G, Unit> = FT.sequence_(AG, this)

inline fun <reified F, A> Kind<F, A>.find(FT: Foldable<F> = foldable(), noinline f: (A) -> Boolean): Option<A> = FT.find(this, f)

inline fun <reified F, A> Kind<F, A>.exists(FT: Foldable<F> = foldable(), noinline f: (A) -> Boolean): Boolean = FT.exists(this, f)

inline fun <reified F, A> Kind<F, A>.forall(FT: Foldable<F> = foldable(), noinline f: (A) -> Boolean): Boolean = FT.forall(this, f)

inline fun <reified F, A> Kind<F, A>.isEmpty(FT: Foldable<F> = foldable()): Boolean = FT.isEmpty(this)

inline fun <reified F, A> Kind<F, A>.nonEmpty(FT: Foldable<F> = foldable()): Boolean = FT.nonEmpty(this)
