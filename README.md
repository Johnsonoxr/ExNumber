# ExNumber - Arithmetic for Large Float in Kotlin

ExNumber is a project designed for performing arithmetic operations on large numbers. This project aims to provide a simple way to easily perform addition, subtraction, multiplication, and division operations on numbers without the constraints of standard data type representations.

## Features

- **Large Number Support:** Handles numbers beyond the range of standard data types, suitable for scenarios requiring high-precision calculations.

- **Arithmetic Operations:** Provides basic arithmetic operations, including addition, subtraction, multiplication, and division, allowing users to conveniently perform mathematical computations.

- **Ease of Use:** Concise API design make it easy for developers to quickly get started and use ExNumber for large number calculations.

## Usage
### Numeric type conversion
- ExNumber simplifies numeric type conversions for convenient arithmetic operations:
```kotlin
    123456.toExFloat()          // Integer to ExFloat
    123456L.toExFloat()         // Long to ExFloat
    123456f.toExFloat()         // Float to ExFloat
    123456.0.toExFloat()        // Double to ExFloat
    "123456".toExFloat()        // String to ExFloat
    "123456.123456".toExFloat() // String to ExFloat
    "123456e-32".toExFloat()    // String with exponent to ExFloat
    "-123.456e32".toExFloat()   // String with exponent to ExFloat
```
### Arithmetic operations
```kotlin
    val a = 123456789123321123L.toExFloat()
    val b = "987654321987654321.654654654654".toExFloat()

    println("Addition: ${a + b}")
    // Addition: 1.111111111110975444654654654654e18

    println("Subtraction: ${a - b}")
    // Subtraction: -8.64197532864333198654654654654e17

    println("Multiplication: ${a * b}")
    // Multiplication: 1.21932631356366540316777953649741976195108456442e35

    println("Division: ${a / b}")
    // Division: 0.124999998860800138106522631206843732498620095875040523243307367428636522459835048717781526
```
### Seamless Numerical Operations
- ExNumber seamlessly integrates with primitive data types like Int, Long, Double, and Float for easy numerical operations:
```kotlin
    123456.toExFloat() * 321    // Same as 123456.toExFloat() * 321.toExFloat()
```
### Rounding
- In addition to basic arithmetic operations, ExNumber provides helpful functions for rounding:
```kotlin
    112233.556677.toExFloat().round()   // Round to nearest integer
    // 112234.0
    112233.556677.toExFloat().round(3)  // Round to nearest 1000
    // 112000.0
    112233.556677.toExFloat().round(-3) // Round to 3 decimal places
    // 112233.557

    112233.556677.toExFloat().ceil()    // Round up to nearest integer
    112233.556677.toExFloat().floor()   // Round down to nearest integer
```
### Approximation
- ExNumber automatically approximates values to maintain precision during arithmetic operations:
```kotlin
    val oneThird = 1.toExFloat() / 3        // 0.333333333333333333333333333333333333333333333
    val twoThirds = 2.toExFloat() / 3       // 0.666666666666666666666666666666666666666666666
    oneThird * 3                            // 1.0
    oneThird + twoThirds                    // 1.0
```
- However, if you want to disable this mechanism and keep the exact representation of the numbers, here's what you can do:
```kotlin
    ExFloat.setGlobalApproximateStrategy(ExFloat.ApproximateStrategy.NONE)
    val oneThird = 1.toExFloat() / 3        // 0.333333333333333333333333333333333333333333333
    val twoThirds = 2.toExFloat() / 3       // 0.666666666666666666666666666666666666666666666
    oneThird * 3                            // 0.999999999999999999999999999999999999999999999
    oneThird + twoThirds                    // 0.999999999999999999999999999999999999999999999
```
### String Conversion
- Although ExNumber has a default `toString()` method, you can customize it using the `ExFloat.setGlobalStringConverter()` method.
```kotlin
    val largePI = "3.14159265358979323846264338327950288419716939937510".toExFloat()
    val billionPI = largePI * 1e9

    ExFloat.setGlobalStringConverter(ExFloat.StringConverter.DECIMAL)
    println(billionPI)
    // 3141592653.5897932384626433832795028841971693993751

    ExFloat.setGlobalStringConverter(ExFloat.StringConverter.SCIENTIFIC)
    println(billionPI)
    // 3.1415926535897932384626433832795028841971693993751e9

    ExFloat.setGlobalStringConverter(ExFloat.StringConverter.DECIMAL_ROUNDED_TO_3)
    println(billionPI)
    // 3141592653.590

    ExFloat.setGlobalStringConverter(ExFloat.StringConverter.SCIENTIFIC_ROUNDED_TO_3)
    println(billionPI)
    // 3.142e9
```
## Installation

To use ExNumber in your project, add the following lines to your `build.gradle.kts` file:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("com.github.johnsonoxr:exnumber:v1.0.4")
}
```

## Motivation

The inspiration for this project arose from a specific challenge encountered during the "AOC2023 Day24 part2." The task involved significant computations with large numbers, leading to time-consuming efforts and the need to address primary overflow issues.

In an effort to streamline and simplify such large-number calculations, this project was initiated. Its primary goal is to provide a reliable and efficient solution for high-precision arithmetic, allowing developers to tackle similar challenges without the constraints of standard data types.

## Contact/Support
For any inquiries or support, please contact me at [johnsonoxr@gmail.com]
