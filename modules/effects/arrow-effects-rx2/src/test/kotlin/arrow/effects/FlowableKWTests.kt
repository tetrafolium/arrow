package arrow.effects

import arrow.test.UnitSpec
import arrow.test.laws.AsyncLaws
import arrow.test.laws.FoldableLaws
import arrow.test.laws.TraverseLaws
import arrow.typeclasses.*
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldNotBe
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.TestSubscriber
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(KTestJUnitRunner::class)
class FlowableKTests : UnitSpec() {

    fun <T> EQ(): Eq<FlowableKOf<T>> = object : Eq<FlowableKOf<T>> {
        override fun eqv(a: FlowableKOf<T>, b: FlowableKOf<T>): Boolean =
            try {
                a.value().blockingFirst() == b.value().blockingFirst()
            } catch (throwable: Throwable) {
                val errA = try {
                    a.value().blockingFirst()
                    throw IllegalArgumentException()
                } catch (err: Throwable) {
                    err
                }
                val errB = try {
                    b.value().blockingFirst()
                    throw IllegalStateException()
                } catch (err: Throwable) {
                    err
                }
                errA == errB
            }
    }

    init {
        "instances can be resolved implicitly" {
            functor<ForFlowableK>() shouldNotBe null
            applicative<ForFlowableK>() shouldNotBe null
            monad<ForFlowableK>() shouldNotBe null
            applicativeError<ForFlowableK, Unit>() shouldNotBe null
            monadError<ForFlowableK, Unit>() shouldNotBe null
            monadSuspend<ForFlowableK>() shouldNotBe null
            async<ForFlowableK>() shouldNotBe null
            effect<ForFlowableK>() shouldNotBe null
            foldable<ForFlowableK>() shouldNotBe null
            traverse<ForFlowableK>() shouldNotBe null
        }

        testLaws(AsyncLaws.laws(FlowableK.async(), EQ(), EQ()))
        testLaws(AsyncLaws.laws(FlowableK.async(), EQ(), EQ()))
        testLaws(AsyncLaws.laws(FlowableK.async(), EQ(), EQ()))

        testLaws(AsyncLaws.laws(FlowableK.asyncDrop(), EQ(), EQ()))
        testLaws(AsyncLaws.laws(FlowableK.asyncDrop(), EQ(), EQ()))
        testLaws(AsyncLaws.laws(FlowableK.asyncDrop(), EQ(), EQ()))

        testLaws(AsyncLaws.laws(FlowableK.asyncError(), EQ(), EQ()))
        testLaws(AsyncLaws.laws(FlowableK.asyncError(), EQ(), EQ()))
        testLaws(AsyncLaws.laws(FlowableK.asyncError(), EQ(), EQ()))

        testLaws(AsyncLaws.laws(FlowableK.asyncLatest(), EQ(), EQ()))
        testLaws(AsyncLaws.laws(FlowableK.asyncLatest(), EQ(), EQ()))
        testLaws(AsyncLaws.laws(FlowableK.asyncLatest(), EQ(), EQ()))

        testLaws(AsyncLaws.laws(FlowableK.asyncMissing(), EQ(), EQ()))
        testLaws(AsyncLaws.laws(FlowableK.asyncMissing(), EQ(), EQ()))
        testLaws(AsyncLaws.laws(FlowableK.asyncMissing(), EQ(), EQ()))

        testLaws(
            FoldableLaws.laws(FlowableK.foldable(), { FlowableK.pure(it) }, Eq.any()),
            TraverseLaws.laws(FlowableK.traverse(), FlowableK.functor(), { FlowableK.pure(it) }, EQ())
        )

        "Multi-thread Flowables finish correctly" {
            val value: Flowable<Long> = FlowableK.monadErrorFlat().bindingCatch {
                val a = Flowable.timer(2, TimeUnit.SECONDS).k().bind()
                a
            }.value()
            val test: TestSubscriber<Long> = value.test()
            test.awaitDone(5, TimeUnit.SECONDS)
            test.assertTerminated().assertComplete().assertNoErrors().assertValue(0)
        }

        "Multi-thread Observables should run on their required threads" {
            val originalThread: Thread = Thread.currentThread()
            var threadRef: Thread? = null
            val value: Flowable<Long> = FlowableK.monadErrorFlat().bindingCatch {
                val a = Flowable.timer(2, TimeUnit.SECONDS, Schedulers.newThread()).k().bind()
                threadRef = Thread.currentThread()
                val b = Flowable.just(a).observeOn(Schedulers.newThread()).k().bind()
                b
            }.value()
            val test: TestSubscriber<Long> = value.test()
            val lastThread: Thread = test.awaitDone(5, TimeUnit.SECONDS).lastThread()
            val nextThread = (threadRef?.name ?: "")

            nextThread shouldNotBeElseLogged originalThread.name
            lastThread.name shouldNotBeElseLogged originalThread.name
            lastThread.name shouldNotBeElseLogged nextThread
        }

        "Flowable cancellation forces binding to cancel without completing too" {
            val value: Flowable<Long> = FlowableK.monadErrorFlat().bindingCatch {
                val a = Flowable.timer(3, TimeUnit.SECONDS).k().bind()
                a
            }.value()
            val test: TestSubscriber<Long> = value.doOnSubscribe { subscription ->
                Flowable.timer(1, TimeUnit.SECONDS).subscribe {
                    subscription.cancel()
                }
            }.test()
            test.awaitTerminalEvent(5, TimeUnit.SECONDS)
            test.assertNotTerminated().assertNotComplete().assertNoErrors().assertNoValues()
        }
    }

    // FIXME(paco): remove if this hasn't triggered in a while - 26 Jan 18
    private infix fun String.shouldNotBeElseLogged(b: String) {
        try {
            this shouldNotBe b
        } catch (t: Throwable) {
            println("$this  <---->  $b")
            throw t
        }
    }
}
