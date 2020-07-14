package arrow.effects.data

import arrow.effects.Duration
import arrow.test.UnitSpec
import arrow.test.generators.genIntSmall
import arrow.test.generators.genTimeUnit
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

/**
 *
 */
@RunWith(KTestJUnitRunner::class)
class DurationTest : UnitSpec() {

    init {
        "plus should be commutative" {
            forAll(genIntSmall(), genTimeUnit(), genIntSmall(), genTimeUnit()) { i, u, j, v ->
                val a = Duration(i.toLong(), u)
                val b = Duration(j.toLong(), v)
                a + b == b + a
            }
        }
    }
}
