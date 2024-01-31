package aldmitry.dev.personalmanager.service

import aldmitry.dev.personalmanager.extendfunctions.putData
import aldmitry.dev.personalmanager.model.User
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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


    fun receiveSettingsKeyboard(stringChatId: String, intMessageId: Int, user: User): EditMessageText {
        val editMessageText = EditMessageText()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val textForMessage = "\uD83D\uDD30  Меню с настройками.\nВ меню настроек вашей учетной записи вы можете:\n" +
                "✷ Установить время для рассылки сообщений, чтобы клиент мог подтвердить визит к вам. По умолчанию время рассылки 12.00 (Мск.)\n" +
                "✷ Установить количество дней, за которое будут отправлены сообщения для подтверждения визита. По умолчанию сообщения отправляются за 1 " +
                "день до приёма, возможный максимум - за 3 дня до приёма.\n✷ Если ваш часовой пояс не соответствует Мск., установите то количество часов, " +
                "на которое ваше время отличается от дефолтного (Мск.)\n✷ В разделе <Мои клиенты и лимиты> можно узнать, сколько клиентов у вас " +
                "добавлено в базу данных и сколько вы еще можете добавить.\n✷ В разделе <Мои данные> вы можете редактировать собственные данные, восстановить учётную запись " +
                "в приложении, если у вас изменился Telegram аккаунт, посмотреть собственную запись к специалисту.\n✷ В случае возникновения проблем связанных " +
                "с работой приложения, вы можете обратиться в чат поддержки.\n\n\uD83D\uDD39 С помощью клавиш ➖ и ➕ вы можете менять параметры настроек времени." +
                "\n\n\uD83D\uDD38 Время рассылки в: ${user.sendTime} часов" +
                "\n\uD83D\uDD38 Рассылка за: ${user.sendBeforeDays} день/дня до приёма\n\uD83D\uDD38 Часовой пояс от Мск. часов: ${user.timeZone}" +
                "\n\uD83D\uDD38 ФИО: ${user.secondName} ${user.firstName} ${user.patronymic}\n\uD83D\uDD38 Специализация: ${user.profession}" +
                "\n\uD83D\uDD38 Очередная абонентская плата: ${formatter.format(LocalDate.parse(user.paymentDate))}"

        editMessageText.putData(stringChatId, intMessageId, textForMessage)

        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()
        val firstRowInlineButton = ArrayList<InlineKeyboardButton>()
        val secondRowInlineButton = ArrayList<InlineKeyboardButton>()
        val thirdRowInlineButton = ArrayList<InlineKeyboardButton>()
        val fourthRowInlineButton = ArrayList<InlineKeyboardButton>()
        val fifthRowInlineButton = ArrayList<InlineKeyboardButton>()
        val sixthRowInlineButton = ArrayList<InlineKeyboardButton>()
        val seventhRowInlineButton = ArrayList<InlineKeyboardButton>()

        val sendTimeMinus = InlineKeyboardButton()
        sendTimeMinus.putData("➖  Время рассылки", "#timedwn")
        firstRowInlineButton.add(sendTimeMinus)

        val sendTimePlus = InlineKeyboardButton()
        sendTimePlus.putData("Время рассылки  ➕", "#timeup")
        firstRowInlineButton.add(sendTimePlus)

        val sendDayMinus = InlineKeyboardButton()
        sendDayMinus.putData("➖  День рассылки", "#daydwn")
        secondRowInlineButton.add(sendDayMinus)

        val sendDayPlus = InlineKeyboardButton()
        sendDayPlus.putData("День рассылки  ➕", "#dayup")
        secondRowInlineButton.add(sendDayPlus)

        val timeZoneMinus = InlineKeyboardButton()
        timeZoneMinus.putData("➖  Часовой пояс", "#zonedwn")
        thirdRowInlineButton.add(timeZoneMinus)

        val timeZonePlus = InlineKeyboardButton()
        timeZonePlus.putData("Часовой пояс  ➕", "#zoneup")
        thirdRowInlineButton.add(timeZonePlus)

        val clientButton = InlineKeyboardButton()
        clientButton.putData("Мои клиенты и лимиты", "#mycli")
        fourthRowInlineButton.add(clientButton)

        val changeDataButton = InlineKeyboardButton()
        changeDataButton.putData("Мои данные", "#mydata")
        fifthRowInlineButton.add(changeDataButton)

        val paymentButton = InlineKeyboardButton()
        paymentButton.putData("Абонемент", "#payment")
        sixthRowInlineButton.add(paymentButton)

        val sendButton = InlineKeyboardButton()
        sendButton.putData("Чат поддержки", "#support")
        sixthRowInlineButton.add(sendButton)

        val backButton = InlineKeyboardButton()
        backButton.putData("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
        seventhRowInlineButton.add(backButton)

        rowsInline.add(firstRowInlineButton)
        rowsInline.add(secondRowInlineButton)
        rowsInline.add(thirdRowInlineButton)
        rowsInline.add(fourthRowInlineButton)
        rowsInline.add(fifthRowInlineButton)
        rowsInline.add(sixthRowInlineButton)
        rowsInline.add(seventhRowInlineButton)
        inlineKeyboardMarkup.keyboard = rowsInline
        editMessageText.replyMarkup = inlineKeyboardMarkup

        return editMessageText
    }



}