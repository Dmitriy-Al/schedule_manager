package aldmitry.dev.personalmanager.service

import aldmitry.dev.personalmanager.extendfunctions.protectedExecute
import aldmitry.dev.personalmanager.extendfunctions.putData
import aldmitry.dev.personalmanager.model.ClientData
import aldmitry.dev.personalmanager.model.ClientDataDao
import aldmitry.dev.personalmanager.model.User
import aldmitry.dev.personalmanager.model.UserDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class InputOutputCommand(@Autowired val clientRepository: ClientDataDao, @Autowired val userRepository: UserDao) :
    TelegramLongPollingBot("5684975537:AAHNI1ulaYG9U0ifSlOet3r6DClVoPWlgUk") {

    init { // Команды меню бота
        val botCommandList: List<BotCommand> = listOf(
            BotCommand("/start", "Запуск программы"),
            BotCommand("/star", "Запуск программы"),
            BotCommand("/help", "Полезная информация"),
            BotCommand("/mydata", "Данные пользователя"),
            BotCommand("/deletedata", "Удаление всех данных пользователя")
        )
        try {
            this.execute(SetMyCommands(botCommandList, BotCommandScopeDefault(), null))
        } catch (e: TelegramApiException) {

        }
    }

    // TODO сделать так, чтобы если фио добавляемого клиента совпадало с имеющимся, предложить замещение

    private val botMenuFunction = BotMenuFunction()

    private val tempData = HashMap<String, String>()
    private val firstName = HashMap<String, String>()
    private val secondName = HashMap<String, String>()
    private val patronymic = HashMap<String, String>()
    private val profession = HashMap<String, String>()
    private val saveChatId = HashMap<String, Long>()
    private val comeBackInfo = HashMap<String, String>()
    private val registerPassword = HashMap<String, Int>()
    private val saveStartMessageId = HashMap<String, Int>()
    // private val blockUpdateMessageInfo = HashMap<String, Boolean>()



    private final val inputFirstName = "INPUT_FIRST_NAME"
    private final val inputPatronymic = "INPUT_PATRONYMIC"
    private final val inputSecondName = "INPUT_SECOND_NAME"
    private final val inputProfession = "INPUT_PROFESSION"
    private final val inputRemark = "INPUT_REMARK"
    private final val findClient = "FIND_CLIENT"


    final val clientMenu = "#climen"
    final val findClientForMenu = "#finmenu"
    final val findClientByName = "#findcli"
    final val callBackClientId = "#clid"
    final val clientData = "#cldata"
    final val addNewClient =  "Добавить нового клиента/пациента"
    final val generationCode = "Генерировать код для клиента" // "Генерировать код и добавить клиента"
    final val addCommonClient = "Добавить клиента без кода" // Добавить клиента без Telegram





    override fun getBotUsername(): String {
        return "TestDemoUnicNameBot"
    }


    // Бот получил команду (сообщение от пользователя)
    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val longChatId: Long = update.message.chatId
            val intMessageId: Int = update.message.messageId
            val updateMessageText: String = update.message.text
            val stringChatId: String = longChatId.toString()


            when (tempData[stringChatId]) { // если в Map добавляется строка-константа, Update-сообщение (updateMessageText) запускает одну из функций в блоке

                inputRemark -> {
                    val client = clientRepository.findById(saveChatId[stringChatId]!!).get()

                    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

                    val remarkText = if (client.remark.isNotEmpty()) {
                        "${client.remark}\n• ${formatter.format(LocalDate.now())}  -  $updateMessageText"
                    } else {
                        "• ${formatter.format(LocalDate.now())}  -  $updateMessageText"
                    }

                    client.remark = remarkText
                    clientRepository.save(client)

                    val editMessageText = EditMessageText()
                    editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!,  "\uD83D\uDCDD Заметки:\n${client.remark}\n\n\uD83D\uDD30  Заметка была добавлена.")
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                }

                findClient -> {
                    findClient(stringChatId, longChatId, updateMessageText, callBackClientId)
                }

                inputSecondName -> {
                    if (updateMessageText.split(" ").size == 3) {
                        val splitUpdateMessage = updateMessageText.split(" ")
                        secondName[stringChatId] = splitUpdateMessage[0].replace(".", "").replace("Ё", "Е").replace("ё", "Е").trim()
                        firstName[stringChatId] = splitUpdateMessage[1].replace(".", "").trim()
                        patronymic[stringChatId] = splitUpdateMessage[2].replace(".", "").trim()
                        setFullName(stringChatId, longChatId)
                    } else {
                        val editMessageText = EditMessageText()
                        secondName[stringChatId] = updateMessageText.replace("Ё", "Е").replace("ё", "Е").trim()
                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "Введите имя и отправьте сообщение")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputFirstName
                    }
                }

                inputFirstName -> {
                    firstName[stringChatId] = updateMessageText.replace(".", "").trim()

                    val editMessageText = EditMessageText()
                    editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "Введите отчество и отправьте сообщение")
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                    tempData[stringChatId] = inputPatronymic

                }

                inputPatronymic -> {
                    patronymic[stringChatId] = updateMessageText.replace(".", "").trim()
                    setFullName(stringChatId, longChatId)
                }

                inputProfession -> {
                    profession[stringChatId] = updateMessageText
                    val date = LocalDate.now()
                    val nextPaymentDate = date.plusMonths(3)

                    val user: User = userRepository.findById(longChatId).get()
                    user.firstName = firstName[stringChatId]!!
                    user.secondName = secondName[stringChatId]!!
                    user.patronymic = patronymic[stringChatId]!!
                    user.profession = profession[stringChatId]!!.lowercase()
                    user.paymentDate = nextPaymentDate.toString()
                    userRepository.save(user)

                    val editMessageText = EditMessageText()
                    editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "✅ Спасибо за регистрацию!")
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                }
            }


            if (!updateMessageText.contains("/") && (tempData[stringChatId].isNullOrEmpty())) {
            val month = when {
                updateMessageText.contains("январ", true) -> " 01"
                updateMessageText.contains("феврал", true) -> " 02"
                updateMessageText.contains("март", true) -> " 03"
                updateMessageText.contains("апрел", true) -> " 04"
                updateMessageText.contains("мая", true) -> " 05"
                updateMessageText.contains("май", true) -> " 05"
                updateMessageText.contains("июн", true) -> " 06"
                updateMessageText.contains("июл", true) -> " 07"
                updateMessageText.contains("август", true) -> " 08"
                updateMessageText.contains("сентябр", true) -> " 09"
                updateMessageText.contains("октябр", true) -> "10"
                updateMessageText.contains("ноябр", true) -> "11"
                updateMessageText.contains("декабр", true) -> "12"
                else -> "null"
            }

            when  {
                updateMessageText.split(" ").size >= 4 && month != "null" -> {
                    val dataText: String = updateMessageText.replace(":", " ").replace(",", " ").replace(".", " ").replace(" в", "").replace(" часов", "").replace(" на", "").replace("- ", "").replace("-", " ").trim()
                    val splitText: List<String> = dataText.split(" ")
                    val clientSecondName: String = splitText[0]
                    val localDate = LocalDate.now()
                    val formatter = DateTimeFormatter.ofPattern("yyyy")

                    val clients = clientRepository.findAll().filter { cli -> cli.specialistId == longChatId && cli.secondName.contains(clientSecondName, true) } // TODO повтор кода 191

                    var textForMessage = "ℹ "
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
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
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
                            button.putData("${elem.secondName} ${elem.firstName} ${elem.patronymic}", "$clientData${elem.clientId}#$date#$stringHour:$stringMinute")
                            rowInlineButton.add(button)
                            rowsInline.add(rowInlineButton)
                        }

                        val rowInlineButton = ArrayList<InlineKeyboardButton>()
                        val returnButton = InlineKeyboardButton()
                        returnButton.putData("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                        rowInlineButton.add(returnButton)
                        rowsInline.add(rowInlineButton)

                        inlineKeyboardMarkup.keyboard = rowsInline
                        editMessageText.replyMarkup = inlineKeyboardMarkup
                    }

                    editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, textForMessage)
                    protectedExecute(editMessageText)
                }

                updateMessageText.split(" ").size == 3 -> {
                    val splitUpdateMessage = updateMessageText.split(" ")
                    saveChatId[stringChatId] = 1
                    secondName[stringChatId] = splitUpdateMessage[0]
                    firstName[stringChatId] = splitUpdateMessage[1]
                    patronymic[stringChatId] = splitUpdateMessage[2]
                    setFullName(stringChatId, longChatId)
                }

                !updateMessageText.contains(" ") && updateMessageText.length in 3..12 -> {
                   findClient(stringChatId, longChatId, updateMessageText, "#getmenu") // TODO "#getmenu"
                }

                updateMessageText.length > 2 && tempData[stringChatId].isNullOrEmpty() -> {
                    val clients = clientRepository.findAll().filter { cli -> cli.specialistId == longChatId && cli.secondName.contains(updateMessageText, true) }
                    val textForMessage: String
                    val editMessageText = EditMessageText()

                    if(clients.isEmpty()){
                        textForMessage = "ℹ  Клиента с таким набором букв в фамилии не найдено"
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    } else {
                        textForMessage = "\uD83D\uDD30 Выберите клиента/пациента из списка ниже"
                        val inlineKeyboardMarkup = InlineKeyboardMarkup()
                        val rowsInline = ArrayList<List<InlineKeyboardButton>>()
                        for (elem in clients) {
                            val rowInlineButton = ArrayList<InlineKeyboardButton>()
                            val button = InlineKeyboardButton()
                            button.putData(elem.secondName + " " +  elem.firstName, "#clid" + elem.clientId)
                            rowInlineButton.add(button)
                            rowsInline.add(rowInlineButton)
                        }

                        val rowInlineButton = ArrayList<InlineKeyboardButton>()
                        val returnButton = InlineKeyboardButton()
                        returnButton.putData("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                        rowInlineButton.add(returnButton)
                        rowsInline.add(rowInlineButton)

                        inlineKeyboardMarkup.keyboard = rowsInline
                        editMessageText.replyMarkup = inlineKeyboardMarkup
                    }

                    editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, textForMessage)
                    protectedExecute(editMessageText)
                }

              }
            }


            if (registerPassword.isNotEmpty()){
                for ((key, value) in registerPassword){
                    if(value.toString() == updateMessageText) {
                       val sendMessage = SendMessage(stringChatId,  "✅ Спасибо! До завершения регистрации специалистом пройдёт ещё немного времени")
                       protectedExecute(sendMessage)

                        val textForMessage = "✅ Пароль был подтверждён, осталось заполнить ФИО и регистрация будет завершена. Введите, пожалуйста, фамилию клиента или ФИО полностью."
                        val editMessageText = EditMessageText().putData(key, saveStartMessageId[key]!!, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        tempData[key] = inputSecondName
                        saveChatId[key] = longChatId

                        if (registerPassword.size == 1) registerPassword.clear()
                        return
                    }
                }
            }


            protectedExecute(DeleteMessage().putData(stringChatId, intMessageId))

            when (updateMessageText) { // команды

                "/star" -> { // начало работы бота
                    tempData[stringChatId] = ""
                    comeBackInfo[stringChatId] = ""
                    saveChatId[stringChatId] = 0

                    if (userRepository.findById(longChatId).isEmpty) {
                        val user = User()
                        user.chatId = longChatId
                        userRepository.save(user)
                    }

                   if (saveStartMessageId[stringChatId] != null) protectedExecute(DeleteMessage().putData(stringChatId, saveStartMessageId[stringChatId]!!))

                    val user: User = userRepository.findById(longChatId).get()

                   if (user.firstName.isEmpty()){
                       val sendMessage = SendMessage(stringChatId, "Если вам сообщили короткий код, введите его и отправьте сообщение. Спасибо за использование программы!")
                       val settingList = listOf("\uD83D\uDCC5  Посмотреть мою запись", "Зарегистрироваться как специалист") // TODO "Посмотреть мою запись", "Зарегистрироваться как специалист"
                       sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                       saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                   } else {
                       val sendMessage = SendMessage(stringChatId, "Вы можете записывать и регистрировать новых пациентов и вести запись пациентов. Спасибо за использование программы!")
                       val settingList = listOf("Записать на приём", addNewClient, "Работа с базой клиентов/пациентов", "⚙  Настройки и абонемент") // TODO "Посмотреть мою запись", "Зарегистрироваться как специалист"
                       sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                       saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                   }
                }

                "/start" -> { // начало работы бота
                    tempData[stringChatId] = ""
                    comeBackInfo[stringChatId] = ""
                    saveChatId[stringChatId] = 0

                    if (userRepository.findById(longChatId).isEmpty) {
                        val user = User()
                        user.chatId = longChatId
                        userRepository.save(user)
                    }

                    if (saveStartMessageId[stringChatId] != null) protectedExecute(DeleteMessage().putData(stringChatId, saveStartMessageId[stringChatId]!!))

                    val user: User = userRepository.findById(longChatId).get()

                    if (user.firstName.isEmpty()){
                        val sendMessage = SendMessage(stringChatId, "Если вам сообщили короткий код, введите его и отправьте сообщение. Спасибо за использование программы!")
                        val settingList = listOf("\uD83D\uDCC5  Посмотреть мою запись", "Зарегистрироваться как специалист") // TODO "Посмотреть мою запись", "Зарегистрироваться как специалист"
                        sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                        saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                    } else {
                        val sendMessage = SendMessage(stringChatId, "Вы можете записывать и регистрировать новых пациентов и вести запись пациентов. Спасибо за использование программы!")
                        val settingList = listOf("Записать на приём", addNewClient, "Работа с базой клиентов/пациентов", "⚙  Настройки и абонемент") // TODO "Посмотреть мою запись", "Зарегистрироваться как специалист"
                        sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                        saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                    }
                }

                "/help" -> { // начало работы бота
                    tempData[stringChatId] = ""
                    comeBackInfo[stringChatId] = ""

                }

                else -> {
                    if (tempData[stringChatId].isNullOrEmpty()) protectedExecute(DeleteMessage().putData(stringChatId, intMessageId))

                }
            }

            } else if (update.hasCallbackQuery()) {
                val intMessageId: Int = update.callbackQuery.message.messageId
                val stringChatId: String = update.callbackQuery.message.chatId.toString()
                val longChatId: Long = update.callbackQuery.message.chatId
                val callBackData: String = update.callbackQuery.data

                when {
                    callBackData.contains("\uD83D\uDD19  Назад в меню") -> {
                        saveChatId[stringChatId] = 0
                        tempData[stringChatId] = ""
                        comeBackInfo[stringChatId] = ""

                        val user: User = userRepository.findById(longChatId).get()

                        if (user.firstName.isEmpty()){
                            val editMessageText = EditMessageText()
                            editMessageText.putData(stringChatId, intMessageId, "Если вам сообщили короткий код, введите его и отправьте сообщение. Спасибо за использование программы!")
                            val settingList = listOf("\uD83D\uDCC5  Посмотреть мою запись", "Зарегистрироваться как специалист") // TODO "Посмотреть мою запись", "Зарегистрироваться как специалист"
                            editMessageText.replyMarkup = botMenuFunction.createButtonSet(settingList)
                            protectedExecute(editMessageText)
                        } else {
                            val editMessageText = EditMessageText()
                            editMessageText.putData(stringChatId, intMessageId, "Вы можете записывать и регистрировать новых пациентов и вести запись пациентов. Спасибо за использование программы!")
                            val settingList = listOf("Записать на приём", addNewClient, "Работа с базой клиентов/пациентов", "⚙  Настройки и абонемент") // TODO "Посмотреть мою запись", "Зарегистрироваться как специалист"
                            editMessageText.replyMarkup = botMenuFunction.createButtonSet(settingList)
                            protectedExecute(editMessageText)
                        }
                    }

                    callBackData == "\uD83D\uDCC5  Посмотреть мою запись" -> { // TODO "Посмотреть мою запись",
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "Ваша запись у специалиста:\n\uD83D\uDD39 Вагина Таисия Олеговна, проктолог: 12.05.2024\n")
                        val settingList = listOf("Выписаться", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                        editMessageText.replyMarkup = botMenuFunction.createButtonSet(settingList)
                        protectedExecute(editMessageText)
                    }

                    callBackData == "Зарегистрироваться как специалист" -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "В этом разделе вы можете зарегистрироваться как специалист и упростить ведение записи клиентов/пациентов.")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("Зарегистрироваться",
                            "#reg", "\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                        protectedExecute(editMessageText)
                    }

                    callBackData == addNewClient -> {
                        val editMessageText = EditMessageText()
                        val text = "\uD83D\uDD30 Если ваш клиент/пациент использует Telegram, вы можете сообщить ему адрес бота - @Bot\nЗатем нажмите клавишу <Сгенерировать код> и " +   // TODO @Bot
                                "сообщите код с экрана клиенту. Клиент должен зайти в бот, ввести этот код и отправить сообщение, больше ничего не требуется. Вам останется только заполнить ФИО клиента и клиент будет добавлен в вашу базу." +
                                "Если вы хотите быстро записать пациента, можно вводить ФИО не поочерёдно, а сразу целиком, или воспользоваться голосовым вводом нажав на клавиатуре телефона значок микрофона \uD83C\uDF99 и произнести " +
                                "ФИО пациента, затем отправить сообщение\n\n\uD83D\uDD30 Если клиент/пациент не использует Telegram, можно записать его данные для того, чтобы программа напоминала вам о необходимости " +
                                "сообщить пациенту о предстоящем приёме. Для записи клиента в базу нажмите клавишу <Добавить клиента/пациента>"
                        editMessageText.putData(stringChatId, intMessageId, text)
                        val settingList = listOf(generationCode, addCommonClient, "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                        editMessageText.replyMarkup = botMenuFunction.createButtonSet(settingList)
                        protectedExecute(editMessageText)
                    }

                    callBackData == addCommonClient -> {
                        val editMessageText = EditMessageText()
                        val text = "\uD83D\uDD30 Введите фамилию клиента/пациента или ФИО полностью и отправьте сообщение."
                        editMessageText.putData(stringChatId, intMessageId, text)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        saveChatId[stringChatId] = 1
                        tempData[stringChatId] = inputSecondName
                    }

                    callBackData == "Работа с базой клиентов/пациентов" -> {
                        val textForMessage = "\uD83D\uDD30 Здесь вы можете удалить и редактировать данные клиента/пациента, а так же добавлять к каждому собственные заметки. Чтобы найти нужного клиента, " +
                        "нажмите клавишу с первой буквой фамилии клиента"
                        val editMessageText = botMenuFunction.receiveFindClientKeyboard(stringChatId, intMessageId, textForMessage, findClientForMenu)
                        protectedExecute(editMessageText)
                    }

                    callBackData == "Записать на приём" -> { // receiveFindClientKeyboard
                        val textForMessage = "\uD83D\uDD30 Чтобы найти нужного клиента, нажмите клавишу с первой буквой фамилии, или введите первую букву/несколько первых букв фамилии клиента и отправьте сообщение."
                        val editMessageText = botMenuFunction.receiveFindClientKeyboard(stringChatId, intMessageId, textForMessage, findClientByName)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = findClient
                    }

                    callBackData == generationCode -> {
                        var password: Int
                        var isSinglePassword = false
                        saveChatId[stringChatId] = 0

                        if (registerPassword[stringChatId] == null || registerPassword[stringChatId] == 0){
                            password = 0
                            while (!isSinglePassword){
                                password = (100..999).random()
                                for ((key, value) in registerPassword) {
                                    if (value == password) break
                                }
                                isSinglePassword = true
                            }
                            registerPassword[stringChatId] = password
                        } else {
                            password = registerPassword[stringChatId]!!
                        }

                        val editMessageText = EditMessageText()
                        val text = "ㅤ\n\n\uD83D\uDD38 Сообщите этот пароль клиенту:  $password\n\nКлиент должен зайти в Telegram, ввести в поиске адрес бота @Bot\nзайти в бот, ввести и отправить сообщение с паролем. " +
                        "Если клиент успешно введёт и отправит пароль, у вас появится меню регистрации нового клиента. !Внимание: пароль будет действителен в течение 10 минут, если до истечения этого времени клиент не " +
                                "будет зарегистрирован, сгенерируйте новый пароль и проведите регистрацию заново."
                        editMessageText.putData(stringChatId, intMessageId, text)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    }

                     callBackData == "⚙  Настройки и абонемент" -> {
                    val editMessageText = EditMessageText()
                    val text = "\uD83D\uDD30 Меню с настройками"
                    editMessageText.putData(stringChatId, intMessageId, text)
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                    protectedExecute(editMessageText)
                }

                    callBackData.contains("#allcli") -> {
                        val returnBackData = callBackData.replace("#allcli", "")
                        val clientsList = mutableListOf<String>()
                        clientRepository.findAll().filter { it.specialistId == longChatId }.sortedBy { it.secondName }.forEach { clientsList.add(it.secondName) }
                        clientsList.add("\uD83D\uDD19  Назад в меню")
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "Список всех клиентов/пациентов")
                        editMessageText.replyMarkup = botMenuFunction.createDataButtonSet(clientsList, returnBackData)
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("Заметки клиента") -> {
                        val clientId = callBackData.replace("Заметки клиента", "").toLong()
                        val client: ClientData = clientRepository.findById(clientId).get()
                        val editMessageText = EditMessageText()
                        val remarkText = "\uD83D\uDCDD Заметки:\n${client.remark}\n\n\uD83D\uDD30  Здесь вы можете добавить или удалить записи, связанные с клиентом. Эти записи доступны только вам. " +
                         "\n\uD83D\uDD39 Для добавления заметки ведите текст и отправьте сообщение."

                        editMessageText.putData(stringChatId, intMessageId, remarkText)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Главное меню", "\uD83D\uDD19  Назад в меню", "Удалить записи", "#delrem$clientId")
                        protectedExecute(editMessageText)
                        saveChatId[stringChatId] = clientId
                        tempData[stringChatId] = inputRemark
                    }

                    callBackData.contains("#delrem") -> {
                        tempData[stringChatId] = ""
                        saveChatId[stringChatId] = 0

                        val clientId = callBackData.replace("#delrem", "").toLong()
                        val client = clientRepository.findById(clientId).get()
                        client.remark = ""
                        clientRepository.save(client)

                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Все заметки были удалены.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("Редактировать ФИО") -> {
                        val clientId = callBackData.replace("Редактировать ФИО", "").toLong()
                        val editMessageText = EditMessageText()
                        val text = "\uD83D\uDD30 Введите фамилию клиента/пациента или ФИО полностью и отправьте сообщение."
                        editMessageText.putData(stringChatId, intMessageId, text)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        saveChatId[stringChatId] = clientId
                        tempData[stringChatId] = inputSecondName
                    }

                    callBackData.contains("❗  Выписать клиента") -> {
                        val clientId = callBackData.replace("❗  Выписать клиента", "").toLong()
                        val user = userRepository.findById(longChatId).get()
                        val client = clientRepository.findById(clientId).get()
                        client.appointmentTime = ""
                        client.appointmentDate = ""
                        clientRepository.save(client)

                        val textForMessage: String

                        if (client.chatId > 1){
                            val editMessageTextForClient = EditMessageText()
                            editMessageTextForClient.putData(client.chatId.toString(), saveStartMessageId[client.chatId.toString()]!!, "Ваша запись у специалиста ${user.secondName} ${user.firstName} ${user.patronymic} была отменена.")
                            editMessageTextForClient.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                            protectedExecute(editMessageTextForClient)
                            textForMessage = "\uD83D\uDD30  Запись клиента была отменена, сообщение об отмене было отправлено клиенту."
                        } else {
                            textForMessage = "\uD83D\uDD30  Запись клиента была отменена."
                        }

                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("❗  Удалить клиента") -> {
                        val clientId = callBackData.replace("❗  Удалить клиента", "").toLong()
                        clientRepository.deleteById(clientId)
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "Все данные клиента были удалены из базы.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData == "!!" -> {

                    }

                    callBackData == "!!" -> { // TODO "!!"

                    }

                    callBackData == "#reg" -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "Введите вашу фамилию или ФИО полностью и отправьте сообщение.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputSecondName
                    }

                    callBackData.contains(clientData) -> {
                    val dataString: String = callBackData.replace("#cldata", "")
                    val splitDataString = dataString.split("#")
                    val clientId = splitDataString[0].toLong()
                    val date = splitDataString[1]
                    val time = splitDataString[2]

                    val client = clientRepository.findById(clientId).get()
                    client.appointmentDate = date
                    client.appointmentTime = time
                    clientRepository.save(client)

                    val editMessageText = EditMessageText()
                    val textForMessage = "\uD83D\uDD30 " + client.secondName + " " + client.firstName + " " + client.patronymic + " записан на приём " + date.replace("-", ".") + " в $time\n" +
                            "\n\uD83D\uDD39 Следует помнить, что если клиент был записан накануне перед приёмом, клиент получит только сообщение о записи, без просьбы подтвердить её. \nЕсли это необходимо, вы можете " +
                            "отменить запись или перезаписать клиента в любое время, сообщение о новой записи будет автоматически отправлено клиенту \n(для зарегистрированных в Telegram)*"
                    editMessageText.putData(stringChatId, intMessageId, textForMessage)
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                        //tempData[stringChatId] = inputSecondName
                }

                    callBackData.contains(callBackClientId) -> {
                        val clientId = callBackData.replace("#clid", "")

                        val client: ClientData = clientRepository.findById(clientId.toLong()).get()
                        val editMessageText = EditMessageText()
                        val date = LocalDate.now()
                        val numFormat = DateTimeFormatter.ofPattern("MM")
                        val stringFormat = DateTimeFormatter.ofPattern("LLLL")

                        val inlineKeyboardMarkup = InlineKeyboardMarkup()
                        val rowsInline = ArrayList<List<InlineKeyboardButton>>()
                        val firstRowInlineButton = ArrayList<InlineKeyboardButton>()

                        val firstButton = InlineKeyboardButton()
                        firstButton.putData(stringFormat.format(date), "${numFormat.format(date)}@$clientId")
                        firstRowInlineButton.add(firstButton)

                        val secondButton = InlineKeyboardButton()
                        secondButton.putData(stringFormat.format(date.plusMonths(1)), "${numFormat.format(date.plusMonths(1))}@$clientId")
                        firstRowInlineButton.add(secondButton)

                        val thirdButton = InlineKeyboardButton()
                        thirdButton.putData(stringFormat.format(date.plusMonths(2)), "${numFormat.format(date.plusMonths(2))}@$clientId")
                        firstRowInlineButton.add(thirdButton)

                        val secondRowInlineButton = ArrayList<InlineKeyboardButton>()
                        val returnButton = InlineKeyboardButton()
                        returnButton.putData("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                        secondRowInlineButton.add(returnButton)

                        rowsInline.add(firstRowInlineButton)
                        rowsInline.add(secondRowInlineButton)
                        inlineKeyboardMarkup.keyboard = rowsInline

                        editMessageText.replyMarkup = inlineKeyboardMarkup

                        val textForMessage: String = if (client.appointmentDate.length == 10) {
                            "\uD83D\uDD38 Клиент уже записан на ${client.appointmentDate.replace("-", ".")}  в  ${client.appointmentTime}, если процедура записи будет продолжена," +
                             " клиент будет перезаписан на новое время.\n\n\uD83D\uDD30 Выберите месяц для записи клиента/пациента:"
                        } else {
                            "\uD83D\uDD30 Выберите месяц для записи клиента/пациента:"
                        }

                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        protectedExecute(editMessageText)
                        //tempData[stringChatId] = inputSecondName
                    }

                    callBackData.contains(findClientByName) -> {
                    val dataText = callBackData.replace(findClientByName, "")
                    findClient(stringChatId, longChatId, dataText, callBackClientId)
                    tempData[stringChatId] = ""
                    }

                    callBackData.contains(findClientForMenu) -> {
                        val dataText = callBackData.replace(findClientForMenu, "")
                        findClient(stringChatId, longChatId, dataText, "#getmenu") // TODO "#"
                    }

                    callBackData.contains("#getmenu") -> {
                        val clientId = callBackData.replace("#getmenu", "")
                        val client: ClientData = clientRepository.findById(clientId.toLong()).get()

                        val textForMessage: String = if (client.appointmentDate.length == 10) {
                            "\uD83D\uDD30 В этом меню вы можете работать с данными клиента: ${client.secondName} ${client.firstName} ${client.patronymic}\n" +
                                    "\n\uD83D\uDD39 Визит клиента запланирован: ${client.appointmentDate.replace("-", ".")}  в  ${client.appointmentTime}"
                        } else {
                            "\uD83D\uDD30 В этом меню вы можете работать с данными клиента: ${client.secondName} ${client.firstName} ${client.patronymic}"
                        }

                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        val menuList = listOf("Заметки клиента", "Редактировать ФИО", "❗  Выписать клиента", "❗  Удалить клиента", "\uD83D\uDD19  Назад в меню")
                        editMessageText.replyMarkup = botMenuFunction.createDataButtonSet(menuList, clientId)
                        protectedExecute(editMessageText)
                    }


                    callBackData.contains("@") -> {
                        val dataString = callBackData.split("@") // TODO
                        val appointmentMonth = dataString[0].toInt()
                        val clientId = dataString[1]
                        val localDate = LocalDate.now()
                        val editMessageText = EditMessageText()
                        val startDay: Int
                        comeBackInfo[stringChatId] = callBackData

                        val stringBuilder = StringBuilder("ℹ  Записано клиентов по датам:\n")
                        val daysAppointment = HashMap<String, Int>()

                        val clients = clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 }.filter { it.appointmentDate.split("-")[1] == dataString[0] }.sortedBy { it.appointmentDate }
                        clients.forEach { if (daysAppointment[it.appointmentDate] == null) daysAppointment[it.appointmentDate] = 1 else daysAppointment[it.appointmentDate] = daysAppointment[it.appointmentDate]!! + 1 }
                        daysAppointment.toSortedMap().forEach { stringBuilder.append("• ${it.key}   записано:   ${it.value}\n") }

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
                        button.putData("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                        fifthRowInlineButton.add(button)

                        rowsInline.add(firstRowInlineButton)
                        rowsInline.add(secondRowInlineButton)
                        rowsInline.add(thirdRowInlineButton)
                        rowsInline.add(fourthRowInlineButton)
                        rowsInline.add(fifthRowInlineButton)
                        inlineKeyboardMarkup.keyboard = rowsInline
                        editMessageText.replyMarkup = inlineKeyboardMarkup

                        stringBuilder.append("\n\n\uD83D\uDD30 Выберите дату для записи:")

                        editMessageText.putData(stringChatId, intMessageId, stringBuilder.toString())
                        protectedExecute(editMessageText)
                        //tempData[stringChatId] = inputSecondName
                    }

                    callBackData.contains("&") -> {
                        val dataText = callBackData.replace("&", "")
                        val splitData = dataText.split("#")
                        val dayOfMonth = if (splitData[0].length == 1) "0${splitData[0]}" else splitData[0]

                        val stringBuilder = StringBuilder("ℹ  Запись по времени:\n")
                        clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 }.filter { it.appointmentDate.split("-")[2] == dayOfMonth }.sortedBy { it.appointmentTime }.forEach { stringBuilder.append("• ${it.appointmentTime}  -  ${it.secondName} ${it.firstName}\n") }

                        val editMessageText = EditMessageText()

                        val inlineKeyboardMarkup = InlineKeyboardMarkup()
                        val rowsInline = ArrayList<List<InlineKeyboardButton>>()
                        val firstRowInlineButton = ArrayList<InlineKeyboardButton>()
                        val secondRowInlineButton = ArrayList<InlineKeyboardButton>()
                        val thirdRowInlineButton = ArrayList<InlineKeyboardButton>()
                        val fourthRowInlineButton = ArrayList<InlineKeyboardButton>()

                        for (i in 0 .. 24) {
                            when (i) {
                                in 8..15 -> {
                                    val button = InlineKeyboardButton()
                                    button.putData("$i", "#hou$i#$dataText")
                                    firstRowInlineButton.add(button)
                                }
                                in 16..23 -> {
                                    val button = InlineKeyboardButton()
                                    button.putData("$i", "#hou$i#$dataText")
                                    secondRowInlineButton.add(button)
                                }
                                in 0..8 -> {
                                    val button = InlineKeyboardButton()
                                    button.putData("$i", "#hou$i#$dataText")
                                    thirdRowInlineButton.add(button)
                                }
                            }
                        }

                        val menuButton = InlineKeyboardButton()
                        menuButton.putData("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        fourthRowInlineButton.add(menuButton)

                        val backButton = InlineKeyboardButton()
                        backButton.putData("Выбрать другую дату", comeBackInfo[stringChatId]!!)
                        fourthRowInlineButton.add(backButton)

                        rowsInline.add(firstRowInlineButton)
                        rowsInline.add(secondRowInlineButton)
                        rowsInline.add(thirdRowInlineButton)
                        rowsInline.add(fourthRowInlineButton)
                        inlineKeyboardMarkup.keyboard = rowsInline

                        stringBuilder.append( "\n\n\uD83D\uDD30 Выберите час для записи:")
                        editMessageText.putData(stringChatId, intMessageId, stringBuilder.toString())
                        editMessageText.replyMarkup = inlineKeyboardMarkup
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("#hou") -> {
                        val dataText = callBackData.replace("#hou", "")
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

                        for (i in 5 .. 56) {
                            when {
                                i == 5 -> {
                                    val button = InlineKeyboardButton()
                                    button.putData("05", "$clientData$clientId#$date#$hour:05")
                                    firstRowInlineButton.add(button)
                                }

                                i % 5 == 0 && i < 35 -> {
                                    val button = InlineKeyboardButton()
                                    button.putData("$i", "$clientData$clientId#$date#$hour:$i")
                                    firstRowInlineButton.add(button)
                                }

                                i % 5 == 0 -> {
                                    val button = InlineKeyboardButton()
                                    button.putData("$i", "$clientData$clientId#$date#$hour:$i")
                                    secondRowInlineButton.add(button)
                                }

                                i == 56 -> {
                                    val button = InlineKeyboardButton()
                                    button.putData("00", "$clientData$clientId#$date#$hour:00")
                                    secondRowInlineButton.add(button)
                                }
                            }
                        }

                        val menuButton = InlineKeyboardButton()
                        menuButton.putData("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        thirdRowInlineButton.add(menuButton)

                        rowsInline.add(firstRowInlineButton)
                        rowsInline.add(secondRowInlineButton)
                        rowsInline.add(thirdRowInlineButton)
                        inlineKeyboardMarkup.keyboard = rowsInline

                        val textForMessage = "\uD83D\uDD30 Выберите минуты для записи:"
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = inlineKeyboardMarkup
                        protectedExecute(editMessageText)
                    }






                }
            }
        }



    private fun findClient(stringChatId: String, longChatId: Long, secondNameText: String, calBackData: String){ // "#clid"
        tempData[stringChatId] = ""
        val clients = clientRepository.findAll().filter { it.specialistId == longChatId && it.secondName.contains(secondNameText, true) }.filter { it.secondName.first().lowercase() == secondNameText.first().lowercase() }.sortedBy { it.secondName } // TODO повтор кода 191
        val textForMessage: String
        val editMessageText = EditMessageText()

        if(clients.isEmpty()){
            textForMessage = "ℹ  Клиента с таким набором букв в фамилии не найдено"
            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню") // TODO "Назад в меню"
        } else {
            textForMessage = "\uD83D\uDD30 Выберите клиента/пациента из списка:"
            val inlineKeyboardMarkup = InlineKeyboardMarkup()
            val rowsInline = ArrayList<List<InlineKeyboardButton>>()
            for (elem in clients) {
                val rowInlineButton = ArrayList<InlineKeyboardButton>()
                val button = InlineKeyboardButton()
                button.putData(elem.secondName + " " +  elem.firstName, calBackData + elem.clientId)
                rowInlineButton.add(button)
                rowsInline.add(rowInlineButton)
            }

            val rowInlineButton = ArrayList<InlineKeyboardButton>()
            val returnButton = InlineKeyboardButton()
            returnButton.putData("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
            rowInlineButton.add(returnButton)
            rowsInline.add(rowInlineButton)

            inlineKeyboardMarkup.keyboard = rowsInline
            editMessageText.replyMarkup = inlineKeyboardMarkup
        }

        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, textForMessage)
        protectedExecute(editMessageText)
    }


    private fun setFullName(stringChatId: String, longChatId: Long) {
        if (saveChatId[stringChatId] != null && saveChatId[stringChatId]!! > 0) {
            val clientData = clientRepository.findById(saveChatId[stringChatId]!!)
            val client: ClientData

            if (clientData.isPresent && (clientData.get().clientId != clientData.get().chatId)) {
                client = clientData.get()
                client.firstName = firstName[stringChatId]!!
                client.secondName = secondName[stringChatId]!!
                client.patronymic = patronymic[stringChatId]!!
                clientRepository.save(client)
            } else {
                client = ClientData()
                client.chatId = saveChatId[stringChatId]!!
                client.specialistId = longChatId
                client.firstName = firstName[stringChatId]!!
                client.secondName = secondName[stringChatId]!!
                client.patronymic = patronymic[stringChatId]!!
                clientRepository.save(client)

                val specialistInfo: String
                val user: User = userRepository.findById(longChatId).get()
                specialistInfo = "${user.firstName} ${user.patronymic} ${user.secondName}, ${user.profession}"

                val sendMessage = SendMessage(saveChatId[stringChatId]!!.toString(), "✅ Спасибо за ожидание, ваш специалист: $specialistInfo")
                protectedExecute(sendMessage)
            }

            val editMessageText = EditMessageText().putData(stringChatId, saveStartMessageId[stringChatId]!!, "✅ Клиент добавлен в базу данных")
            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
            protectedExecute(editMessageText)
            saveChatId[stringChatId] = 0
            registerPassword[stringChatId] = 0
        } else {
            val editMessageText = EditMessageText()
            editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "Введите вашу специализацию (профессию) и отправьте сообщение")
            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
            protectedExecute(editMessageText)
            tempData[stringChatId] = inputProfession
        }

    }








    }

