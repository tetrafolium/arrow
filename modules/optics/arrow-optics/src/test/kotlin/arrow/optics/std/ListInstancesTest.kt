package arrow.optics

import arrow.core.Option
import arrow.core.monoid
import arrow.data.ListK
import arrow.data.NonEmptyList
import arrow.data.k
import arrow.data.monoid
import arrow.data.semigroup
import arrow.test.UnitSpec
import arrow.test.generators.genFunctionAToB
import arrow.test.generators.genNonEmptyList
import arrow.test.generators.genNullable
import arrow.test.generators.genOption
import arrow.test.laws.IsoLaws
import arrow.test.laws.OptionalLaws
import arrow.typeclasses.Eq
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.Gen
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class ListInstancesTest : UnitSpec() {

    init {

        testLaws(
            OptionalLaws.laws(
                optional = listHead(),
                aGen = Gen.list(Gen.int()),
                bGen = Gen.int(),
                funcGen = genFunctionAToB(Gen.int()),
                EQA = Eq.any(),
                EQOptionB = Eq.any()
            )
        )

        testLaws(
            OptionalLaws.laws(
                optional = listTail(),
                aGen = Gen.list(Gen.int()),
                bGen = Gen.list(Gen.int()),
                funcGen = genFunctionAToB(Gen.list(Gen.int())),
                EQA = Eq.any(),
                EQOptionB = Eq.any()
            )
        )

        testLaws(
            IsoLaws.laws(
                iso = listToOptionNel(),
                aGen = Gen.list(Gen.int()),
                bGen = genOption(genNonEmptyList(Gen.int())),
                funcGen = genFunctionAToB(genOption(genNonEmptyList(Gen.int()))),
                EQA = Eq.any(),
                EQB = Eq.any(),
                bMonoid = Option.monoid(NonEmptyList.semigroup<Int>())
            )
        )

        testLaws(
            IsoLaws.laws(
                iso = listToListK(),
                aGen = Gen.list(Gen.int()),
                bGen = Gen.create { Gen.list(Gen.int()).generate().k() },
                funcGen = genFunctionAToB(Gen.create { Gen.list(Gen.int()).generate().k() }),
                EQA = Eq.any(),
                EQB = Eq.any(),
                bMonoid = ListK.monoid()
            )
        )

        testLaws(
            OptionalLaws.laws(
                optional = nullableOptional(),
                aGen = genNullable(Gen.int()),
                bGen = Gen.int(),
                funcGen = genFunctionAToB(Gen.int()),
                EQA = Eq.any(),
                EQOptionB = Eq.any()
            )
        )

        testLaws(
            OptionalLaws.laws(
                optional = optionOptional(),
                aGen = genOption(Gen.int()),
                bGen = Gen.int(),
                funcGen = genFunctionAToB(Gen.int()),
                EQA = Eq.any(),
                EQOptionB = Eq.any()
            )
        )
    }
}
