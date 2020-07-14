package arrow.optics

import arrow.instances.StringMonoidInstance
import arrow.test.UnitSpec
import arrow.test.generators.genFunctionAToB
import arrow.test.generators.genNonEmptyList
import arrow.test.generators.genOption
import arrow.test.laws.IsoLaws
import arrow.test.laws.LensLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.Monoid
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.Gen
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class NonEmptyListInstancesTest : UnitSpec() {

    init {

        testLaws(
            LensLaws.laws(
                lens = nelHead(),
                aGen = genNonEmptyList(Gen.string()),
                bGen = Gen.string(),
                funcGen = genFunctionAToB(Gen.string()),
                EQA = Eq.any(),
                EQB = Eq.any(),
                MB = StringMonoidInstance
            ),

            IsoLaws.laws(
                iso = optionNelToList(),
                aGen = genOption(genNonEmptyList(Gen.int())),
                bGen = Gen.list(Gen.int()),
                funcGen = genFunctionAToB(Gen.list(Gen.int())),
                EQA = Eq.any(),
                EQB = Eq.any(),
                bMonoid = object : Monoid<List<Int>> {
                    override fun combine(a: List<Int>, b: List<Int>): List<Int> = a + b
                    override fun empty(): List<Int> = emptyList()
                }
            )
        )
    }
}
