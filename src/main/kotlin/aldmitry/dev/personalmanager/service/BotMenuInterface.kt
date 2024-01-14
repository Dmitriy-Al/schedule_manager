package aldmitry.dev.personalmanager.service

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

interface BotMenuInterface {

    fun createButtonSet(textForButton: List<String>) : InlineKeyboardMarkup

    fun createDataButtonSet(textForButton: List<String>, callBackData: String = "") : InlineKeyboardMarkup

    fun receiveOneButtonMenu(buttonText: String, buttonData: String): InlineKeyboardMarkup

    fun receiveTwoButtonsMenu(firstButtonText: String, firstData: String, secondButtonText: String, secondData: String): InlineKeyboardMarkup

}