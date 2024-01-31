package aldmitry.dev.personalmanager.service

import aldmitry.dev.personalmanager.backup.BackupCreator
import aldmitry.dev.personalmanager.backup.ServerBackup
import aldmitry.dev.personalmanager.extendfunctions.protectedExecute
import aldmitry.dev.personalmanager.extendfunctions.putData
import aldmitry.dev.personalmanager.model.ClientData
import aldmitry.dev.personalmanager.model.ClientDataDao
import aldmitry.dev.personalmanager.model.User
import aldmitry.dev.personalmanager.model.UserDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Component
class InputOutputCommand(@Autowired val clientRepository: ClientDataDao, @Autowired val userRepository: UserDao) :
    TelegramLongPollingBot("5684975537") { //

    init { // Команды меню бота
        val botCommandList: List<BotCommand> = listOf(
            BotCommand("/start", "Запуск программы"),
            BotCommand("/star", "Запуск программы"),
            BotCommand("/help", "Полезная информация"),
            BotCommand("/deletedata", "Удаление всех данных пользователя")
        )
        try {
            this.execute(SetMyCommands(botCommandList, BotCommandScopeDefault(), null))
        } catch (e: TelegramApiException) {

        }
    }


    private val botMenuFunction = BotMenuFunction()
    private var textForStartMessage: String = ""

    private val tempData = HashMap<String, String>()
    private val saveChatId = HashMap<String, Long>()
    private val firstName = HashMap<String, String>()
    private val secondName = HashMap<String, String>()
    private val patronymic = HashMap<String, String>()
    private val saveClientId = HashMap<String, String>()
    private val comeBackInfo = HashMap<String, String>()
    private val registerPassword = HashMap<String, Int>()
    private val saveStartMessageId = HashMap<String, Int>()


    private final val findClient = "FIND_CLIENT"
    private final val inputRemark = "INPUT_REMARK"
    private final val inputPassword = "INPUT_PASSWORD"
    private final val inputFirstName = "INPUT_FIRST_NAME"
    private final val inputProfession = "INPUT_PROFESSION"
    private final val inputPatronymic = "INPUT_PATRONYMIC"
    private final val inputSecondName = "INPUT_SECOND_NAME"
    private final val inputChangeUser = "INPUT_CHANGE_USER"
    private final val inputOldPassword = "INPUT_OLD_PASSWORD"
    private final val inputMessageForAll = "INPUT_MESSAGE_FOR_ALL"
    private final val inputRepairPassword = "INPUT_REPAIR_PASSWORD"
    private final val inputSupportMessage = "INPUT_SUPPORT_MESSAGE"
    private final val inputMessageForUser = "INPUT_MESSAGE_FOR_USER"
    private final val inputLoadUserBackup = "INPUT_LOAD_USER_BACKUP"
    private final val inputSaveUserBackup = "INPUT_SAVE_USER_BACKUP"
    private final val inputSaveClientBackup = "INPUT_SAVE_CLIENT_BACKUP"
    private final val inputLoadClientBackup = "INPUT_LOAD_CLIENT_BACKUP"
    private final val inputTextForStartMessage = "INPUT_FOR_START_MESSAGE"


    private final val qSym = " ？"

    final val callData_clientData = "#cldata"
    final val callData_callBackClientId = "#clid"
    final val callData_findClientByName = "#findcli"
    final val callData_findClientForMenu = "#finmenu"
    final val callData_generationCode = "Генерировать код для клиента" // "Генерировать код и добавить клиента"
    final val callData_addCommonClient = "Добавить клиента без кода" // Добавить клиента без Telegram
    final val callData_addNewClient =  "Добавить нового клиента/пациента"


    override fun getBotUsername(): String {
        return "Test"
    }

    // Бот получил команду (сообщение от пользователя)
    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val longChatId: Long = update.message.chatId
            val intMessageId: Int = update.message.messageId
            val updateMessageText: String = update.message.text
            val stringChatId: String = longChatId.toString()


            when (tempData[stringChatId]) { // если в Map добавляется строка-константа, Update-сообщение (updateMessageText) запускает одну из функций в блоке

                inputLoadUserBackup -> {
                    val backupDirectory = updateMessageText + "user_backup.xml"
                    val serverBackup = ServerBackup()
                    serverBackup.startBackup(backupDirectory)
                    val userList = serverBackup.receiveUsersBackup(mutableListOf())
                    userList.filter { it.chatId != longChatId }.forEach { userRepository.save(it) }

                    val textForMessage = "\uD83D\uDD30  Бэкап сервера User получен из директории:\n$backupDirectory"
                    val editMessageText = EditMessageText()
                    editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, textForMessage)
                    editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
                    protectedExecute(editMessageText)
                }

                inputLoadClientBackup -> {
                    val backupDirectory = updateMessageText + "client_backup.xml"
                    val serverBackup = ServerBackup()
                    serverBackup.startBackup(backupDirectory)
                    val clientsDataList = serverBackup.receiveClientsBackup(mutableListOf())
                    clientsDataList.forEach { clientRepository.save(it) }

                    val textForMessage = "\uD83D\uDD30  Бэкап сервера ClientData получен из директории:\n$backupDirectory"
                    val editMessageText = EditMessageText()
                    editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, textForMessage)
                    editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
                    protectedExecute(editMessageText)
                }

                inputSaveUserBackup -> {
                    val backupDirectory = updateMessageText + "user_backup.xml"
                    val userList = mutableListOf<String>()
                    userRepository.findAll().forEach { userList.add(it.toString()) }
                    val backupCreator = BackupCreator()
                    val backupFile = backupCreator.receiveBackupFile("users", userList)
                    backupCreator.createBackupXml(backupFile, backupDirectory)
                    val textForMessage = "\uD83D\uDD30  Бэкап-файл данных User создан в директории:\n$backupDirectory"

                    val editMessageText = EditMessageText().putData(stringChatId, saveStartMessageId[stringChatId]!!, textForMessage)
                    editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
                    protectedExecute(editMessageText)
                }

                inputSaveClientBackup -> {
                    val backupDirectory = updateMessageText + "client_backup.xml"
                    val clientList = mutableListOf<String>()
                    clientRepository.findAll().forEach { clientList.add(it.toString()) }
                    val backupCreator = BackupCreator()
                    val backupFile = backupCreator.receiveBackupFile("clients", clientList)
                    backupCreator.createBackupXml(backupFile, backupDirectory)
                    val textForMessage = "\uD83D\uDD30  Бэкап-файл данных ClientData создан в директории:\n$backupDirectory"

                    val editMessageText = EditMessageText().putData(stringChatId, saveStartMessageId[stringChatId]!!, textForMessage)
                    editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
                    protectedExecute(editMessageText)

                }

                inputMessageForAll -> {
                    if (updateMessageText.length > 3) {
                        for (user in userRepository.findAll()){
                            val sendMessage = SendMessage(user.chatId.toString(), "ℹ  Сообщение от администратора:\n$updateMessageText")
                            sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", "#delmes")
                            protectedExecute(sendMessage)
                        }
                    } else {
                        val sendMessage = SendMessage(stringChatId, "ℹ  Сообщение от администратора не было отправлено.")
                        sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", "#delmes")
                        protectedExecute(sendMessage)
                    }
                }

                inputChangeUser -> {
                    val changeData = updateMessageText.split("#")
                    val editMessageText = EditMessageText()

                    if (changeData.size !in 10..11) {
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Ошибка ввода данных")
                    } else {
                        val newUser = userRepository.findById(changeData[4].toLong()).get()
                        newUser.secondName = changeData[0]
                        newUser.firstName = changeData[1]
                        newUser.patronymic = changeData[2]
                        newUser.profession = changeData[3]
                        newUser.password = changeData[5]
                        newUser.sendTime = changeData[6].toInt()
                        newUser.timeZone = changeData[7].toLong()
                        newUser.sendBeforeDays = changeData[8].toLong()
                        newUser.paymentDate = changeData[9]
                        newUser.chatId = if (changeData.size == 11) changeData[10].toLong() else changeData[4].toLong()
                        userRepository.save(newUser)

                        val user = userRepository.findById(changeData[4].toLong()).get()
                        val textForMessage = "\uD83D\uDD30 Новые данные пользователя: " +
                                "\nФамилия: ${user.secondName}\nИмя: ${user.firstName}\nОтчество: ${user.patronymic}\nПрофессия: ${user.profession}" +
                                "\nChat id: ${user.chatId}\nПароль: ${user.password}\nВремя отправки сообщений клиенту: ${user.sendTime}\nВременная зона: ${user.timeZone}" +
                                "\nОтправка сообщений за дней до приема: ${user.sendBeforeDays}\nДата абонентского платежа: ${user.paymentDate}"

                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                    }

                    protectedExecute(editMessageText)
                    tempData[stringChatId] = inputChangeUser
                }

                inputTextForStartMessage -> {
                    textForStartMessage = if (updateMessageText.length > 3) "$updateMessageText\n\n" else ""
                    val editMessageText = EditMessageText().putData(stringChatId, saveStartMessageId[stringChatId]!!,  "\uD83D\uDD30  Сообщение для пользователей было отправлено.")
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                }

                inputMessageForUser -> {
                    val splitMessageText = updateMessageText.split("#")
                    val sendMessage = SendMessage(splitMessageText[0], splitMessageText[1])
                    sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", "#delmes")
                    protectedExecute(sendMessage)

                    val editMessageText = EditMessageText().putData(stringChatId, saveStartMessageId[stringChatId]!!,  "\uD83D\uDD30  Сообщение было отправлено пользователю.")
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                }

                inputSupportMessage -> {
                    val adminId = userRepository.findAll().first { it.profession == "admin" }
                    val user = userRepository.findById(longChatId).get()
                    protectedExecute(SendMessage(adminId!!.chatId.toString(), "< Обращение в техническую поддержку >\nОт пользователя: $user\nСообщение: $updateMessageText"))

                    val editMessageText = EditMessageText().putData(stringChatId, saveStartMessageId[stringChatId]!!,  "Сообщение было отправлено в чат поддержки.")
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                }

                inputRepairPassword -> {
                    val oldUser: User
                    val user = userRepository.findById(longChatId).get()
                    val users = userRepository.findAll().filter { it.chatId != user.chatId && it.password == updateMessageText && it.secondName == user.secondName && it.firstName == user.firstName && it.patronymic == user.patronymic }
                    val editMessageText = EditMessageText()

                    if (users.isNotEmpty() && users.size == 1) {
                        oldUser = users[0]
                        clientRepository.findAll().filter { it.specialistId == oldUser.chatId }.forEach { it.specialistId = longChatId; clientRepository.save(it) }
                        user.password = oldUser.password
                        userRepository.save(user)
                        userRepository.delete(oldUser)

                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!,  "✅  Аккаунт восстановлен")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    } else {
                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!,  "❌  Ошибка")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    }
                    protectedExecute(editMessageText)
                }

                inputPassword -> {
                    val editMessageText = EditMessageText()
                    if(updateMessageText.length in 5..15){
                        val user = userRepository.findById(longChatId).get()
                        user.password = updateMessageText
                        userRepository.save(user)
                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!,  "✅️Пароль $updateMessageText был установлен.")
                    } else {
                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!,  "❌  Недопустимая длинна пароля $updateMessageText\nПароль не был установлен.")
                    }
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                }

                inputOldPassword -> {
                    val user = userRepository.findById(longChatId).get()
                    val editMessageText = EditMessageText()

                    if (updateMessageText == user.password){
                        tempData[stringChatId] = inputPassword
                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!,  "✅  Пароль подтверждён, введите новый пароль и отправьте сообщение в чат. Помните о том, что длинна пароля должна быть не менее 5 и не более 15 символов.")
                    } else {
                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!,  "❌  Ошибка")
                    }
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                }

                inputRemark -> {
                    val client = clientRepository.findById(saveChatId[stringChatId]!!).get()
                    val editMessageText = EditMessageText()

                    if (client.remark.length < 3000){
                        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

                        val remarkText = if (client.remark.isNotEmpty()) {
                            "${client.remark}\n• ${formatter.format(LocalDate.now())}:  $updateMessageText"
                        } else {
                            "• ${formatter.format(LocalDate.now())}:  $updateMessageText"
                        }

                        client.remark = remarkText
                        clientRepository.save(client)

                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!,  "\uD83D\uDCDD Заметки:\n${client.remark}\n\n\uD83D\uDD30  Заметка была добавлена.")
                    } else {
                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!,  "\uD83D\uDCDD Заметки:\n${client.remark}\n\nℹ  Достигнут максимальный размер записи, заметка не была добавлена.")
                    }


                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                }

                findClient -> {
                    findClient(stringChatId, longChatId, updateMessageText, callData_callBackClientId)
                }

                inputSecondName -> {
                    val editMessageText = EditMessageText()

                    if (updateMessageText.split(" ").size == 3) {
                        val splitUpdateMessage = updateMessageText.split(" ")
                        val secondNameText = splitUpdateMessage[0].replace(".", "").replace("Ё", "Е").trim()
                        val firstNameText = splitUpdateMessage[1].replace(".", "").trim()
                        val patronymicText = splitUpdateMessage[2].replace(".", "").trim()

                        if (secondNameText.length > 14 || firstNameText.length > 14 || patronymicText.length > 14){
                            registerPassword[stringChatId] = 0
                            editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "ℹ  Превышена максимальная длинна фамилии, имени, или отчества. Для корректной работы приложения необходимо сократить количество символов в ФИО.")
                            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                            protectedExecute(editMessageText)
                        } else {
                            val client = clientRepository.findAll().filter { it.specialistId == longChatId && it.secondName == secondNameText && it.firstName == firstNameText && it.patronymic == patronymicText }
                            secondName[stringChatId] = secondNameText
                            firstName[stringChatId] = firstNameText
                            patronymic[stringChatId] = patronymicText

                            if (client.isEmpty()){
                                setFullName(stringChatId, longChatId)
                            } else {
                                editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "ℹ  У вас уже добавлен клиент с ФИО $secondNameText $firstNameText $patronymicText. Продолжить регистрацию нового клиента с совпадающими ФИО?")
                                editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню", "Зарегистрировать", "#acceptregister")
                                protectedExecute(editMessageText)
                            }
                        }
                    } else {
                        val secondNameText = updateMessageText.replace("Ё", "Е").replace("ё", "Е").trim()

                        if (secondNameText.length > 14){
                            registerPassword[stringChatId] = 0
                            editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "ℹ  Превышена максимальная длинна фамилии. Для корректной работы приложения необходимо сократить количество символов в ФИО.")
                            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                            protectedExecute(editMessageText)
                        } else {
                            secondName[stringChatId] = secondNameText
                            editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "\uD83D\uDD30  Введите имя и отправьте сообщение в чат.")
                            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                            protectedExecute(editMessageText)
                            tempData[stringChatId] = inputFirstName
                        }
                    }
                }

                inputFirstName -> {
                    val firstNameText = updateMessageText.replace(".", "").trim()
                    val editMessageText = EditMessageText()

                    if (firstNameText.length > 14){
                        registerPassword[stringChatId] = 0
                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "ℹ  Превышена максимальная длинна имени. Для корректной работы приложения необходимо сократить количество символов в ФИО.")
                    } else {
                        firstName[stringChatId] = firstNameText
                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "\uD83D\uDD30  Введите отчество и отправьте сообщение в чат.")
                        tempData[stringChatId] = inputPatronymic
                    }
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                    protectedExecute(editMessageText)
                }

                inputPatronymic -> {
                    val patronymicText = updateMessageText.replace(".", "").trim()

                    if (patronymicText.length > 14){
                        registerPassword[stringChatId] = 0
                        val editMessageText = EditMessageText().putData(stringChatId, saveStartMessageId[stringChatId]!!, "ℹ  Превышена максимальная длинна отчества. Для корректной работы приложения необходимо сократить количество символов в ФИО.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    } else {
                        patronymic[stringChatId] = patronymicText
                        setFullName(stringChatId, longChatId)
                    }

                }

                inputProfession -> {
                    val admin: User? = userRepository.findAll().find { it.profession == "admin" }
                    val user: User = userRepository.findById(longChatId).get()
                    val editMessageText = EditMessageText()

                    if (updateMessageText == "admin" && admin != null || updateMessageText.length > 20){
                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "❌ Пожалуйста, выберите другое название специальности.")
                    } else {
                        val date = LocalDate.now()
                        val nextPaymentDate = date.plusMonths(3)

                        user.firstName = firstName[stringChatId]!!
                        user.secondName = secondName[stringChatId]!!
                        user.patronymic = patronymic[stringChatId]!!
                        user.profession = updateMessageText.lowercase()
                        user.paymentDate = nextPaymentDate.toString()
                        userRepository.save(user)

                        editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "✅ Спасибо за регистрацию!")
                    }
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                }
            }


            if (registerPassword.isNotEmpty()){
                for ((key, value) in registerPassword){
                    if(value.toString() == updateMessageText) {
                        protectedExecute(DeleteMessage().putData(stringChatId, intMessageId))
                        val sendMessage = SendMessage(stringChatId,  "✅ Код принят, спасибо!")
                        sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", "#delmes")
                        protectedExecute(sendMessage)

                        val editMessageText = EditMessageText()
                        val textForMessage: String

                        if (saveClientId[key].isNullOrEmpty()) {
                            textForMessage = "✅ Код был подтверждён, осталось заполнить ФИО и регистрация будет завершена. Введите, пожалуйста, фамилию клиента или ФИО полностью (например: Иванов Иван Иванович) и отправьте сообщение в чат."
                            editMessageText.putData(key, saveStartMessageId[key]!!, textForMessage)
                            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                            tempData[key] = inputSecondName
                            saveChatId[key] = longChatId
                        } else {
                            val clientId = saveClientId[key]!!.toLong()
                            val client = clientRepository.findById(clientId).get()
                            client.chatId = longChatId
                            clientRepository.save(client)

                            textForMessage = "✅ Код был подтверждён, теперь перед предстоящим приёмом клиенту буду приходить сообщения с просьбой подтвердить посещение."
                            editMessageText.putData(key, saveStartMessageId[key]!!, textForMessage)
                            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                            saveClientId[key] = ""
                        }
                        protectedExecute(editMessageText)

                        if (registerPassword.size == 1) registerPassword.clear() else registerPassword[key] = 0
                        return
                    }
                }
            }


            if (!updateMessageText.contains("/") && tempData[stringChatId].isNullOrEmpty() && userRepository.findById(longChatId).get().firstName.isNotEmpty()) {
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
                else -> "NULL"
            }

            when  {
                updateMessageText.split(" ").size >= 4 && month != "NULL" -> {
                    val dataText: String = updateMessageText.replace(" :", "").replace(":", " ").replace(", ", " ").replace(",", " ").replace(".", "").replace(" в", "").replace(" часов", "").replace(" на", "").replace("- ", "").replace("-", " ").trim()
                    val splitText: List<String> = dataText.split(" ")
                    val clientSecondName: String = splitText[0]
                    val localDate = LocalDate.now()
                    val formatter = DateTimeFormatter.ofPattern("yyyy")

                    val clients = clientRepository.findAll().filter { it.specialistId == longChatId && it.secondName.contains(clientSecondName, true) }

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
                            button.putData("${elem.secondName} ${elem.firstName} ${elem.patronymic}", "$callData_clientData${elem.clientId}#$date#$stringHour:$stringMinute")
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
                    val secondNameText = splitUpdateMessage[0].replace(".", "").replace("Ё", "Е").trim()
                    val firstNameText = splitUpdateMessage[1].replace(".", "").trim()
                    val patronymicText = splitUpdateMessage[2].replace(".", "").trim()

                    if (secondNameText.length > 14 || firstNameText.length > 14 || patronymicText.length > 14){
                        val editMessageText = EditMessageText().putData(stringChatId, saveStartMessageId[stringChatId]!!, "ℹ  Превышена максимальная длинна фамилии, имени, или отчества. Для корректной работы приложения необходимо сократить количество символов в ФИО.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    } else {
                        val client = clientRepository.findAll().filter { it.specialistId == longChatId && it.secondName == secondNameText && it.firstName == firstNameText && it.patronymic == patronymicText }
                        secondName[stringChatId] = secondNameText
                        firstName[stringChatId] = firstNameText
                        patronymic[stringChatId] = patronymicText
                        saveChatId[stringChatId] = 1

                        if (client.isEmpty()){
                            setFullName(stringChatId, longChatId)
                        } else {
                            val editMessageText = EditMessageText().putData(stringChatId, saveStartMessageId[stringChatId]!!, "ℹ  У вас уже добавлен клиент с ФИО $secondNameText $firstNameText $patronymicText. Продолжить регистрацию нового клиента с совпадающими ФИО?")
                            editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню", "Зарегистрировать", "#acceptregister")
                            protectedExecute(editMessageText)
                        }
                    }
                }

                !updateMessageText.contains(" ") && updateMessageText.length in 3..14 -> {
                   findClient(stringChatId, longChatId, updateMessageText, "#getmenu") // TODO "#getmenu"
                }

                updateMessageText.length > 2 && tempData[stringChatId].isNullOrEmpty() -> { // TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    val clients = clientRepository.findAll().filter { cli -> cli.specialistId == longChatId && cli.secondName.contains(updateMessageText, true) }
                    val textForMessage: String
                    val editMessageText = EditMessageText()

                    if(clients.isEmpty()){
                        textForMessage = "ℹ  Клиента с таким набором символов в фамилии не найдено."
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    } else {
                        textForMessage = "\uD83D\uDD30 Выберите клиента/пациента из списка ниже:"
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
                        val sendMessage = SendMessage(stringChatId, "Здравствуйте, уважаемый пользователь! Здесь вы можете получать информацию о предстоящем визите к своему специалисту. Для этого, если вам был сообщён короткий код, введите его и отправьте сообщение в чат.")
                        val settingList = listOf("\uD83D\uDCC5  Посмотреть мою запись", "Зарегистрироваться как специалист")
                        sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                        saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                    } else {
                        if (user.profession == "admin") {
                            val users = userRepository.findAll()
                            val specialists = users.filter { it.profession.isNotEmpty() }
                            val clients = clientRepository.findAll()

                            val textForMessage = "$textForStartMessage\uD83D\uDD30  Меню администратора.\nВсего пользователей: ${users.count()}" +
                                    "\nВсего специалистов: ${specialists.count() - 1}\nДобавлено клиентов: ${clients.count()}\nВсего стартовых сообщений: ${saveStartMessageId.size}"
                            val sendMessage = SendMessage(stringChatId, textForMessage)
                            val settingList = listOf("Редактировать пользователя", "Сообщение всем пользователям", "Сообщение пользователю", "StartMessage текст", "Меню специалиста", "Очистить чат", "Backup меню")
                            sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                            saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                        } else {
                            var textForMessageLength = 0
                            val localDate = LocalDate.now()
                            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                            val textForMessage = StringBuilder()
                            val nextDate: String? = clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 && LocalDate.parse(it.appointmentDate).isAfter(localDate) }.minByOrNull { it.appointmentDate }?.appointmentDate

                            if (nextDate.isNullOrEmpty()) {
                                textForMessage.append("✷ Пожалуйста, ознакомьтесь с информацией в меню /help\n\n$textForStartMessage\uD83D\uDD30  Следующая запись: нет.")
                            } else {
                                textForMessage.append("✷ Пожалуйста, ознакомьтесь с информацией в меню /help\n\n$textForStartMessage\uD83D\uDD30  Следующая запись - ${formatter.format(LocalDate.parse(nextDate))}:")
                            }

                            clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate == nextDate }.sortedBy { it.appointmentTime}.forEach { textForMessage.append("\n${it.visitAgreement} ${it.appointmentTime} - ${it.secondName} ${it.firstName.first()}. ${it.patronymic.first()}.") }

                            textForMessage.append("\n\n\uD83D\uDD30  Запись на сегодня:")
                            textForMessageLength = textForMessage.length
                            clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 && it.appointmentDate == localDate.toString() && it.visitAgreement != "✖" }.sortedBy { it.appointmentTime}.forEach { textForMessage.append("\n• ${it.appointmentTime} - ${it.secondName} ${it.firstName.first()}. ${it.patronymic.first()}.") }
                            if (textForMessageLength == textForMessage.length) textForMessage.append(" нет.")

                            val sendMessage = SendMessage(stringChatId, textForMessage.toString())
                            val settingList = listOf("Записать на приём", "Посмотреть запись ко мне", callData_addNewClient, "Работа с базой клиентов/пациентов", "⚙  Моя учетная запись")
                            sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                            saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                        }
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
                        val sendMessage = SendMessage(stringChatId, "Здравствуйте, уважаемый пользователь! Здесь вы можете получать информацию о предстоящем визите к своему специалисту. Для этого, если вам был сообщён короткий код, введите его и отправьте сообщение в чат.")
                        val settingList = listOf("\uD83D\uDCC5  Посмотреть мою запись", "Зарегистрироваться как специалист")
                        sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                        saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                    } else {
                        if (user.profession == "admin") {
                            val users = userRepository.findAll()
                            val specialists = users.filter { it.profession.isNotEmpty() }
                            val clients = clientRepository.findAll()

                            val textForMessage = "$textForStartMessage\uD83D\uDD30  Меню администратора.\nВсего пользователей: ${users.count()}" +
                                    "\nВсего специалистов: ${specialists.count() - 1}\nДобавлено клиентов: ${clients.count()}\nВсего стартовых сообщений: ${saveStartMessageId.size}"
                            val sendMessage = SendMessage(stringChatId, textForMessage)
                            val settingList = listOf("Редактировать пользователя", "Сообщение всем пользователям", "Сообщение пользователю", "StartMessage текст", "Меню специалиста", "Очистить чат", "Backup меню")
                            sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                            saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                        } else {
                            var textForMessageLength = 0
                            val localDate = LocalDate.now()
                            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                            val textForMessage = StringBuilder()
                            val nextDate: String? = clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 && LocalDate.parse(it.appointmentDate).isAfter(localDate) }.minByOrNull { it.appointmentDate }?.appointmentDate

                            if (nextDate.isNullOrEmpty()) {
                                textForMessage.append("✷ Пожалуйста, ознакомьтесь с информацией в меню /help\n\n$textForStartMessage\uD83D\uDD30  Следующая запись: нет.")
                            } else {
                                textForMessage.append("✷ Пожалуйста, ознакомьтесь с информацией в меню /help\n\n$textForStartMessage\uD83D\uDD30  Следующая запись - ${formatter.format(LocalDate.parse(nextDate))}:")
                            }

                            clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate == nextDate }.sortedBy { it.appointmentTime}.forEach { textForMessage.append("\n${it.visitAgreement} ${it.appointmentTime} - ${it.secondName} ${it.firstName.first()}. ${it.patronymic.first()}.") }

                            textForMessage.append("\n\n\uD83D\uDD30  Запись на сегодня:")
                            textForMessageLength = textForMessage.length
                            clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 && it.appointmentDate == localDate.toString() && it.visitAgreement != "✖" }.sortedBy { it.appointmentTime}.forEach { textForMessage.append("\n• ${it.appointmentTime} - ${it.secondName} ${it.firstName.first()}. ${it.patronymic.first()}.") }
                            if (textForMessageLength == textForMessage.length) textForMessage.append(" нет.")

                            val sendMessage = SendMessage(stringChatId, textForMessage.toString())
                            val settingList = listOf("Записать на приём", "Посмотреть запись ко мне", callData_addNewClient, "Работа с базой клиентов/пациентов", "⚙  Моя учетная запись")
                            sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                            saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                        }
                    }
                }

                "/help" -> {
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

                    val helpText = "\uD83D\uDD30  Программа предназначена для ведения клиентской/пациентской записи. Особенностью приложения является то, что не происходит прямого взаимодействия между пользователями, " +
                            "от клиента не требуется ввода своих персональных данных, карточку клиента создаёт специалист и доступна она только ему самому. Приложение не предоставляет возможности для поиска " +
                            "одним пользователям других пользователей, тем самым обеспечивая конфиденциальность."

                    if (user.firstName.isEmpty()){
                        val sendMessage = SendMessage(stringChatId,  helpText)
                        val settingList = listOf("\uD83D\uDCC5  Посмотреть мою запись", "Зарегистрироваться как специалист")
                        sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                        saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                    } else {
                        val textForMessage = "$helpText\nКак добавить клиента:\n1. Клавиша меню <Добавить нового клиента/пациента>, затем " +
                                "<Генерировать код для клиента>, сообщить сгенерированный код клиенту и далее следовать сообщениям-подсказкам. Если клиент зарегистрирован с помощью кода через Telegram, он будет получать оповещение накануне визита к вам" +
                                " и должен будет подтвердить или отменить визит, при этом вы получите оповещение с результатами рассылки. \n2. Клавиша меню <Добавить нового клиента/пациента>, затем <Добавить клиента без кода> и следовать " +
                                "подсказкам программы. Добавленному таким образом клиенту код регистрации можно отправить позднее.\n3. С помощью быстрой команды в чат - примеры команд описаны ниже.\nДалее можно запланировать визит клиента к вам, " +
                                "сделать это можно с помощью клавиши меню <Записать на приём> или " +
                                "отправить в чат быструю команду, в последнем случае можно использовать голосовой ввод - таким образом можно записать клиента на приём максимально быстро. Раздел меню <Работа с базой клиентов/пациентов>" +
                                " содержит подразделы, позволяющие делать заметки, выписать клиента, удалить, отправить код уже добавленному в базу клиенту для регистрации через Telegram.\n\n\uD83D\uDD30  Индикация клиентов на экране главного меню нужна для того, " +
                                "чтобы специалист видел, кто из клиентов подтвердил свой предстоящий визит. Тем клиентам, которые зарегистрированы в приложении через Telegram, " +
                                "накануне приёма приходит сообщение для подтверждения или отмены визита.\n\nСимволы индикации клиентов:\n❔ - клиент должен подтвердить визит\n✔ - клиент подтвердил визит\n✖ - клиент отменил визит\n" +
                                "？- клиент не зарегистрирован через Telegram и не получает сообщений.\n\n\uD83D\uDD30  Быстрые команды служат для того, чтобы добавить в базу клиента, работать с его данными, записать его на приём. " +
                                "Для использования таких команд, надо с клавиатуры или с помощью голосового ввода (самый быстрый способ) написать сообщение и отправить его в чат.\n\nСообщения-быстрые команды, которые можно " +
                                "отправить в чат:\n\uD83D\uDD39 Фамилия или первые три буквы фамилии (например: Иванов) - найти клиента для действий с его данными." +
                                "\n\uD83D\uDD39 ФИО клиента (например: Иванов Иван Иванович) - добавить нового пациента в базу данных.\n\uD83D\uDD39 Фамилию дату месяц час минуты (например: Иванов 10 мая 15 30) - " +
                                "записать клиента на указанные дату и время."
                        val sendMessage = SendMessage(stringChatId, textForMessage)
                        sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                    }
                }

                "/deletedata" -> { // начало работы бота
                    tempData[stringChatId] = ""
                    comeBackInfo[stringChatId] = ""
                    saveChatId[stringChatId] = 0

                    if (userRepository.findById(longChatId).isEmpty) {
                        val user = User()
                        user.chatId = longChatId
                        userRepository.save(user)
                    }

                    if (saveStartMessageId[stringChatId] != null) protectedExecute(DeleteMessage().putData(stringChatId, saveStartMessageId[stringChatId]!!))

                        val sendMessage = SendMessage(stringChatId, "❗ Пожалуйста, подтвердите удаление вашей учётной записи. После подтверждения все ваши данные будут удалены.")
                        sendMessage.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню", "Удалить данные", "#delmydata")
                        saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                    }

                else -> {
                    if (tempData[stringChatId].isNullOrEmpty()) protectedExecute(DeleteMessage().putData(stringChatId, intMessageId)) // TODO удалить if (tempData[stringChatId].isNullOrEmpty())?

                }
            }

            } else if (update.hasCallbackQuery()) {
                val intMessageId: Int = update.callbackQuery.message.messageId
                val stringChatId: String = update.callbackQuery.message.chatId.toString()
                val longChatId: Long = update.callbackQuery.message.chatId
                val callBackData: String = update.callbackQuery.data

                when {
                    callBackData.contains("\uD83D\uDD19  Назад в меню") -> {
                        tempData[stringChatId] = ""
                        comeBackInfo[stringChatId] = ""
                        saveChatId[stringChatId] = 0

                        if (userRepository.findById(longChatId).isEmpty) {
                            val user = User()
                            user.chatId = longChatId
                            userRepository.save(user)
                        }

                        val user: User = userRepository.findById(longChatId).get()

                        if (user.firstName.isEmpty()){
                            val editMessageText = EditMessageText().putData(stringChatId, intMessageId, "Здравствуйте, уважаемый пользователь! Здесь вы можете получать информацию о предстоящем визите к своему специалисту. Для этого, если вам был сообщён короткий код, введите его и отправьте сообщение в чат.")
                            val settingList = listOf("\uD83D\uDCC5  Посмотреть мою запись", "Зарегистрироваться как специалист")
                            editMessageText.replyMarkup = botMenuFunction.createButtonSet(settingList)
                            protectedExecute(editMessageText)
                        } else {
                            if (user.profession == "admin") {
                                val users = userRepository.findAll()
                                val specialists = users.filter { it.profession.isNotEmpty() }
                                val clients = clientRepository.findAll()

                                val textForMessage = "$textForStartMessage\uD83D\uDD30  Меню администратора.\nВсего пользователей: ${users.count()}" +
                                        "\nВсего специалистов: ${specialists.count() - 1}\nДобавлено клиентов: ${clients.count()}\nВсего стартовых сообщений: ${saveStartMessageId.size}"
                                val sendMessage = SendMessage(stringChatId, textForMessage)
                                val settingList = listOf("Редактировать пользователя", "Сообщение всем пользователям", "Сообщение пользователю", "StartMessage текст", "Меню специалиста", "Очистить чат", "Backup меню")
                                sendMessage.replyMarkup = botMenuFunction.createButtonSet(settingList)
                                saveStartMessageId[stringChatId] = protectedExecute(sendMessage)
                            } else {
                                var textForMessageLength = 0
                                val localDate = LocalDate.now()
                                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                val textForMessage = StringBuilder()
                                val nextDate: String? = clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 && LocalDate.parse(it.appointmentDate).isAfter(localDate) }.minByOrNull { it.appointmentDate }?.appointmentDate

                                if (nextDate.isNullOrEmpty()) {//<i>you</i>
                                    textForMessage.append("✷ Пожалуйста, ознакомьтесь с информацией в меню /help\n\n$textForStartMessage\uD83D\uDD30  Следующая запись: нет.")
                                } else {
                                    textForMessage.append("✷ Пожалуйста, ознакомьтесь с информацией в меню /help\n\n$textForStartMessage\uD83D\uDD30  Следующая запись - ${formatter.format(LocalDate.parse(nextDate))}:")//" ？"
                                }

                                clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate == nextDate }.sortedBy { it.appointmentTime}.forEach { textForMessage.append("\n${it.visitAgreement} ${it.appointmentTime} - ${it.secondName} ${it.firstName.first()}. ${it.patronymic.first()}.") }

                                textForMessage.append("\n\n\uD83D\uDD30  Запись на сегодня:")
                                textForMessageLength = textForMessage.length
                                clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 && it.appointmentDate == localDate.toString() && it.visitAgreement != "✖" }.sortedBy { it.appointmentTime}.forEach { textForMessage.append("\n• ${it.appointmentTime} - ${it.secondName} ${it.firstName.first()}. ${it.patronymic.first()}.") }
                                if (textForMessageLength == textForMessage.length) textForMessage.append(" нет.")

                                val editMessageText = EditMessageText().putData(stringChatId, intMessageId, textForMessage.toString())
                                val settingList = listOf("Записать на приём", "Посмотреть запись ко мне", callData_addNewClient, "Работа с базой клиентов/пациентов", "⚙  Моя учетная запись")
                                editMessageText.replyMarkup = botMenuFunction.createButtonSet(settingList)
                                protectedExecute(editMessageText)
                            }
                        }
                    }

                    callBackData == "Очистить чат" -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "❗  Данное действие необходимо подтвердить.")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню", "Очистить чат", "#cleanchat")
                        protectedExecute(editMessageText)
                    }

                    callBackData == "#cleanchat" -> {
                        for ((chatId, messageId) in saveStartMessageId){
                            protectedExecute(DeleteMessage().putData(chatId, messageId))
                        }
                    }

                    callBackData == "#acceptregister" -> {
                        setFullName(stringChatId, longChatId)
                    }

                    callBackData == "Редактировать пользователя" -> {
                        val usersList = mutableListOf<String>()
                        userRepository.findAll().filter { it.secondName.length > 1 }.forEach { usersList.add("${it.chatId} ${it.secondName}") }
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, "\uD83D\uDD30  Выберите пользователя из списка:")
                        usersList.add("\uD83D\uDD19  Назад в меню")
                        editMessageText.replyMarkup = botMenuFunction.createDataButtonSet(usersList, "#usr")
                        protectedExecute(editMessageText)
                    }

                    callBackData == "Сообщение пользователю" -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Введите id пользователя и # (пример: 123456789#), затем текст сообщения для пользователя.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputMessageForUser
                    }

                    callBackData == "Сообщение всем пользователям" -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Введите текст сообщения и отправьте его в чат. Текст длинной менее 3х символов не будет отправлен.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputMessageForAll
                    }

                    callBackData == "StartMessage текст" -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Введите текст сообщения для всех пользователей. Текст будет отображаться в главном (стартовом) сообщении. " +
                                "Текст длинной менее 3 символов удаляет уже отображающийся текст не устанавливая новый.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputTextForStartMessage
                    }

                    callBackData == "Backup меню" -> {
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, "Здравствуйте, уважаемый пользователь! Здесь вы можете получать информацию о предстоящем визите к своему специалисту. Для этого, если вам был сообщён короткий код, введите его и отправьте сообщение в чат.")
                        val menuList = listOf("Получить backup-файлы", "Сохранить user_backup", "Сохранить client_backup", "Произвести бэкап user", "Произвести бэкап client", "❗ Удалить сервер", "\uD83D\uDD19  Назад в меню")
                        editMessageText.replyMarkup = botMenuFunction.createButtonSet(menuList)
                        protectedExecute(editMessageText)
                }

                    callBackData == "Сохранить user_backup" -> {
                        val backupDirectory = "C:\\Users\\admit\\OneDrive\\Рабочий стол\\users_backup.xml"
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Бэкап-файл данных User с расширением .xml будет создан в директории по умолчанию: $backupDirectory после нажатия клавиши <Создать>. Если необходимо создать файл <users_backup.xml> в другой директории, необходимо прописать путь к ней (например: C:/) и отправить сообщение в чат.")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "Создать", "#createservuse")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputSaveUserBackup
                    }

                    callBackData == "Сохранить client_backup" -> {
                        val backupDirectory = "C:\\Users\\admit\\OneDrive\\Рабочий стол\\client_backup.xml"
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Бэкап-файл данных ClientData с расширением .xml будет создан в директории по умолчанию: $backupDirectory после нажатия клавиши <Создать>. Если необходимо создать файл <client_backup.xml> в другой директории, необходимо прописать путь к ней (например: C:/) и отправить сообщение в чат.")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "Создать", "#createservcli")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputSaveClientBackup
                    }

                    callBackData == "#createservuse" -> {
                        val backupDirectory = "C:\\Users\\admit\\OneDrive\\Рабочий стол\\user_backup.xml"
                        val userList = mutableListOf<String>()
                        userRepository.findAll().forEach { userList.add(it.toString()) }

                        val backupCreator = BackupCreator()
                        val backupFile = backupCreator.receiveBackupFile("users", userList)
                        backupCreator.createBackupXml(backupFile, backupDirectory)
                        val textForMessage = "\uD83D\uDD30  Бэкап-файл данных User создан в директории:\n$backupDirectory"

                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData == "#createservcli" -> {
                        val backupDirectory = "C:\\Users\\admit\\OneDrive\\Рабочий стол\\client_backup.xml"
                        val clientList = mutableListOf<String>()
                        clientRepository.findAll().forEach { clientList.add(it.toString()) }

                        val backupCreator = BackupCreator()
                        val backupFile = backupCreator.receiveBackupFile("clients", clientList)
                        backupCreator.createBackupXml(backupFile, backupDirectory)
                        val textForMessage = "\uD83D\uDD30  Бэкап-файл данных ClientData создан в директории:\n$backupDirectory"

                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData == "Произвести бэкап client" -> {
                        val backupDirectory = "C:/Users/admit/OneDrive/Рабочий стол/client_backup.xml"
                        val textForMessage = "\uD83D\uDD30  Бэкап сервера ClientData будет получен из директории по умолчанию: $backupDirectory после нажатия клавиши <Загрузить файл>. Если необходимо загрузить файл <client_backup.xml> из другой директории, необходимо прописать путь к ней (например: C:/) и отправить сообщение в чат."
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "Загрузить файл", "#loadbackupcli")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputLoadClientBackup

                    }

                    callBackData == "Произвести бэкап user" -> {
                        val backupDirectory = "C:/Users/admit/OneDrive/Рабочий стол/user_backup.xml"
                        val textForMessage = "\uD83D\uDD30  Бэкап сервера User будет получен из директории по умолчанию: $backupDirectory после нажатия клавиши <Загрузить файл>. Если необходимо загрузить файл <user_backup.xml> из другой директории, необходимо прописать путь к ней (например: C:/) и отправить сообщение в чат."
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "Загрузить файл", "#loadbackupuse")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputLoadUserBackup
                    }

                    callBackData == "#loadbackupcli" -> {
                        val backupDirectory = "C:/Users/admit/OneDrive/Рабочий стол/client_backup.xml"
                        val serverBackup = ServerBackup()
                        serverBackup.startBackup(backupDirectory)
                        val clientsDataList = serverBackup.receiveClientsBackup(mutableListOf())
                        clientsDataList.forEach { clientRepository.save(it) }

                        val textForMessage = "\uD83D\uDD30  Бэкап сервера ClientData будет получен из директории по умолчанию:\n$backupDirectory"
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
                        protectedExecute(editMessageText)

                    }

                    callBackData == "#loadbackupuse" -> {
                        val backupDirectory = "C:/Users/admit/OneDrive/Рабочий стол/user_backup.xml"
                        val serverBackup = ServerBackup()
                        serverBackup.startBackup(backupDirectory)
                        val userList = serverBackup.receiveUsersBackup(mutableListOf())
                        userList.filter { it.chatId != longChatId }.forEach { userRepository.save(it) }

                        val textForMessage = "\uD83D\uDD30  Бэкап сервера User получен из директории по умолчанию:\n$backupDirectory"
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
                        protectedExecute(editMessageText)

                    }

                    callBackData == "❗ Удалить сервер" -> {
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, "❗  Внимание, удаление сервера может привести к необратимой потере данных, убедитесь в наличии резервной копии данных.")
                        val menuList = listOf("Удалить client сервер", "Удалить user сервер", "Backup меню", "\uD83D\uDD19  Назад в меню")
                        editMessageText.replyMarkup = botMenuFunction.createButtonSet(menuList)
                        protectedExecute(editMessageText)
                    }

                    callBackData == "Удалить user сервер" -> {
                        val adminUser = userRepository.findById(longChatId).get()
                        userRepository.deleteAll()
                        userRepository.save(adminUser)
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, "\uD83D\uDD30  Данные User-сервера были удалены.")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
                        protectedExecute(editMessageText)

                    }

                    callBackData == "Удалить client сервер" -> {
                        clientRepository.deleteAll()
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId,  "\uD83D\uDD30  Данные ClientData-сервера были удалены.")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData == "Получить backup-файлы" -> {
                    val backupUserDirectory = "C:/Users/admit/OneDrive/Рабочий стол/user_backup.xml"
                    val backupClientDirectory = "C:/Users/admit/OneDrive/Рабочий стол/client_backup.xml"

                    val sendUserBackup = SendDocument()
                        sendUserBackup.setChatId(longChatId)
                        sendUserBackup.document = InputFile(java.io.File(backupUserDirectory))
                        sendUserBackup.caption = "ℹ  Файл User backup."
                        protectedExecute(sendUserBackup)

                        val sendClientBackup = SendDocument()
                        sendClientBackup.setChatId(longChatId)
                        sendClientBackup.document = InputFile(java.io.File(backupClientDirectory))
                        sendClientBackup.caption = "ℹ  Файл ClientData backup."
                        protectedExecute(sendClientBackup)

                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId,  "\uD83D\uDD30  Файлы резервного копирования были отправлены в чат.")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "В backup меню", "Backup меню")
                        protectedExecute(editMessageText)
                }

                    callBackData == "Меню специалиста" -> {
                        var textForMessageLength = 0
                        val localDate = LocalDate.now()
                        val nextDate: String? = clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 && it.appointmentDate != localDate.toString() }.minByOrNull { it.appointmentDate }?.appointmentDate
                        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        val textForMessage = StringBuilder()

                        if (nextDate.isNullOrEmpty()) {
                            textForMessage.append("$textForStartMessage\uD83D\uDD30  Следующая запись: нет.")
                        } else {
                            textForMessage.append("$textForStartMessage\uD83D\uDD30  Следующая запись - ${formatter.format(LocalDate.parse(nextDate))}:")
                        }

                        clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate == nextDate }.sortedBy { it.appointmentTime}.forEach { textForMessage.append("\n${it.visitAgreement} ${it.appointmentTime} - ${it.secondName} ${it.firstName.first()}. ${it.patronymic.first()}.") }

                        textForMessage.append("\n\n\uD83D\uDD30  Запись на сегодня:")
                        textForMessageLength = textForMessage.length
                        clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 && it.appointmentDate == localDate.toString() && it.visitAgreement != "✖" }.sortedBy { it.appointmentTime}.forEach { textForMessage.append("\n• ${it.appointmentTime} - ${it.secondName} ${it.firstName.first()}. ${it.patronymic.first()}.") }
                        if (textForMessageLength == textForMessage.length) textForMessage.append(" нет.")

                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, textForMessage.toString())
                        val settingList = listOf("Записать на приём", "Посмотреть запись ко мне", callData_addNewClient, "Работа с базой клиентов/пациентов", "⚙  Моя учетная запись")
                        editMessageText.replyMarkup = botMenuFunction.createButtonSet(settingList)
                        protectedExecute(editMessageText)
                    }

                    callBackData == "\uD83D\uDCC5  Посмотреть мою запись" -> {
                        val firstText = "ℹ  Ваша запись у специалиста:"
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
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData == "Зарегистрироваться как специалист" -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "ℹ  В этом разделе вы можете зарегистрироваться как специалист и упростить ведение записи своих клиентов/пациентов.")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("Зарегистрироваться",
                            "#reg", "\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                        protectedExecute(editMessageText)
                    }

                    callBackData == callData_addNewClient -> {
                        val user = userRepository.findById(longChatId).get()
                        val clientsAmount = clientRepository.findAll().filter { it.specialistId == longChatId }.size
                        val editMessageText = EditMessageText()

                        if ((LocalDate.now().isAfter(LocalDate.parse(user.paymentDate)) && clientsAmount > 50) || clientsAmount == 1000){
                            val textForMessage = "\uD83D\uDD30  Лимит добавления клиентов исчерпан. Без подключения абонемента специалист может зарегистрировать до 50 клиентов, с действующим абонементом до 1000. Сейчас у " +
                                    "вас добавлено $clientsAmount клиентов."
                            editMessageText.putData(stringChatId, intMessageId, textForMessage)
                            editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "Абонемент", "#payment")
                        } else {
                        val word = "𝙥𝙚𝙧𝙨𝙤𝙣𝙖𝙡 𝙘𝙡𝙞𝙚𝙣𝙩 𝙢𝙖𝙣𝙖𝙜𝙚𝙧"
                        val bigWord = "\uD835\uDE4B\uD835\uDE5A\uD835\uDE67\uD835\uDE68\uD835\uDE64\uD835\uDE63\uD835\uDE56\uD835\uDE61 \uD835\uDE62\uD835\uDE56\uD835\uDE63\uD835\uDE56\uD835\uDE5C\uD835\uDE5A\uD835\uDE67"
                        val textForMessage = "\uD83D\uDD30  Если ваш клиент/пациент использует Telegram, вы можете сообщить ему имя бота, которое надо ввести в строку поиска Telegram: $word. Из списка предложенных программ выбрать $bigWord." +
                                "\nЗатем нажмите клавишу <Генерировать код для клиента> и сообщите код с экрана клиенту. Клиент должен зайти в бот и отправить сообщение с кодом в чат. Вам останется только заполнить ФИО клиента " +
                                "и клиент будет добавлен в вашу базу.\n\n\uD83D\uDD30  Также, можно сначала добавить клиента/пациента в базу без кода, и если клиент использует Telegram, отправить код позже из меню <Работа с базой клиентов/пациентов>. " +
                                "Для добавления клиента без генерации кода, нажмите клавишу <Добавить клиента без кода>."
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        val settingList = listOf(callData_generationCode, callData_addCommonClient, "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                        editMessageText.replyMarkup = botMenuFunction.createButtonSet(settingList)
                        }
                        protectedExecute(editMessageText)
                    }

                    callBackData == callData_addCommonClient -> {
                        val editMessageText = EditMessageText()
                        val text = "\uD83D\uDD30 Введите фамилию клиента/пациента или ФИО полностью (например: Иванов Иван Иванович) и отправьте сообщение в чат."
                        editMessageText.putData(stringChatId, intMessageId, text)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        saveChatId[stringChatId] = 1
                        tempData[stringChatId] = inputSecondName
                    }

                    callBackData == "Работа с базой клиентов/пациентов" -> { // и редактировать данные
                        val textForMessage = "\uD83D\uDD30 Чтобы найти клиента, с данными которого вы хотите работать, " +
                        "нажмите клавишу с первой буквой фамилии клиента."
                        val editMessageText = botMenuFunction.receiveFindClientKeyboard(stringChatId, intMessageId, textForMessage, callData_findClientForMenu)
                        protectedExecute(editMessageText)
                    }

                    callBackData == "Записать на приём" -> { // receiveFindClientKeyboard
                        val textForMessage = "\uD83D\uDD30 Чтобы найти клиента для записи на приём, нажмите клавишу с первой буквой фамилии, или введите первую букву/несколько первых букв фамилии клиента и отправьте сообщение в чат."
                        val editMessageText = botMenuFunction.receiveFindClientKeyboard(stringChatId, intMessageId, textForMessage, callData_findClientByName)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = findClient
                    }

                    callBackData.contains(callData_generationCode) -> {
                        val clientId = callBackData.replace(callData_generationCode, "")
                        var password: Int
                        var isSinglePassword = false
                        saveChatId[stringChatId] = 0  // TODO надо ли?

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

                        val editMessageText = EditMessageText() // ввести в строку поиска Telegram: personal client manager. Из списка программ выбрать "Personal manager"
                        val word = "𝙥𝙚𝙧𝙨𝙤𝙣𝙖𝙡 𝙘𝙡𝙞𝙚𝙣𝙩 𝙢𝙖𝙣𝙖𝙜𝙚𝙧"
                        val bigWord = "\uD835\uDE4B\uD835\uDE5A\uD835\uDE67\uD835\uDE68\uD835\uDE64\uD835\uDE63\uD835\uDE56\uD835\uDE61 \uD835\uDE62\uD835\uDE56\uD835\uDE63\uD835\uDE56\uD835\uDE5C\uD835\uDE5A\uD835\uDE67"
                        val textForMessage = "ㅤ\n\n\uD83D\uDD38 Сообщите этот код клиенту:  $password\n\nКлиент должен зайти в Telegram, ввести в строку поиска Telegram: $word. " +
                        "Из списка программ выбрать $bigWord, зайти в бот, ввести трёхзначный код и отправить сообщение в чат. " +
                        "Если клиент успешно введёт и отправит код, у вас появится меню регистрации нового клиента. Время существования кода ограничено, сброс происходит каждый час. Если до истечения этого времени клиент не " +
                        "будет зарегистрирован, сгенерируйте новый код. Новый код не будет генерироваться, пока действителен предыдущий.\n\uD83D\uDD38 Внимание! - если вы добавляете нового клиента, не выходя из данного раздела " +
                        "дождитесь подтверждения кода, в противном случае процесс регистрации будет прерван. Если код отправлен уже зарегистрированному клиенту, выход в главное меню не повлечет никаких последствий."
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        saveClientId[stringChatId] = clientId
                    }

                    callBackData == "⚙  Моя учетная запись" -> {
                        val user: User = userRepository.findById(longChatId).get()
                        val editMessageText: EditMessageText = botMenuFunction.receiveSettingsKeyboard(stringChatId, intMessageId, user)
                        protectedExecute(editMessageText)
                    }

                    callBackData == "#mycli" -> { // "#allcli"
                        val localDate = LocalDate.now()
                        val clients = clientRepository.findAll().filter { it.specialistId == longChatId }
                        val amount = clients.size
                        val user = userRepository.findById(longChatId).get()

                        val tex = "\n\nПри необходимости обойти данное ограничение, вы можете связаться с автором приложения."
                        val text = "\n\nЧтобы увеличить лимит до 1000, вы можете внести абонентскую плату. В период действия абонемента можно добавлять новых клиентов вплоть до исчерпания ограничения в 1000 записей."

                        var textForMessage = "\uD83D\uDD30  Всего клиентов добавлено: $amount\n\n"

                        textForMessage += if (localDate.isAfter(LocalDate.parse(user.paymentDate))){
                            if ((50 - amount) > 0) "\uD83D\uDD38Лимит добавления составляет 50 клиентов, до исчерпания осталось: ${50 - amount}$text" else "\uD83D\uDD38Лимит добавления составляет 50 клиентов, сейчас лимит превышен.$text"
                        } else {
                            if ((1000 - amount) > 0) "\uD83D\uDD39Лимит добавления составляет 1000 клиентов, до исчерпания осталось: ${1000 - amount}" else "\uD83D\uDD39Лимит добавления составляет 1000 клиентов, сейчас лимит исчерпан.$tex"
                        }

                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "Назад  ⚙", "⚙  Моя учетная запись")
                        protectedExecute(editMessageText)
                    }

                    callBackData == "Посмотреть запись ко мне" -> {
                        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        var dateText = ""
                        val textForMessage = StringBuilder()
                        val localDate = LocalDate.now()
                        textForMessage.append("\uD83D\uDD30  Запись к вам:")

                        clientRepository.findAll().filter { it.specialistId == longChatId && it.appointmentDate.length == 10 &&
                         (localDate.isEqual(LocalDate.parse(it.appointmentDate)) || localDate.isBefore(LocalDate.parse(it.appointmentDate))) }
                            .sortedBy { it.appointmentDate }.asReversed().forEach { textForMessage.append( "\n" +
                                    (if (dateText == it.appointmentDate) "" else "\n") + "\uD83D\uDD39 ${formatter.
                            format(LocalDate.parse(it.appointmentDate))} в ${it.appointmentTime}  - ${it.secondName} " +
                                    "${it.firstName.first()}"); dateText = it.appointmentDate }

                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage.toString())
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("#allcli") -> { // "#allcli"
                        val returnBackData = callBackData.replace("#allcli", "")
                        val clientsList = mutableListOf<String>()
                        val clients = clientRepository.findAll().filter { it.specialistId == longChatId }.sortedBy { it.secondName }
                        clients.distinctBy { it.secondName }.forEach { clientsList.add(it.secondName) }

                        clientsList.add("\uD83D\uDD19  Назад в меню")
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Всего добавлено: ${clients.size}\nСписок фамилий клиентов/пациентов:")
                        editMessageText.replyMarkup = botMenuFunction.createDataButtonSet(clientsList, returnBackData)
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("Заметки клиента") -> {
                        val clientId = callBackData.replace("Заметки клиента", "").toLong()
                        val client: ClientData = clientRepository.findById(clientId).get()
                        val editMessageText = EditMessageText()
                        val remarkText = "\uD83D\uDCDD Заметки:\n${client.remark}\n\n\uD83D\uDD30  Здесь вы можете добавить или удалить записи, связанные с клиентом. Эти записи доступны для просмотра только вам. " +
                         "\n\uD83D\uDD39 Для добавления заметки ведите текст и отправьте сообщение в чат."

                        editMessageText.putData(stringChatId, intMessageId, remarkText)

                        if (client.remark.isEmpty()){
                            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        } else {
                            editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню", "Удалить записи", "#delrem$clientId")
                        }

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

                    /*
                    callBackData.contains("Редактировать ФИО") -> {
                        val clientId = callBackData.replace("Редактировать ФИО", "").toLong()
                        val editMessageText = EditMessageText()
                        val text = "\uD83D\uDD30 Введите фамилию клиента/пациента или ФИО полностью (например: Иванов Иван Иванович) и отправьте сообщение в чат."
                        editMessageText.putData(stringChatId, intMessageId, text)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        saveChatId[stringChatId] = clientId
                        tempData[stringChatId] = inputSecondName
                    }*/

                    callBackData.contains("Выписать клиента") -> {
                        val clientId = callBackData.replace("Выписать клиента", "").toLong()
                        val editMessageText = EditMessageText()
                        val text = "❗ Подтвердите, пожалуйста, отмену записи."
                        editMessageText.putData(stringChatId, intMessageId, text)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню", "Выписать клиента", "#cancapp$clientId")
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("#cancapp") -> {
                        val clientId = callBackData.replace("#cancapp", "").toLong()
                        val user = userRepository.findById(longChatId).get()
                        val client = clientRepository.findById(clientId).get()
                        client.appointmentTime = ""
                        client.appointmentDate = ""
                        clientRepository.save(client)

                        val textForMessage: String

                        if (client.chatId > 1){
                            val sendMessage = SendMessage(client.chatId.toString(), "Здравствуйте, ${client.firstName} ${client.patronymic}, ваша запись у специалиста: ${user.secondName} ${user.firstName} ${user.patronymic} была отменена.")
                            sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", "#delmes")
                            protectedExecute(sendMessage)
                            textForMessage = "\uD83D\uDD30  Запись клиента была отменена, сообщение об отмене было отправлено клиенту."
                        } else {
                            textForMessage = "\uD83D\uDD30  Запись клиента была отменена."
                        }

                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("Удалить клиента") -> {
                        val clientId = callBackData.replace("Удалить клиента", "").toLong()
                        val editMessageText = EditMessageText()
                        val text = "❗ Внимание: клиент/пациент будет безвозвратно удалён из базы данных. Подтвердите, пожалуйста, удаление."
                        editMessageText.putData(stringChatId, intMessageId, text)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню", "Удалить клиента", "#delcli$clientId")
                        protectedExecute(editMessageText)
                    }


                    callBackData.contains("#delcli") -> {
                        val clientId = callBackData.replace("#delcli", "").toLong()
                        clientRepository.deleteById(clientId)
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Все данные клиента были удалены.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("#timedwn") -> {
                        var user = userRepository.findById(longChatId).get()
                        val sendMessageTime = user.sendTime

                        if (sendMessageTime > 6) {
                            user.sendTime = sendMessageTime - 1
                            userRepository.save(user)
                        }

                        user = userRepository.findById(longChatId).get()
                        val editMessageText: EditMessageText = botMenuFunction.receiveSettingsKeyboard(stringChatId, intMessageId, user)
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("#timeup") -> {
                        var user = userRepository.findById(longChatId).get()
                        val sendMessageTime = user.sendTime

                        if (sendMessageTime < 22) {
                            user.sendTime = sendMessageTime + 1
                            userRepository.save(user)
                        }

                        user = userRepository.findById(longChatId).get()
                        val editMessageText: EditMessageText = botMenuFunction.receiveSettingsKeyboard(stringChatId, intMessageId, user)
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("#daydwn") -> {
                        var user = userRepository.findById(longChatId).get()
                        val sendMessageDay = user.sendBeforeDays

                        if (sendMessageDay > 1) {
                            user.sendBeforeDays = sendMessageDay - 1
                            userRepository.save(user)
                        }

                        user = userRepository.findById(longChatId).get()
                        val editMessageText: EditMessageText = botMenuFunction.receiveSettingsKeyboard(stringChatId, intMessageId, user)
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("#dayup") -> {
                        var user = userRepository.findById(longChatId).get()
                        val sendMessageDay = user.sendBeforeDays

                        if (sendMessageDay < 3) {
                            user.sendBeforeDays = sendMessageDay + 1
                            userRepository.save(user)
                        }

                        user = userRepository.findById(longChatId).get()
                        val editMessageText: EditMessageText = botMenuFunction.receiveSettingsKeyboard(stringChatId, intMessageId, user)
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("#zonedwn") -> {
                        var user = userRepository.findById(longChatId).get()
                        val timeZoneHour = user.timeZone

                        if (timeZoneHour > -5) {
                            user.timeZone = timeZoneHour - 1
                            userRepository.save(user)
                        }

                        user = userRepository.findById(longChatId).get()
                        val editMessageText: EditMessageText = botMenuFunction.receiveSettingsKeyboard(stringChatId, intMessageId, user)
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("#zoneup") -> {
                        var user = userRepository.findById(longChatId).get()
                        val timeZoneHour = user.timeZone

                        if (timeZoneHour < 10) {
                            user.timeZone = timeZoneHour + 1
                            userRepository.save(user)
                        }

                        user = userRepository.findById(longChatId).get()
                        val editMessageText: EditMessageText = botMenuFunction.receiveSettingsKeyboard(stringChatId, intMessageId, user)
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("#mydata") -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Раздел пользовательских данных. При необходимости, вы можете изменить свои ФИО, добавить пароль учетной записи, " +
                                "чтобы в случае, если ваш аккаунт в Telegram изменится, восстановить доступ к учётной записи с нового аккаунта. Также, вы можете посмотреть собственную запись к специалисту, если вы сами являетесь" +
                                "клиентом/пациентом специалиста.")
                        val menuList = listOf("Редактировать мои ФИО", "Установить пароль", "Восстановить аккаунт", "Моя запись к специалисту", "\uD83D\uDD19  Назад в меню")
                        editMessageText.replyMarkup = botMenuFunction.createDataButtonSet(menuList, stringChatId)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputSecondName
                    }

                    callBackData.contains("Моя запись к специалисту") -> {
                        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        val editMessageText = EditMessageText()
                        val clients = clientRepository.findAll().filter { it.chatId == longChatId && it.appointmentDate.length == 10 }.sortedBy { it.appointmentTime }.sortedBy { it.appointmentDate } // TODO it.appointmentTime .sortedBy  it.appointmentDate

                        if (clients.isEmpty()){
                            editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  У вас нет записи к специалисту.")
                        } else {
                            val textForMessage = StringBuilder()
                            textForMessage.append("\uD83D\uDD30  Ваша запись к специалисту:")
                            clients.forEach { textForMessage.append("\n\uD83D\uDD39 ${formatter.format(LocalDate.parse(it.appointmentDate))} в ${it.appointmentTime}  у специалиста ${userRepository.findById(it.specialistId).get().secondName} ${userRepository.findById(it.specialistId).get().firstName} ${userRepository.findById(it.specialistId).get().patronymic}") }
                            editMessageText.putData(stringChatId, intMessageId, textForMessage.toString())
                        }
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("#support") -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Если вы столкнулись с проблемами в процессе использования приложения, опишите вашу проблему и отправьте сообщение в чат.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputSupportMessage
                    }

                    callBackData.contains("#payment") -> {
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, "\uD83D\uDD30  Приложение находится на стадии бета-тестирования и в данный момент является бесплатным.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains("Восстановить аккаунт") -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Введите пароль от вашей учётной записи и отправьте сообщение в чат.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputRepairPassword
                    }

                    callBackData.contains("Установить пароль") -> { // TODO м.б. == \contains
                        val user = userRepository.findById(longChatId).get()
                        val editMessageText = EditMessageText()

                        if (user.password.isEmpty()){
                            editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Для пароля установлено ограничение длинны: не менее 5 и не более 15 символов. Введите пароль в поле ввода и отправьте сообщение в чат.")
                            tempData[stringChatId] = inputPassword
                        } else {
                            editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  У вашей учётной записи уже имеется пароль. Чтобы его изменить, введите действующий пароль и отправьте сообщение в чат.")
                            tempData[stringChatId] = inputOldPassword
                        }
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)

                    }

                    callBackData.contains("Редактировать мои ФИО") -> { // TODO м.б. == \contains
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Введите вашу фамилию или ФИО полностью (например: Иванов Иван Иванович) и отправьте сообщение в чат.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputSecondName
                    }

                    callBackData == "#reg" -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, "\uD83D\uDD30  Введите вашу фамилию или ФИО полностью (например: Иванов Иван Иванович) и отправьте сообщение в чат.")
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputSecondName
                    }

                    callBackData == "#delmydata" -> {
                        clientRepository.findAll().filter { it.specialistId == longChatId }.forEach { clientRepository.delete(it) }
                        userRepository.deleteById(longChatId)
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, "\uD83D\uDD30  Все данные пользователя были удалены.")
                        protectedExecute(editMessageText)
                   }

                   callBackData.contains("#deluser") -> {
                       val userId = callBackData.replace("#deluser", "").toLong()

                       userRepository.deleteById(userId)
                       val editMessageText = EditMessageText().putData(stringChatId, intMessageId, "\uD83D\uDD30  Все данные пользователя были удалены.")
                       editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                       protectedExecute(editMessageText)
                    }

                    callBackData.contains(callData_clientData) -> {
                    val dataString: String = callBackData.replace("#cldata", "")
                    val splitDataString = dataString.split("#")
                    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    val clientId = splitDataString[0].toLong()
                    val date = splitDataString[1]
                    val time = splitDataString[2]
                    var textAddition = ""

                    val client = clientRepository.findById(clientId).get()
                    client.appointmentDate = date
                    client.appointmentTime = time
                    client.visitAgreement = if (client.chatId > 1) "❔" else qSym
                    clientRepository.save(client)

                   val user = userRepository.findById(longChatId).get()

                   if (client.chatId > 1){
                       textAddition = "\n\n\uD83D\uDD39 Клиент зарегистрирован через Telegram и получит сообщение с просьбой подтвердить предстоящий приём. Однако, если клиент был записан накануне даты приема и рассылка " +
                               "сообщений для подтверждения визита уже произошла, это сообщение клиент уже не получит и символ-индикатор клиента в главном меню останется \"❔\""
                       val sendMessage = SendMessage(client.chatId.toString(), "ℹ  Здравствуйте, у вас запланирован визит к специалисту: ${user.secondName} ${user.firstName} ${user.patronymic} на ${formatter.format(LocalDate.parse(date))} в $time\n")
                       sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", "#delmes")
                       protectedExecute(sendMessage)
                   }

                    val editMessageText = EditMessageText()
                    val textForMessage = "\uD83D\uDD30 ${client.secondName} ${client.firstName} ${client.patronymic} записан на ${formatter.format(LocalDate.parse(date))} в $time$textAddition"
                    editMessageText.putData(stringChatId, intMessageId, textForMessage)
                    editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
                    protectedExecute(editMessageText)
                }

                    callBackData.contains(callData_callBackClientId) -> {
                        val clientId = callBackData.replace("#clid", "")

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
                        returnButton.putData("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню")
                        secondRowInlineButton.add(returnButton)

                        rowsInline.add(firstRowInlineButton)
                        rowsInline.add(secondRowInlineButton)
                        inlineKeyboardMarkup.keyboard = rowsInline

                        editMessageText.replyMarkup = inlineKeyboardMarkup

                        val textForMessage: String = if (client.appointmentDate.length == 10 && LocalDate.now().isBefore(LocalDate.parse(client.appointmentDate))) {
                            "\uD83D\uDD38 Клиент уже записан на ${formatter.format(LocalDate.parse(client.appointmentDate))} в ${client.appointmentTime}, если процесс записи будет продолжен," +
                             " клиент будет перезаписан на новое время.\n\n\uD83D\uDD30 Выберите месяц для записи клиента/пациента:"
                        } else {
                            "\uD83D\uDD30 Выберите месяц для записи клиента/пациента:"
                        }

                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains(callData_findClientByName) -> {
                    val dataText = callBackData.replace(callData_findClientByName, "")
                    findClient(stringChatId, longChatId, dataText, callData_callBackClientId)
                    tempData[stringChatId] = ""
                    }

                    callBackData.contains(callData_findClientForMenu) -> {
                        val dataText = callBackData.replace(callData_findClientForMenu, "")
                        findClient(stringChatId, longChatId, dataText, "#getmenu") // TODO "#"
                    }

                    callBackData.contains("#getmenu") -> {
                        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        val clientId = callBackData.replace("#getmenu", "")
                        val client: ClientData = clientRepository.findById(clientId.toLong()).get()

                        val menuList = mutableListOf("Заметки клиента")

                        val regText = if (client.chatId.toInt() != 1) {
                            "\n\uD83D\uDD38 Клиент зарегистрирован в Telegram и получает сообщения о предстоящем приеме."
                        } else {
                            menuList.add(callData_generationCode)
                            "\n\uD83D\uDD38 Вы можете сгенерировать код для регистрации клиента, если у клиента есть Telegram."
                        }

                        val appointmentText = if (client.appointmentDate.length != 10) {
                            "нет"
                        } else {
                            menuList.add("Выписать клиента")
                            "${formatter.format(LocalDate.parse(client.appointmentDate))}  в  ${client.appointmentTime}"
                        }


                        menuList.add("Удалить клиента")
                        menuList.add("\uD83D\uDD19  Назад в меню")

                        val textForMessage = "\uD83D\uDD39  История посещений:${client.visitHistory}\n\n" +
                                "\uD83D\uDD30 Клиент: ${client.secondName} ${client.firstName} ${client.patronymic}" +
                                "\n\n✷ В этом меню вы можете добавить текст-заметку в карточку клиента (заметка видна только вам), " +
                                "удалить данные этого клиента, отменить запись, если у клиента запланирован визит к вам.\n" +
                                "$regText\n\uD83D\uDD38 Визит клиента запланирован:  $appointmentText"

                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.createDataButtonSet(menuList, clientId)
                        protectedExecute(editMessageText)
                    }


                    callBackData.contains("@") -> {
                        val dataString = callBackData.split("@") // TODO
                        val appointmentMonth = dataString[0].toInt()
                        val clientId = dataString[1]
                        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        val localDate = LocalDate.now()
                        val editMessageText = EditMessageText()
                        val startDay: Int
                        comeBackInfo[stringChatId] = callBackData

                        val stringBuilder = StringBuilder("ℹ  Записано клиентов по датам:\n")
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
                        button.putData("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
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
                        menuButton.putData("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
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
                        menuButton.putData("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
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

                    callBackData.contains("#disapp") -> {
                        val idData = callBackData.replace("#disapp", "").split("#")
                        val userChatId = idData[0]
                        val clientId = idData[1].toLong()

                        val client = clientRepository.findById(clientId).get()
                        client.visitAgreement = "✖"
                        clientRepository.save(client)

                        val specialistEditMessageText = EditMessageText().putData(userChatId, saveStartMessageId[userChatId]!!, "❌  Клиент ${client.secondName} ${client.firstName} ${client.patronymic} отменил запись.")
                        specialistEditMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(specialistEditMessageText)

                        val clientEditMessageText = EditMessageText().putData(stringChatId, intMessageId, "ℹ  Вы отменили запись у специалиста. Если запись была отменена случайно или вы изменили свое решение, свяжитесь со своим специалистом.")
                        clientEditMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", "#delmes")
                        protectedExecute(clientEditMessageText)
                    }

                    callBackData.contains("#approve") -> {
                        val idData = callBackData.replace("#approve", "").split("#")
                        val userChatId = idData[0]
                        val clientId = idData[1].toLong()

                        val client = clientRepository.findById(clientId).get()
                        client.visitAgreement = "✔"
                        clientRepository.save(client)

                        val specialistEditMessageText = EditMessageText()
                        specialistEditMessageText.putData(userChatId, saveStartMessageId[userChatId]!!, "✅  Клиент ${client.secondName} ${client.firstName} ${client.patronymic} подтвердил запись.")
                        specialistEditMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(specialistEditMessageText)

                        val clientEditMessageText = EditMessageText()
                        clientEditMessageText.putData(stringChatId, intMessageId, "ℹ  Спасибо за подтверждение, если возникнет необходимость отменить предстоящий визит, сообщите об этом вашему специалисту.")
                        clientEditMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", "#delmes")
                        protectedExecute(clientEditMessageText)
                    }

                    callBackData.contains("#delmes") -> {
                        val deleteMessage = DeleteMessage()
                        deleteMessage.putData(stringChatId, intMessageId)
                        protectedExecute(deleteMessage)
                    }

                    callBackData.contains("#usr") -> {
                        val idData = callBackData.replace("#usr", "").split(" ")
                        val userId = idData[0]
                        val user = userRepository.findById(userId.toLong()).get()

                        val textForMessage = "\uD83D\uDD30 Для изменения данных пользователя, введите разделяя символом # следующие данные, в порядке, представленном ниже: " +
                        "\nФамилия: ${user.secondName}\nИмя: ${user.firstName}\nОтчество: ${user.patronymic}\nПрофессия: ${user.profession}" +
                        "\nChat id: ${user.chatId}\nПароль: ${user.password}\nВремя отправки сообщений клиенту: ${user.sendTime}\nВременная зона: ${user.timeZone}" +
                        "\nОтправка сообщений за дней до приема: ${user.sendBeforeDays}\nДата абонентского платежа: ${user.paymentDate}\nНовый chat id (опционально)" +
                        "\nЗатем отправьте сообщение в чат."

                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("Удалить пользователя", "#deluser$userId", "\uD83D\uDD19  В главное меню", "\uD83D\uDD19  Назад в меню")
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputChangeUser
                    }

                }
            }
        }


    // TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private fun findClient(stringChatId: String, longChatId: Long, secondNameText: String, calBackData: String){ // "#clid"
        tempData[stringChatId] = ""
        val clients = clientRepository.findAll().filter { it.specialistId == longChatId && it.secondName.contains(secondNameText, true) }.filter { it.secondName.first().lowercase() == secondNameText.first().lowercase() }.sortedBy { it.secondName } // TODO повтор кода 191
        val textForMessage: String
        val editMessageText = EditMessageText()

        if(clients.isEmpty()){
            textForMessage = "ℹ  Клиента с таким сочетанием символов в фамилии не найдено."
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
                val client = ClientData()
                client.chatId = saveChatId[stringChatId]!!
                client.firstName = firstName[stringChatId]!!
                client.secondName = secondName[stringChatId]!!
                client.patronymic = patronymic[stringChatId]!!
                client.specialistId = longChatId
                client.visitAgreement = if (saveChatId[stringChatId]!! > 1) "❔" else qSym
                clientRepository.save(client)

            val editMessageText = EditMessageText().putData(stringChatId, saveStartMessageId[stringChatId]!!, "✅ Клиент добавлен в базу данных.")
            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню", "\uD83D\uDD19  Назад в меню")
            protectedExecute(editMessageText)
            saveChatId[stringChatId] = 0
            registerPassword[stringChatId] = 0
        } else {
            val editMessageText = EditMessageText()
            editMessageText.putData(stringChatId, saveStartMessageId[stringChatId]!!, "Введите вашу специализацию (профессию) и отправьте сообщение в чат.")
            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", "\uD83D\uDD19  Назад в меню") // TODO ##regspec
            protectedExecute(editMessageText)
            tempData[stringChatId] = inputProfession
        }

    }

    /*

    @Scheduled(cron = "0 * * * * *")
    fun testCron() {
        val localDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        clientRepository.findAll().filter { localDate.toString() == it.appointmentDate && it.visitAgreement != "✖"}.
        forEach { it.visitHistory = "${it.visitHistory}\n• ${formatter.format(localDate)} в ${it.appointmentTime}"; clientRepository.save(it) }
    }

  */



    @Scheduled(cron = "0 24 * * * *")
    fun actionEveryDayRepeat() {
        saveHistoryOfAppointment()
        removeClientsAppointment()
    }


    @Scheduled(cron = "0 0 * * * *")
    fun actionEveryHourRepeat() {
        registerPassword.clear()
        sendApproveMessage()
    }


    private fun saveHistoryOfAppointment() {
        val localDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        clientRepository.findAll().filter { localDate.minusDays(1).toString() == it.appointmentDate && it.visitAgreement != "✖"}.
        forEach { it.visitHistory = "${it.visitHistory}\n• ${formatter.format(localDate.minusDays(1))} в ${it.appointmentTime}"; clientRepository.save(it) }
    }


    private fun removeClientsAppointment() {
        val localDate = LocalDate.now()
        clientRepository.findAll().filter { localDate.minusDays(1).toString() == it.appointmentDate && it.visitAgreement != "❔" && it.visitAgreement != qSym}.
        forEach { it.appointmentDate = ""; it.appointmentTime = ""; it.visitAgreement = "❔"; clientRepository.save(it) }
    }




    private fun sendApproveMessage() {
        val localTime =  LocalTime.now()
        val localDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        clientRepository.findAll().filter { it.appointmentDate.length == 10 && localDateTime.minusHours(12) == LocalDateTime.parse("${it.appointmentDate}T${it.appointmentTime}:00") }.forEach { it.appointmentDate = ""; it.appointmentTime = ""; it.visitAgreement = "❔"; clientRepository.save(it) }

        for (user in userRepository.findAll()){
            if(localTime.plusHours(user.timeZone).hour == user.sendTime){ // localTime.plusHours(user.timeZone).hour == user.sendTime      // in 0..24
            for(client in clientRepository.findAll()){
                if (client.specialistId == user.chatId && client.appointmentDate.length == 10){
                    if (formatter.format(localDateTime.plusDays(user.sendBeforeDays).plusHours(user.timeZone)) == formatter.format(LocalDate.parse(client.appointmentDate))){
                        val sendMessage = SendMessage(client.chatId.toString(), "Здравствуйте, ${client.firstName} ${client.patronymic}, ${client.appointmentDate.replace("-", ".")} в ${client.appointmentTime} у вас запланирован визит к специалисту: ${user.secondName} ${user.firstName} ${user.patronymic}. \nПодтверждаете запись?")
                        sendMessage.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("❌  Выписаться", "#disapp${user.chatId}#${client.clientId}", "✅  Подтвердить", "#approve${user.chatId}#${client.clientId}")
                        protectedExecute(sendMessage)
                    }
                }
            }
          }
        }
    }



// val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
// ${formatter.format(LocalDate.parse(it.appointmentDate))}
// "❔"  "✔"  "✖"   -"ㅤ"-  "？"

    }

