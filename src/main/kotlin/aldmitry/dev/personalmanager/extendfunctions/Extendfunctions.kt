package aldmitry.dev.personalmanager.extendfunctions

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException


fun InlineKeyboardButton.putData(text: String, callbackData: String) {
    this.text = text
    this.callbackData = callbackData
}


fun EditMessageText.putData(stringChatId: String, intMessageId: Int, messageText: String): EditMessageText {
    this.chatId = stringChatId
    this.messageId = intMessageId
    this.text = messageText
    return this
}


fun EditMessageMedia.putData(stringChatId: String, intMessageId: Int?, pictureUrl: String): EditMessageMedia {
    this.chatId = stringChatId
    this.messageId = intMessageId
    this.media = InputMediaPhoto(pictureUrl)
    return this
}


fun SendPhoto.putData(stringChatId: String, pictureUrl: String): SendPhoto {
    this.chatId = stringChatId
    this.photo =  InputFile(pictureUrl)
    return this
}


fun DeleteMessage.putData(stringChatId: String, messageId: Int): DeleteMessage {
    this.chatId = stringChatId
    this.messageId = messageId
    return this
}


fun AbsSender.protectedExecute(sendMessage: SendMessage): Int {
    var messageId = 0
    try {
        messageId = this.execute(sendMessage).messageId
    } catch (e: TelegramApiException) {
        //  val logger = LoggerFactory.getLogger("extendfunctions <protectedExecute SendMessage>")
        //  logger.error(e.message)
    }
    return messageId
}


fun AbsSender.protectedExecute(sendPhoto: SendPhoto): Int {
    var messageId = 0
    try {
        messageId = this.execute(sendPhoto).messageId
    } catch (e: TelegramApiException) {
        //  val logger = LoggerFactory.getLogger("extendfunctions <protectedExecute SendPhoto>")
        //  logger.error(e.message)
    }
    return messageId
}


fun AbsSender.protectedExecute(editMessageText: EditMessageText) {
    try {
        this.execute(editMessageText)
    } catch (e: TelegramApiException) {
        //  val logger = LoggerFactory.getLogger("extendfunctions <protectedExecute editMessageText>")
        //   logger.error(e.message)
        println("Err >>>  $e")
    }
}


fun AbsSender.protectedExecute(editMessageMedia: EditMessageMedia) {
    try {
        this.execute(editMessageMedia)
    } catch (e: TelegramApiException) {
        // without logger: exceptions here is ordinary case, because there are often no messages to delete
    }
}


fun AbsSender.protectedExecute(deleteMessage: DeleteMessage) {
    try {
        this.execute(deleteMessage)
    } catch (e: TelegramApiException) {
        // without logger: exceptions here is ordinary case, because there are often no messages to delete
    }

}
