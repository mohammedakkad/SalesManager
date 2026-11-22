package com.trader.testing.recomposition

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Test-only recomposition counter (a Layout Inspector substitute for JVM tests).
 *
 * The Compose compiler emits a trace marker at the start of every composable body that actually
 * runs; skipped calls emit nothing. Installing a [CompositionTracer] therefore counts real
 * (re)compositions per composable, keyed by its fully qualified name. It lives only in unit-test
 * source sets, so it can never be part of a debug or release APK.
 */
@OptIn(InternalComposeTracingApi::class)
object RecompositionTracker {
    private val counts = ConcurrentHashMap<String, AtomicInteger>()
    @Volatile private var active = false

    private val tracer = object : CompositionTracer {
        override fun isTraceInProgress() = active
        override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
            counts.getOrPut(nameOf(info)) { AtomicInteger() }.incrementAndGet()
        }
        override fun traceEventEnd() = Unit
    }

    fun start() {
        counts.clear()
        Composer.setTracer(tracer)
        active = true
    }

    fun stop() { active = false }

    fun reset() = counts.clear()

    /** Snapshot of executions per composable, optionally limited to names starting with [prefix]. */
    fun snapshot(prefix: String = ""): Map<String, Int> =
        counts.filterKeys { it.startsWith(prefix) }.mapValues { it.value.get() }.toSortedMap()

    fun total(prefix: String = ""): Int = snapshot(prefix).values.sum()

    /** "com.x.Foo (Foo.kt:12)" -> "com.x.Foo"; lambdas keep their location ("...<anonymous> (Foo.kt:40)"). */
    private fun nameOf(info: String): String {
        val name = info.substringBefore(" (")
        return if (name.isBlank() || name.contains("<anonymous>")) info.substringBefore(" @") else name
    }
}
