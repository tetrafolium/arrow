package arrow.optics

import arrow.core.Id
import arrow.instances.IntMonoid
import arrow.test.UnitSpec
import arrow.test.generators.genFunctionAToB
import arrow.test.laws.IsoLaws
import arrow.typeclasses.Eq
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.Gen
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class IdInstancesTest : UnitSpec() {

    init {
        testLaws(IsoLaws.laws(
                iso = idToType(),
                aGen = Gen.create { Id(Gen.int().generate()) },
                bGen = Gen.int(),
                funcGen = genFunctionAToB(Gen.int()),
                EQA = Eq.any(),
                EQB = Eq.any(),
                bMonoid = IntMonoid
        ))
    }
}
