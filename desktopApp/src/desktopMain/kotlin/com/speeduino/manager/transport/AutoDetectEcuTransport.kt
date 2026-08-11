package com.speeduino.manager.transport

import io.ecucore.transport.EcuTransport
import io.ecucore.definition.IniDefinition
import io.ecucore.ecu.FirmwareInfo
import io.ecucore.model.AfrTable
import io.ecucore.model.EcuCapabilities
import io.ecucore.model.EcuFamily
import io.ecucore.model.EcuPageDescriptor
import io.ecucore.model.EngineConstants
import io.ecucore.model.EngineProtectionConfig
import io.ecucore.model.IgnitionTable
import io.ecucore.model.PinLayoutInfo
import io.ecucore.model.PressureCalibration
import io.ecucore.model.RusefiInputOutputSnapshot
import io.ecucore.model.SecondarySerialConfig
import io.ecucore.model.TableDefinitions
import io.ecucore.model.TpsCalibration
import io.ecucore.model.TriggerSettings
import io.ecucore.model.VeTable
import io.ecucore.protocol.SerialCapability
import com.speeduino.manager.telemetry.ConnectionDiagnosticsLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tenta protocolo padrão (Speeduino/Mega/rusEFI) e faz fallback para OBD2.
 */
class AutoDetectEcuTransport(
    private val primaryTransport: EcuTransport,
    private val obd2FallbackTransport: EcuTransport,
) : EcuTransport {
    private val connectMutex = Mutex()
    private var activeTransport: EcuTransport? = null

    override suspend fun connect() {
        connectMutex.withLock {
            val current = activeTransport
            if (current?.isConnected() == true) {
                return
            }

            val primaryError = runCatching {
                primaryTransport.connect()
                activeTransport = primaryTransport
                ConnectionDiagnosticsLogger.log("autodetect", "protocol", "selected=primary")
            }.exceptionOrNull()

            if (primaryError == null) {
                return
            }
            if (primaryError is CancellationException) {
                throw primaryError
            }

            runCatching { primaryTransport.disconnect() }

            val fallbackError = runCatching {
                obd2FallbackTransport.connect()
                activeTransport = obd2FallbackTransport
                ConnectionDiagnosticsLogger.log("autodetect", "protocol", "selected=obd2_fallback")
            }.exceptionOrNull()

            if (fallbackError == null) {
                return
            }
            if (fallbackError is CancellationException) {
                throw fallbackError
            }

            activeTransport = null
            runCatching { obd2FallbackTransport.disconnect() }

            val primaryMessage = primaryError.message ?: primaryError.javaClass.simpleName
            val fallbackMessage = fallbackError.message ?: fallbackError.javaClass.simpleName
            throw IllegalStateException(
                "Falha no protocolo padrão e no fallback OBD2. " +
                    "Padrão: $primaryMessage | OBD2: $fallbackMessage",
                fallbackError
            )
        }
    }

    override fun disconnect() {
        runCatching { primaryTransport.disconnect() }
        runCatching { obd2FallbackTransport.disconnect() }
        activeTransport = null
    }

    override fun isConnected(): Boolean {
        return activeTransport?.isConnected() == true
    }

    private fun selected(): EcuTransport = activeTransport ?: primaryTransport

    override fun isStreaming(): Boolean = selected().isStreaming()

    override fun startLiveDataStream(intervalMs: Long) = selected().startLiveDataStream(intervalMs)

    override fun stopLiveDataStream() = selected().stopLiveDataStream()

    override suspend fun pauseLiveDataStream(timeoutMs: Long) = selected().pauseLiveDataStream(timeoutMs)

    override suspend fun getFirmwareInfo(): String = selected().getFirmwareInfo()

    override fun getFirmwareInfoCached(): FirmwareInfo? = selected().getFirmwareInfoCached()

    override fun getEcuFamily(): EcuFamily = selected().getEcuFamily()

    override fun getEcuCapabilities(): EcuCapabilities? = selected().getEcuCapabilities()

    override fun getTableDefinitions(): TableDefinitions? = selected().getTableDefinitions()

    override fun getEcuPageCatalog(): List<EcuPageDescriptor> = selected().getEcuPageCatalog()

    override fun getPinLayoutInfoCached(): PinLayoutInfo? = selected().getPinLayoutInfoCached()

    override fun getConnectionProfileTag(): String? = selected().getConnectionProfileTag()

    override fun cachePinLayoutInfo(info: PinLayoutInfo?) = selected().cachePinLayoutInfo(info)

    override fun isReadOnlySafeMode(): Boolean = selected().isReadOnlySafeMode()

    override fun applyIniDefinition(definition: IniDefinition): Boolean = selected().applyIniDefinition(definition)

    override fun setManualFirmwareProfile(signature: String, readOnly: Boolean) {
        primaryTransport.setManualFirmwareProfile(signature, readOnly)
        obd2FallbackTransport.setManualFirmwareProfile(signature, readOnly)
    }

    override suspend fun getSerialCapability(): SerialCapability = selected().getSerialCapability()

    override suspend fun readFullPage(pageNum: Int, pageSize: Int, blockSize: Int): ByteArray =
        selected().readFullPage(pageNum, pageSize, blockSize)

    override suspend fun readVeTable(mapIndex: Int): VeTable = selected().readVeTable(mapIndex)

    override suspend fun readIgnitionTable(mapIndex: Int): IgnitionTable = selected().readIgnitionTable(mapIndex)

    override suspend fun readAfrTable(): AfrTable = selected().readAfrTable()

    override suspend fun readEngineConstants(): EngineConstants = selected().readEngineConstants()

    override suspend fun readTriggerSettings(): TriggerSettings = selected().readTriggerSettings()

    override suspend fun readRusefiInputOutputSnapshot(): RusefiInputOutputSnapshot =
        selected().readRusefiInputOutputSnapshot()

    override suspend fun readEngineProtectionConfig(): EngineProtectionConfig =
        selected().readEngineProtectionConfig()

    override suspend fun readIdleControlSettings() = selected().readIdleControlSettings()

    override suspend fun writeIdleControlSettings(
        settings: io.ecucore.model.IdleControlSettings,
        burn: Boolean,
    ) = selected().writeIdleControlSettings(settings, burn)

    override suspend fun readClosedLoopCorrectionConfig() = selected().readClosedLoopCorrectionConfig()

    override suspend fun writeClosedLoopCorrectionConfig(
        config: io.ecucore.model.ClosedLoopCorrectionConfig,
        burn: Boolean,
    ) = selected().writeClosedLoopCorrectionConfig(config, burn)

    override suspend fun readMapSelectionSupport() = selected().readMapSelectionSupport()

    override suspend fun readPressureCalibration(): PressureCalibration = selected().readPressureCalibration()

    override suspend fun writePressureCalibration(calibration: PressureCalibration, burn: Boolean) =
        selected().writePressureCalibration(calibration, burn)

    override suspend fun readTpsCalibration(): TpsCalibration = selected().readTpsCalibration()

    override suspend fun writeTpsCalibration(calibration: TpsCalibration, burn: Boolean) =
        selected().writeTpsCalibration(calibration, burn)

    override suspend fun readSecondarySerialConfig(): SecondarySerialConfig =
        selected().readSecondarySerialConfig()

    override suspend fun writeSecondarySerialConfig(config: SecondarySerialConfig, burn: Boolean) =
        selected().writeSecondarySerialConfig(config, burn)

    override suspend fun writeEngineProtectionConfig(config: EngineProtectionConfig, burn: Boolean) =
        selected().writeEngineProtectionConfig(config, burn)

    override suspend fun writeTriggerSettings(settings: TriggerSettings, burn: Boolean) =
        selected().writeTriggerSettings(settings, burn)

    override suspend fun writeRawPage(pageNum: Int, data: ByteArray) = selected().writeRawPage(pageNum, data)

    override suspend fun writeRawPageWithoutBurn(pageNum: Int, data: ByteArray) =
        selected().writeRawPageWithoutBurn(pageNum, data)

    override suspend fun burnConfigs() = selected().burnConfigs()

    override suspend fun writeVeTable(veTable: VeTable, mapIndex: Int) =
        selected().writeVeTable(veTable, mapIndex)

    override suspend fun writeIgnitionTable(ignitionTable: IgnitionTable, mapIndex: Int) =
        selected().writeIgnitionTable(ignitionTable, mapIndex)

    override suspend fun writeAfrTable(afrTable: AfrTable) = selected().writeAfrTable(afrTable)

    override suspend fun writeEngineConstants(engineConstants: EngineConstants) =
        selected().writeEngineConstants(engineConstants)
}
