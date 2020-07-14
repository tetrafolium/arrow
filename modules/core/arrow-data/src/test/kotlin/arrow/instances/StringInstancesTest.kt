package arrow.instances

import arrow.test.UnitSpec
import arrow.test.laws.EqLaws
import arrow.typeclasses.eq
import arrow.typeclasses.monoid
import arrow.typeclasses.semigroup
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldNotBe
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class StringInstancesTest : UnitSpec() {
    init {
        "instances can be resolved implicitly" {
            semigroup<String>() shouldNotBe null
            monoid<String>() shouldNotBe null
            eq<String>() shouldNotBe null
        }

        testLaws(EqLaws.laws { it.toString() })
    }
}
