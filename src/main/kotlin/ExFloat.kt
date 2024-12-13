package com.johnsonoxr.exnumber

import kotlin.math.*

class ExFloat private constructor(
    private val partitions: List<Int> = emptyList(),
    private val offset: Int = 0,
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
            var offset = 0
            while (carry != 0L) {
                val v = (carry % EXP_MAX).toInt()
                if (v == 0 && partitions.isEmpty()) {
                    offset++
                } else {
                    partitions.add(v)
                }
                carry /= EXP_MAX
            }
            return ExFloat(partitions, offset, this >= 0)
        }

        private fun fromFloatString(string: String): ExFloat {
            val floatMatch = "(-?)(\\d+)(\\.\\d+)?[e|E]?(-?\\d+)?".toRegex().matchEntire(string)
            if (floatMatch != null) {
                val (signStr, intStr, decimalStr, expStr) = floatMatch.destructured
                val decimalStrWithoutDot = decimalStr.removePrefix(".")
                val exp = (expStr.toIntOrNull() ?: 0) - (decimalStrWithoutDot.length)
                val expRem = exp.remPositive(DIGIT)
                var nStr = (intStr + decimalStrWithoutDot) + "0".repeat(expRem)
                nStr = "0".repeat(DIGIT - nStr.length % DIGIT) + nStr
                val nList = nStr.chunked(DIGIT) { it.toString().toInt() }.asReversed()
                return ExFloat(
                    partitions = nList,
                    offset = (exp - expRem) / DIGIT,
                    positive = signStr.isEmpty()
                ).trim()
            }
            throw IllegalArgumentException("Invalid string: $string")
        }

        private fun Int.remPositive(divider: Int): Int {
            val rem = this % divider
            return if (rem < 0) rem + divider else rem
        }
    }

    interface StringConverter {

        class RoundedDecimal(private val decimalCount: Int) : StringConverter {

            init {
                require(decimalCount >= 0) { "Decimal count must be positive." }
            }

            override fun convert(exFloat: ExFloat): String {
                if (exFloat.sign == 0) return if (decimalCount == 0) "0" else "0." + "0".repeat(decimalCount)

                val rounded = exFloat.round(-decimalCount)
                val roundedStr = DECIMAL.convert(rounded)

                return if (decimalCount == 0) {
                    roundedStr.substring(0, roundedStr.indexOf('.'))
                } else {
                    roundedStr + "0".repeat(decimalCount - roundedStr.substringAfter('.', "NA").length)
                }
            }
        }

        class RoundedScience(private val decimalCount: Int) : StringConverter {

            init {
                require(decimalCount >= 0) { "Decimal count must be positive." }
            }

            override fun convert(exFloat: ExFloat): String {
                if (exFloat.sign == 0) return if (decimalCount == 0) "0" else "0." + "0".repeat(decimalCount)

                val pow = log10(exFloat.partitions.last().toDouble()).toInt()
                val rounded = exFloat.round((exFloat.partitions.size + exFloat.offset - 1) * DIGIT + pow - decimalCount)

                return if (decimalCount == 0) {
                    val str = SCIENTIFIC.convert(rounded)
                    val dotIdx = str.indexOf('.')
                    val eIdx = str.indexOf('e')
                    if (dotIdx == -1) str else str.removeRange(dotIdx, eIdx)
                } else SCIENTIFIC.convert(rounded)
            }
        }

        companion object {

            val DEFAULT: StringConverter = object : StringConverter {
                override fun convert(exFloat: ExFloat): String {
                    if (exFloat.sign == 0) return "0.0"

                    val digits = (exFloat.partitions.size + exFloat.offset - 1) * DIGIT + log10(exFloat.partitions.last().toDouble()).toInt()
                    return when {
                        digits < 6 -> DECIMAL.convert(exFloat)
                        else -> SCIENTIFIC.convert(exFloat)
                    }
                }
            }

            val DECIMAL: StringConverter = object : StringConverter {
                override fun convert(exFloat: ExFloat): String {
                    if (exFloat.sign == 0) return "0.0"

                    val decimalExpTail = min(exFloat.offset, 0)
                    val intExpHead = max(exFloat.partitions.size + exFloat.offset, 0)

                    val decimalStr = (-1 downTo decimalExpTail)
                        .joinToString("") { exFloat.getPartition(it).toString().padStart(DIGIT, '0') }
                        .trimEnd('0').ifEmpty { '0' }

                    val intStr = (intExpHead - 1 downTo 0)
                        .joinToString("") { exFloat.getPartition(it).toString().padStart(DIGIT, '0') }
                        .trimStart('0').ifEmpty { '0' }

                    return "${if (exFloat.positive) "" else "-"}${intStr}.${decimalStr}"
                }
            }

            val SCIENTIFIC: StringConverter = object : StringConverter {
                override fun convert(exFloat: ExFloat): String {
                    if (exFloat.sign == 0) return "0.0e0"

                    val digits = (exFloat.partitions.size + exFloat.offset - 1) * DIGIT + log10(exFloat.partitions.last().toDouble()).toInt()

                    var numbers = exFloat.partitions.asReversed().joinToString("") { it.toString().padStart(DIGIT, '0') }.trim('0')
                    if (numbers.length < 2) numbers = numbers.padEnd(2, '0')
                    return "${if (exFloat.positive) "" else "-"}${numbers[0]}.${numbers.substring(1)}e${digits}"
                }
            }

            val DECIMAL_ROUNDED_TO_1: StringConverter = RoundedDecimal(1)
            val DECIMAL_ROUNDED_TO_2: StringConverter = RoundedDecimal(2)
            val DECIMAL_ROUNDED_TO_3: StringConverter = RoundedDecimal(3)

            val SCIENTIFIC_ROUNDED_TO_1: StringConverter = RoundedScience(1)
            val SCIENTIFIC_ROUNDED_TO_2: StringConverter = RoundedScience(2)
            val SCIENTIFIC_ROUNDED_TO_3: StringConverter = RoundedScience(3)
        }

        fun convert(exFloat: ExFloat): String
    }

    interface ApproximateStrategy {

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
                        val meaningfulOffset = partitionSize - i - 1 + continuousPartitionCount
                        return ExFloat(
                            partitions = exFloat.partitions.subList(meaningfulOffset, partitionSize),
                            offset = exFloat.offset + meaningfulOffset,
                            positive = exFloat.positive
                        )
                    }

                    if (ninesCnt >= continuousPartitionCount) {
                        val meaningfulOffset = partitionSize - i - 1 + continuousPartitionCount
                        return ExFloat(
                            partitions = exFloat.partitions.subList(meaningfulOffset, partitionSize),
                            offset = exFloat.offset + meaningfulOffset,
                            positive = exFloat.positive
                        ) + ExFloat(
                            partitions = listOf(1),
                            offset = exFloat.offset + meaningfulOffset,
                            positive = exFloat.positive
                        )
                    }
                }
                return exFloat
            }
        }

        companion object {

            val NONE: ApproximateStrategy = object : ApproximateStrategy {
                override operator fun invoke(exFloat: ExFloat): ExFloat {
                    return exFloat
                }
            }

            val ROUND_IN_1_PARTITION: ApproximateStrategy = Round(1)
            val ROUND_IN_2_PARTITIONS: ApproximateStrategy = Round(2)
            val ROUND_IN_3_PARTITIONS: ApproximateStrategy = Round(3)
        }

        operator fun invoke(exFloat: ExFloat): ExFloat
    }

    private fun trim(): ExFloat {
        val trimStart = partitions.indexOfFirst { it != 0 }
        if (trimStart == -1) return ExFloat()

        val trimEnd = partitions.indexOfLast { it != 0 }
        if (trimStart == 0 && trimEnd == partitions.lastIndex) return this

        return ExFloat(
            partitions = partitions.subList(trimStart, trimEnd + 1),
            offset = offset + trimStart,
            positive = positive
        )
    }

    override fun toLong(): Long {
        if (sign == 0) return 0L

        val offsetUpperBound = max(partitions.size + offset, 0)
        val offsetLowerBound = 0

        var result = 0L
        for (i in offsetUpperBound - 1 downTo offsetLowerBound) {
            result = result * EXP_MAX + getPartition(i)
        }
        return if (positive) result else -result
    }

    override fun toShort(): Short = toLong().toShort()

    override fun toDouble(): Double {
        if (sign == 0) return 0.0

        val offsetUpperBound = max(partitions.size + offset, 0)
        val offsetLowerBound = min(offset, 0)

        var result = 0.0
        for (i in offsetUpperBound - 1 downTo offsetLowerBound) {
            result = result * EXP_MAX + getPartition(i)
        }
        result *= 10.0.pow(offsetLowerBound * DIGIT)
        return if (positive) result else -result
    }

    override fun toFloat(): Float = toDouble().toFloat()

    override fun toInt(): Int = toLong().toInt()

    override fun toByte(): Byte = toLong().toByte()

    operator fun compareTo(number: Number): Int {
        return when (number) {
            is ExFloat -> compareTo(number)
            is Double -> compareTo(number.toExFloat())
            is Float -> compareTo(number.toExFloat())
            is Long -> compareTo(number.toExFloat())
            is Int -> compareTo(number.toExFloat())
            is Short -> compareTo(number.toExFloat())
            is Byte -> compareTo(number.toExFloat())
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

    private fun getPartition(offset: Int): Int {
        return partitions.getOrElse(offset - this.offset) { 0 }
    }

    private fun expShiftLeft(offset: Int): ExFloat {
        return ExFloat(partitions, this.offset + offset, positive)
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
        val offsetStart = min(this.offset, other.offset)
        val offsetEnd = max(this.partitions.size + this.offset, other.partitions.size + other.offset)

        var carry = 0
        for (i in offsetStart until offsetEnd) {
            val sum = this.getPartition(i) + other.getPartition(i) + carry
            partitions.add(sum % EXP_MAX)
            carry = sum / EXP_MAX
        }
        if (carry != 0) {
            partitions.add(carry)
        }

        return approxStrategy(ExFloat(partitions, offsetStart, this.positive).trim())
    }

    private fun minusInternal(other: ExFloat, approxStrategy: ApproximateStrategy = ApproximateStrategy.NONE): ExFloat {
        if (this.sign == 0) return -other
        if (other.sign == 0) return this
        if (this.positive != other.positive) return this + -other

        val partitions = mutableListOf<Int>()
        val offsetStart = min(this.offset, other.offset)
        val offsetEnd = max(this.partitions.size + this.offset, other.partitions.size + other.offset)

        val forwardMinus = this > other == this.positive
        val (minuend, subtrahend) = if (forwardMinus) this to other else other to this

        var carry = 0
        for (i in offsetStart until offsetEnd) {
            val diff = minuend.getPartition(i) - subtrahend.getPartition(i) - carry
            partitions.add(if (diff < 0) diff + EXP_MAX else diff)
            carry = if (diff < 0) 1 else 0
        }
        if (carry != 0) {
            partitions.add(carry)
        }

        return approxStrategy(ExFloat(partitions, offsetStart, forwardMinus == this.positive).trim())
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

        return approxStrategy(ExFloat(partitions, this.offset + other.offset, this.positive == other.positive).trim())
    }

    private fun divInternal(other: ExFloat, approxStrategy: ApproximateStrategy = ApproximateStrategy.NONE): ExFloat {
        if (other.sign == 0) throw ArithmeticException("Division by zero")
        if (this.sign == 0) return this

        val positiveThis = if (this.positive) this else -this
        val positiveOther = if (other.positive) other else -other

        val revPartitions = mutableListOf<Int>()

        val thisMaxOffset = positiveThis.partitions.size + positiveThis.offset
        val otherMaxOffset = positiveOther.partitions.size + positiveOther.offset
        var curOffset = thisMaxOffset - otherMaxOffset + 1

        var remainder = positiveThis

        val divHead: Long = positiveOther.partitions.last().toLong() * EXP_MAX +
                positiveOther.partitions.getOrElse(positiveOther.partitions.size - 2) { 0 }.toLong()

        while (remainder.sign != 0 && revPartitions.size * DIGIT < DIV_PRECISION) {
            curOffset--

            var d = (remainder.getPartition(curOffset + otherMaxOffset + 1).toLong() * EXP_MAX * EXP_MAX +
                    remainder.getPartition(curOffset + otherMaxOffset).toLong() * EXP_MAX +
                    remainder.getPartition(curOffset + otherMaxOffset - 1).toLong())

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

            remainder = remainder.minusInternal(positiveOther.timesInternal(div.toExFloat().expShiftLeft(curOffset + 1)))
        }

        return approxStrategy(ExFloat(revPartitions.asReversed(), curOffset + 1, this.positive == other.positive).trim())
    }

    operator fun compareTo(other: ExFloat): Int {
        if (this.sign != other.sign) return this.sign - other.sign

        val offsetStart = min(this.offset, other.offset)
        val offsetEnd = max(this.partitions.size + this.offset, other.partitions.size + other.offset)

        for (i in offsetEnd - 1 downTo offsetStart) {
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
        return ExFloat(partitions, offset, !positive)
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
        return this.partitions.hashCode() + this.offset + this.positive.hashCode()
    }

    fun floor(digit: Int = 0): ExFloat {
        val rem = digit.remPositive(DIGIT)
        val sliceOffset = (digit - rem) / DIGIT

        val slicedPartition = getPartition(sliceOffset)

        val trimmedPartition = when {
            rem == 0 -> slicedPartition
            else -> {
                val remPow = 10.0.pow(rem).toInt()
                val trimmedPartition = slicedPartition / remPow * remPow
                trimmedPartition
            }
        }

        val partitionStartIndex = (sliceOffset + 1 - offset).coerceIn(0, partitions.size)
        val upperPartitions = listOf(trimmedPartition) + partitions.subList(partitionStartIndex, partitions.size)
        val trimmedExFloat = ExFloat(upperPartitions, offset + partitions.size - upperPartitions.size, positive).trim()

        return if (positive) {
            trimmedExFloat
        } else {
            val hasRem = (slicedPartition - trimmedPartition > 0) || partitions.subList(0, partitionStartIndex).any { it != 0 }
            when {
                hasRem -> trimmedExFloat + ExFloat(listOf(10.0.pow(rem).toInt()), sliceOffset, false)
                else -> trimmedExFloat
            }
        }
    }

    fun ceil(digit: Int = 0): ExFloat {
        return -(-this).floor(digit)
    }

    fun round(digit: Int = 0): ExFloat {
        val rem = digit.remPositive(DIGIT)
        val sliceOffset = (digit - rem) / DIGIT

        val slicedPartition = getPartition(sliceOffset)

        val (trimmedPartition, nextDigit) = when {
            rem == 0 -> {
                val nextPartition = getPartition(sliceOffset - 1)
                slicedPartition to (nextPartition / (EXP_MAX / 10))
            }

            else -> {
                val remPow = 10.0.pow(rem).toInt()
                val trimmedPartition = slicedPartition / remPow * remPow
                val nextDigit = ((slicedPartition - trimmedPartition) / 10.0.pow(rem - 1).toInt())
                trimmedPartition to nextDigit
            }
        }

        val partitionStartIndex = (sliceOffset + 1 - offset).coerceIn(0, partitions.size)
        val upperPartitions = listOf(trimmedPartition) + partitions.subList(partitionStartIndex, partitions.size)
        val trimmedExFloat = ExFloat(upperPartitions, offset + partitions.size - upperPartitions.size, positive).trim()

        return if (nextDigit < 5) {
            trimmedExFloat
        } else {
            trimmedExFloat + ExFloat(listOf(10.0.pow(rem).toInt()), sliceOffset, positive)
        }
    }

    fun content(): String {
        return "ExFloat(pos=$positive, expOffset=$offset, partitions=$partitions)"
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