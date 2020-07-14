package arrow.syntax.collections

import arrow.core.GetterOperation
import arrow.core.GetterOperationImpl
import arrow.core.Option
import arrow.syntax.option.*

val <K, V> Map<K, V>.option: GetterOperation<K, Option<V>>
    get() {
        return GetterOperationImpl { k -> this[k].toOption() }
    }
