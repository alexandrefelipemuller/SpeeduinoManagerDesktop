package com.speeduino.manager.transport

import com.speeduino.manager.definition.IniDefinition
import com.speeduino.manager.ecu.FirmwareInfo
import com.speeduino.manager.model.AfrTable
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
import com.speeduino.manager.telemetry.ConnectionDiagnosticsLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Usa o fluxo OBD2 genérico como entrada rápida e promove para um fluxo proprietário
 * quando o fingerprint básico do veículo indicar uma família suportada.
 */
class PromotingObd2Transport(
    private val genericTransport: Obd2Transport,
    private val psaTransport: EcuTransport,
) : EcuTransport {
    private val connectMutex = Mutex()
    private var activeTransport: EcuTransport? = null

    override suspend fun connect() {
        connectMutex.withLock {
            val current = activeTransport
            if (current?.isConnected() == true) {
                return
            }

            genericTransport.connect()
            activeTransport = genericTransport

            val brandHint = genericTransport.getVehicleBrandHint()
            val vin = genericTransport.getDetectedVin().orEmpty()
            val shouldPromoteToPsa = genericTransport.shouldPromoteToPsa()
            ConnectionDiagnosticsLogger.log(
                "autodetect",
                "brand_hint",
                "primary=obd2 brand=$brandHint vin=${vin.ifBlank { "-" }} promote_psa=$shouldPromoteToPsa"
            )

            if (!shouldPromoteToPsa) {
                return
            }

            runCatching { genericTransport.disconnect() }

            val promotionError = runCatching {
                psaTransport.connect()
                activeTransport = psaTransport
                ConnectionDiagnosticsLogger.log("autodetect", "protocol", "selected=psa_promoted")
            }.exceptionOrNull()

            if (promotionError == null) {
                return
            }
            if (promotionError is CancellationException) {
                activeTransport = null
                throw promotionError
            }

            ConnectionDiagnosticsLogger.log(
                "autodetect",
                "promotion",
                "target=psa failed=${promotionError.message ?: promotionError.javaClass.simpleName}"
            )
            genericTransport.connect()
            activeTransport = genericTransport
        }
    }

    override fun disconnect() {
        runCatching { psaTransport.disconnect() }
        runCatching { genericTransport.disconnect() }
        activeTransport = null
    }

    override fun isConnected(): Boolean = activeTransport?.isConnected() == true

    private fun selected(): EcuTransport = activeTransport ?: genericTransport

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
        genericTransport.setManualFirmwareProfile(signature, readOnly)
        psaTransport.setManualFirmwareProfile(signature, readOnly)
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

    override suspend fun readIdleControlSettings(): IdleControlSettings = selected().readIdleControlSettings()

    override suspend fun writeIdleControlSettings(settings: IdleControlSettings, burn: Boolean) =
        selected().writeIdleControlSettings(settings, burn)

    override suspend fun readClosedLoopCorrectionConfig() = selected().readClosedLoopCorrectionConfig()

    override suspend fun writeClosedLoopCorrectionConfig(
        config: ClosedLoopCorrectionConfig,
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
