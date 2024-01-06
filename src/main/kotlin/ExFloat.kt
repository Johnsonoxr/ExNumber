package com.johnsonoxr.exnumber

import com.johnsonoxr.exnumber.ExFloat.Companion.toExFloat
import kotlin.math.*

class ExFloat private constructor(
    private val partitions: List<Int> = emptyList(),
    private val expOffset: Int = 0,
    private val positive: Boolean = true
) : Number() {

    companion object {
        const val EXP_MAX = 1_000_000
        const val DIGIT = 6
        var DIV_PRECISION = 100
            private set

        private var globalStringConverter: StringConverter = StringConverter.DEFAULT
        private var globalApproximateStrategy: ApproximateStrategy = ApproximateStrategy.ROUND_IN_2_PARTITIONS

        fun setGlobalStringConverter(stringConverter: StringConverter) {
            globalStringConverter = stringConverter
        }

        fun setGlobalApproximateStrategy(approximateStrategy: ApproximateStrategy) {
            globalApproximateStrategy = approximateStrategy
        }

        fun setDividePrecision(precision: Int) {
            require(precision > 0) { "Precision must be positive" }
            DIV_PRECISION = precision
        }

        fun String.toExFloat() = fromFloatString(this)

        fun Double.toExFloat(): ExFloat = fromFloatString(this.toString())

        fun Float.toExFloat(): ExFloat = fromFloatString(this.toString())

        fun Int.toExFloat(): ExFloat = toLong().toExFloat()

        fun Short.toExFloat(): ExFloat = toLong().toExFloat()

        fun Byte.toExFloat(): ExFloat = toLong().toExFloat()

        fun Long.toExFloat(): ExFloat {
            val partitions = mutableListOf<Int>()
            var carry = abs(this)
            var expOffset = 0
            while (carry != 0L) {
                val v = (carry % EXP_MAX).toInt()
                if (v == 0 && partitions.isEmpty()) {
                    expOffset--
                } else {
                    partitions.add((carry % EXP_MAX).toInt())
                }
                carry /= EXP_MAX
            }
            return ExFloat(partitions, expOffset, this >= 0)
        }

        private fun fromFloatString(string: String): ExFloat {
            val floatMatch = "(-?)(\\d+)(\\.\\d+)?[e|E]?(-?\\d+)?".toRegex().matchEntire(string)
            if (floatMatch != null) {
                val (signStr, intStr, decimalStr, expStr) = floatMatch.destructured
                val decimalStrWithoutDot = decimalStr.removePrefix(".")
                val exp = (expStr.toIntOrNull() ?: 0) - (decimalStrWithoutDot.length)
                val expRem = (exp % DIGIT + DIGIT) % DIGIT
                var nStr = (intStr + decimalStrWithoutDot) + "0".repeat(expRem)
                nStr = "0".repeat(DIGIT - nStr.length % DIGIT) + nStr
                val nList = nStr.chunked(DIGIT).map { it.toInt() }.asReversed()
                val expOffset = (expRem - exp) / DIGIT
                val (trimmedList, newExpOffset) = trim(nList, expOffset)
                return ExFloat(
                    partitions = trimmedList,
                    expOffset = newExpOffset,
                    positive = signStr.isEmpty()
                )
            }
            throw IllegalArgumentException("Invalid string: $string")
        }

        private fun trim(partitions: List<Int>, exp: Int): Pair<List<Int>, Int> {
            val trimStart = partitions.indexOfFirst { it != 0 }
            return when (trimStart) {
                -1 -> emptyList<Int>() to 0
                else -> partitions.subList(trimStart, partitions.indexOfLast { it != 0 } + 1) to exp - trimStart
            }
        }
    }

    interface StringConverter {

        class RoundedDecimal(private val decimalCount: Int) : StringConverter {

            init {
                require(decimalCount >= 0) { "Decimal count must be positive." }
            }

            override fun convert(exFloat: ExFloat): String {
                if (exFloat.sign == 0) return "0.0"

                val rounded = exFloat.round(-decimalCount)
                val roundedStr = DECIMAL.convert(rounded)

                return roundedStr + "0".repeat(decimalCount - roundedStr.substringAfter('.', "NA").length)
            }
        }

        class RoundedScience(private val decimalCount: Int) : StringConverter {

            init {
                require(decimalCount >= 0) { "Decimal count must be positive." }
            }

            override fun convert(exFloat: ExFloat): String {
                if (exFloat.sign == 0) return "0.0"

                val pow = log10(exFloat.partitions.last().toDouble()).toInt()
                val rounded = exFloat.round((exFloat.partitions.size - exFloat.expOffset - 1) * DIGIT + pow - decimalCount)
                return SCIENCE.convert(rounded)
            }
        }

        companion object {

            val DEFAULT: StringConverter = object : StringConverter {
                override fun convert(exFloat: ExFloat): String {
                    if (exFloat.sign == 0) return "0.0"

                    val digits = (exFloat.partitions.size - exFloat.expOffset - 1) * DIGIT + log10(exFloat.partitions.last().toDouble()).toInt()
                    return when {
                        digits < 6 -> DECIMAL.convert(exFloat)
                        else -> SCIENCE.convert(exFloat)
                    }
                }
            }

            val DECIMAL: StringConverter = object : StringConverter {
                override fun convert(exFloat: ExFloat): String {
                    if (exFloat.sign == 0) return "0.0"

                    val decimalExpTail = min(-exFloat.expOffset, 0)
                    val intExpHead = max(exFloat.partitions.size - exFloat.expOffset, 0)

                    val decimalStr = (-1 downTo decimalExpTail)
                        .joinToString("") { exFloat.getPartition(it).toString().padStart(DIGIT, '0') }
                        .trimEnd('0').ifEmpty { '0' }

                    val intStr = (intExpHead - 1 downTo 0)
                        .joinToString("") { exFloat.getPartition(it).toString().padStart(DIGIT, '0') }
                        .trimStart('0').ifEmpty { '0' }

                    return "${if (exFloat.positive) "" else "-"}${intStr}.${decimalStr}"
                }
            }

            val SCIENCE: StringConverter = object : StringConverter {
                override fun convert(exFloat: ExFloat): String {
                    if (exFloat.sign == 0) return "0.0"

                    val readableExp = (exFloat.partitions.size - exFloat.expOffset - 1) * DIGIT +
                            floor(log10(exFloat.partitions.last().toDouble())).toInt()

                    val numbers = exFloat.partitions.asReversed().joinToString("") { it.toString().padStart(DIGIT, '0') }.trim('0')
                    return "${if (exFloat.positive) "" else "-"}${numbers[0]}.${numbers.substring(1)}e${readableExp}"
                }
            }

            val DECIMAL_ROUNDED_TO_1: StringConverter = RoundedDecimal(1)
            val DECIMAL_ROUNDED_TO_2: StringConverter = RoundedDecimal(2)
            val DECIMAL_ROUNDED_TO_3: StringConverter = RoundedDecimal(3)

            val SCIENCE_ROUNDED_TO_1: StringConverter = RoundedScience(1)
            val SCIENCE_ROUNDED_TO_2: StringConverter = RoundedScience(2)
            val SCIENCE_ROUNDED_TO_3: StringConverter = RoundedScience(3)
        }

        fun convert(exFloat: ExFloat): String
    }

    interface ApproximateStrategy {

        companion object {

            val NONE: ApproximateStrategy = object : ApproximateStrategy {
                override operator fun invoke(exFloat: ExFloat): ExFloat {
                    return exFloat
                }
            }

            private class Round(val continuousPartitionCount: Int) : ApproximateStrategy {
                override fun invoke(exFloat: ExFloat): ExFloat {
                    val partitionSize = exFloat.partitions.size
                    val revList = exFloat.partitions.asReversed()
                    var zerosCnt = 0
                    var ninesCnt = 0
                    for ((i, partition) in revList.withIndex()) {
                        when (partition) {
                            0 -> {
                                zerosCnt++
                                ninesCnt = 0
                            }

                            EXP_MAX - 1 -> {
                                zerosCnt = 0
                                ninesCnt++
                            }

                            else -> {
                                zerosCnt = 0
                                ninesCnt = 0
                            }
                        }

                        if (zerosCnt >= continuousPartitionCount) {
                            val meaningfulPartitionExpOffset = partitionSize - i - 1 + continuousPartitionCount
                            return ExFloat(
                                partitions = exFloat.partitions.subList(meaningfulPartitionExpOffset, partitionSize),
                                expOffset = exFloat.expOffset - meaningfulPartitionExpOffset,
                                positive = exFloat.positive
                            )
                        }

                        if (ninesCnt >= continuousPartitionCount) {
                            val meaningfulPartitionExpOffset = partitionSize - i - 1 + continuousPartitionCount
                            return ExFloat(
                                partitions = exFloat.partitions.subList(meaningfulPartitionExpOffset, partitionSize),
                                expOffset = exFloat.expOffset - meaningfulPartitionExpOffset,
                                positive = exFloat.positive
                            ) + ExFloat(
                                partitions = listOf(1),
                                expOffset = exFloat.expOffset - meaningfulPartitionExpOffset,
                                positive = exFloat.positive
                            )
                        }
                    }
                    return exFloat
                }
            }

            val ROUND_IN_1_PARTITION: ApproximateStrategy = Round(1)
            val ROUND_IN_2_PARTITIONS: ApproximateStrategy = Round(2)
            val ROUND_IN_3_PARTITIONS: ApproximateStrategy = Round(3)
        }

        operator fun invoke(exFloat: ExFloat): ExFloat
    }

    override fun toLong(): Long {
        if (this.sign == 0) return 0L

        val expEnd = max(this.partitions.size - this.expOffset, 0)

        var result = 0L
        for (i in expEnd - 1 downTo 0) {
            result = result * EXP_MAX + this.getPartition(i)
        }
        return if (this.positive) result else -result
    }

    override fun toShort(): Short = toLong().toShort()

    override fun toDouble(): Double {
        if (this.sign == 0) return 0.0

        val expEnd = max(this.partitions.size - this.expOffset, 0)
        val expStart = min(-this.expOffset, 0)

        var result = 0.0
        for (i in expEnd - 1 downTo expStart) {
            result = result * EXP_MAX + this.getPartition(i)
        }
        result *= 10.0.pow(expStart * DIGIT)
        return if (this.positive) result else -result
    }

    override fun toFloat(): Float = toDouble().toFloat()

    override fun toInt(): Int = toLong().toInt()

    override fun toByte(): Byte = toLong().toByte()

    operator fun compareTo(number: Number): Int {
        return when (number) {
            is ExFloat -> compareTo(number)
            is Double -> toDouble().compareTo(number)
            is Float -> toFloat().compareTo(number)
            is Long -> toLong().compareTo(number)
            is Int -> toInt().compareTo(number)
            is Short -> toShort().compareTo(number)
            is Byte -> toByte().compareTo(number)
            else -> throw IllegalArgumentException("Unsupported number type: ${number::class.simpleName}")
        }
    }

    operator fun plus(number: Number): ExFloat {
        return when (number) {
            is ExFloat -> this + number
            is Double -> this + number.toExFloat()
            is Float -> this + number.toExFloat()
            is Long -> this + number.toExFloat()
            is Int -> this + number.toExFloat()
            is Short -> this + number.toExFloat()
            is Byte -> this + number.toExFloat()
            else -> throw IllegalArgumentException("Unsupported number type: ${number::class.simpleName}")
        }
    }

    operator fun minus(number: Number): ExFloat {
        return when (number) {
            is ExFloat -> this - number
            is Double -> this - number.toExFloat()
            is Float -> this - number.toExFloat()
            is Long -> this - number.toExFloat()
            is Int -> this - number.toExFloat()
            is Short -> this - number.toExFloat()
            is Byte -> this - number.toExFloat()
            else -> throw IllegalArgumentException("Unsupported number type: ${number::class.simpleName}")
        }
    }

    operator fun times(number: Number): ExFloat {
        return when (number) {
            is ExFloat -> this * number
            is Double -> this * number.toExFloat()
            is Float -> this * number.toExFloat()
            is Long -> this * number.toExFloat()
            is Int -> this * number.toExFloat()
            is Short -> this * number.toExFloat()
            is Byte -> this * number.toExFloat()
            else -> throw IllegalArgumentException("Unsupported number type: ${number::class.simpleName}")
        }
    }

    operator fun div(number: Number): ExFloat {
        return when (number) {
            is ExFloat -> this / number
            is Double -> this / number.toExFloat()
            is Float -> this / number.toExFloat()
            is Long -> this / number.toExFloat()
            is Int -> this / number.toExFloat()
            is Short -> this / number.toExFloat()
            is Byte -> this / number.toExFloat()
            else -> throw IllegalArgumentException("Unsupported number type: ${number::class.simpleName}")
        }
    }

    private fun getPartition(exp: Int): Int {
        return partitions.getOrElse(expOffset + exp) { 0 }
    }

    private fun expShiftLeft(exp: Int): ExFloat {
        return ExFloat(partitions, expOffset - exp, positive)
    }

    operator fun plus(other: ExFloat): ExFloat {
        return plusInternal(other, globalApproximateStrategy)
    }

    operator fun minus(other: ExFloat): ExFloat {
        return minusInternal(other, globalApproximateStrategy)
    }

    operator fun times(other: ExFloat): ExFloat {
        return timesInternal(other, globalApproximateStrategy)
    }

    operator fun div(other: ExFloat): ExFloat {
        return divInternal(other, globalApproximateStrategy)
    }

    private fun plusInternal(other: ExFloat, approxStrategy: ApproximateStrategy = ApproximateStrategy.NONE): ExFloat {
        if (this.sign == 0) return other
        if (other.sign == 0) return this
        if (this.positive && !other.positive) return this - -other
        if (!this.positive && other.positive) return other - -this

        val partitions = mutableListOf<Int>()
        val expStart = min(-this.expOffset, -other.expOffset)
        val expEnd = max(this.partitions.size - this.expOffset, other.partitions.size - other.expOffset)

        var carry = 0
        for (i in expStart until expEnd) {
            val sum = this.getPartition(i) + other.getPartition(i) + carry
            partitions.add(sum % EXP_MAX)
            carry = sum / EXP_MAX
        }
        if (carry != 0) {
            partitions.add(carry)
        }

        val (trimmedPartitions, trimExp) = trim(partitions, -expStart)

        val result = ExFloat(trimmedPartitions, trimExp, this.positive)

        return approxStrategy(result)
    }

    private fun minusInternal(other: ExFloat, approxStrategy: ApproximateStrategy = ApproximateStrategy.NONE): ExFloat {
        if (this.sign == 0) return -other
        if (other.sign == 0) return this
        if (this.positive != other.positive) return this + -other

        val partitions = mutableListOf<Int>()
        val expStart = min(-this.expOffset, -other.expOffset)
        val expEnd = max(this.partitions.size - this.expOffset, other.partitions.size - other.expOffset)

        val forwardMinus = this > other == this.positive
        val (minuend, subtrahend) = if (forwardMinus) this to other else other to this

        var carry = 0
        for (i in expStart until expEnd) {
            val diff = minuend.getPartition(i) - subtrahend.getPartition(i) - carry
            partitions.add(if (diff < 0) diff + EXP_MAX else diff)
            carry = if (diff < 0) 1 else 0
        }
        if (carry != 0) {
            partitions.add(carry)
        }

        val (trimmedPartitions, trimExp) = trim(partitions, -expStart)

        return approxStrategy(ExFloat(trimmedPartitions, trimExp, forwardMinus == this.positive))
    }

    private fun timesInternal(other: ExFloat, approxStrategy: ApproximateStrategy = ApproximateStrategy.NONE): ExFloat {
        if (this.sign == 0 || other.sign == 0) return ExFloat()

        val partitions = MutableList(this.partitions.size + other.partitions.size) { 0 }

        this.partitions.forEachIndexed { thisIdx, n ->
            other.partitions.forEachIndexed { otherIdx, m ->
                var idx = thisIdx + otherIdx
                var carry = n.toLong() * m.toLong()

                while (carry > 0L) {
                    val sum = carry + partitions[idx]
                    partitions[idx] = (sum % EXP_MAX).toInt()
                    carry = sum / EXP_MAX
                    idx++
                }
            }
        }

        val (trimmedPartitions, trimExp) = trim(partitions, this.expOffset + other.expOffset)

        return approxStrategy(ExFloat(trimmedPartitions, trimExp, this.positive == other.positive))
    }

    private fun divInternal(other: ExFloat, approxStrategy: ApproximateStrategy = ApproximateStrategy.NONE): ExFloat {
        if (other.sign == 0) throw ArithmeticException("Division by zero")
        if (this.sign == 0) return this

        val positiveThis = if (this.positive) this else -this
        val positiveOther = if (other.positive) other else -other

        val revPartitions = mutableListOf<Int>()

        val thisMaxExp = positiveThis.partitions.size - positiveThis.expOffset
        val otherMaxExp = positiveOther.partitions.size - positiveOther.expOffset
        var exp = thisMaxExp - otherMaxExp + 1

        var remainder = positiveThis

        val divHead: Long = positiveOther.partitions.last().toLong() * EXP_MAX +
                positiveOther.partitions.getOrElse(positiveOther.partitions.size - 2) { 0 }.toLong()

        while (remainder.sign != 0 && revPartitions.size * DIGIT < DIV_PRECISION) {
            exp--

            var d = (remainder.getPartition(exp + otherMaxExp + 1).toLong() * EXP_MAX * EXP_MAX +
                    remainder.getPartition(exp + otherMaxExp).toLong() * EXP_MAX +
                    remainder.getPartition(exp + otherMaxExp - 1).toLong())

            if (!remainder.positive) {
                d *= -1
            }

            val div = d / divHead

            revPartitions.add(div.toInt())
            for (idx in revPartitions.lastIndex downTo 1) {
                val v = revPartitions[idx]

                if (v >= EXP_MAX) {
                    val carry = v / EXP_MAX
                    revPartitions[idx] %= EXP_MAX
                    revPartitions[idx - 1] += carry
                } else if (v < 0) {
                    val carry = v / EXP_MAX - 1
                    revPartitions[idx] -= carry * EXP_MAX
                    revPartitions[idx - 1] += carry
                } else {
                    break
                }
            }

            remainder = remainder.minusInternal(positiveOther.timesInternal(div.toExFloat().expShiftLeft(exp + 1)))
        }

        val (trimmedPartitions, trimExp) = trim(revPartitions.asReversed(), -exp - 1)

        return approxStrategy(ExFloat(trimmedPartitions, trimExp, this.positive == other.positive))
    }

    operator fun compareTo(other: ExFloat): Int {
        if (this.sign != other.sign) return this.sign - other.sign

        val expStart = min(-this.expOffset, -other.expOffset)
        val expEnd = max(this.partitions.size - this.expOffset, other.partitions.size - other.expOffset)

        for (i in expEnd - 1 downTo expStart) {
            val thisPartition = this.getPartition(i)
            val otherPartition = other.getPartition(i)
            if (thisPartition != otherPartition) {
                return if (this.positive) (thisPartition - otherPartition).sign else (otherPartition - thisPartition).sign
            }
        }
        return 0
    }

    operator fun inc(): ExFloat {
        return this + 1
    }

    operator fun dec(): ExFloat {
        return this - 1
    }

    operator fun unaryPlus(): ExFloat {
        return this
    }

    operator fun unaryMinus(): ExFloat {
        return ExFloat(partitions, expOffset, !positive)
    }

    override fun toString(): String {
        return globalStringConverter.convert(this)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ExFloat -> this.compareTo(other) == 0
            else -> false
        }
    }

    override fun hashCode(): Int {
        return this.partitions.hashCode() + this.expOffset.hashCode() + this.positive.hashCode()
    }

    fun floor(digit: Int = 0): ExFloat {
        val rem = (digit % DIGIT + DIGIT) % DIGIT
        val partitionIdx = (digit - rem) / DIGIT

        val slicedPartition = getPartition(partitionIdx)

        val trimmedPartition = when {
            rem == 0 -> slicedPartition
            else -> {
                val remPow = 10.0.pow(rem).toInt()
                val trimmedPartition = slicedPartition / remPow * remPow
                trimmedPartition
            }
        }

        val partitionFrom = (partitionIdx + 1 + expOffset).coerceIn(0, partitions.size)
        val upperPartitions = listOf(trimmedPartition) + partitions.subList(partitionFrom, partitions.size)
        val (trimmedPartitions, trimExp) = trim(upperPartitions, expOffset - partitions.size + upperPartitions.size)

        return if (positive) {
            ExFloat(trimmedPartitions, trimExp, true)
        } else {
            val hasRem = (slicedPartition - trimmedPartition > 0) || partitions.subList(0, partitionFrom).any { it != 0 }
            when {
                hasRem -> ExFloat(trimmedPartitions, trimExp, false) + ExFloat(listOf(10.0.pow(rem).toInt()), -partitionIdx, false)
                else -> ExFloat(trimmedPartitions, trimExp, false)
            }
        }
    }

    fun ceil(digit: Int = 0): ExFloat {
        return -(-this).floor(digit)
    }

    fun round(digit: Int = 0): ExFloat {
        val rem = (digit % DIGIT + DIGIT) % DIGIT
        val partitionIdx = (digit - rem) / DIGIT

        val slicedPartition = getPartition(partitionIdx)

        val (trimmedPartition, nextDigit) = when {
            rem == 0 -> {
                val nextPartition = getPartition(partitionIdx - 1)
                slicedPartition to (nextPartition / (EXP_MAX / 10))
            }

            else -> {
                val remPow = 10.0.pow(rem).toInt()
                val trimmedPartition = slicedPartition / remPow * remPow
                val nextDigit = ((slicedPartition - trimmedPartition) / 10.0.pow(rem - 1).toInt())
                trimmedPartition to nextDigit
            }
        }

        val partitionFrom = (partitionIdx + 1 + expOffset).coerceIn(0, partitions.size)
        val upperPartitions = listOf(trimmedPartition) + partitions.subList(partitionFrom, partitions.size)
        val (trimmedPartitions, trimExp) = trim(upperPartitions, expOffset - partitions.size + upperPartitions.size)

        return if (nextDigit < 5) {
            ExFloat(trimmedPartitions, trimExp, positive)
        } else {
            val one = ExFloat(listOf(10.0.pow(rem).toInt()), -partitionIdx, positive)
            ExFloat(trimmedPartitions, trimExp, positive) + one
        }
    }

    fun content(): String {
        return "ExFloat(pos=$positive, expOffset=$expOffset, partitions=$partitions)"
    }

    val sign: Int = when {
        partitions.isEmpty() || partitions.all { it == 0 } -> 0
        positive -> 1
        else -> -1
    }

    fun isZero(): Boolean {
        return sign == 0
    }
}