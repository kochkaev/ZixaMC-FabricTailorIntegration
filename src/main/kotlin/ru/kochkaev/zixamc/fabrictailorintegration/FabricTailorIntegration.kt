package ru.kochkaev.zixamc.fabrictailorintegration

import com.google.gson.JsonObject
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import kotlinx.coroutines.launch
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import org.samo_lego.fabrictailor.casts.TailoredPlayer
import org.samo_lego.fabrictailor.command.SkinCommand
import ru.kochkaev.zixamc.api.Initializer
import ru.kochkaev.zixamc.api.sql.SQLCallback
import ru.kochkaev.zixamc.api.sql.SQLProcess
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.api.sql.callback.CallbackCanExecute
import ru.kochkaev.zixamc.api.sql.callback.CancelCallbackData
import ru.kochkaev.zixamc.api.sql.callback.TgCBHandlerResult
import ru.kochkaev.zixamc.api.sql.callback.TgMenu
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataTypes
import ru.kochkaev.zixamc.api.sql.data.MinecraftAccountType
import ru.kochkaev.zixamc.api.sql.process.ProcessData
import ru.kochkaev.zixamc.api.telegram.Menu
import ru.kochkaev.zixamc.api.telegram.ServerBot
import ru.kochkaev.zixamc.api.telegram.model.ITgMenuButton
import ru.kochkaev.zixamc.api.telegram.model.TgCallbackQuery
import ru.kochkaev.zixamc.api.telegram.model.TgChatMemberStatuses
import ru.kochkaev.zixamc.api.telegram.model.TgMessage
import ru.kochkaev.zixamc.api.telegram.model.TgReplyMarkup
import ru.kochkaev.zixamc.api.telegram.model.TgReplyParameters
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Optional
import java.util.Scanner
import java.util.UUID
import java.util.function.Supplier
import javax.imageio.ImageIO
import javax.net.ssl.HttpsURLConnection

object FabricTailorIntegration {

    val isModLoaded: Boolean
        get() = FabricLoader.getInstance().isModLoaded("fabrictailor")

    suspend fun callbackProcessor(cbq: TgCallbackQuery, sql: SQLCallback<Menu.MenuCallbackData<AdditionalData>>): TgCBHandlerResult {
        val user = SQLUser.get(cbq.from.id) ?: return TgCBHandlerResult.SUCCESS
        val menuData = sql.data ?: Menu.MenuCallbackData.of(
            operation = "fabricTailor",
            additionalType = AdditionalData::class.java,
            additional = AdditionalData(),
        )
        val data = menuData.additional
        if (data.nickname == null) {
            val nicknames = user.data.getCasted(ChatDataTypes.MINECRAFT_ACCOUNTS)?.filter { MinecraftAccountType.getAllActiveNow().contains(it.accountStatus) } ?.map { it.nickname } ?: listOf()
            if (nicknames.size == 1) {
                data.nickname = nicknames[0]
            } else if (nicknames.isEmpty()) {
                ServerBot.bot.editMessageText(
                    chatId = cbq.message.chat.id,
                    messageId = cbq.message.messageId,
                    text = Config.config.messageErrorUpload,
                )
                ServerBot.bot.editMessageReplyMarkup(
                    chatId = cbq.message.chat.id,
                    messageId = cbq.message.messageId,
                    replyMarkup = TgMenu(listOf(listOf(Menu.getBackButtonExecuteOnly(user))))
                )
                return TgCBHandlerResult.DELETE_LINKED
            } else {
                ServerBot.bot.editMessageText(
                    chatId = cbq.message.chat.id,
                    messageId = cbq.message.messageId,
                    text = Config.config.messageUploadPlayer,
                )
                ServerBot.bot.editMessageReplyMarkup(
                    chatId = cbq.message.chat.id,
                    messageId = cbq.message.messageId,
                    replyMarkup = TgMenu(nicknames.map {
                        listOf<ITgMenuButton>(
                            SQLCallback.of(
                                display = it,
                                type = "menu",
                                data = Menu.MenuCallbackData.of(
                                    operation = "fabricTailor",
                                    additionalType = AdditionalData::class.java,
                                    additional = AdditionalData(
                                        nickname = it,
                                    ),
                                ),
                                canExecute = CallbackCanExecute(
                                    statuses = listOf(TgChatMemberStatuses.CREATOR, TgChatMemberStatuses.ADMINISTRATOR),
                                    users = listOf(user.id),
                                    display = user.nickname ?: "",
                                )
                            )
                        )
                    }.toMutableList().apply { add(listOf<ITgMenuButton>(Menu.getBackButtonExecuteOnly(user))) })
                )
                return TgCBHandlerResult.DELETE_LINKED
            }
        }
        if (data.slim == null) {
            ServerBot.bot.editMessageText(
                chatId = cbq.message.chat.id,
                messageId = cbq.message.messageId,
                text = Config.config.messageUploadModel,
            )
            ServerBot.bot.editMessageReplyMarkup(
                chatId = cbq.message.chat.id,
                messageId = cbq.message.messageId,
                replyMarkup = TgMenu(
                    listOf(
                        listOf(
                            SQLCallback.of(
                                display = Config.config.buttonModelClassic,
                                type = "menu",
                                data = Menu.MenuCallbackData.of(
                                    operation = "fabricTailor",
                                    additionalType = AdditionalData::class.java,
                                    additional = AdditionalData(
                                        nickname = data.nickname,
                                        slim = false,
                                    ),
                                ),
                                canExecute = CallbackCanExecute(
                                    statuses = listOf(TgChatMemberStatuses.CREATOR, TgChatMemberStatuses.ADMINISTRATOR),
                                    users = listOf(user.id),
                                    display = user.nickname ?: "",
                                )
                            )
                        ),
                        listOf(
                            SQLCallback.of(
                                display = Config.config.buttonModelSlim,
                                type = "menu",
                                data = Menu.MenuCallbackData.of(
                                    operation = "fabricTailor",
                                    additionalType = AdditionalData::class.java,
                                    additional = AdditionalData(
                                        nickname = data.nickname,
                                        slim = true,
                                    ),
                                ),
                                canExecute = CallbackCanExecute(
                                    statuses = listOf(TgChatMemberStatuses.CREATOR, TgChatMemberStatuses.ADMINISTRATOR),
                                    users = listOf(user.id),
                                    display = user.nickname ?: "",
                                )
                            )
                        ),
                        listOf(Menu.getBackButtonExecuteOnly(user)),
                    )
                )
            )
            return TgCBHandlerResult.DELETE_LINKED
        } else {
            ServerBot.bot.editMessageText(
                chatId = cbq.message.chat.id,
                messageId = cbq.message.messageId,
                text = Config.config.messageUploadFile,
            )
            ServerBot.bot.editMessageReplyMarkup(
                chatId = cbq.message.chat.id,
                messageId = cbq.message.messageId,
                replyMarkup = TgMenu(
                    listOf(
                        listOf(
                            CancelCallbackData(
                                cancelProcesses = listOf(FabricTailorUploadProcess),
                                asCallbackSend = CancelCallbackData.CallbackSend(
                                    type = "menu",
                                    data = Menu.MenuCallbackData.of("back"),
                                    result = TgCBHandlerResult.Companion.DELETE_LINKED,
                                ),
                                canExecute = CallbackCanExecute(
                                    statuses = listOf(TgChatMemberStatuses.CREATOR, TgChatMemberStatuses.ADMINISTRATOR),
                                    users = listOf(user.id),
                                    display = user.nickname ?: "",
                                )
                            ).build()
                        )
                    )
                ),
            )
            SQLProcess.get(cbq.message.chat.id, FabricTailorUploadProcess)?.also {
                it.data?.run {
                    try { ServerBot.bot.editMessageReplyMarkup(
                        chatId = cbq.message.chat.id,
                        messageId = this.messageId,
                        replyMarkup = TgReplyMarkup()
                    ) } catch (_: Exception) {}
                    SQLCallback.Companion.dropAll(cbq.message.chat.id, this.messageId)
                }
            } ?.drop()
            SQLProcess.of(
                FabricTailorUploadProcess, FTProcessData(
                    nickname = data.nickname!!,
                    slim = data.slim!!,
                    messageId = cbq.message.messageId,
                    topicId = cbq.message.messageThreadId
                )
            ).pull(cbq.message.chat.id)
            return TgCBHandlerResult.Companion.DELETE_LINKED
        }
    }

    suspend fun messageProcessor(msg: TgMessage, process: SQLProcess<FTProcessData>, data: FTProcessData) = Initializer.coroutineScope.launch {
        val user = SQLUser.Companion.get(msg.from?.id ?: return@launch) ?: return@launch
        if (!user.nicknames.contains(data.nickname)) return@launch
        ServerBot.bot.editMessageReplyMarkup(
            chatId = msg.chat.id,
            messageId = data.messageId,
            replyMarkup = TgReplyMarkup()
        )
        SQLCallback.dropAll(msg.chat.id, data.messageId)
        msg.document?.let {
            val message = ServerBot.bot.sendMessage(
                chatId = msg.chat.id,
                replyParameters = TgReplyParameters(msg.messageId),
                text = Config.config.messagePreparing
            )
            process.data = FTProcessData(message.messageId, data.nickname, data.slim, data.topicId)
            val path = FabricLoader.getInstance().gameDir.toAbsolutePath()
            val path1 = path.resolve("$path/fabrictailor_uploads/")
            path1.toFile().mkdirs()
            val filename = "${data.nickname}.${it.file_name?.let { it1 -> it1.substring(it1.lastIndexOf('.')+1).lowercase() }}"
            val downloaded = try {
                ServerBot.bot.saveFile(it.file_id, "$path1/$filename")
            } catch (_: Exception) {
                ServerBot.bot.editMessageText(
                    chatId = msg.chat.id,
                    messageId = message.messageId,
                    text = Config.config.messageErrorUpload,
                )
                ServerBot.bot.editMessageReplyMarkup(
                    chatId = msg.chat.id,
                    messageId = message.messageId,
                    replyMarkup = TgMenu(
                        listOf(listOf(CancelCallbackData(
                            cancelProcesses = listOf(FabricTailorUploadProcess),
                            asCallbackSend = CancelCallbackData.CallbackSend(
                                type = "menu",
                                data = Menu.MenuCallbackData.of("back"),
                                result = TgCBHandlerResult.Companion.DELETE_LINKED,
                            ),
                            canExecute = CallbackCanExecute(
                                statuses = listOf(TgChatMemberStatuses.CREATOR, TgChatMemberStatuses.ADMINISTRATOR),
                                users = listOf(user.id),
                                display = user.nickname ?: "",
                            )
                        ).build()))
                    ),
                )
                return@launch
            }
            val result = setSkinFromFile(File(downloaded), data.slim, data.nickname)
//            val player = (FabricLoader.getInstance().gameInstance as MinecraftServer).playerManager.getPlayer(data.nickname)
//            setSkinFromFile(File(downloaded), data.slim, player as ServerPlayerEntity)
            ServerBot.bot.editMessageText(
                chatId = process.chatId,
                messageId = message.messageId,
                text = result.message,
            )
//            ServerBot.bot.editMessageReplyMarkup(
//                chatId = process.chatId,
//                messageId = message.messageId,
//                replyMarkup = TgMenu(listOf(listOf(Menu.BACK_BUTTON)))
//            )
            if (result == SkinUploadResult.SUCCESS) {
                process.data?.run {
                    try { ServerBot.bot.editMessageReplyMarkup(
                        chatId = process.chatId,
                        messageId = this.messageId,
                        replyMarkup = TgReplyMarkup()
                    ) } catch (_: Exception) {}
                    SQLCallback.Companion.dropAll(process.chatId, this.messageId)
                }
                process.drop()
                ServerBot.bot.editMessageReplyMarkup(
                    chatId = process.chatId,
                    messageId = message.messageId,
                    replyMarkup = TgMenu(listOf(listOf(Menu.BACK_BUTTON)))
                )
            } else {
                ServerBot.bot.editMessageReplyMarkup(
                    chatId = msg.chat.id,
                    messageId = message.messageId,
                    replyMarkup = TgMenu(
                        listOf(listOf(CancelCallbackData(
                            cancelProcesses = listOf(FabricTailorUploadProcess),
                            asCallbackSend = CancelCallbackData.CallbackSend(
                                type = "menu",
                                data = Menu.MenuCallbackData.of("back"),
                                result = TgCBHandlerResult.Companion.DELETE_LINKED,
                            ),
                            canExecute = CallbackCanExecute(
                                statuses = listOf(TgChatMemberStatuses.CREATOR, TgChatMemberStatuses.ADMINISTRATOR),
                                users = listOf(user.id),
                                display = user.nickname ?: "",
                            )
                        ).build()))
                    ),
                )
            }
            return@let
        } ?: run {
            val message = ServerBot.bot.sendMessage(
                chatId = msg.chat.id,
                replyParameters = TgReplyParameters(msg.messageId),
                text = Config.config.messageErrorNotAnImage,
                replyMarkup = TgMenu(
                    listOf(listOf(CancelCallbackData(
                        cancelProcesses = listOf(FabricTailorUploadProcess),
                        asCallbackSend = CancelCallbackData.CallbackSend(
                            type = "menu",
                            data = Menu.MenuCallbackData.of("back"),
                            result = TgCBHandlerResult.Companion.DELETE_LINKED,
                        ),
                        canExecute = CallbackCanExecute(
                            statuses = listOf(TgChatMemberStatuses.CREATOR, TgChatMemberStatuses.ADMINISTRATOR),
                            users = listOf(user.id),
                            display = user.nickname ?: "",
                        )
                    ).build()))
                ),
            )
            process.data = FTProcessData(message.messageId, data.nickname, data.slim, data.topicId)
        }
    }

    data class AdditionalData(
        var nickname: String? = null,
        var slim: Boolean? = null,
    )
    class FTProcessData(
        messageId: Int,
        var nickname: String,
        var slim: Boolean,
        var topicId: Int? = null,
    ): ProcessData(messageId)

    suspend fun setSkinFromFile(file: File, slim: Boolean, nickname: String): SkinUploadResult {
        if (!isModLoaded) return SkinUploadResult.OTHER_ERROR
        var skin: Property? = null
        try {
            val input = FileInputStream(file)
            val fileType = input.read()
            if (fileType == 137) {
                // Check image dimensions
                val image = ImageIO.read(file);
                if (image.width != 64 || (image.height != 64 && image.height != 32)) {
                    return SkinUploadResult.WRONG_RESOLUTION
                }
                try {
                    val reply = urlRequest(URI.create("https://api.mineskin.org/v2/generate").toURL(), false, file, null, if (slim) "slim" else "classic");
                    skin =  getSkinFromReply(reply)
                } catch (e: IOException) {
                    // Error uploading
                    return SkinUploadResult.UPLOAD_ERROR
                }
            }
        } catch (_: IOException) {
            // Not an image
            return SkinUploadResult.NOT_AN_IMAGE
        }
        skin?.let {
            val server = (FabricLoader.getInstance().gameInstance as MinecraftServer)
            val player = server.playerManager.getPlayer(nickname)
            if (player != null) {
                // TODO: Add support to fabric tailor errors
                SkinCommand.setSkin(player) { Optional.of(it) }
            } else {
                // TODO: Add support for set skin for offline player
//                val profile = server.userCache?.findByName(nickname)?.getOrNull()
//                return profile?.let { _ -> setSkin(profile) { it } } ?: SkinUploadResult.SET_ERROR
                return SkinUploadResult.NOT_ONLINE
            }
        }
        return SkinUploadResult.SUCCESS
    }

    enum class SkinUploadResult {
        WRONG_RESOLUTION {
            override val message: String
                get() = Config.config.messageErrorWrongResolution
        },
        NOT_AN_IMAGE {
            override val message: String
                get() = Config.config.messageErrorNotAnImage
        },
        UPLOAD_ERROR {
            override val message: String
                get() = Config.config.messageErrorUpload
        },
        SET_ERROR {
            override val message: String
                get() = Config.config.messageErrorSet
        },
        NOT_ONLINE {
            override val message: String
                get() = Config.config.messageErrorNotOnline
        },
        OTHER_ERROR {
            override val message: String
                get() = Config.config.messageErrorUpload
        },
        SUCCESS {
            override val message: String
                get() = Config.config.messageDone
        };
        abstract val message: String
    }

    private fun getSkinFromReply(reply: String?): Property? {
        if (reply == null || reply.contains("error") || reply.isEmpty()) {
            return null
        }

        val value: String? =
            reply.split("\"value\":\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split("\"".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        val signature: String? = reply.split("\"signature\":\"".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]

        return Property(TailoredPlayer.PROPERTY_TEXTURES, value, signature)
    }

    /**
     * Gets reply from a skin website.
     * Used internally only.
     *
     * @param url url of the website
     * @param useGetMethod whether to use GET method instead of POST
     * @param image image to upload, otherwise null
     * @return reply from website as string
     * @throws IOException IOException is thrown when connection fails for some reason.
     */
    @Throws(IOException::class)
    private fun urlRequest(url: URL, useGetMethod: Boolean, image: File?, skinUrl: String?, variant: String?): String? {
        val connection: URLConnection? = url.openConnection()

        var reply: String? = null

        if (connection is HttpsURLConnection) {
            connection.setUseCaches(false)
            connection.setDoOutput(true)
            connection.setDoInput(true)
            connection.setRequestMethod(if (useGetMethod) "GET" else "POST")

            if (image != null) {
                // Do a POST request
                val boundary: String? = UUID.randomUUID().toString()
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
                connection.setRequestProperty("User-Agent", "User-Agent")
                val outputStream = connection.getOutputStream()
                val writer: PrintWriter = PrintWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)

                val LINE = "\r\n"
                writer.append("--").append(boundary).append(LINE)
                writer.append("Content-Disposition: form-data; name=\"file\"").append(LINE)
                writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE)
                writer.append(LINE)
                writer.append(image.getName()).append(LINE)
                writer.flush()

                writer.append("--").append(boundary).append(LINE)
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(image.getName())
                    .append("\"")
                if (variant != null) writer.append("; variant=\"").append(variant).append("\"")
                writer.append(LINE)
                writer.append("Content-Type: image/png").append(LINE)
                writer.append("Content-Transfer-Encoding: binary").append(LINE)
                writer.append(LINE)
                writer.flush()

                val fileBytes: ByteArray = Files.readAllBytes(image.toPath())
                outputStream.write(fileBytes, 0, fileBytes.size)

                outputStream.flush()
                writer.append(LINE)
                writer.flush()

                writer.append("--").append(boundary).append("--").append(LINE)
                writer.close()
            }

            if (skinUrl != null) {
                // Do a POST request
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "User-Agent")
                val outputStream = connection.getOutputStream()
                val writer: PrintWriter = PrintWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))

                val json: JsonObject = JsonObject()
                json.addProperty("url", skinUrl)
                json.addProperty("variant", variant)

                writer.write(json.toString())
                writer.flush()
                writer.close()
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                reply = getContent(connection)
            }
            connection.disconnect()
        } else {
            reply = getContent(connection)
        }
        return reply
    }

    @Throws(IOException::class)
    private fun getContent(connection: URLConnection?): String? {
        connection?.getInputStream()?.use { `is` ->
            InputStreamReader(`is`).use { isr ->
                Scanner(isr).use { scanner ->
                    val reply = StringBuilder()
                    while (scanner.hasNextLine()) {
                        val line: String = scanner.next()
                        if (line.trim { it <= ' ' }.isEmpty()) continue
                        reply.append(line)
                    }
                    return reply.toString()
                }
            }
        }
        return null
    }

    suspend fun setSkin(profile: GameProfile, skinProvider: Supplier<Property?>): SkinUploadResult {
        val skinData = skinProvider.get() ?: return SkinUploadResult.SET_ERROR
//        (player as TailoredPlayer).fabrictailor_setSkin(skinData, true)
        profile.properties.put(TailoredPlayer.PROPERTY_TEXTURES, skinData)
        (FabricLoader.getInstance().gameInstance as MinecraftServer).userCache?.add(profile)
        return SkinUploadResult.SUCCESS
    }

}