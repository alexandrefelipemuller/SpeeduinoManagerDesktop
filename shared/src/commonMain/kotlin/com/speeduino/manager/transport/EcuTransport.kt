package com.speeduino.manager.transport

import com.speeduino.manager.SpeeduinoLiveData
import com.speeduino.manager.definition.IniDefinition
import com.speeduino.manager.ecu.FirmwareInfo
import com.speeduino.manager.model.AfrTable
import com.speeduino.manager.model.DwellTable
import com.speeduino.manager.model.ClosedLoopCorrectionConfig
import com.speeduino.manager.model.EcuCapabilities
import com.speeduino.manager.model.EcuFamily
import com.speeduino.manager.model.EcuPageDescriptor
import com.speeduino.manager.model.EngineConstants
import com.speeduino.manager.model.EngineProtectionConfig
import com.speeduino.manager.model.IdleControlSettings
import com.speeduino.manager.model.IgnitionTable
import com.speeduino.manager.model.PinLayoutInfo
import com.speeduino.manager.model.PressureCalibration
import com.speeduino.manager.model.RusefiInputOutputSnapshot
import com.speeduino.manager.model.SecondarySerialConfig
import com.speeduino.manager.model.TableDefinitions
import com.speeduino.manager.model.TpsCalibration
import com.speeduino.manager.model.TriggerSettings
import com.speeduino.manager.model.VeTable
import com.speeduino.manager.protocol.SerialCapability

interface EcuTransport {
    suspend fun connect()

    fun disconnect()

    fun isConnected(): Boolean

    fun isStreaming(): Boolean

    fun startLiveDataStream(intervalMs: Long = 100)

    fun stopLiveDataStream()

    suspend fun pauseLiveDataStream(timeoutMs: Long = 6000) {
        stopLiveDataStream()
    }

    suspend fun getFirmwareInfo(): String

    fun getFirmwareInfoCached(): FirmwareInfo?

    fun getEcuFamily(): EcuFamily

    fun getEcuCapabilities(): EcuCapabilities?

    fun getTableDefinitions(): TableDefinitions?

    fun getEcuPageCatalog(): List<EcuPageDescriptor> = emptyList()

    fun getPinLayoutInfoCached(): PinLayoutInfo? = null

    fun getConnectionProfileTag(): String? = null

    fun getConnectionInfo(): String = "unknown"

    fun getProductString(): String = "Unknown"

    fun cachePinLayoutInfo(info: PinLayoutInfo?) {}

    fun isReadOnlySafeMode(): Boolean = false

    fun applyIniDefinition(definition: IniDefinition): Boolean = false

    fun setManualFirmwareProfile(signature: String, readOnly: Boolean = true) {}

    fun clearManualFirmwareProfile() {}

    suspend fun getSerialCapability(): SerialCapability =
        SerialCapability(protocolVersion = 1, blockingFactor = 256, tableBlockingFactor = 256)

    suspend fun readFullPage(pageNum: Int, pageSize: Int, blockSize: Int): ByteArray =
        throw UnsupportedOperationException("Page read not supported by this transport")

    suspend fun readVeTable(mapIndex: Int = 1): VeTable =
        throw UnsupportedOperationException("VE table not supported by this transport")

    suspend fun readIgnitionTable(mapIndex: Int = 1): IgnitionTable =
        throw UnsupportedOperationException("Ignition table not supported by this transport")

    suspend fun readDwellTable(mapIndex: Int = 1): DwellTable =
        throw UnsupportedOperationException("Dwell table not supported by this transport")

    suspend fun readAfrTable(): AfrTable =
        throw UnsupportedOperationException("AFR table not supported by this transport")

    suspend fun readEngineConstants(): EngineConstants =
        throw UnsupportedOperationException("Engine constants not supported by this transport")

    suspend fun readTriggerSettings(): TriggerSettings =
        throw UnsupportedOperationException("Trigger settings not supported by this transport")

    suspend fun readRusefiInputOutputSnapshot(): RusefiInputOutputSnapshot =
        throw UnsupportedOperationException("I/O snapshot not supported by this transport")

    suspend fun readEngineProtectionConfig(): EngineProtectionConfig =
        throw UnsupportedOperationException("Engine protection not supported by this transport")

    suspend fun readIdleControlSettings(): IdleControlSettings =
        throw UnsupportedOperationException("Idle control not supported by this transport")

    suspend fun writeIdleControlSettings(settings: IdleControlSettings, burn: Boolean = true): Unit =
        throw UnsupportedOperationException("Idle control write not supported by this transport")

    suspend fun readClosedLoopCorrectionConfig(): ClosedLoopCorrectionConfig =
        throw UnsupportedOperationException("Closed-loop correction not supported by this transport")

    suspend fun writeClosedLoopCorrectionConfig(
        config: ClosedLoopCorrectionConfig,
        burn: Boolean = true,
    ): Unit = throw UnsupportedOperationException("Closed-loop correction write not supported by this transport")

    suspend fun readMapSelectionSupport(): MapSelectionSupport =
        throw UnsupportedOperationException("Map selection support not supported by this transport")

    suspend fun readPressureCalibration(): PressureCalibration =
        throw UnsupportedOperationException("Pressure calibration not supported by this transport")

    suspend fun writePressureCalibration(calibration: PressureCalibration, burn: Boolean = true): Unit =
        throw UnsupportedOperationException("Pressure calibration write not supported by this transport")

    suspend fun readTpsCalibration(): TpsCalibration =
        throw UnsupportedOperationException("TPS calibration not supported by this transport")

    suspend fun writeTpsCalibration(calibration: TpsCalibration, burn: Boolean = true): Unit =
        throw UnsupportedOperationException("TPS calibration write not supported by this transport")

    suspend fun readSecondarySerialConfig(): SecondarySerialConfig =
        throw UnsupportedOperationException("Secondary serial config not supported by this transport")

    suspend fun writeSecondarySerialConfig(config: SecondarySerialConfig, burn: Boolean = true): Unit =
        throw UnsupportedOperationException("Secondary serial config write not supported by this transport")

    suspend fun writeEngineProtectionConfig(config: EngineProtectionConfig, burn: Boolean = true): Unit =
        throw UnsupportedOperationException("Engine protection write not supported by this transport")

    suspend fun writeTriggerSettings(settings: TriggerSettings, burn: Boolean = true): Unit =
        throw UnsupportedOperationException("Trigger settings write not supported by this transport")

    suspend fun writeRawPage(pageNum: Int, data: ByteArray): Unit =
        throw UnsupportedOperationException("Raw page write not supported by this transport")

    suspend fun writeRawPageWithoutBurn(pageNum: Int, data: ByteArray): Unit =
        throw UnsupportedOperationException("Raw page write not supported by this transport")

    suspend fun burnConfigs(): Unit =
        throw UnsupportedOperationException("Burn not supported by this transport")

    suspend fun writeVeTable(veTable: VeTable, mapIndex: Int = 1): Unit =
        throw UnsupportedOperationException("VE table write not supported by this transport")

    suspend fun writeIgnitionTable(ignitionTable: IgnitionTable, mapIndex: Int = 1): Unit =
        throw UnsupportedOperationException("Ignition table write not supported by this transport")

    suspend fun writeDwellTable(dwellTable: DwellTable, mapIndex: Int = 1): Unit =
        throw UnsupportedOperationException("Dwell table write not supported by this transport")

    suspend fun writeAfrTable(afrTable: AfrTable): Unit =
        throw UnsupportedOperationException("AFR table write not supported by this transport")

    suspend fun writeEngineConstants(engineConstants: EngineConstants): Unit =
        throw UnsupportedOperationException("Engine constants write not supported by this transport")
}
