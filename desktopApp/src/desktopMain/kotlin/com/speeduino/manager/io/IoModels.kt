package com.speeduino.manager.io

import java.util.UUID

enum class IoChannelType {
    SENSOR,
    ACTUATOR,
    VIRTUAL
}

enum class IoStatus {
    OK,
    ERROR,
    LOCAL_ONLY
}

data class IoChannel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: IoChannelType,
    val enabled: Boolean = true,
    val config: IoConfig
)

sealed interface IoConfig {
    val kind: IoChannelType
}

data class SensorConfig(
    val sourceChannel: String,
    val conversion: Conversion
) : IoConfig {
    override val kind: IoChannelType = IoChannelType.SENSOR
}

data class VirtualConfig(
    val mode: VirtualMode,
    val sourceChannel: String? = null,
    val operation: VirtualOperationConfig? = null,
    val outputFormat: VirtualOutputFormat = VirtualOutputFormat.ZERO_ONE,
    val expression: String? = null
) : IoConfig {
    override val kind: IoChannelType = IoChannelType.VIRTUAL
}

data class ActuatorConfig(
    val outputName: String,
    val outputType: ActuatorType,
    val controlMode: ActuatorControlMode,
    val expression: String? = null,
    val manualValue: Double? = null
) : IoConfig {
    override val kind: IoChannelType = IoChannelType.ACTUATOR
}

sealed interface Conversion {
    val unit: String
}

data class LinearConversion(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    override val unit: String
) : Conversion

data class TableConversion(
    val points: List<TablePoint>,
    override val unit: String
) : Conversion

data class ExpressionConversion(
    val expression: String,
    override val unit: String
) : Conversion

data class TablePoint(
    val x: Double,
    val y: Double
)

enum class VirtualMode {
    SIMPLE,
    EXPRESSION
}

enum class VirtualOperation {
    BIT_ACTIVE,
    COMPARE
}

enum class CompareOp {
    LT,
    LTE,
    GT,
    GTE,
    EQ,
    NEQ
}

enum class VirtualOutputFormat {
    ZERO_ONE,
    INACTIVE_ACTIVE,
    OFF_ON
}

enum class ActuatorType {
    DIGITAL,
    PWM
}

enum class ActuatorControlMode {
    MANUAL,
    EXPRESSION
}

sealed interface VirtualOperationConfig {
    val opType: VirtualOperation
}

data class VirtualBitConfig(
    val bit: Int
) : VirtualOperationConfig {
    override val opType: VirtualOperation = VirtualOperation.BIT_ACTIVE
}

data class VirtualCompareConfig(
    val compareOp: CompareOp,
    val compareValue: Double
) : VirtualOperationConfig {
    override val opType: VirtualOperation = VirtualOperation.COMPARE
}
