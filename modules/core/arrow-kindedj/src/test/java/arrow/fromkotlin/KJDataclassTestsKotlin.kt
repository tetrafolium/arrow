package arrow.fromkotlin

import arrow.kindedj.KJDataclassArrowShow
import arrow.kindedj.fromKindedJ
import arrow.kindedj.fromkindedj.ForKJDataclass.KJDataclass1
import arrow.kindedj.fromkindedj.KJDataclassKindedJShow
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class KJDataclassTestsKotlin : StringSpec() {

    private val kinded = KJDataclass1(0)

    init {
        "Values should be convertible" {
            KJDataclassKindedJShow.INSTANCE.show(kinded) shouldBe KJDataclassArrowShow.show(kinded.fromKindedJ())
        }
    }
}
