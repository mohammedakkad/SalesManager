package com.trader.salesmanager.ui.recomposition

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * Relaxed view-model fakes for the recomposition audit. Every public `Flow` property is stubbed with
 * a stable [MutableStateFlow] (an unstubbed relaxed mock would return a *new* mock per call, which
 * is itself a recomposition source and would distort the numbers). Values come from [overrides]
 * (by property name) or a default instance of the declared element type.
 */
object FakeViewModels {
    inline fun <reified T : Any> fake(overrides: Map<String, Any?> = emptyMap()): Pair<T, Map<String, MutableStateFlow<Any?>>> {
        val vm = mockk<T>(relaxed = true)
        val flows = linkedMapOf<String, MutableStateFlow<Any?>>()
        T::class.java.methods
            .filter { it.parameterCount == 0 && it.name.startsWith("get") && Flow::class.java.isAssignableFrom(it.returnType) }
            .forEach { m ->
                val name = m.name.removePrefix("get").replaceFirstChar { it.lowercase() }
                val value = if (name in overrides) overrides[name] else defaultOf(elementType(m.genericReturnType))
                // A one-shot event stream (Flow/SharedFlow) with no sensible default never emits.
                val flow: Flow<Any?> = if (value == null && name !in overrides && !StateFlow::class.java.isAssignableFrom(m.returnType))
                    MutableSharedFlow() else MutableStateFlow(value).also { flows[name] = it }
                every { m.invoke(vm) } returns flow
            }
        return vm to flows
    }

    fun elementType(type: Type): Type? = (type as? ParameterizedType)?.actualTypeArguments?.firstOrNull()?.let {
        if (it is WildcardType) it.upperBounds.first() else it
    }

    fun defaultOf(type: Type?): Any? {
        val raw = when (type) { is ParameterizedType -> type.rawType as Class<*>; is Class<*> -> type; else -> return null }
        return when {
            List::class.java.isAssignableFrom(raw) || Collection::class.java.isAssignableFrom(raw) -> emptyList<Any>()
            Map::class.java.isAssignableFrom(raw) -> emptyMap<Any, Any>()
            Set::class.java.isAssignableFrom(raw) -> emptySet<Any>()
            raw == java.lang.Boolean::class.java || raw == Boolean::class.javaPrimitiveType -> false
            raw == java.lang.Integer::class.java -> 0
            raw == java.lang.Long::class.java -> 0L
            raw == java.lang.Double::class.java -> 0.0
            raw == java.lang.Float::class.java -> 0f
            raw == String::class.java -> ""
            raw.isEnum -> raw.enumConstants.first()
            else -> runCatching { raw.getDeclaredConstructor().newInstance() }.getOrNull()
        }
    }
}
