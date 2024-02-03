package aldmitry.dev.personalmanager.service

import aldmitry.dev.personalmanager.config.Config
import aldmitry.dev.personalmanager.extendfunctions.protectedExecute
import aldmitry.dev.personalmanager.extendfunctions.putData
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import java.io.File

class BotMessageFunction {

    private val config = Config()
    private val botMenuFunction = BotMenuFunction()


    fun sendBackup(longChatId: Long, textForMessage: String, backupDirectory: String): SendDocument {
        val sendDocument = SendDocument()
        sendDocument.setChatId(longChatId)
        sendDocument.document = InputFile(File(backupDirectory))
        sendDocument.caption = textForMessage
        return sendDocument
    }


    fun backupServer(textForMessage: String, stringChatId: String, messageId: Int): EditMessageText {
        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, messageId, textForMessage)
        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
        return editMessageText
    }









}