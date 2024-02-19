package aldmitry.dev.personalmanager.service

import aldmitry.dev.personalmanager.apptexts.*
import aldmitry.dev.personalmanager.backup.BackupCreator
import aldmitry.dev.personalmanager.backup.ServerBackup
import aldmitry.dev.personalmanager.extendfunctions.protectedExecute
import aldmitry.dev.personalmanager.extendfunctions.putData
import aldmitry.dev.personalmanager.model.ClientData
import aldmitry.dev.personalmanager.model.ClientDataDao
import aldmitry.dev.personalmanager.model.User
import aldmitry.dev.personalmanager.model.UserDao
import org.telegram.telegrambots.meta.api.methods.invoices.CreateInvoiceLink
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BotMenuFunction : BotMenuInterface {

    private val specialistMenuList = listOf(callData_setAppointment, callData_appointmentToMe, callData_addNewClient, callData_clientBaseMenu, callData_myAccount)
    private val clientMenuList = listOf(callData_myAppointment, callData_regAsSpec)
    

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


    fun sendBackup(longChatId: Long, textForMessage: String, backupDirectory: String): SendDocument {
        val sendDocument = SendDocument()
        sendDocument.setChatId(longChatId)
        sendDocument.document = InputFile(File(backupDirectory))
        sendDocument.caption = textForMessage
        return sendDocument
    }


    fun receiveSubscriptionMessage(intMessageId: Int, stringChatId: String): EditMessageText {
        val editMessageText = EditMessageText() // в период действия абонемента
        val textForMessage = "$text_limitPartOne${config.freeClientsAmount}$text_limitPartTwo${config.maxClientsAmount}."
        editMessageText.putData(stringChatId, intMessageId, textForMessage)
        editMessageText.replyMarkup = receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", callData_mainMenu, "Абонемент", callData_paymentMenu)
        return editMessageText
    }


    fun receiveClientMessage(stringChatId: String): SendMessage {
        val sendMessage = SendMessage(stringChatId, text_clientStartMessage)
        sendMessage.replyMarkup = createButtonSet(clientMenuList)
        return sendMessage
    }


    fun receiveClientEditMessage (stringChatId: String, intMessageId: Int): EditMessageText {
        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, text_clientStartMessage)
        editMessageText.replyMarkup = createButtonSet(clientMenuList)
        return editMessageText
    }


    fun receiveAdministratorSendMessage(stringChatId: String, textForStartMessage: String, saveMessageIdSize: Int, users: Iterable<User>, clientData: Iterable<ClientData>): SendMessage {
        val specialists = users.filter { it.profession.isNotEmpty() }
        val textForMessage = "$textForStartMessage\uD83D\uDD30  Меню администратора.\nВсего пользователей: ${users.count()}" +
                "\nВсего специалистов: ${specialists.count() - 1}\nДобавлено клиентов: ${clientData.count()}\nВсего стартовых сообщений: $saveMessageIdSize"
        val sendMessage = SendMessage(stringChatId, textForMessage)
        val settingList = listOf(callData_editeUser, callData_messageToAllUsers, callData_messageToUser, callData_messageToMainMenu, callData_specMenu, callData_cleanChatMenu, callData_backupMenu)
        sendMessage.replyMarkup = createButtonSet(settingList)
        return sendMessage
    }


    fun receiveSpecialistSendMessage(longChatId: Long, stringChatId: String, textForStartMessage: String, clientData: Iterable<ClientData>): SendMessage {
        val textForMessage = receiveTextForStartMessage(longChatId, textForStartMessage, clientData)
        val sendMessage = SendMessage(stringChatId, textForMessage)
        sendMessage.replyMarkup = createButtonSet(specialistMenuList)
        return sendMessage
    }


    fun receiveSpecialistEditMessage(longChatId: Long, stringChatId: String, intMessageId: Int, textForStartMessage: String, clientData: Iterable<ClientData>): EditMessageText {
        val textForMessage = receiveTextForStartMessage(longChatId, textForStartMessage, clientData)
        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, textForMessage)
        editMessageText.replyMarkup = createButtonSet(specialistMenuList)
        return editMessageText
    }


    private fun receiveTextForStartMessage(longChatId: Long, textForStartMessage: String, clientData: Iterable<ClientData>): String {
        val textForMessageLength: Int
        val localDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val textForMessage = StringBuilder()
        val nextDate: String? =clientData.filter { it.specialistId == longChatId && it.appointmentDate.length == 10 && localDate.isBefore(LocalDate.parse(it.appointmentDate)) }.minByOrNull { it.appointmentDate }?.appointmentDate

        if (nextDate.isNullOrEmpty()) {
            textForMessage.append("$text_info$textForStartMessage$text_infoOne")
        } else {
            textForMessage.append("$text_info$textForStartMessage$text_infoTwo${formatter.format(LocalDate.parse(nextDate))}:")
        }

        clientData.filter { it.specialistId == longChatId && it.appointmentDate == nextDate }.sortedBy { it.appointmentTime}.forEach { textForMessage.append("\n${it.visitAgreement} ${it.appointmentTime} - ${it.secondName} ${it.firstName.first()}. ${it.patronymic.first()}.") }

        textForMessage.append(text_todayAppointment)
        textForMessageLength = textForMessage.length
        clientData.filter { it.specialistId == longChatId && it.appointmentDate.length == 10 && it.appointmentDate == localDate.toString() && it.visitAgreement != "✖" }.sortedBy { it.appointmentTime}.forEach { textForMessage.append("\n• ${it.appointmentTime} - ${it.secondName} ${it.firstName.first()}. ${it.patronymic.first()}.") }
        if (textForMessageLength == textForMessage.length) textForMessage.append(" нет.")
        return textForMessage.toString()
    }


    fun sendMessageToUser(updateMessageText: String): SendMessage {
        val splitMessageText = updateMessageText.split("#")
        val sendMessage = SendMessage(splitMessageText[0], "$text_adminMessage${splitMessageText[1]}")
        sendMessage.replyMarkup = receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", callData_delMessage)
        return sendMessage
    }


    fun sendMessageToAdminNotification(stringChatId: String, intMessageId: Int): EditMessageText {
        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, text_sentMessage)
        editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  В главное меню", callData_mainMenu)
        return editMessageText
    }


    fun specialistUserMenu(longChatId: Long, stringChatId: String, intMessageId: Int, textForStartMessage: String, clientRepository: ClientDataDao): EditMessageText {
        return  receiveSpecialistEditMessage(longChatId, stringChatId, intMessageId, textForStartMessage, clientRepository.findAll())
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
        backButton.putData("\uD83D\uDD19  Назад в меню", callData_mainMenu)
        fifthRowInlineButton.add(backButton)

        val findAllButton = InlineKeyboardButton()
        findAllButton.putData("Список всех клиентов", "$callData_allClients$callBackData")
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


    fun receiveUserSettingsMenu(stringChatId: String, intMessageId: Int, user: User): EditMessageText {
        val editMessageText = EditMessageText()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val textForMessage = "$text_userSettingsMenuOne${user.sendTime}$text_userSettingsMenuTwo${user.sendBeforeDays}$text_userSettingsMenuThree${user.timeZone}" +
                "\n\uD83D\uDD38 ФИО: ${user.secondName} ${user.firstName} ${user.patronymic}\n\uD83D\uDD38 Специализация: ${user.profession}" +
                "$text_userSettingsMenuFour${formatter.format(LocalDate.parse(user.paymentDate))}"

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
        sendTimeMinus.putData("➖  Время рассылки", callData_timeDown)
        firstRowInlineButton.add(sendTimeMinus)

        val sendTimePlus = InlineKeyboardButton()
        sendTimePlus.putData("Время рассылки  ➕", callData_timeUp)
        firstRowInlineButton.add(sendTimePlus)

        val sendDayMinus = InlineKeyboardButton()
        sendDayMinus.putData("➖  День рассылки", callData_dayDown)
        secondRowInlineButton.add(sendDayMinus)

        val sendDayPlus = InlineKeyboardButton()
        sendDayPlus.putData("День рассылки  ➕", callData_dayUp)
        secondRowInlineButton.add(sendDayPlus)

        val timeZoneMinus = InlineKeyboardButton()
        timeZoneMinus.putData("➖  Часовой пояс", callData_zoneDown)
        thirdRowInlineButton.add(timeZoneMinus)

        val timeZonePlus = InlineKeyboardButton()
        timeZonePlus.putData("Часовой пояс  ➕", callData_zoneUp)
        thirdRowInlineButton.add(timeZonePlus)

        val clientButton = InlineKeyboardButton()
        clientButton.putData("Мои клиенты и лимиты", callData_myClients)
        fourthRowInlineButton.add(clientButton)

        val changeDataButton = InlineKeyboardButton()
        changeDataButton.putData("Мои данные", callData_myData)
        fifthRowInlineButton.add(changeDataButton)

        val getBackupButton = InlineKeyboardButton()
        getBackupButton.putData("Backup-файл", "$callData_getBackup$stringChatId")
        fifthRowInlineButton.add(getBackupButton)

        val paymentButton = InlineKeyboardButton()
        paymentButton.putData("Абонемент", callData_paymentMenu)
        sixthRowInlineButton.add(paymentButton)

        val sendButton = InlineKeyboardButton()
        sendButton.putData("Чат поддержки", callData_messageToSupport)
        sixthRowInlineButton.add(sendButton)

        val backButton = InlineKeyboardButton()
        backButton.putData("\uD83D\uDD19  Назад в меню", callData_mainMenu)
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


    fun receiveFindClientMenu(intMessageId: Int, stringChatId: String, longChatId: Long, secondNameText: String,
                              calBackData: String, clients: Iterable<ClientData>): EditMessageText {
        val client = clients.filter { it.specialistId == longChatId && it.secondName.contains(secondNameText,
        true) }.filter { it.secondName.first().lowercase() == secondNameText.first().lowercase() }.sortedBy { it.secondName }

        val textForMessage: String
        val editMessageText = EditMessageText()

        if(client.isEmpty()){
            textForMessage = text_clientNotFound
            editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  Назад в меню",
                    callData_mainMenu)

        } else {
            textForMessage = text_chooseClient
            val inlineKeyboardMarkup = InlineKeyboardMarkup()
            val rowsInline = ArrayList<List<InlineKeyboardButton>>()
            for (elem in client) {
                val rowInlineButton = ArrayList<InlineKeyboardButton>()
                val button = InlineKeyboardButton()
                button.putData(elem.secondName + " " +  elem.firstName, calBackData + elem.clientId)
                rowInlineButton.add(button)
                rowsInline.add(rowInlineButton)
            }

            val rowInlineButton = ArrayList<InlineKeyboardButton>()
            val returnButton = InlineKeyboardButton()
            returnButton.putData("\uD83D\uDD19  Назад в меню", callData_mainMenu)
            rowInlineButton.add(returnButton)
            rowsInline.add(rowInlineButton)

            inlineKeyboardMarkup.keyboard = rowsInline
            editMessageText.replyMarkup = inlineKeyboardMarkup
        }

        editMessageText.putData(stringChatId, intMessageId, textForMessage)
       return editMessageText
    }


    private fun receiveBackupMenuMessage(textForMessage: String, stringChatId: String, messageId: Int): EditMessageText {
        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, messageId, textForMessage)
        editMessageText.replyMarkup = receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", callData_mainMenu, "В backup меню", callData_backupMenu)
        return editMessageText
    }


    fun createClientAppointment(stringChatId: String, messageId: Int, updateMessageText: String, month: String, callData: String, clientRepository: Iterable<ClientData>): EditMessageText {
        val dataText: String = updateMessageText.replace(" :", "").replace(":", " ").replace(", ", " ").replace(",", " ").replace(".", "").replace(" в", "").replace(" часов", "").replace(" на", "").trim()
        val splitText: List<String> = dataText.split(" ")
        val clientSecondName: String = splitText[0]
        val localDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy")
        val longChatId = stringChatId.toLong()

        val clients = clientRepository.filter { it.specialistId == longChatId && it.secondName.contains(clientSecondName, true) }

        var textForMessage = "ℹ  "
        val editMessageText = EditMessageText()
        var day = 0
        var hour = 0
        var minute = 0
        val date: String

        try{
            day = splitText[1].replace(" 0", "").toInt()
            hour = splitText[3].replace(" 0", "").toInt()
            minute = splitText[4].replace(" 0", "").toInt()
        } catch (e: NumberFormatException){
            textForMessage = "$textForMessage ошибка ввода даты-времени или"
        }

        val stringMinute: String = if (minute < 10 ) "0$minute" else "$minute"
        val stringHour: String = if (hour < 10 ) "0$hour" else "$hour"

        if(clients.isEmpty() || day == 0 || hour == 0 || (minute == 0 && splitText[4] != stringMinute)){
            textForMessage = "$textForMessage ошибка ввода фамилии клиента"
            editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  Отмена", callData_backupMenu)
        } else {
            val year = if (localDate.month.value <= month.replace(" 0", "").toInt()) {
                formatter.format(localDate).toInt()
            } else {
                formatter.format(localDate).toInt() + 1
            }

            date = if (day < 10) "$year-${month.replace(" ", "")}-0$day" else "$year-${month.replace(" ", "")}-$day"
            textForMessage = "\uD83D\uDD30 Вы можете записать на $day ${splitText[2]} в $hour:$stringMinute пациента:"

            val inlineKeyboardMarkup = InlineKeyboardMarkup()
            val rowsInline = ArrayList<List<InlineKeyboardButton>>()
            for (elem in clients) {
                val rowInlineButton = ArrayList<InlineKeyboardButton>()
                val button = InlineKeyboardButton()
                button.putData("${elem.secondName} ${elem.firstName} ${elem.patronymic}", "$callData${elem.clientId}#$date#$stringHour:$stringMinute")
                rowInlineButton.add(button)
                rowsInline.add(rowInlineButton)
            }

            val rowInlineButton = ArrayList<InlineKeyboardButton>()
            val returnButton = InlineKeyboardButton()
            returnButton.putData("\uD83D\uDD19  Отмена", callData_backupMenu)
            rowInlineButton.add(returnButton)
            rowsInline.add(rowInlineButton)
            inlineKeyboardMarkup.keyboard = rowsInline
            editMessageText.replyMarkup = inlineKeyboardMarkup
        }
        editMessageText.putData(stringChatId, messageId, textForMessage)
        return editMessageText
    }


    fun receiveSearchClientMenu(stringChatId: String, messageId: Int, longChatId: Long, updateMessageText: String, clientData: Iterable<ClientData>): EditMessageText {
        val clients = clientData.filter { cli -> cli.specialistId == longChatId && cli.secondName.contains(updateMessageText, true) }
        val textForMessage: String
        val editMessageText = EditMessageText()

        if(clients.isEmpty()){
            textForMessage = text_clientNotFound
            editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  Отмена", callData_backupMenu)
        } else {
            textForMessage = text_chooseClient
            val inlineKeyboardMarkup = InlineKeyboardMarkup()
            val rowsInline = ArrayList<List<InlineKeyboardButton>>()
            for (elem in clients) {
                val rowInlineButton = ArrayList<InlineKeyboardButton>()
                val button = InlineKeyboardButton()
                button.putData(elem.secondName + " " +  elem.firstName, "$callData_callBackClientId${elem.clientId}")
                rowInlineButton.add(button)
                rowsInline.add(rowInlineButton)
            }

            val rowInlineButton = ArrayList<InlineKeyboardButton>()
            val returnButton = InlineKeyboardButton()
            returnButton.putData("\uD83D\uDD19  Отмена", callData_mainMenu)
            rowInlineButton.add(returnButton)
            rowsInline.add(rowInlineButton)

            inlineKeyboardMarkup.keyboard = rowsInline
            editMessageText.replyMarkup = inlineKeyboardMarkup
        }

        editMessageText.putData(stringChatId, messageId, textForMessage)
        return editMessageText
    }


    fun receiveAppointmentMonth(stringChatId: String, intMessageId: Int, callBackData: String, clientRepository: ClientDataDao): EditMessageText {
        val clientId = callBackData.replace(callData_callBackClientId, "")

        val client: ClientData = clientRepository.findById(clientId.toLong()).get()
        val editMessageText = EditMessageText()
        val date = LocalDate.now()
        val numFormat = DateTimeFormatter.ofPattern("MM")
        val buttonFormat = DateTimeFormatter.ofPattern("MM.yyyy")
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()
        val firstRowInlineButton = ArrayList<InlineKeyboardButton>()

        val firstButton = InlineKeyboardButton()
        firstButton.putData(buttonFormat.format(date), "${numFormat.format(date)}@$clientId")
        firstRowInlineButton.add(firstButton)

        val secondButton = InlineKeyboardButton()
        secondButton.putData(buttonFormat.format(date.plusMonths(1)), "${numFormat.format(date.plusMonths(1))}@$clientId")
        firstRowInlineButton.add(secondButton)

        val thirdButton = InlineKeyboardButton()
        thirdButton.putData(buttonFormat.format(date.plusMonths(2)), "${numFormat.format(date.plusMonths(2))}@$clientId")
        firstRowInlineButton.add(thirdButton)

        val secondRowInlineButton = ArrayList<InlineKeyboardButton>()
        val returnButton = InlineKeyboardButton()
        returnButton.putData("\uD83D\uDD19  Отмена", callData_mainMenu)
        secondRowInlineButton.add(returnButton)

        rowsInline.add(firstRowInlineButton)
        rowsInline.add(secondRowInlineButton)
        inlineKeyboardMarkup.keyboard = rowsInline

        editMessageText.replyMarkup = inlineKeyboardMarkup

        val textForMessage: String = if (client.appointmentDate.length == 10 && LocalDate.now().isBefore(LocalDate.parse(client.appointmentDate))) {
            "$text_cliAppointmentOne${formatter.format(LocalDate.parse(client.appointmentDate))} в ${client.appointmentTime}$text_cliAppointmentTwo"
        } else {
            text_chooseMonth
        }

        editMessageText.putData(stringChatId, intMessageId, textForMessage)
        return editMessageText
    }


    fun receiveClientSettingMenu(stringChatId: String, intMessageId: Int, callBackData: String, isSubscriptionExpire: Boolean, clientRepository: ClientDataDao): EditMessageText {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val clientId = callBackData.replace(callData_clientSettingMenu, "")
        val client: ClientData = clientRepository.findById(clientId.toLong()).get()

        val menuList = mutableListOf(callData_clientRemark)

        val appointmentText = if (client.appointmentDate.length != 10) {
            "нет"
        } else {
            menuList.add(callData_removeAppointment)
            "${formatter.format(LocalDate.parse(client.appointmentDate))}  в  ${client.appointmentTime}"
        }

        menuList.add(callData_deleteClientMenu)

        if (!isSubscriptionExpire) menuList.add(callData_editeClientName)

        val regText = if (client.chatId.toInt() != 1) {
            text_clientRegistered
        } else {
            menuList.add(callData_generateCode)
            text_canGenPassword
        }

        menuList.add(callData_mainMenu)

        val textForMessage = "$text_forCliMenuOne\n${client.visitHistory}\n\n" +
                "\uD83D\uDD30 Клиент: ${client.secondName} ${client.firstName} ${client.patronymic}" +
                "\n\n$text_forCliMenuTwo$regText$text_forCliMenuThree$appointmentText"

        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, textForMessage)
        editMessageText.replyMarkup = createDataButtonSet(menuList, clientId)
        return editMessageText
    }


    fun receiveAppointmentDay(longChatId: Long, stringChatId: String, intMessageId: Int, callBackData: String, comeBackInfo: HashMap<String, String>, clientRepository: ClientDataDao): EditMessageText {
        val dataString = callBackData.split(callData_appointmentDay)
        val appointmentMonth = dataString[0].toInt()
        val clientId = dataString[1]
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val localDate = LocalDate.now()
        val editMessageText = EditMessageText()
        val startDay: Int
        comeBackInfo[stringChatId] = callBackData

        val stringBuilder = StringBuilder(text_appointmentDate)
        val daysAppointment = HashMap<String, Int>()

        val clients = clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 && it.appointmentDate.split("-")[1] == dataString[0] && (localDate.isEqual(LocalDate.parse(it.appointmentDate)) || localDate.isBefore(LocalDate.parse(it.appointmentDate))) }.sortedBy { it.appointmentDate }
        clients.forEach { if (daysAppointment[it.appointmentDate] == null) daysAppointment[it.appointmentDate] = 1 else daysAppointment[it.appointmentDate] = daysAppointment[it.appointmentDate]!! + 1 }
        daysAppointment.toSortedMap().forEach { stringBuilder.append("• ${formatter.format(LocalDate.parse(it.key))}   записано клиентов:   ${it.value}\n") }

        val appointmentDate = if (localDate.monthValue > appointmentMonth) {
            localDate.plusYears(1).withMonth(appointmentMonth)
        } else {
            localDate.withMonth(appointmentMonth)
        }

        if (appointmentDate.monthValue == localDate.monthValue) {
            startDay = localDate.dayOfMonth
            localDate.lengthOfMonth() - localDate.dayOfMonth // return
        } else {
            startDay = 1
            appointmentDate.lengthOfMonth() // return
        }

        val allDays = appointmentDate.lengthOfMonth()

        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()
        val firstRowInlineButton = ArrayList<InlineKeyboardButton>()
        val secondRowInlineButton = ArrayList<InlineKeyboardButton>()
        val thirdRowInlineButton = ArrayList<InlineKeyboardButton>()
        val fourthRowInlineButton = ArrayList<InlineKeyboardButton>()
        val fifthRowInlineButton = ArrayList<InlineKeyboardButton>()

        var iterationCount = 1
        for (day in startDay .. allDays) {
            when {
                iterationCount < 9 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$day", "&$day#$appointmentMonth#$clientId")
                    firstRowInlineButton.add(button)
                }
                iterationCount < 17 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$day", "&$day#$appointmentMonth#$clientId")
                    secondRowInlineButton.add(button)
                }
                iterationCount < 25 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$day", "&$day#$appointmentMonth#$clientId")
                    thirdRowInlineButton.add(button)
                }
                iterationCount < 33 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$day", "&$day#$appointmentMonth#$clientId")
                    fourthRowInlineButton.add(button)
                }
            }
            iterationCount++
        }

        val button = InlineKeyboardButton()
        button.putData("\uD83D\uDD19  В главное меню", callData_mainMenu)
        fifthRowInlineButton.add(button)

        rowsInline.add(firstRowInlineButton)
        rowsInline.add(secondRowInlineButton)
        rowsInline.add(thirdRowInlineButton)
        rowsInline.add(fourthRowInlineButton)
        rowsInline.add(fifthRowInlineButton)
        inlineKeyboardMarkup.keyboard = rowsInline
        editMessageText.replyMarkup = inlineKeyboardMarkup

        stringBuilder.append(text_chooseDate)

        editMessageText.putData(stringChatId, intMessageId, stringBuilder.toString())
        return editMessageText
    }


    fun receiveAppointmentHour(longChatId: Long, stringChatId: String, intMessageId: Int, callBackData: String, comeBackInfo: HashMap<String, String>, clientRepository: ClientDataDao): EditMessageText {
        val dataText = callBackData.replace(callData_appointmentHour, "")
        val splitData = dataText.split("#")
        val dayOfMonth = if (splitData[0].length == 1) "0${splitData[0]}" else splitData[0]

        val stringBuilder = StringBuilder(text_appointmentTime)
        clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 }.filter { it.appointmentDate.split("-")[2] == dayOfMonth }.sortedBy { it.appointmentTime }.forEach { stringBuilder.append("• ${it.appointmentTime}  -  ${it.secondName} ${it.firstName}\n") }

        val editMessageText = EditMessageText()

        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()
        val firstRowInlineButton = ArrayList<InlineKeyboardButton>()
        val secondRowInlineButton = ArrayList<InlineKeyboardButton>()
        val thirdRowInlineButton = ArrayList<InlineKeyboardButton>()
        val fourthRowInlineButton = ArrayList<InlineKeyboardButton>()

        for (i in 0..24) {
            when (i) {
                in 8..15 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$i", "$callData_appointmentMin$i#$dataText")
                    firstRowInlineButton.add(button)
                }

                in 16..23 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$i", "$callData_appointmentMin$i#$dataText")
                    secondRowInlineButton.add(button)
                }

                in 0..8 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$i", "$callData_appointmentMin$i#$dataText")
                    thirdRowInlineButton.add(button)
                }
            }
        }

        val menuButton = InlineKeyboardButton()
        menuButton.putData("\uD83D\uDD19  В главное меню", callData_mainMenu)
        fourthRowInlineButton.add(menuButton)

        val backButton = InlineKeyboardButton()
        backButton.putData("Выбрать другую дату", comeBackInfo[stringChatId]!!)
        fourthRowInlineButton.add(backButton)

        rowsInline.add(firstRowInlineButton)
        rowsInline.add(secondRowInlineButton)
        rowsInline.add(thirdRowInlineButton)
        rowsInline.add(fourthRowInlineButton)
        inlineKeyboardMarkup.keyboard = rowsInline

        stringBuilder.append(text_chooseHour)
        editMessageText.putData(stringChatId, intMessageId, stringBuilder.toString())
        editMessageText.replyMarkup = inlineKeyboardMarkup
        return editMessageText
    }


    fun receiveAppointmentMinute(stringChatId: String, intMessageId: Int, callBackData: String, comeBackInfo: HashMap<String, String>): EditMessageText {
        val dataText = callBackData.replace(callData_appointmentMin, "")
        val splitData = dataText.split("#")
        comeBackInfo[stringChatId] = ""

        val editMessageText = EditMessageText()
        val localDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy")
        val clientId: String = splitData[3]
        val month: Int = splitData[2].toInt()
        val stringMonth: String = if (month < 10) "0$month" else "$month"
        val day: String = if (splitData[1].toInt() < 10) "0${splitData[1]}" else splitData[1]
        val hour: String = if (splitData[0].toInt() < 10) "0${splitData[0]}" else splitData[0]


        val year: Int = if (localDate.month.value <= month) {
            formatter.format(localDate).toInt()
        } else {
            formatter.format(localDate).toInt() + 1
        }

        val date = "$year-$stringMonth-$day"

        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()
        val firstRowInlineButton = ArrayList<InlineKeyboardButton>()
        val secondRowInlineButton = ArrayList<InlineKeyboardButton>()
        val thirdRowInlineButton = ArrayList<InlineKeyboardButton>()

        for (i in 5..56) {
            when {
                i == 5 -> {
                    val button = InlineKeyboardButton()
                    button.putData("05", "$callData_clientData$clientId#$date#$hour:05")
                    firstRowInlineButton.add(button)
                }

                i % 5 == 0 && i < 35 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$i", "$callData_clientData$clientId#$date#$hour:$i")
                    firstRowInlineButton.add(button)
                }

                i % 5 == 0 -> {
                    val button = InlineKeyboardButton()
                    button.putData("$i", "$callData_clientData$clientId#$date#$hour:$i")
                    secondRowInlineButton.add(button)
                }

                i == 56 -> {
                    val button = InlineKeyboardButton()
                    button.putData("00", "$callData_clientData$clientId#$date#$hour:00")
                    secondRowInlineButton.add(button)
                }
            }
        }

        val menuButton = InlineKeyboardButton()
        menuButton.putData("\uD83D\uDD19  В главное меню", callData_mainMenu)
        thirdRowInlineButton.add(menuButton)

        rowsInline.add(firstRowInlineButton)
        rowsInline.add(secondRowInlineButton)
        rowsInline.add(thirdRowInlineButton)
        inlineKeyboardMarkup.keyboard = rowsInline

        val textForMessage = text_chooseMinute
        editMessageText.putData(stringChatId, intMessageId, textForMessage)
        editMessageText.replyMarkup = inlineKeyboardMarkup
        return editMessageText
    }


    fun changeUserData(stringChatId: String, intMessageId: Int, callBackData: String, tempData: HashMap<String, String>, userRepository: UserDao): EditMessageText {
        val idData = callBackData.replace(callData_changeUser, "").split(" ")
        val userId = idData[0]
        val user = userRepository.findById(userId.toLong()).get()

        val textForMessage = "$text_changeUserDataOne${user.secondName}\nИмя: ${user.firstName}\nОтчество: ${user.patronymic}\nПрофессия: ${user.profession}" +
                "\n❗ Неизменяемое поле Chat id: ${user.chatId}\nПароль: ${user.password}\nВремя отправки сообщений клиенту: ${user.sendTime}\nВременная зона: ${user.timeZone}" +
                "\nОтправка сообщений за дней до приема: ${user.sendBeforeDays}\nДата абонентского платежа: ${user.paymentDate}$text_changeUserDataTwo"

        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, textForMessage)
        editMessageText.replyMarkup = receiveTwoButtonsMenu("❗ Удалить пользователя", "$callData_delAllUserData$userId", "В главное меню  \uD83D\uDD19", callData_mainMenu)
        tempData[stringChatId] = inputChangeUser
        return editMessageText
    }


    fun saveHistoryOfAppointment(clientRepository: ClientDataDao) {
        val localDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        clientRepository.findAll().filter { localDate.minusDays(1).toString() == it.appointmentDate &&
                it.visitAgreement != xSym}.forEach { it.visitHistory = "${it.visitHistory}\n• ${formatter.format(localDate
                .minusDays(1))} в ${it.appointmentTime}"; clientRepository.save(it) }
    }


    fun removeClientsAppointment(clientRepository: ClientDataDao) {
        val localDate = LocalDate.now()
        clientRepository.findAll().filter { localDate.minusDays(1).toString() == it.appointmentDate &&
            it.visitAgreement != wqSym && it.visitAgreement != qSym}.forEach {
            it.appointmentDate = ""; it.appointmentTime = ""; it.visitAgreement = wqSym; clientRepository.save(it) }
    }


    fun changeUserData(updateMessageText: String, stringChatId: String, tempData: HashMap<String, String>, intMessageId: Int, userRepository: UserDao): EditMessageText {
        tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
        val changeData = updateMessageText.split("#")
        val editMessageText = EditMessageText()

        if (changeData.size !in 10..11) {
            editMessageText.putData(stringChatId, intMessageId, text_wrongText)
        } else {
            val newUser = userRepository.findById(changeData[4].toLong()).get()
            val userChatId: Long = if (changeData.size == 11) changeData[10].toLong() else changeData[4].toLong()

            newUser.secondName = changeData[0]
            newUser.firstName = changeData[1]
            newUser.patronymic = changeData[2]
            newUser.profession = changeData[3]
            newUser.password = changeData[5]
            newUser.sendTime = changeData[6].toInt()
            newUser.timeZone = changeData[7].toLong()
            newUser.sendBeforeDays = changeData[8].toLong()
            newUser.paymentDate = changeData[9]
            newUser.chatId = userChatId
            userRepository.save(newUser)

            val user = userRepository.findById(userChatId).get()
            val textForMessage = "$text_userData${user.secondName}\nИмя: " +
                    "${user.firstName}\nОтчество: ${user.patronymic}\nПрофессия: ${user.profession}\nChat id: ${user.chatId}\n" +
                    "Пароль: ${user.password}\nВремя отправки сообщений клиенту: ${user.sendTime}\nВременная зона: ${user.timeZone}" +
                    "\nОтправка сообщений за дней до приема: ${user.sendBeforeDays}\nДата абонентского платежа: ${user.paymentDate}"

            editMessageText.putData(stringChatId, intMessageId, textForMessage)
            editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  В главное меню", callData_mainMenu)
        }
        return editMessageText // фамилия#имя#отчество#профессия#10#555#12#0#1#2050-04-20
    }


    fun putClientBackupToServer(stringChatId: String, intMessageId: Int, directory: String, tempData: HashMap<String, String>, clientRepository: ClientDataDao): EditMessageText {
        tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
        val backupDirectory: String = directory + config.clientBackupTitle
        val serverBackup = ServerBackup()

        serverBackup.startBackup(backupDirectory)
        val clientsList: MutableList<ClientData> = serverBackup.receiveClientsBackup(mutableListOf())
        clientRepository.deleteAll()
        clientsList.forEach { clientRepository.save(it) }
        val clientsAmount: Int = clientRepository.findAll().count()

        val textForMessage = "$text_setBackupOne$clientsAmount$text_setBackupTwo$backupDirectory"
        return receiveBackupMenuMessage(textForMessage, stringChatId, intMessageId)
    }


    fun putUserBackupToServer(stringChatId: String, intMessageId: Int, longChatId: Long, directory: String, tempData: HashMap<String, String>, userRepository: UserDao): EditMessageText {
        tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
        val backupDirectory: String = directory + config.userBackupTitle
        val serverBackup = ServerBackup()

        serverBackup.startBackup(backupDirectory)
        val usersList: MutableList<User> = serverBackup.receiveUsersBackup(mutableListOf())
        usersList.filter { it.chatId != longChatId }.forEach { userRepository.save(it) }
        val usersAmount: Int = userRepository.findAll().count()

        val textForMessage = "$text_setBackupOne$usersAmount$text_setBackupTwo$backupDirectory"
        return receiveBackupMenuMessage(textForMessage, stringChatId, intMessageId)
    }


    fun createBackupInDirectory(stringChatId: String, intMessageId: Int, savedGroupTitle: String, directory: String, backupTitle: String, savedGroupList: List<String>): EditMessageText {
        val backupDirectory = directory + backupTitle
        val backupCreator = BackupCreator()
        val backupFile = backupCreator.receiveBackupFile(savedGroupTitle, savedGroupList)
        backupCreator.createBackupXml(backupFile, backupDirectory)
        val textForMessage = "$text_createBackupOne$savedGroupTitle$text_createBackupTwo$backupDirectory"

        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, textForMessage)
        editMessageText.replyMarkup = receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню",
        callData_mainMenu, "В backup меню", callData_backupMenu)
        return  editMessageText
    }


    fun repairUserAccount(stringChatId: String, intMessageId: Int, longChatId: Long, updateMessageText: String, userRepository: UserDao, clientRepository: ClientDataDao): EditMessageText {
        val oldUser: User
        val user = userRepository.findById(longChatId).get()
        val users = userRepository.findAll().filter { it.chatId != user.chatId && it.password == updateMessageText &&
                it.secondName == user.secondName && it.firstName == user.firstName && it.patronymic == user.patronymic }
        val editMessageText = EditMessageText()

        if (users.isNotEmpty() && users.size == 1) {
            oldUser = users[0]
            clientRepository.findAll().filter { it.specialistId == oldUser.chatId
            }.forEach { it.specialistId = longChatId; clientRepository.save(it) }
            user.password = oldUser.password
            userRepository.save(user)
            userRepository.delete(oldUser)

            editMessageText.putData(stringChatId, intMessageId, text_accountRepaired)
            editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  В главное меню", callData_backupMenu)
        } else {
            editMessageText.putData(stringChatId, intMessageId, text_err)
            editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  В главное меню", callData_mainMenu)
        }
        return editMessageText
    }


    fun setUserPassword(stringChatId: String, intMessageId: Int, longChatId: Long, updateMessageText: String, userRepository: UserDao): EditMessageText {
        val editMessageText = EditMessageText()

        if (updateMessageText.length in 5..15) {
            val user = userRepository.findById(longChatId).get()
            user.password = updateMessageText
            userRepository.save(user)
            editMessageText.putData(stringChatId, intMessageId, "$text_setPass$updateMessageText")
        } else {
            editMessageText.putData(stringChatId, intMessageId, "$text_errPassOne$updateMessageText$text_errPassTwo")
        }
        editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  В главное меню", callData_mainMenu)
        return editMessageText
    }


    fun inputOldUserPassword(stringChatId: String, intMessageId: Int, longChatId: Long, tempData: HashMap<String, String>, updateMessageText: String, userRepository: UserDao): EditMessageText {
        val user = userRepository.findById(longChatId).get()
        val editMessageText = EditMessageText()

        if (updateMessageText == user.password) {
            tempData[stringChatId] = inputPassword
            editMessageText.putData(stringChatId, intMessageId, text_passwordAccepted)
        } else {
            editMessageText.putData(stringChatId, intMessageId, text_err)
        }
        editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  В главное меню", callData_mainMenu)
        return editMessageText
    }


    fun addClientRemark(stringChatId: String, intMessageId: Int, savedId: HashMap<String, Long>, updateMessageText: String, clientRepository: ClientDataDao): EditMessageText {
        val client = clientRepository.findById(savedId[stringChatId]!!).get()
        val editMessageText = EditMessageText()

        if (client.remark.length < 3000) {
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

            val remarkText = if (client.remark.isNotEmpty()) {
                "${client.remark}\n• ${formatter.format(LocalDate.now())}:  $updateMessageText"
            } else {
                "• ${formatter.format(LocalDate.now())}:  $updateMessageText"
            }

            client.remark = remarkText
            clientRepository.save(client)

            editMessageText.putData(stringChatId, intMessageId, "$text_remarkAddOne${client.remark}$text_remarkAddTwo")
        } else {
            editMessageText.putData(stringChatId, intMessageId, "$text_remarkAddOne${client.remark}$text_remarkAddThree")
        }
        editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                callData_mainMenu)
        return editMessageText
    }


    fun createDefaultBackup(stringChatId: String, intMessageId: Int, backupTitle: String, elementList: List<String>): EditMessageText {
        val backupDirectory: String = config.defaultDirectory + backupTitle
        val backupCreator = BackupCreator()
        val backupFile = backupCreator.receiveBackupFile(backupTitle, elementList)
        backupCreator.createBackupXml(backupFile, backupDirectory)
        val textForMessage = "$text_createBackupOne$backupTitle$text_createBackupTwo$backupDirectory"

        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, textForMessage)
        editMessageText.replyMarkup = receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", callData_mainMenu, "В backup меню", callData_backupMenu)
        return editMessageText
    }


    fun lookClientAppointment(longChatId: Long, stringChatId: String, intMessageId: Int, clientRepository: ClientDataDao, userRepository: UserDao): EditMessageText {
        val firstText = text_yourAppointment
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val localDate = LocalDate.now()
        val textForMessage = StringBuilder()
        textForMessage.append(firstText)

        clientRepository.findAll().filter { it.chatId == longChatId && it.appointmentDate.length == 10 &&
                (localDate.isEqual(LocalDate.parse(it.appointmentDate)) || localDate.isAfter(LocalDate.parse(it.appointmentDate))) }.
        sortedBy { it.appointmentDate }.forEach { textForMessage.append("\n\uD83D\uDD39 ${userRepository.findById(it.specialistId).
        get().profession}, ${formatter.format(LocalDate.parse(it.appointmentDate))} в ${it.appointmentTime}") }

        if (textForMessage.toString() == firstText) textForMessage.append(" нет записи.")

        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, textForMessage.toString())
        editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", callData_mainMenu)
        return editMessageText
    }


    fun generateCode(stringChatId: String, callBackData: String, intMessageId: Int, savedId: HashMap<String, Long>, registerPassword: HashMap<String, Int>, clientIdExistCheck: HashMap<String, String>): EditMessageText {
        val clientId = callBackData.replace(callData_generateCode, "")
        var password: Int
        savedId[stringChatId] = 0
        var isSinglePassword = false

        if (registerPassword[stringChatId] == null || registerPassword[stringChatId] == 0){
            password = 0
            while (!isSinglePassword){
                password = (100..999).random()
                for ((_, value) in registerPassword) {
                    if (value == password) break
                }
                isSinglePassword = true
            }
            registerPassword[stringChatId] = password
        } else {
            password = registerPassword[stringChatId]!!
        }

        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, "$text_addClientOne$password$text_addClientTwo")
        editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", callData_mainMenu)
        clientIdExistCheck[stringChatId] = clientId
        return editMessageText
    }


    fun receiveClientsMenu(longChatId: Long, stringChatId: String, intMessageId: Int, clientRepository: ClientDataDao, userRepository: UserDao): EditMessageText {
        val localDate = LocalDate.now()
        val clients = clientRepository.findAll().filter { it.specialistId == longChatId }
        val amount = clients.size
        val user = userRepository.findById(longChatId).get()

        var textForMessage = "$text_clientLimitFour$amount\n\n"

        textForMessage += if (localDate.isAfter(LocalDate.parse(user.paymentDate))){
            if ((config.freeClientsAmount - amount) > 0){
                "$text_clientLimitFive${config.freeClientsAmount}$text_clientLimitSix${config.freeClientsAmount - amount}" +
                "$text_clientLimitOne${config.maxClientsAmount}$text_clientLimitTwo${config.maxClientsAmount}$text_clientLimitThree"
            } else {
                "$text_clientLimitFive${config.freeClientsAmount}$text_clientLimitEight$text_clientLimitOne" +
                "${config.maxClientsAmount}$text_clientLimitTwo${config.maxClientsAmount}$text_clientLimitThree"
            }
        } else {
            if ((config.maxClientsAmount - amount) > 0) {
                "$text_clientLimitFive${config.maxClientsAmount}$text_clientLimitSix${config.maxClientsAmount - amount}"
            } else {
                "$text_clientLimitFive${config.maxClientsAmount}$text_clientLimitSeven"
            }
        }

        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, textForMessage)
        editMessageText.replyMarkup = receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", callData_mainMenu, "Назад  ⚙", callData_myAccount)
        return editMessageText
    }


    fun receiveSchedule(longChatId: Long, stringChatId: String, intMessageId: Int, clientRepository: ClientDataDao): EditMessageText {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        var dateText = ""
        val textForMessage = StringBuilder()
        val localDate = LocalDate.now()
        textForMessage.append(text_yourSchedule)

        clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 &&
                !localDate.isAfter(LocalDate.parse(it.appointmentDate)) }.sortedBy { it.appointmentDate }.asReversed()
                .forEach { textForMessage.append( "\n" + (if (dateText == it.appointmentDate) "" else "\n") + "\uD83D\uDD39 " +
                        "${formatter.format(LocalDate.parse(it.appointmentDate))} в " + "${it.appointmentTime}  - ${it.secondName} " +
                        "${it.firstName.first()}"); dateText = it.appointmentDate }

        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, textForMessage.toString())
        editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", callData_mainMenu)
        return editMessageText
    }

    fun receiveAllClients(longChatId: Long, stringChatId: String, callBackData: String, intMessageId: Int, clientRepository: ClientDataDao): EditMessageText {
        val returnBackData = callBackData.replace(callData_allClients, "")
        val clientsList = mutableListOf<String>()
        val clients = clientRepository.findAll().filter { it.specialistId == longChatId }.sortedBy { it.secondName }
        clients.distinctBy { it.secondName }.forEach { clientsList.add(it.secondName) }

        clientsList.add(callData_mainMenu)
        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, "$text_allAddedOne${clients.size}$text_allAddedTwo")
        editMessageText.replyMarkup = createDataButtonSet(clientsList, returnBackData)
        return editMessageText
    }


    fun receiveClientRemark(stringChatId: String, callBackData: String, intMessageId: Int, tempData: HashMap<String, String>, savedId: HashMap<String, Long>, clientRepository: ClientDataDao): EditMessageText {
        val clientId = callBackData.replace(callData_clientRemark, "").toLong()
        val client: ClientData = clientRepository.findById(clientId).get()
        val editMessageText = EditMessageText()
        val remarkText = "$text_addRemarkOne${client.remark}$text_addRemarkTwo"

        editMessageText.putData(stringChatId, intMessageId, remarkText)

        if (client.remark.isEmpty()){
            editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  В главное меню", callData_mainMenu)
        } else {
            editMessageText.replyMarkup = receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", callData_mainMenu, "Удалить записи", "$callData_delClientRemark$clientId")
        }
        savedId[stringChatId] = clientId
        tempData[stringChatId] = inputRemark
        return editMessageText
    }


    fun putSendMessageTime(longChatId: Long, stringChatId: String, intMessageId: Int, putTime: Int, userRepository: UserDao): EditMessageText {
        val user = userRepository.findById(longChatId).get()
        val sendMessageTime = user.sendTime

        when {
            putTime == -1 && sendMessageTime > 6 -> user.sendTime = sendMessageTime - 1
            putTime == 1 && sendMessageTime < 22 -> user.sendTime = sendMessageTime + 1
        }
        userRepository.save(user)
        return receiveUserSettingsMenu(stringChatId, intMessageId, user)
    }


    fun putSendMessageDay(longChatId: Long, stringChatId: String, intMessageId: Int, putDay: Int, userRepository: UserDao): EditMessageText {
        val user = userRepository.findById(longChatId).get()
        val sendMessageDay = user.sendBeforeDays

        when {
            putDay == -1 && sendMessageDay > 1 -> user.sendBeforeDays = sendMessageDay - 1
            putDay == 1 && sendMessageDay < 3 -> user.sendBeforeDays = sendMessageDay + 1
        }

        userRepository.save(user)
        return receiveUserSettingsMenu(stringChatId, intMessageId, user)
    }


    fun putTimeZone(longChatId: Long, stringChatId: String, intMessageId: Int, putTimeZone: Int, userRepository: UserDao): EditMessageText {
        val user = userRepository.findById(longChatId).get()
        val timeZoneHour = user.timeZone

        when {
            putTimeZone == -1 && timeZoneHour > -5 -> user.timeZone = timeZoneHour - 1
            putTimeZone == 1 && timeZoneHour < 10 -> user.timeZone = timeZoneHour + 1
        }

        userRepository.save(user)
        return receiveUserSettingsMenu(stringChatId, intMessageId, user)
    }


    fun receiveAppointmentForClient(longChatId: Long, stringChatId: String, intMessageId: Int, userRepository: UserDao, clientRepository: ClientDataDao): EditMessageText {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val editMessageText = EditMessageText()
        val clients = clientRepository.findAll().filter { it.chatId == longChatId && it.appointmentDate.length == 10 }.sortedBy { it.appointmentTime }.sortedBy { it.appointmentDate } // TODO it.appointmentTime .sortedBy  it.appointmentDate

        if (clients.isEmpty()) {
            editMessageText.putData(stringChatId, intMessageId, text_notAppointment)
        } else {
            val textForMessage = StringBuilder()
            textForMessage.append(text_appointmentOne)
            clients.forEach { textForMessage.append("\n\uD83D\uDD39 ${formatter.format(LocalDate.parse(it.appointmentDate))} в ${it.appointmentTime}$text_appointmentTwo${userRepository.findById(it.specialistId).get().getFullName()}") }
            editMessageText.putData(stringChatId, intMessageId, textForMessage.toString())
        }
        editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  В главное меню", callData_mainMenu)
        return editMessageText
    }


    fun deleteClientRemark(stringChatId: String, callBackData: String, intMessageId: Int, clientRepository: ClientDataDao): EditMessageText {
        val clientId = callBackData.replace(callData_delClientRemark, "").toLong()
        val client = clientRepository.findById(clientId).get()
        client.remark = ""
        clientRepository.save(client)

        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, text_remarkDeleted)
        editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  В главное меню", callData_mainMenu)
        return editMessageText
    }


    fun receiveUserPasswordMenu(longChatId: Long, stringChatId: String, intMessageId: Int, tempData: HashMap<String, String>, userRepository: UserDao): EditMessageText {
        val user = userRepository.findById(longChatId).get()
        val editMessageText = EditMessageText()

        if (user.password.isEmpty()){
            editMessageText.putData(stringChatId, intMessageId, text_addPassword)
            tempData[stringChatId] = inputPassword
        } else {
            editMessageText.putData(stringChatId, intMessageId, text_hasPassword)
            tempData[stringChatId] = inputOldPassword
        }
        editMessageText.replyMarkup = receiveOneButtonMenu("\uD83D\uDD19  Отмена", callData_mainMenu)
        return editMessageText
    }


    fun addNewClient(stringChatId: String, intMessageId: Int, isSubscriptionExpire: Boolean): EditMessageText {
        val editMessageText: EditMessageText
        if (isSubscriptionExpire) {
            editMessageText = receiveSubscriptionMessage(intMessageId, stringChatId)
        } else {
            editMessageText = EditMessageText()
            editMessageText.putData(stringChatId, intMessageId, text_addNewClient)
            val settingList = listOf(callData_generateCode, callData_addCommonClient, callData_mainMenu)
            editMessageText.replyMarkup = createButtonSet(settingList)
        }
        return editMessageText
    }


    fun receiveInvoiceLink(stringChatId: String, textForDescription: String): CreateInvoiceLink {
    val labeledPriceList: List<LabeledPrice> = listOf(LabeledPrice("Оплата абонемента", config.subscriptionPrice * 100))
        return CreateInvoiceLink("Абонентская плата", // @NonNull String title
            textForDescription, // @NonNull String description
            stringChatId, // @NonNull String payload - определенная полезная нагрузка счета-фактуры, 1-128 байт. Информация не видна пользователю, используйте для своих внутренних процессов
            config.payToken, // @NonNull String providerToken - токен банка/провайдера платежей - "381764678:TEST:62053", // @NonNull String providerToken - Юкасса токен (Юкасса подключает физлиц)
            "RUB", // @NonNull String currency (валюта)
            labeledPriceList, // @NonNull List<LabeledPrice> prices
            "https://disk.yandex.ru/i/AbFv3vCPhpVypA", // String photoUrl - фотография в меню покупки
            null, null, null, //  Integer photoSize, Integer photoWidth, Integer photoHeight

            // Boolean needName, Boolean needPhoneNumber, Boolean needEmail, Boolean needShippingAddress, Boolean isFlexible, Boolean sendPhoneNumberToProvider, Boolean sendEmailToProvider
            false, false, false, false /* needShippingAddress */, false /* isFlexible - цена зависит от доставки */, false/* sendPhoneNumberToProvider */, false,
            config.bill, // String providerData - JSON-сериализованные данные о счете-фактуре, которые будут переданы поставщику платежей. Подробное описание обязательных полей должно быть предоставлено поставщиком платежных услуг //TODO
            0, // Integer maxTipAmount - максимальный размер чаевых
            null)
    }


    fun receivePayMenu(stringChatId: String, intMessageId: Int, payLync: String, textForMessage: String): EditMessageText {
        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, textForMessage)
        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()
        val firstRowInlineButton = ArrayList<InlineKeyboardButton>()

        val firstButton = InlineKeyboardButton()
        firstButton.putData("\uD83D\uDD19  Отмена", callData_startMenu)
        firstRowInlineButton.add(firstButton)

        val secondButton = InlineKeyboardButton()
        secondButton.putData("Оплатить", callData_startMenu)
        secondButton.url = payLync
        firstRowInlineButton.add(secondButton)

        rowsInline.add(firstRowInlineButton)
        inlineKeyboardMarkup.keyboard = rowsInline
        editMessageText.replyMarkup = inlineKeyboardMarkup
        return editMessageText
    }


    fun receiveClientList(userId: Long, clientRepository: ClientDataDao): String {
        val stringBuilder = StringBuilder()
        clientRepository.findAll().filter { it.specialistId == userId }.sortedBy { it.appointmentDate }.
        forEach { stringBuilder.append("${it.getFullName()}, запись: ${it.appointmentDate} - ${it.appointmentTime}; заметки клиента: ${it.remark}.\n") }
        return stringBuilder.toString()
    }


    fun receiveBackupList(stringChatId: String, fileDirectory: String): SendDocument {
        val sendDocument = SendDocument()
        sendDocument.document = InputFile(File(fileDirectory))
        sendDocument.caption = text_backupText
        sendDocument.replyMarkup = receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", callData_delMessage)
        sendDocument.disableNotification = true
        sendDocument.chatId = stringChatId
        return sendDocument
    }






}
