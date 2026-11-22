package com.trader.testing.recomposition

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
            .filter { it.parameterCount == 0 && (it.name.startsWith("get") || it.name.startsWith("is")) && Flow::class.java.isAssignableFrom(it.returnType) }
            .forEach { m ->
                // Kotlin names the getter of `isX` just `isX()`.
                val name = if (m.name.startsWith("is")) m.name else m.name.removePrefix("get").replaceFirstChar { it.lowercase() }
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
            raw == Boolean::class.javaObjectType || raw == Boolean::class.javaPrimitiveType -> false
            raw == Int::class.javaObjectType -> 0
            raw == Long::class.javaObjectType -> 0L
            raw == Double::class.javaObjectType -> 0.0
            raw == Float::class.javaObjectType -> 0f
            raw == String::class.java -> ""
            raw == Any::class.java -> null
            raw.isEnum -> raw.enumConstants.first()
            else -> runCatching { raw.getDeclaredConstructor().newInstance() }.getOrNull()
        }
    }
}
