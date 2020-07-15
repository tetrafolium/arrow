package arrow.test.laws

import arrow.*
import arrow.core.Tuple2
import arrow.mtl.MonadWriter
import arrow.typeclasses.Eq
import arrow.typeclasses.Monad
import arrow.typeclasses.Monoid
import arrow.typeclasses.monoid
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

object MonadWriterLaws {

    inline fun <reified F, reified W> laws(MF: Monad<F>,
                                           MW: MonadWriter<F, W>,
                                           MOW: Monoid<W> = monoid<W>(),
                                           genW: Gen<W>,
                                           genTupleWA: Gen<Tuple2<W, Int>>,
                                           EqInt: Eq<Kind<F, Int>>,
                                           EqTupleWA: Eq<Kind<F, Tuple2<W, Int>>>): List<Law> =

            MonadLaws.laws(MF, EqInt) + listOf(
                    Law("Monad Writer Laws: writer pure", { monadWriterWriterPure(MW, MOW, EqInt) }),
                    Law("Monad Writer Laws: tell fusion", { monadWriterTellFusion(genW, MW, MOW) }),
                    Law("Monad Writer Laws: listen pure", { monadWriterListenPure(MW, MOW, EqTupleWA) }),
                    Law("Monad Writer Laws: listen writer", { monadWriterListenWriter(genTupleWA, MW, EqTupleWA) }))

    inline fun <reified F, reified W> monadWriterWriterPure(MW: MonadWriter<F, W>,
                                                            MOW: Monoid<W> = monoid<W>(),
                                                            EQ: Eq<Kind<F, Int>>) {
        forAll(Gen.int(), { a: Int ->
            MW.writer(Tuple2(MOW.empty(), a)).equalUnderTheLaw(MW.pure(a), EQ)
        })
    }

    inline fun <reified F, reified W> monadWriterTellFusion(genW: Gen<W>,
                                                            MW: MonadWriter<F, W>,
                                                            MOW: Monoid<W> = monoid<W>()) {
        forAll(genW, genW, { x: W, y: W ->
            MW.flatMap(MW.tell(x), { MW.tell(y) }).equalUnderTheLaw(MW.tell(MOW.combine(x, y)), Eq.any())
        })
    }

    inline fun <reified F, reified W> monadWriterListenPure(MW: MonadWriter<F, W>,
                                                            MOW: Monoid<W> = monoid<W>(),
                                                            EqTupleWA: Eq<Kind<F, Tuple2<W, Int>>>) {
        forAll(Gen.int(), { a: Int ->
            MW.listen(MW.pure(a)).equalUnderTheLaw(MW.pure(Tuple2(MOW.empty(), a)), EqTupleWA)
        })
    }

    fun <F, W> monadWriterListenWriter(genTupleWA: Gen<Tuple2<W, Int>>,
                                       MW: MonadWriter<F, W>,
                                       EqTupleWA: Eq<Kind<F, Tuple2<W, Int>>>) {
        forAll(genTupleWA, { tupleWA: Tuple2<W, Int> ->
            MW.listen(MW.writer(tupleWA)).equalUnderTheLaw(MW.map(MW.tell(tupleWA.a), { tupleWA }), EqTupleWA)
        })
    }
}
