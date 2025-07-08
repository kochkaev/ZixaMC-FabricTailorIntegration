package ru.kochkaev.zixamc.fabrictailorintegration

import ru.kochkaev.zixamc.api.sql.process.ProcessType
import ru.kochkaev.zixamc.api.sql.process.ProcessorType

object FabricTailorUploadProcess: ProcessType<FabricTailorIntegration.FTProcessData>(
    model = FabricTailorIntegration.FTProcessData::class.java,
    serializedName = "MENU_FABRIC_TAILOR_UPLOAD",
    processorType = ProcessorType.ANY_MESSAGE,
    processor = FabricTailorIntegration::messageProcessor,
    cancelOnMenuSend = true,
)