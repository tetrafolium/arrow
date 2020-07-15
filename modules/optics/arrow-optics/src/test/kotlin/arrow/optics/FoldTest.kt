package arrow.optics

import arrow.core.Option
import arrow.data.ForListK
import arrow.data.k
import arrow.test.UnitSpec
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class FoldTest : UnitSpec() {

    init {

        val intFold = Fold.fromFoldable<ForListK, Int>()
        val stringFold = Fold.fromFoldable<ForListK, String>()

        "Fold select a list that contains one" {
            val select = Fold.select<List<Int>> { it.contains(1) }

            forAll(Gen.list(Gen.int()), { ints ->
                select.getAll(ints).list.firstOrNull() ==
                        ints.let { if (it.contains(1)) it else null }
            })
        }

        "Folding a list of ints" {
            forAll(Gen.list(Gen.int()), { ints ->
                intFold.fold(ints.k()) == ints.sum()
            })
        }

        "Folding a list should yield same result as combineAll" {
            forAll(Gen.list(Gen.int()), { ints ->
                intFold.combineAll(ints.k()) == ints.sum()
            })
        }

        "Folding and mapping a list of strings" {
            forAll(Gen.list(Gen.int()), { ints ->
                stringFold.foldMap(ints.map(Int::toString).k(), String::toInt) == ints.sum()
            })
        }

        "Get all targets" {
            forAll(Gen.list(Gen.int()), { ints ->
                intFold.getAll(ints.k()) == ints.k()
            })
        }

        "Get the size of the fold" {
            forAll(Gen.list(Gen.int()), { ints ->
                intFold.size(ints.k()) == ints.size
            })
        }

        "Find the first element matching the predicate" {
            forAll(Gen.list(Gen.choose(-100, 100)), { ints ->
                intFold.find(ints.k()) { it > 10 } == Option.fromNullable(ints.firstOrNull { it > 10 })
            })
        }

        "Checking existence of a target" {
            forAll(Gen.list(Gen.int()), Gen.bool(), { ints, predicate ->
                intFold.exists(ints.k()) { predicate } == predicate
            })
        }

        "Check if all targets match the predicate" {
            forAll(Gen.list(Gen.int()), { ints ->
                intFold.forall(ints.k()) { it % 2 == 0 } == ints.all { it % 2 == 0 }
            })
        }

        "Check if there is no target" {
            forAll(Gen.list(Gen.int()), { ints ->
                intFold.isEmpty(ints.k()) == ints.isEmpty()
            })
        }

        "Check if there is a target" {
            forAll(Gen.list(Gen.int()), { ints ->
                intFold.nonEmpty(ints.k()) == ints.isNotEmpty()
            })
        }
    }
}
