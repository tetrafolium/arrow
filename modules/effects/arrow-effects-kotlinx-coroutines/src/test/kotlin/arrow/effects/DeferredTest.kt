package arrow.effects

import arrow.Kind
import arrow.test.UnitSpec
import arrow.test.generators.genIntSmall
import arrow.test.laws.AsyncLaws
import arrow.typeclasses.*
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.fail
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.properties.forAll
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class DeferredKTest : UnitSpec() {
    fun <A> EQ(): Eq<Kind<ForDeferredK, A>> = Eq { a, b ->
        a.unsafeAttemptSync() == b.unsafeAttemptSync()
    }

    init {
        testLaws(AsyncLaws.laws(DeferredK.async(), EQ(), EQ()))

        "instances can be resolved implicitly" {
            functor<ForDeferredK>() shouldNotBe null
            applicative<ForDeferredK>() shouldNotBe null
            monad<ForDeferredK>() shouldNotBe null
            applicativeError<ForDeferredK, Throwable>() shouldNotBe null
            monadError<ForDeferredK, Throwable>() shouldNotBe null
            monadSuspend<ForDeferredK>() shouldNotBe null
            async<ForDeferredK>() shouldNotBe null
            effect<ForDeferredK>() shouldNotBe null
        }

        "DeferredK is awaitable" {
            forAll(
                genIntSmall(), genIntSmall(), genIntSmall(),
                { x: Int, y: Int, z: Int ->
                    runBlocking {
                        val a = DeferredK { x }.await()
                        val b = DeferredK { y + a }.await()
                        val c = DeferredK { z + b }.await()
                        c
                    } == x + y + z
                }
            )
        }

        "should complete when running a pure value with unsafeRunAsync" {
            val expected = 0
            DeferredK.pure(expected).unsafeRunAsync { either ->
                either.fold({ fail("") }, { it shouldBe expected })
            }
        }

        class MyException : Exception()

        "should return an error when running an exception with unsafeRunAsync" {
            DeferredK.raiseError<Int>(MyException()).unsafeRunAsync { either ->
                either.fold(
                    {
                        when (it) {
                            is MyException -> {
                            }
                            else -> fail("Should only throw MyException")
                        }
                    },
                    { fail("") }
                )
            }
        }

        "should return exceptions within main block with unsafeRunAsync" {
            val exception = MyException()
            val ioa = DeferredK<Int>(Unconfined, CoroutineStart.DEFAULT) { throw exception }
            ioa.unsafeRunAsync { either ->
                either.fold({ it shouldBe exception }, { fail("") })
            }
        }

        "should not catch exceptions within run block with unsafeRunAsync" {
            try {
                val exception = MyException()
                val ioa = DeferredK<Int>(Unconfined, CoroutineStart.DEFAULT) { throw exception }
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
            DeferredK.pure(expected).runAsync { either ->
                either.fold({ fail("") }, { DeferredK { it shouldBe expected } })
            }
        }

        "should complete when running a return value with runAsync" {
            val expected = 0
            DeferredK(Unconfined, CoroutineStart.DEFAULT) { expected }.runAsync { either ->
                either.fold({ fail("") }, { DeferredK { it shouldBe expected } })
            }
        }

        "should return an error when running an exception with runAsync" {
            DeferredK.raiseError<Int>(MyException()).runAsync { either ->
                either.fold(
                    {
                        when (it) {
                            is MyException -> {
                                DeferredK { }
                            }
                            else -> fail("Should only throw MyException")
                        }
                    },
                    { fail("") }
                )
            }
        }

        "should return exceptions within main block with runAsync" {
            val exception = MyException()
            val ioa = DeferredK<Int>(Unconfined, CoroutineStart.DEFAULT) { throw exception }
            ioa.runAsync { either ->
                either.fold({ DeferredK { it shouldBe exception } }, { fail("") })
            }
        }

        "should catch exceptions within run block with runAsync" {
            try {
                val exception = MyException()
                val ioa = DeferredK<Int>(Unconfined, CoroutineStart.DEFAULT) { throw exception }
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
    }
}
