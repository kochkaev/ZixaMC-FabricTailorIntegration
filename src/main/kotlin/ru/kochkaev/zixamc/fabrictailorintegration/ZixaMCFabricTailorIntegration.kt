package ru.kochkaev.zixamc.fabrictailorintegration

import net.fabricmc.api.ModInitializer
import ru.kochkaev.zixamc.api.config.ConfigManager
import ru.kochkaev.zixamc.api.sql.process.ProcessTypes
import ru.kochkaev.zixamc.api.telegram.Menu

class ZixaMCFabricTailorIntegration: ModInitializer {

    override fun onInitialize() {
        ConfigManager.registerConfig(Config)
        ProcessTypes.registerType(FabricTailorUploadProcess)
        Menu.addIntegration(Menu.Integration.of(
            callbackName = "fabricTailor",
            menuDisplay = Config.config.buttonMenu,
            processor = FabricTailorIntegration::callbackProcessor,
            customDataType = FabricTailorIntegration.AdditionalData::class.java,
            customDataInitial = FabricTailorIntegration.AdditionalData(),
            filter = { chatId, userId -> chatId == userId },
        ))
    }

}