package aldmitry.dev.personalmanager.service

import aldmitry.dev.personalmanager.extendfunctions.putData
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

class BotMenuFunction : BotMenuInterface {

    // Экранные кнопки
    override fun createButtonSet(textForButton: List<String>): InlineKeyboardMarkup {
        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()

        for (element in textForButton) {
            val rowInlineButton = ArrayList<InlineKeyboardButton>()
            val button = InlineKeyboardButton()
            button.putData(element, element)
            rowInlineButton.add(button)
            rowsInline.add(rowInlineButton)
        }
        inlineKeyboardMarkup.keyboard = rowsInline
        return inlineKeyboardMarkup
    }

    // Экранные кнопки с добавленным текстом для callBackData
    override fun createDataButtonSet(textForButton: List<String>, callBackData: String): InlineKeyboardMarkup {
        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()

        for (element in textForButton) {
            val rowInlineButton = ArrayList<InlineKeyboardButton>()
            val button = InlineKeyboardButton()
            button.putData(element, element + callBackData)
            rowInlineButton.add(button)
            rowsInline.add(rowInlineButton)
        }
        inlineKeyboardMarkup.keyboard = rowsInline
        return inlineKeyboardMarkup
    }


    // Меню с одной кнопкой
    override fun receiveOneButtonMenu(buttonText: String, buttonData: String): InlineKeyboardMarkup {
        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()

        val rowInlineButton = ArrayList<InlineKeyboardButton>()
        val button = InlineKeyboardButton()
        button.putData(buttonText, buttonData)
        rowInlineButton.add(button)
        rowsInline.add(rowInlineButton)
        inlineKeyboardMarkup.keyboard = rowsInline
        return inlineKeyboardMarkup
    }

    // Меню с двумя кнопками
    override fun receiveTwoButtonsMenu(firstButtonText: String, firstData: String, secondButtonText: String, secondData: String): InlineKeyboardMarkup {
        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()
        val firstRowInlineButton = ArrayList<InlineKeyboardButton>()

        val firstButton = InlineKeyboardButton()
        firstButton.putData(firstButtonText, firstData)
        firstRowInlineButton.add(firstButton)

        val secondButton = InlineKeyboardButton()
        secondButton.putData(secondButtonText, secondData)
        firstRowInlineButton.add(secondButton)

        rowsInline.add(firstRowInlineButton)
        inlineKeyboardMarkup.keyboard = rowsInline
        return inlineKeyboardMarkup
    }

    fun receiveFindClientKeyboard(stringChatId: String, intMessageId: Int, messageText: String, callBackData: String): EditMessageText {
        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, messageText)

        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()
        val firstRowInlineButton = ArrayList<InlineKeyboardButton>()
        val secondRowInlineButton = ArrayList<InlineKeyboardButton>()
        val thirdRowInlineButton = ArrayList<InlineKeyboardButton>()
        val fourthRowInlineButton = ArrayList<InlineKeyboardButton>()
        val fifthRowInlineButton = ArrayList<InlineKeyboardButton>()

        for (i in 'А' .. 'Я') {
            if (i == 'Ь' || i == 'Ъ' || i == 'Ы' || i == 'Й') continue
            when {
                i.code < 1047 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$i", callBackData + i) // #findcli
                    firstRowInlineButton.add(button)
                }

                i.code < 1055 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$i", callBackData + i)
                    secondRowInlineButton.add(button)
                }

                i.code < 1062 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$i", callBackData + i)
                    thirdRowInlineButton.add(button)
                }

                i.code < 1072 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$i", callBackData + i)
                    fourthRowInlineButton.add(button)
                }
            }
        }

        val backButton = InlineKeyboardButton()
        backButton.putData("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
        fifthRowInlineButton.add(backButton)

        val findAllButton = InlineKeyboardButton()
        findAllButton.putData("Список всех клиентов", "#allcli$callBackData")
        fifthRowInlineButton.add(findAllButton)


        rowsInline.add(firstRowInlineButton)
        rowsInline.add(secondRowInlineButton)
        rowsInline.add(thirdRowInlineButton)
        rowsInline.add(fourthRowInlineButton)
        rowsInline.add(fifthRowInlineButton)
        inlineKeyboardMarkup.keyboard = rowsInline
        editMessageText.replyMarkup = inlineKeyboardMarkup

        return editMessageText
    }





}