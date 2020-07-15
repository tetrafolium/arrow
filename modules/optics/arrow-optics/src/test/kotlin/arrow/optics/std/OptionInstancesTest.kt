package arrow.optics

import arrow.core.*
import arrow.instances.IntMonoid
import arrow.syntax.either.right
import arrow.test.UnitSpec
import arrow.test.generators.genEither
import arrow.test.generators.genFunctionAToB
import arrow.test.generators.genNullable
import arrow.test.generators.genOption
import arrow.test.laws.IsoLaws
import arrow.test.laws.PrismLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.Monoid
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.Gen
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class OptionInstancesTest : UnitSpec() {

    init {

        testLaws(PrismLaws.laws(
                prism = somePrism(),
                aGen = genOption(Gen.int()),
                bGen = Gen.int(),
                funcGen = genFunctionAToB(Gen.int()),
                EQA = Eq.any(),
                EQOptionB = Eq.any()
        ))

        testLaws(PrismLaws.laws(
                prism = nonePrism(),
                aGen = genOption(Gen.int()),
                bGen = Gen.create { Unit },
                funcGen = genFunctionAToB(Gen.create { Unit }),
                EQA = Eq.any(),
                EQOptionB = Eq.any()
        ))

        testLaws(IsoLaws.laws(
                iso = nullableToOption<Int>(),
                aGen = genNullable(Gen.int()),
                bGen = genOption(Gen.int()),
                EQA = Eq.any(),
                EQB = Eq.any(),
                funcGen = genFunctionAToB(genOption(Gen.int())),
                bMonoid = Option.monoid(IntMonoid)
        ))

        testLaws(IsoLaws.laws(
                iso = optionToEither(),
                aGen = genOption(Gen.int()),
                bGen = genEither(Gen.create { Unit }, Gen.int()),
                funcGen = genFunctionAToB(genEither(Gen.create { Unit }, Gen.int())),
                EQA = Eq.any(),
                EQB = Eq.any(),
                bMonoid = object : Monoid<Either<Unit, Int>> {
                    override fun combine(a: Either<Unit, Int>, b: Either<Unit, Int>): Either<Unit, Int> =
                            Either.applicative<Unit>().map2(a, b) { (a, b) -> a + b }.fix()

                    override fun empty(): Either<Unit, Int> = 0.right()
                }
        ))
    }
}
