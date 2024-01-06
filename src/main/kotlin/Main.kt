package com.johnsonoxr.exnumber

import com.johnsonoxr.exnumber.ExFloat.Companion.toExFloat
import kotlin.math.abs
import kotlin.math.pow
import kotlin.system.measureNanoTime

fun main() {

    fun String.println() = println(this)

    val testCount = 100000
    val intRange = Int.MIN_VALUE..Int.MAX_VALUE
    val longRange = Long.MIN_VALUE..Long.MAX_VALUE
    val doubleExpRange = -50..50

    val intList = mutableListOf<Pair<Int, ExFloat>>()
    val doubleList = mutableListOf<Pair<Double, ExFloat>>()

    measureNanoTime {
        repeat(testCount) {
            val n = intRange.random()
            intList.add(n to n.toExFloat())
        }
    }.let { "int to ExFloat test, average time spent: ${"%.2f".format(it / 1000f / testCount)}us".println() }

    measureNanoTime {
        repeat(testCount) {
            val double = longRange.random().toFloat() * 10.0.pow(doubleExpRange.random())
            doubleList.add(double to double.toExFloat())
        }
    }.let { "double to ExFloat test, average time spent: ${"%.2f".format(it / 1000f / testCount)}us".println() }

    measureNanoTime {
        repeat(testCount) {
            val (n1, f1) = intList.random()
            val (n2, f2) = intList.random()
            val l1 = n1.toLong()
            val l2 = n2.toLong()
            val plusResult = f1 + f2
            if (plusResult.toLong() != (l1 + l2)) {
                "Test #${it + 1} failed".println()
                throw Exception("plus test failed on $n1 + $n2, expected: ${l1 + l2}, result: ${plusResult.toLong()}, content: ${plusResult.content()}")
            }
            val minusResult = f1 - f2
            if (minusResult.toLong() != (l1 - l2)) {
                "Test #${it + 1} failed".println()
                throw Exception("minus test failed on $n1 - $n2, expected: ${l1 - l2}, result: ${minusResult.toLong()}, content: ${minusResult.content()}")
            }
        }
    }.let { "int plus/minus test passed, average time spent: ${"%.2f".format(it / 1000f / testCount)}us".println() }

    measureNanoTime {
        repeat(testCount) {
            val (n1, f1) = intList.random()
            val (n2, f2) = intList.random()
            val l1 = n1.toLong()
            val l2 = n2.toLong()
            val timesResult = f1 * f2
            if (timesResult.toLong() != (l1 * l2)) {
                "Test #${it + 1} failed".println()
                throw Exception("times test failed on $n1 * $n2, expected: ${l1 * l2}, result: ${timesResult.toLong()}, content: ${timesResult.content()}")
            }
        }
    }.let { "int times test passed, average time spent: ${"%.2f".format(it / 1000f / testCount)}us".println() }

    measureNanoTime {
        repeat(testCount) {
            val (n1, f1) = intList.random()
            val (n2, f2) = intList.random()
            val l1 = n1.toLong()
            val l2 = n2.toLong()
            val divideResult = f1 / f2
            if (divideResult.toLong() != (l1 / l2)) {
                "Test #${it + 1} failed".println()
                throw Exception("divide test failed on $n1 / $n2, expected: ${l1 / l2}, result: ${divideResult.toLong()}, content: ${divideResult.content()}")
            }
        }
    }.let { "int divide test passed, average time spent: ${"%.2f".format(it / 1000f / testCount)}us".println() }

    measureNanoTime {
        repeat(testCount) {
            val (d1, f1) = doubleList.random()
            val (d2, f2) = doubleList.random()
            val expectedPlus = d1 + d2
            val plusResult = f1 + f2
            if (abs(plusResult.toDouble() - expectedPlus) > abs(expectedPlus) * 1e-10) {
                "Test #${it + 1} failed".println()
                throw Exception("plus test failed on $d1 + $d2, expected: $expectedPlus, result: ${plusResult.toDouble()}, content: ${plusResult.content()}")
            }
            val expectedMinus = d1 - d2
            val minusResult = f1 - f2
            if (abs(minusResult.toDouble() - expectedMinus) > abs(expectedMinus) * 1e-10) {
                "Test #${it + 1} failed".println()
                throw Exception("minus test failed on $d1 - $d2, expected: $expectedMinus, result: ${minusResult.toDouble()}, content: ${minusResult.content()}")
            }
        }
    }.let { "double plus/minus test passed, average time spent: ${"%.2f".format(it / 1000f / testCount)}us".println() }

    measureNanoTime {
        repeat(testCount) {
            val (d1, f1) = doubleList.random()
            val (d2, f2) = doubleList.random()
            val expect = d1 * d2
            val timesResult = f1 * f2
            if (abs(timesResult.toDouble() - expect) > abs(expect) * 1e-10) {
                "Test #${it + 1} failed".println()
                throw Exception("times test failed on $d1 * $d2, expected: $expect, result: ${timesResult.toDouble()}, content: ${timesResult.content()}")
            }
        }
    }.let { "double times test passed, average time spent: ${"%.2f".format(it / 1000f / testCount)}us".println() }

    measureNanoTime {
        repeat(testCount) {
            val (d1, f1) = doubleList.random()
            val (d2, f2) = doubleList.random()
            val expect = d1 / d2
            val divideResult = f1 / f2
            if (abs(divideResult.toDouble() - expect) > abs(expect) * 1e-10) {
                "Test #${it + 1} failed".println()
                throw Exception("divide test failed on $d1 / $d2, expected: $expect, result: ${divideResult.toDouble()}, content: ${divideResult.content()}")
            }
        }
    }.let { "double divide test passed, average time spent: ${"%.2f".format(it / 1000f / testCount)}us".println() }
}
