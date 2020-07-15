package arrow.optics

import arrow.data.*

/**
 * [PIso] that defines the equality between a [Set] and a [arrow.SetK]
 */
fun <A, B> pSetToSetK(): PIso<Set<A>, Set<B>, SetK<A>, SetK<B>> = PIso(
        get = { it.k() },
        reverseGet = { it.set }
)

/**
 * [Iso] that defines the equality between a [Set] and a [arrow.SetK]
 */
fun <A> setToSetK(): Iso<Set<A>, SetK<A>> = pSetToSetK()
