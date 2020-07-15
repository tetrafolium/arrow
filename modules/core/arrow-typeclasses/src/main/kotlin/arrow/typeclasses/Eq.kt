package arrow.typeclasses

import arrow.*

/**
 * A type class used to determine equality between 2 instances of the same type [F] in a type safe way.
 *
 * @see <a href="http://arrow-kt.io/docs/typeclasses/eq/">Eq documentation</a>
 */
@typeclass
interface Eq<in F> : TC {

    /**
     * Compares two instances of [F] and returns true if they're considered equal for this instance.
     *
     * @param a object to compare with [b]
     * @param b object to compare with [a]
     * @returns true if [a] and [b] are equivalent, false otherwise.
     */
    fun eqv(a: F, b: F): Boolean

    /**
     * Compares two instances of [F] and returns true if they're considered not equal for this instance.
     *
     * @param a object to compare with [b]
     * @param b object to compare with [a]
     * @returns false if [a] and [b] are equivalent, true otherwise.
     */
    fun neqv(a: F, b: F): Boolean = !eqv(a, b)

    companion object {

        /**
         * Construct an [Eq] from a function `(F, F) -> Boolean`.
         *
         * @param feqv function that defines if two instances of type [F] are equal.
         * @returns an [Eq] instance that is defined by the [feqv] function.
         */
        inline operator fun <F> invoke(crossinline feqv: (F, F) -> Boolean): Eq<F> = object : Eq<F> {
            override fun eqv(a: F, b: F): Boolean =
                    feqv(a, b)
        }

        /**
         * Retrieve an [Eq] that defines all instances as equal for type [F].
         *
         * @returns an [Eq] instance wherefore all instances of type [F] are equal.
         */
        fun any(): Eq<Any?> = EqAny

        private object EqAny : Eq<Any?> {
            override fun eqv(a: Any?, b: Any?): Boolean = a == b

            override fun neqv(a: Any?, b: Any?): Boolean = a != b
        }
    }
}
