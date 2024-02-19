package aldmitry.dev.personalmanager.extendfunctions

import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery
import org.telegram.telegrambots.meta.api.methods.invoices.CreateInvoiceLink
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
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
        val logger = LoggerFactory.getLogger("extendfunctions <protectedExecute SendMessage>")
        logger.error(e.message)
    }
    return messageId
}


fun AbsSender.protectedExecute(editMessageText: EditMessageText) {
    try {
        this.execute(editMessageText)
    } catch (e: TelegramApiException) {
        val logger = LoggerFactory.getLogger("extendfunctions <protectedExecute editMessageText>")
        logger.error(e.message)
        println("Err >>>  $e")
    }
}


fun AbsSender.protectedExecute(sendDocument: SendDocument) {
    try {
        this.execute(sendDocument)
    } catch (e: TelegramApiException) {
        val logger = LoggerFactory.getLogger("extendfunctions <protectedExecute SendDocument>")
        println("Err >>>  $e")
    }
}


fun AbsSender.protectedExecute(deleteMessage: DeleteMessage) {
    try {
        this.execute(deleteMessage)
    } catch (e: TelegramApiException) {
        // without logger: exceptions here is ordinary case, because there are often no messages to delete
    }
}


fun AbsSender.protectedExecute(createInvoiceLink: CreateInvoiceLink): String {
    var link = ""
    try {
        link = this.execute(createInvoiceLink)
    } catch (e: TelegramApiException) {
        // without logger: exceptions here is ordinary case, because there are often no messages to delete
        println("Err >>>  $e")
    }
    return link
}


fun AbsSender.protectedExecute(answerPreCheckoutQuery: AnswerPreCheckoutQuery) {
    try {
        this.execute(answerPreCheckoutQuery)
    } catch (e: TelegramApiException) {
        // without logger: exceptions here is ordinary case, because there are often no messages to delete
        println("Err >>>  $e")
    }
}