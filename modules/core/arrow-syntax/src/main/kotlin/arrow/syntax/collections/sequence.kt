package arrow.syntax.collections

import arrow.core.Option
import arrow.core.Predicate
import arrow.syntax.option.*

fun <T> Sequence<T?>.firstOption(): Option<T> = firstOrNull().toOption()

fun <T> Sequence<T>.firstOption(predicate: Predicate<T>): Option<T> = firstOrNull(predicate).toOption()
