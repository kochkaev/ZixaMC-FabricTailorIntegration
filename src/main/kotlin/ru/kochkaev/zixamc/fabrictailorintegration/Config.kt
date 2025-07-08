package ru.kochkaev.zixamc.fabrictailorintegration

import net.fabricmc.loader.api.FabricLoader
import ru.kochkaev.zixamc.api.config.ConfigFile
import java.io.File

data class Config(
    val buttonMenu: String = "Загрузить скин на сервер 👘",
    val messageUploadPlayer: String = "Выберите аккаунт, для которого вы хотите загрузить скин.",
    val messageUploadFile: String = "Отправьте файл скина в этот чат.\nОбратите внимание, вы должны <u>отправить изображение как файл</u> (без сжатия).",
    val messageUploadModel: String = "Выберите модель загружаемого скина:\n- Classic — обычный скин,\n- Slim — скин с тонкими руками.",
    val buttonModelClassic: String = "Classic",
    val buttonModelSlim: String = "Slim",
    val messageErrorUpload: String = "Ошибка загрузки изображения скина! Обратитесь за помощью к администратору.",
    val messageErrorSet: String = "Ошибка установки скина на сервере!\nОшибка могла возникнуть, если вы ни разу не заходили на сервер с вашим никнеймом. Попробуйте зайти на сервер и попробовать снова. Если ошибка осталась, обратитесь за помощью к администратору.",
    val messageErrorNotOnline: String = "Ошибка установки скина! Вы должны находиться онлайн (в игре) на сервере!",
    val messageErrorNotAnImage: String = "Ошибка! Вы должны <u>отправить изображение как файл</u> (без сжатия).",
    val messageErrorWrongResolution: String = "Ошибка! Вы должны отправить изображение с разрешением 64x64 (стандартный скин) или 64x32 (устаревший скин).",
    val messageDone: String = "Скин успешно установлен!",
    val messagePreparing: String = "<b>Пожалуйста, подождите...</b>"
) {
    companion object: ConfigFile<Config>(
        file = File(FabricLoader.getInstance().configDir.toFile(), "ZixaMC-FabricTailorIntegration.json"),
        model = Config::class.java,
        supplier = ::Config
    )
}