package arrow.effects

import arrow.Kind
import arrow.core.*
import arrow.test.UnitSpec
import arrow.test.concurrency.SideEffect
import arrow.test.laws.AsyncLaws
import arrow.typeclasses.*
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.fail
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.matchers.shouldNotBe
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class IOTest : UnitSpec() {
    fun <A> EQ(): Eq<Kind<ForIO, A>> = Eq { a, b ->
        Option.eq(Eq.any()).eqv(a.fix().attempt().unsafeRunTimed(60.seconds), b.fix().attempt().unsafeRunTimed(60.seconds))
    }

    init {
        testLaws(AsyncLaws.laws(IO.async(), EQ(), EQ()))

        "instances can be resolved implicitly" {
            functor<ForIO>() shouldNotBe null
            applicative<ForIO>() shouldNotBe null
            monad<ForIO>() shouldNotBe null
            applicativeError<ForIO, Throwable>() shouldNotBe null
            monadError<ForIO, Throwable>() shouldNotBe null
            monadSuspend<ForIO>() shouldNotBe null
            async<ForIO>() shouldNotBe null
            effect<ForIO>() shouldNotBe null
            semigroup<IOOf<Int>>() shouldNotBe null
            monoid<IOOf<Int>>() shouldNotBe null
        }

        "should defer evaluation until run" {
            var run = false
            val ioa = IO { run = true }
            run shouldEqual false
            ioa.unsafeRunSync()
            run shouldEqual true
        }

        class MyException : Exception()

        "should catch exceptions within main block" {
            val exception = MyException()
            val ioa = IO { throw exception }
            val result: Either<Throwable, Nothing> = ioa.attempt().unsafeRunSync()

            val expected = Left(exception)

            result shouldBe expected
        }

        "should yield immediate successful invoke value" {
            val run = IO { 1 }.unsafeRunSync()

            val expected = 1

            run shouldBe expected
        }

        "should yield immediate successful pure value" {
            val run = IO.pure(1).unsafeRunSync()

            val expected = 1

            run shouldBe expected
        }

        "should yield immediate successful pure value" {
            val run = IO.pure(1).unsafeRunSync()

            val expected = 1

            run shouldBe expected
        }

        "should throw immediate failure by raiseError" {
            try {
                IO.raiseError<Int>(MyException()).unsafeRunSync()
                fail("")
            } catch (myException: MyException) {
                // Success
            } catch (throwable: Throwable) {
                fail("Should only throw MyException")
            }
        }

        "should time out on unending unsafeRunTimed" {
            val never = IO.async<Int> { Unit }
            val start = System.currentTimeMillis()
            val received = never.unsafeRunTimed(100.milliseconds)
            val elapsed = System.currentTimeMillis() - start

            received shouldBe None
            (elapsed >= 100) shouldBe true
        }

        "should return a null value from unsafeRunTimed" {
            val never = IO.pure<Int?>(null)
            val received = never.unsafeRunTimed(100.milliseconds)

            received shouldBe Some(null)
        }

        "should return a null value from unsafeRunSync" {
            val value = IO.pure<Int?>(null).unsafeRunSync()

            value shouldBe null
        }

        "should complete when running a pure value with unsafeRunAsync" {
            val expected = 0
            IO.pure(expected).unsafeRunAsync { either ->
                either.fold({ fail("") }, { it shouldBe expected })
            }
        }

        "should complete when running a return value with unsafeRunAsync" {
            val expected = 0
            IO { expected }.unsafeRunAsync { either ->
                either.fold({ fail("") }, { it shouldBe expected })
            }
        }

        "should return an error when running an exception with unsafeRunAsync" {
            IO.raiseError<Int>(MyException()).unsafeRunAsync { either ->
                either.fold({
                    when (it) {
                        is MyException -> {
                        }
                        else -> fail("Should only throw MyException")
                    }
                }, { fail("") })
            }
        }

        "should return exceptions within main block with unsafeRunAsync" {
            val exception = MyException()
            val ioa = IO<Int> { throw exception }
            ioa.unsafeRunAsync { either ->
                either.fold({ it shouldBe exception }, { fail("") })
            }
        }

        "should not catch exceptions within run block with unsafeRunAsync" {
            try {
                val exception = MyException()
                val ioa = IO<Int> { throw exception }
                ioa.unsafeRunAsync { either ->
                    either.fold({ throw exception }, { fail("") })
                }
            } catch (myException: MyException) {
                // Success
            } catch (throwable: Throwable) {
                fail("Should only throw MyException")
            }
        }

        "should complete when running a pure value with runAsync" {
            val expected = 0
            IO.pure(expected).runAsync { either ->
                either.fold({ fail("") }, { IO { it shouldBe expected } })
            }
        }

        "should complete when running a return value with runAsync" {
            val expected = 0
            IO { expected }.runAsync { either ->
                either.fold({ fail("") }, { IO { it shouldBe expected } })
            }
        }

        "should return an error when running an exception with runAsync" {
            IO.raiseError<Int>(MyException()).runAsync { either ->
                either.fold({
                    when (it) {
                        is MyException -> {
                            IO { }
                        }
                        else -> fail("Should only throw MyException")
                    }
                }, { fail("") })
            }
        }

        "should return exceptions within main block with runAsync" {
            val exception = MyException()
            val ioa = IO<Int> { throw exception }
            ioa.runAsync { either ->
                either.fold({ IO { it shouldBe exception } }, { fail("") })
            }
        }

        "should catch exceptions within run block with runAsync" {
            try {
                val exception = MyException()
                val ioa = IO<Int> { throw exception }
                ioa.runAsync { either ->
                    either.fold({ throw it }, { fail("") })
                }.unsafeRunSync()
                fail("Should rethrow the exception")
            } catch (throwable: AssertionError) {
                fail("${throwable.message}")
            } catch (throwable: Throwable) {
                // Success
            }
        }

        "should map values correctly on success" {
            val run = IO.functor().map(IO.pure(1)) { it + 1 }.unsafeRunSync()

            val expected = 2

            run shouldBe expected
        }

        "should flatMap values correctly on success" {
            val run = IO.monad().flatMap(IO.pure(1)) { num -> IO { num + 1 } }.unsafeRunSync()

            val expected = 2

            run shouldBe expected
        }

        "invoke is called on every run call" {
            val sideEffect = SideEffect()
            val io = IO { sideEffect.increment(); 1 }
            io.unsafeRunSync()
            io.unsafeRunSync()

            sideEffect.counter shouldBe 2
        }

        "unsafeRunTimed times out with None result" {
            val never = IO.async<Int> { }
            val result = never.unsafeRunTimed(100.milliseconds)
            result shouldBe None
        }

        "IO.binding should for comprehend over IO" {
            val result = IO.monad().binding {
                val x = IO.pure(1).bind()
                val y = bind { IO { x + 1 } }
                y
            }.fix()
            result.unsafeRunSync() shouldBe 2
        }
    }
}
