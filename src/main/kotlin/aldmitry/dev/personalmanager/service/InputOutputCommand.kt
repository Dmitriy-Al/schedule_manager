package aldmitry.dev.personalmanager.service

import aldmitry.dev.personalmanager.apptexts.*
import aldmitry.dev.personalmanager.backup.BackupCreator
import aldmitry.dev.personalmanager.config.Config
import aldmitry.dev.personalmanager.config.*
import aldmitry.dev.personalmanager.extendfunctions.protectedExecute
import aldmitry.dev.personalmanager.extendfunctions.putData
import aldmitry.dev.personalmanager.model.ClientData
import aldmitry.dev.personalmanager.model.ClientDataDao
import aldmitry.dev.personalmanager.model.User
import aldmitry.dev.personalmanager.model.UserDao
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.collections.HashMap

val config = Config()

@Component
class InputOutputCommand(@Autowired val clientRepository: ClientDataDao, @Autowired val userRepository: UserDao) :
    TelegramLongPollingBot(config.botToken) {

    init { // Команды меню бота
        val botCommandList: List<BotCommand> = listOf(
            BotCommand(callData_startBot, "Запуск программы"),
            BotCommand(callData_helpCommand, "Полезная информация"),
            BotCommand(callData_deleteUser, "Удаление всех данных пользователя")
        )
        try {
            this.execute(SetMyCommands(botCommandList, BotCommandScopeDefault(), null))
        } catch (e: TelegramApiException) {
            val logger = LoggerFactory.getLogger("InputOutputCommand <botCommandList>")
            logger.error(e.message)
        }
    }


    private var textForStartMessage: String = "" // сообщение в главном меню
    private val botMenuFunction = BotMenuFunction()

    private final val savedId = HashMap<String, Long>() // в Map добавляется id клиента для последующего сохранения в бд
    private final val tempData = HashMap<String, String>() // в Map добавляется строка-константа для выполнения определенной задачи в блоке when (tempData[stringChatId]){}
    private final val firstName = HashMap<String, String>() // в Map добавляется имя для последующего сохранения в бд
    private final val secondName = HashMap<String, String>() // в Map добавляется фамилия для последующего сохранения в бд
    private final val patronymic = HashMap<String, String>() // в Map добавляется отчество для последующего сохранения в бд
    private final val savedMessageId = HashMap<String, Int>() // в Map добавляется Id первого сообщения для последующего изменения методами EditMessageText
    private final val comeBackInfo = HashMap<String, String>() // в Map добавляется callData_тег для вызова предыдущего меню
    private final val registerPassword = HashMap<String, Int>() // в Map добавляется код для регистрации нового клиента
    private final val clientIdExistCheck = HashMap<String, String>() // если клиент существует, clientId добавляется в Map для последующей проверки


    override fun getBotUsername(): String {
        return config.botUsername
    }

    // Бот получил команду (сообщение от пользователя)
    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val longChatId: Long = update.message.chatId
            val intMessageId: Int = update.message.messageId
            val updateMessageText: String = update.message.text
            val stringChatId: String = longChatId.toString()
            val startMessageId: Int = savedMessageId[stringChatId] ?: 0


            if (!updateMessageText.contains(callData_startBot) && !updateMessageText.contains(callData_helpCommand) &&
                    !updateMessageText.contains(callData_deleteUser)) {

                /**
                 * В процессе взаимодействия с ботом может понадобиться ввод некоторых данных в чат и до момента
                 * отправки этих данных, функция добавляет в Map tempData строку-триггер, в таком случае данные
                 * введенные пользователем интерпретируются должным образом. Если в Map tempData добавляется
                 * строка-константа, сообщение-updateMessageText запускает одну из функций в блоке.
                 */
                when (tempData[stringChatId]) {
                    input_firstName -> checkFirstname(stringChatId, updateMessageText) // проверка валидности имени
                    input_loadSettingsPath -> config.loadSettings(updateMessageText) // установка настроек из xml-файла в указанной директории
                    input_messageForAll -> messageForAllUsers(updateMessageText, stringChatId) // сообщение для всех пользователей
                    input_messageForUser -> sendMessageForUser(stringChatId, updateMessageText) // отправка сообщения пользователю
                    input_patronymic -> setPatronymic(stringChatId, longChatId, updateMessageText) // функция добавления отчества
                    input_profession -> setUserDataInDB(stringChatId, longChatId, updateMessageText) // запись созданного user в бд
                    input_uploadBackup -> uploadServerToChat(longChatId, stringChatId, updateMessageText) // выгрузка backup в чат
                    input_textForStartMessage -> setTextIntoStartMessage(stringChatId, updateMessageText) // добавить текст в стартовое сообщение
                    input_supportMessage -> sendMessageForSupport(stringChatId, longChatId, updateMessageText) // сообщение от user администратору
                    input_userSecondName -> checkTextContentForRegister(stringChatId, longChatId, // применение функций в зависимости от содержимого введённого текста
                            updateMessageText, input_userSecondName)
                    input_clientSecondName -> checkTextContentForRegister(stringChatId, longChatId,// применение функций в зависимости от содержимого введённого текста
                            updateMessageText, input_clientSecondName)
                    input_changeUser -> protectedExecute(botMenuFunction.changeUserData(updateMessageText, // изменить данные для учётной записи user
                            stringChatId, tempData, startMessageId, userRepository))
                    input_oldPassword -> protectedExecute(botMenuFunction.inputOldUserPassword(stringChatId, // ввести старый пароль учётной записи user
                            startMessageId, longChatId, tempData, updateMessageText, userRepository))
                    input_loadUserBackup -> protectedExecute(botMenuFunction.putUserBackupToServer(stringChatId, // восстановить user-сервер из backup-файла в заданной директории
                            startMessageId, longChatId, updateMessageText, tempData, userRepository))
                    input_repairPassword -> protectedExecute(botMenuFunction.repairUserAccount(stringChatId, // восстановление учётной записи user
                            startMessageId, longChatId, updateMessageText, userRepository, clientRepository))
                    input_remark -> protectedExecute(botMenuFunction.addClientRemark(stringChatId, intMessageId, // добавление заметки для клиента
                            savedId, updateMessageText, clientRepository))
                    input_loadClientBackup -> protectedExecute(botMenuFunction.putClientBackupToServer(stringChatId, // восстановить client-сервер из backup-файла в заданной директории
                            startMessageId, updateMessageText, tempData, clientRepository))
                    input_password -> protectedExecute(botMenuFunction.setUserPassword(stringChatId, startMessageId, // добавить пароль для учётной записи user
                            longChatId, updateMessageText, userRepository))
                    input_findClient -> protectedExecute(botMenuFunction.receiveClientBySecondName(startMessageId,// поиск клиента, callData_callBackClientId - запись на приём
                            stringChatId, longChatId, updateMessageText, callData_callBackClientId, clientRepository))
                    input_findClientForSettings -> protectedExecute(botMenuFunction.receiveClientBySecondName(startMessageId, // поиск клиента, callData_findClientForSettings - меню для работы с данными клиента
                            stringChatId, longChatId, updateMessageText, callData_findClientForSettings, clientRepository))
                    input_saveUserBackup -> protectedExecute(botMenuFunction.createBackupInDirectory(stringChatId, startMessageId, config_userXmlGroupTitle, // создать backup-файл user в заданной директории
                            updateMessageText, config_userBackupTitle, userRepository.findAll().flatMap { mutableListOf(it.toString()) }))
                    input_saveClientBackup -> protectedExecute(botMenuFunction.createBackupInDirectory(stringChatId, startMessageId, config_clientXmlGroupTitle, // создать backup-файл client в заданной директории
                            updateMessageText, config_userBackupTitle, clientRepository.findAll().flatMap { mutableListOf(it.toString()) }))
                }
            }

            // Регистрация клиента, который ввел сгенерированный специалистом код
            registerClientByPassword(stringChatId, longChatId, intMessageId, updateMessageText)

            // Если сообщение от пользователя содержит название месяца, строке month присваивается численное значение для последующей записи даты визита клиента
            if (!updateMessageText.contains("/") && tempData[stringChatId].isNullOrEmpty() &&
                    userRepository.findById(longChatId).get().profession.isNotEmpty()) {

                // Если в updateMessageText содержится название месяца, функция вернет численное значение этого месяца (01 - 12)
                val monthNumber: String = botMenuFunction.receiveMonthNumber(updateMessageText)

                when  {
                    updateMessageText.split(" ").size >= 4 && monthNumber != monthNotFoundText -> {
                        tempData[stringChatId] = plugText
                        protectedExecute(botMenuFunction.createClientAppointment(stringChatId, startMessageId, // запись визита клиента в назначенное время
                                updateMessageText, monthNumber, callData_clientData, clientRepository))
                    }
                    updateMessageText.split(" ").size == 3 -> {
                        /**
                         * 1 - id клиента по умолчанию, если клиент добавляется без регистрации через Telegram
                        */
                        savedId[stringChatId] = 1
                        if (isSubscriptionExpire(longChatId)) protectedExecute(botMenuFunction.receiveSubscriptionMessage( // если лимит добавления не исчерпан, в бд добавляется новый клиент
                           startMessageId, stringChatId)) else checkDoubleOfClient(stringChatId, longChatId, updateMessageText)
                    }
                    !updateMessageText.contains(" ") && updateMessageText.length in 2..15 -> { // функция предоставляет список клиентов, с фамилиями, последовательность символов в которых совпадают с updateMessageText
                        protectedExecute(botMenuFunction.receiveClientBySecondName(startMessageId, stringChatId, longChatId, // поиск клиента, callData_callBackClientId - запись на приём
                                updateMessageText, callData_callBackClientId, clientRepository))
                    }
                }
            }

            // Удаление отправленных в чат сообщений (чтобы не засорять экран чата)
            protectedExecute(DeleteMessage().putData(stringChatId, intMessageId))

            /**
             *  Команды отправленные в чат запускают функции в блоке.
            */
            when (updateMessageText) { //
                callData_startBot -> sendStartMessage(stringChatId, longChatId) // начало работы бота
                callData_helpCommand -> sendHelpMessage(stringChatId, longChatId) // меню help
                callData_deleteUser -> deleteUserData(stringChatId, longChatId) // меню удаления данных user
                // Удаление отправленных в чат сообщений (чтобы не засорять экран чата)
                else -> protectedExecute(DeleteMessage().putData(stringChatId, intMessageId))
            }

            /**
             * При использовании клавиш прикреплённой к сообщению клавиатуры, запускаются функции в блоке ниже.
             * Все строки-константы для callBackData имеют префикс callData_
             */
            } else if (update.hasCallbackQuery()) {
            val callBackData: String = update.callbackQuery.data
            val longChatId: Long = update.callbackQuery.message.chatId
            val intMessageId: Int = update.callbackQuery.message.messageId
            val stringChatId: String = update.callbackQuery.message.chatId.toString()
            val startMessageId: Int = savedMessageId[stringChatId] ?: 0

                when {
                    callBackData.contains(callData_mainMenu) -> {
                        sendEditStartMessage(stringChatId, longChatId, intMessageId)
                    }

                    callBackData == callData_startMenu -> {
                        sendStartMessage(stringChatId, longChatId)
                    }

                    callBackData == callData_sendUserBackupLists -> {
                        sendBackupListToEachUser()
                    }

                    callBackData == callData_chatBackup -> {
                        uploadServerToChat(longChatId, stringChatId, config_backupDirectory)
                    }

                    callBackData.contains(callData_paymentMenu) -> {
                        receiveSubscriptionMenu(longChatId, stringChatId, intMessageId)
                    }

                    callBackData.contains(callData_cancelAppointment) -> {
                        cancelAppointment(stringChatId, intMessageId, callBackData)
                    }

                    callBackData.contains(callData_approveAppointment) -> {
                        approveAppointment(stringChatId, intMessageId, callBackData)
                    }

                    callBackData.contains(callData_clientData) -> {
                        createAppointmentMessages(longChatId, stringChatId, intMessageId, callBackData)
                    }

                    callBackData == callData_addNewClient -> {
                        protectedExecute(botMenuFunction.addNewClient(stringChatId,
                                intMessageId, isSubscriptionExpire(longChatId)))
                    }

                    callBackData == callData_appointmentToMe -> {
                        protectedExecute(botMenuFunction.receiveSchedule(longChatId,
                        stringChatId, intMessageId, clientRepository))
                    }

                    callBackData == callData_timeDown -> {
                        protectedExecute(botMenuFunction.putSendMessageTime(longChatId,
                                stringChatId, intMessageId, -1, userRepository))
                    }

                    callBackData == callData_timeUp -> {
                        protectedExecute(botMenuFunction.putSendMessageTime(longChatId,
                                stringChatId, intMessageId, 1, userRepository))
                    }

                    callBackData == callData_dayDown -> {
                        protectedExecute(botMenuFunction.putSendMessageDay(longChatId,
                                stringChatId, intMessageId, -1, userRepository))
                    }

                    callBackData == callData_dayUp -> {
                        protectedExecute(botMenuFunction.putSendMessageDay(longChatId,
                                stringChatId, intMessageId, 1, userRepository))
                    }

                    callBackData == callData_zoneDown -> {
                        protectedExecute(botMenuFunction.putTimeZone(longChatId, stringChatId,
                                intMessageId, -1, userRepository))
                    }

                    callBackData == callData_zoneUp -> {
                        protectedExecute(botMenuFunction.putTimeZone(longChatId, stringChatId,
                                intMessageId, 1, userRepository))
                    }

                    callBackData.contains(callData_generateCode) -> {
                        protectedExecute(botMenuFunction.generateCode(stringChatId, callBackData, intMessageId,
                        savedId, registerPassword, clientIdExistCheck))
                    }

                    callBackData == callData_specMenu -> {
                        protectedExecute(botMenuFunction.specialistUserMenu(longChatId, stringChatId, intMessageId,
                        textForStartMessage, clientRepository))
                    }

                    callBackData == callData_myAppointment -> {
                        protectedExecute(botMenuFunction.lookClientAppointment(longChatId,
                                stringChatId, intMessageId, clientRepository, userRepository))
                    }

                    callBackData == callData_myClients -> {
                        protectedExecute(botMenuFunction.receiveClientsMenu(longChatId, stringChatId, intMessageId,
                                clientRepository, userRepository))
                    }

                    callBackData.contains(callData_allClients) -> {
                        protectedExecute(botMenuFunction.receiveAllClients(longChatId, stringChatId, callBackData,
                                intMessageId, clientRepository))
                    }

                    callBackData.contains(callData_clientRemark) -> {
                        protectedExecute(botMenuFunction.receiveClientRemark(stringChatId, callBackData,
                                intMessageId, tempData, savedId, clientRepository))
                    }

                    callBackData.contains(callData_delClientRemark) -> {
                        cleanTemporaryData(stringChatId)
                        protectedExecute(botMenuFunction.deleteClientRemark(stringChatId,
                                callBackData, intMessageId, clientRepository))
                    }

                    callBackData.contains(callData_clientSettingMenu) -> {
                        protectedExecute(botMenuFunction.receiveClientSettingMenu(stringChatId,
                                intMessageId, callBackData, isSubscriptionExpire(longChatId), clientRepository))
                    }

                    callBackData.contains(callData_appointmentDay) -> {
                        protectedExecute(botMenuFunction.receiveAppointmentDay(longChatId, stringChatId,
                                intMessageId, callBackData, comeBackInfo, clientRepository))
                    }

                    callBackData.contains(callData_appointmentHour) -> {
                        protectedExecute(botMenuFunction.receiveAppointmentHour(longChatId, stringChatId,
                                intMessageId, callBackData, comeBackInfo, clientRepository))
                    }

                    callBackData.contains(callData_appointmentMin) -> {
                        protectedExecute(botMenuFunction.receiveAppointmentMinute(stringChatId,
                                intMessageId, callBackData, comeBackInfo))
                    }

                    callBackData.contains(callData_changeUser) -> {
                        protectedExecute(botMenuFunction.changeUserData(stringChatId,
                                intMessageId, callBackData, tempData, userRepository))
                    }

                    callBackData.contains(callData_appointmentToSpec) -> {
                        protectedExecute(botMenuFunction.receiveAppointmentForClient(longChatId, stringChatId,
                                intMessageId, userRepository, clientRepository))
                    }

                    callBackData.contains(callData_callBackClientId) -> {
                        protectedExecute(botMenuFunction.receiveAppointmentMonth(stringChatId,
                                intMessageId, callBackData, clientRepository))
                    }

                    callBackData == callData_setPassword -> {
                        protectedExecute(botMenuFunction.receiveUserPasswordMenu(longChatId,
                                stringChatId, intMessageId, tempData, userRepository))
                    }

                    callBackData.contains(callData_cancelClientAppointment) -> {
                        cancelClientAppointment(longChatId, stringChatId, callBackData, intMessageId)
                    }

                    callBackData == callData_loadBackupClient -> {
                        protectedExecute(botMenuFunction.putClientBackupToServer(stringChatId,
                                intMessageId, config_backupDirectory, tempData, clientRepository))
                    }

                    callBackData == callData_loadBackupUser -> {
                        protectedExecute(botMenuFunction.putUserBackupToServer(stringChatId, intMessageId, longChatId,
                                config_backupDirectory, tempData, userRepository))
                    }

                    callBackData == callData_cleanChat -> {
                        for ((chatId, messageId) in savedMessageId){
                            protectedExecute(DeleteMessage().putData(chatId, messageId))
                        }
                    }

                    callBackData == callData_setFullNameInDb -> {
                        savedId[stringChatId] = 1
                        setFullNameInDb(stringChatId, longChatId)
                    }

                    callBackData == callData_defaultUserBackup -> {
                        val usersList: List<String> = userRepository.findAll().flatMap { listOf(it.toString()) }
                        protectedExecute(botMenuFunction.createDefaultBackup(stringChatId, intMessageId,
                                config_userBackupTitle, usersList))
                    }

                    callBackData == callData_defaultClientBackup -> {
                        val clientsList: List<String> = clientRepository.findAll().flatMap { listOf(it.toString()) }
                        protectedExecute(botMenuFunction.createDefaultBackup(stringChatId, intMessageId,
                                config_clientBackupTitle, clientsList))
                    }

                    callBackData == callData_clientBaseMenu -> {
                        val editMessageText = botMenuFunction.receiveFindClientKeyboard(stringChatId,
                                intMessageId, text_findClient, callData_clientForSettingsMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_findClientForSettings
                    }

                    callBackData.contains(callData_clientForSettingsMenu) -> {
                        val dataText = callBackData.replace(callData_clientForSettingsMenu, "")
                        protectedExecute(botMenuFunction.receiveClientBySecondName(startMessageId,
                        stringChatId, longChatId, dataText, callData_clientSettingMenu, clientRepository))
                    }

                    callBackData.contains(callData_delMessage) -> {
                        val deleteMessage = DeleteMessage()
                        deleteMessage.putData(stringChatId, intMessageId)
                        protectedExecute(deleteMessage)
                    }

                    callBackData == callData_setAppointment -> {
                        val editMessageText = botMenuFunction.receiveFindClientKeyboard(stringChatId,
                                intMessageId, text_findClientToAppointment, callData_clientForAppointment)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_findClient
                    }

                    callBackData ==  callData_myAccount -> {
                        val user: User = userRepository.findById(longChatId).get()
                        val editMessageText: EditMessageText = botMenuFunction.receiveUserSettingsMenu(stringChatId,
                                intMessageId, user)
                        protectedExecute(editMessageText)
                    }

                    callBackData == callData_delServerMenu -> {
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, text_warningText)
                        val menuList = listOf(callData_delClientServer, callData_delUserServer,
                                callData_backupMenu, callData_mainMenu)
                        editMessageText.replyMarkup = botMenuFunction.createButtonSet(menuList)
                        protectedExecute(editMessageText)
                    }

                    callBackData == callData_cleanChatMenu -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_actAccept)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu(
            "\uD83D\uDD19  Назад в меню", callData_mainMenu, "Очистить чат", callData_cleanChat)
                        protectedExecute(editMessageText)
                    }

                    callBackData == callData_editeUser -> {
                        val usersList: MutableList<String> = userRepository.findAll().flatMap {
                            mutableListOf("${it.chatId} ${it.secondName}") }.toMutableList()
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId,
                       "\uD83D\uDD30  Выберите пользователя из списка:")
                        usersList.add(callData_mainMenu)
                        editMessageText.replyMarkup = botMenuFunction.createDataButtonSet(usersList, callData_changeUser)
                        protectedExecute(editMessageText)
                    }

                    callBackData == callData_loadSettings -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_setSettings)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  В главное меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_loadSettingsPath
                    }

                    callBackData == callData_messageToUser -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_messageToUser)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Назад в меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_messageForUser
                    }

                    callBackData == callData_messageToAllUsers -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_messageToAllUsers)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Назад в меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_messageForAll
                    }

                    callBackData == callData_messageToMainMenu -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_forMainMenuMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Назад в меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_textForStartMessage
                    }

                    callBackData == callData_backupMenu -> {
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, text_backupToChat)
                        val menuList = listOf(callData_sendUserBackupLists, callData_backupToChat,
                        callData_saveClientBackup, callData_saveUserBackup, callData_backupInClient, callData_backupInUser,
                                callData_delServerMenu, callData_mainMenu)
                        editMessageText.replyMarkup = botMenuFunction.createButtonSet(menuList)
                        protectedExecute(editMessageText)
                    }

                    callBackData == callData_saveUserBackup -> {
                        val backupDirectory = config_backupDirectory + config_userBackupTitle
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId,
                                "$text_backupDataPartOne$backupDirectory$text_backupDataPartTwo")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню",
                                callData_mainMenu, "Создать", callData_defaultUserBackup)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_saveUserBackup
                    }

                    callBackData == callData_saveClientBackup -> {
                        val backupDirectory = config_backupDirectory + config_clientBackupTitle
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId,
                                "$text_backupDataPartOne$backupDirectory$text_backupDataPartTwo")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню",
                                callData_mainMenu, "Создать", callData_defaultClientBackup)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_saveClientBackup
                    }

                    callBackData == callData_backupInClient -> {
                        val backupDirectory = config_backupDirectory + config_clientBackupTitle
                        val textForMessage = "$text_getBackupPartOne$backupDirectory$text_getBackupPartTwo"
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню",
                        callData_mainMenu, "Загрузить файл", callData_loadBackupClient)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_loadClientBackup
                    }

                    callBackData ==  callData_backupInUser -> {
                        val backupDirectory = config_backupDirectory + config_userBackupTitle
                        val textForMessage = "$text_getBackupPartOne$backupDirectory$text_getBackupPartTwo"
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню",
                                callData_mainMenu, "Загрузить файл", callData_loadBackupUser)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_loadUserBackup
                    }

                    callBackData == callData_delUserServer -> {
                        val adminUser: User = userRepository.findById(longChatId).get()
                        userRepository.deleteAll()
                        userRepository.save(adminUser)
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, text_wasDeleted)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню",
                                callData_mainMenu, "В backup меню", callData_backupMenu)
                        protectedExecute(editMessageText)
                    }

                    callBackData == callData_delClientServer -> {
                        clientRepository.deleteAll()
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId,  text_wasDeleted)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню",
                                callData_mainMenu, "В backup меню", callData_backupMenu)
                        protectedExecute(editMessageText)
                    }

                    callBackData == callData_backupToChat -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_uploadToServer)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("В backup меню",
                                callData_backupMenu, "По умолчанию", callData_chatBackup)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_uploadBackup
                    }

                    callBackData == callData_regAsSpec -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_regAsSpec)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("Зарегистрироваться",
                                callData_registration, "\uD83D\uDD19  Назад в меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                    }

                    callBackData == callData_addCommonClient -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_inputSecondName)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  В главное меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                        savedId[stringChatId] = 1
                        tempData[stringChatId] = input_clientSecondName
                    }

                    callBackData.contains(callData_removeAppointment) -> {
                        val clientId = callBackData.replace(callData_removeAppointment, "").toLong()
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_cancelAppointment)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Отмена",
                        callData_mainMenu, "Выписать клиента", "$callData_cancelClientAppointment$clientId")
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains(callData_deleteClientMenu) -> {
                        val clientId = callBackData.replace(callData_deleteClientMenu, "").toLong()
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_delClient)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Отмена",
                        callData_mainMenu, "Удалить клиента", "$callData_deleteClient$clientId")
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains(callData_deleteClient) -> {
                        val clientId = callBackData.replace(callData_deleteClient, "").toLong()
                        clientRepository.deleteById(clientId)
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_deletedClient)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  В главное меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains(callData_myData) -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_specialistMenu)
                        val menuList = listOf(callData_editeUsername, callData_setPassword,
                                callData_repairAccount, callData_appointmentToSpec, callData_mainMenu)
                        editMessageText.replyMarkup = botMenuFunction.createDataButtonSet(menuList, stringChatId)
                        protectedExecute(editMessageText)
                    }

                    callBackData.contains(callData_messageToSupport) -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_messageToAdmin)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  В главное меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_supportMessage
                    }

                    callBackData.contains(callData_repairAccount) -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_repairAccount)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Отмена", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_repairPassword
                    }

                    callBackData.contains(callData_editeUsername) -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_inputName)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Отмена", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_userSecondName
                    }

                    callBackData == callData_registration -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_inputName)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Отмена", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = input_userSecondName
                    }

                    callBackData.contains(callData_delAllUserData) -> {
                        val userIdData: String = callBackData.replace(callData_delAllUserData , "")
                        val userChatId: Long = if (userIdData.isNotEmpty()) userIdData.toLong() else longChatId
                        clientRepository.findAll().filter { it.specialistId == userChatId }.
                        forEach { clientRepository.delete(it) }
                        userRepository.deleteById(userChatId)
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId,
                                text_deletedAllUseData)
                        protectedExecute(editMessageText)
                   }

                    callBackData.contains(callData_clientForAppointment) -> {
                        val dataText = callBackData.replace(callData_clientForAppointment, "")
                        protectedExecute(botMenuFunction.receiveClientBySecondName(startMessageId,
                        stringChatId, longChatId, dataText, callData_callBackClientId, clientRepository))
                        tempData[stringChatId] = ""
                    }

                    callBackData.contains(callData_editeClientName) -> {
                        val clientId = callBackData.replace(callData_editeClientName, "").toLong()
                        val client = clientRepository.findById(clientId).get()
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_inputClientName)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  В главное меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                        savedId[stringChatId] = client.clientId
                        tempData[stringChatId] = input_clientSecondName
                    }

                    callBackData.contains(callData_getBackup) -> {
                        val userId: Long = callBackData.replace(callData_getBackup, "").toLong()
                        val clientList: String = botMenuFunction.receiveClientList(userId, clientRepository)
                        BackupCreator().createClientListTxt(clientList, "${config_backupListDirectory}$userId.txt")
                        protectedExecute(botMenuFunction.receiveBackupList(stringChatId,
                                "${config_backupListDirectory}$userId.txt"))
                    }
                }

        } else if (update.hasPreCheckoutQuery()) {
            val preCheckoutQuery: PreCheckoutQuery = update.preCheckoutQuery
            paySubscription(update, preCheckoutQuery)
        }
    }

    // Функционал выполняемый один раз в сутки
    @Scheduled(cron = "0 0 23 * * *") // cron раз в минуту @Scheduled(cron = "0 * * * * *")
    fun actionEveryDayRepeat() {
        botMenuFunction.saveHistoryOfAppointment(clientRepository)
        botMenuFunction.removeClientsAppointment(clientRepository)
        if (LocalDate.now().dayOfWeek == DayOfWeek.MONDAY) sendBackupListToEachUser()
    }

    // Функционал выполняемый один раз в час
    @Scheduled(cron = "0 0 * * * *")
    fun actionEveryHourRepeat() {
        createBackup()
        sendBackupToAdmin()
        sendApproveMessage()
        registerPassword.clear()
        sendSubscriptionExpireMessage()
    }

    // Удаление временных данных
    private fun cleanTemporaryData(stringChatId: String){
        tempData[stringChatId] = ""
        comeBackInfo[stringChatId] = ""
        savedId[stringChatId] = 0
    }

    // Сохранение первичных данных пользователя, зашедшего в бот
    private fun saveCommonUser(longChatId: Long){
        if (userRepository.findById(longChatId).isEmpty) {
            val user = User()
            user.chatId = longChatId
            userRepository.save(user)
        }
    }

    // Отправка сообщения от администратора пользователю
    private fun sendMessageForUser(stringChatId: String, updateMessageText: String) {
        protectedExecute(botMenuFunction.sendMessageToUser(updateMessageText))
        protectedExecute(botMenuFunction.sendMessageToAdminNotification(stringChatId, savedMessageId[stringChatId] ?: 0))
    }

    // Проверка, истёк ли срок абонемента
    private fun isSubscriptionExpire(longChatId: Long): Boolean {
        val user: User = userRepository.findById(longChatId).get()
        val clientsAmount = clientRepository.findAll().filter { it.specialistId == longChatId }.size
        return ((LocalDate.now().isAfter(LocalDate.parse(user.paymentDate)) && clientsAmount >= config_freeClientsAmount) ||
                clientsAmount == config_maxClientsAmount)
    }

    // Отправка списка пациентов в чат специалистам
    private fun sendBackupListToEachUser() {
        val backupCreator = BackupCreator()
        userRepository.findAll().filter { it.secondName.isNotEmpty() }.forEach {
            val clientList: String = botMenuFunction.receiveClientList(it.chatId, clientRepository)
            backupCreator.createClientListTxt(clientList, "${config_backupListDirectory}${it.chatId}.txt")
            protectedExecute(botMenuFunction.receiveBackupList(it.chatId.toString(),
                    "${config_backupListDirectory}${it.chatId}.txt"))
        }
    }

    // Добавление к сообщению на стартовом экране сообщения от администратора
    private fun setTextIntoStartMessage(stringChatId: String, updateMessageText: String) {
        tempData[stringChatId] = plugText
        textForStartMessage = if (updateMessageText.length > 3) "$updateMessageText\n\n" else ""
        val savedMessageId: Int = savedMessageId[stringChatId] ?: 0
        val editMessageText = EditMessageText().putData(stringChatId, savedMessageId,
                "$text_forStartMessage$textForStartMessage")
        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                callData_mainMenu)
        protectedExecute(editMessageText)
    }

    // Сообщение администратору от пользователя
    private fun sendMessageForSupport(stringChatId: String, longChatId: Long, updateMessageText: String) {
        val adminId = userRepository.findAll().first { it.profession == config.adminUser }
        val user = userRepository.findById(longChatId).get()
        protectedExecute(SendMessage(adminId!!.chatId.toString(), "$text_support$user\n$updateMessageText"))
        val savedMessageId: Int = savedMessageId[stringChatId] ?: 0
        val editMessageText = EditMessageText().putData(stringChatId, savedMessageId, text_forSupportSent)
        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                callData_mainMenu)
        protectedExecute(editMessageText)
    }

    // Сообщение об истечении срока подписки
    private fun sendSubscriptionExpireMessage() {
        val sendHour: Int = LocalDateTime.now().hour
        val beforeExpireTerm = LocalDate.now().plusDays(3).toString()
        userRepository.findAll().filter { it.paymentDate == beforeExpireTerm && it.sendTime == sendHour }.
        forEach {
            val sendMessage = SendMessage(it.chatId.toString(), text_subscriptionExpire)
            sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu(okButton, callData_delMessage)
            protectedExecute(sendMessage)
        }
    }

    // Отправка backup-файлов в чат администратора
    private fun uploadServerToChat(longChatId: Long, stringChatId: String, backupDirectory: String) {
        tempData[stringChatId] = plugText
        val userBackupMessage: SendDocument = botMenuFunction.sendBackup(longChatId,
                "ℹ  Файл User backup.", backupDirectory + config_userBackupTitle)
        protectedExecute(userBackupMessage)
        val clientBackupMessage: SendDocument = botMenuFunction.sendBackup(longChatId,
                "ℹ  Файл ClientData backup.", backupDirectory + config_clientBackupTitle)
        protectedExecute(clientBackupMessage)
    }

    // Отправка backup-файлов в чат администратора в заданное время
    private fun sendBackupToAdmin() {
        if(LocalTime.now().hour == config_createBackupTime){
            val admin: User? = userRepository.findAll().find { it.profession == config.adminUser }
            val adminChatId: Long = admin?.chatId ?: 0

            val userBackupMessage: SendDocument = botMenuFunction.sendBackup(adminChatId,
                    "ℹ  Файл User backup.", config_backupDirectory + config_userBackupTitle)
            protectedExecute(userBackupMessage)
            val clientBackupMessage: SendDocument = botMenuFunction.sendBackup(adminChatId,
                    "ℹ  Файл ClientData backup.", config_backupDirectory + config_clientBackupTitle)
            protectedExecute(clientBackupMessage)
        }
    }

    // Создание backup серверов
    private fun createBackup() {
        if(LocalTime.now().hour == config_createBackupTime){
            val backupCreator = BackupCreator()
            val userList: List<String> = userRepository.findAll().flatMap { listOf(it.toString()) }
            val clientList: List<String> = clientRepository.findAll().flatMap { listOf(it.toString()) }

            val usersBackupFile: String = backupCreator.receiveBackupFile(config_userXmlGroupTitle, userList)
            backupCreator.createBackupXml(usersBackupFile, config_backupDirectory + config_userBackupTitle)

            val backupFile = backupCreator.receiveBackupFile(config_clientXmlGroupTitle, clientList)
            backupCreator.createBackupXml(backupFile, config_backupDirectory + config_clientBackupTitle)
        }
    }

    // Отправка сообщения пользователю для подтверждения визита к специалисту
    private fun sendApproveMessage() {
        val localTime =  LocalTime.now()
        val localDateTime = LocalDateTime.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val textFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        clientRepository.findAll().filter { it.appointmentDate.length == 10 && localDateTime.minusHours(12) ==
                LocalDateTime.parse("${it.appointmentDate}T${it.appointmentTime}:00") }.forEach {
            it.appointmentDate = ""; it.appointmentTime = ""; it.visitAgreement = "❔"; clientRepository.save(it) }

        for (user in userRepository.findAll()){
            if (localTime.plusHours(user.timeZone).hour == user.sendTime) {
                for(client in clientRepository.findAll()){
                    if (client.specialistId == user.chatId && client.appointmentDate.length == 10) {
                        if (dateFormatter.format(localDateTime.plusDays(user.sendBeforeDays).plusHours(user.timeZone)) ==
                                dateFormatter.format(LocalDate.parse(client.appointmentDate))){
                            val sendMessage = SendMessage(client.chatId.toString(), "Здравствуйте, ${client.firstName} " +
                                    "${client.patronymic}, ${textFormatter.format(LocalDate.parse(client.appointmentDate))} " +
                                    "в ${client.appointmentTime} у вас запланирован визит к специалисту: ${user.getFullName()}. " +
                                    "\nВы подтверждаете визит?")

                            sendMessage.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("❌  Выписаться",
                                    "$callData_cancelAppointment${user.chatId}#${client.clientId}",
                                    "✅  Подтвердить",
                                    "$callData_approveAppointment${user.chatId}#${client.clientId}")
                            protectedExecute(sendMessage)
                        }
                    }
                }
            }
        }
    }

    // Отправка сообщения стартового экрана
    private fun sendStartMessage(stringChatId: String, longChatId: Long) {
        cleanTemporaryData(stringChatId)
        saveCommonUser(longChatId)

        if (savedMessageId[stringChatId] != null) protectedExecute(DeleteMessage().putData(stringChatId, savedMessageId[stringChatId]!!))
        val user: User = userRepository.findById(longChatId).get()

        when{
            user.secondName.isEmpty() -> {
                savedMessageId[stringChatId] = protectedExecute(botMenuFunction.receiveClientMessage(stringChatId))
            }
            user.profession == config.adminUser -> {
                savedMessageId[stringChatId] = protectedExecute(botMenuFunction.receiveAdministratorSendMessage(stringChatId, textForStartMessage,
                        savedMessageId.size, userRepository, clientRepository))
            }
            else -> {
                val sendMessage: SendMessage = botMenuFunction.receiveSpecialistSendMessage(longChatId, stringChatId,
                        textForStartMessage, clientRepository)
                savedMessageId[stringChatId] = protectedExecute(sendMessage)
            }
        }
    }

    // Отправка сообщения стартового экрана (после нажатия клавиши <Назад в меню> экранной клавиатуры)
    private fun sendEditStartMessage(stringChatId: String, longChatId: Long, intMessageId: Int) {
        val user: User = userRepository.findById(longChatId).get()
        cleanTemporaryData(stringChatId)

        when{
            user.secondName.isEmpty() -> {
                protectedExecute(botMenuFunction.receiveClientEditMessage (stringChatId, intMessageId))
            }
            user.profession == config.adminUser -> {
                savedMessageId[stringChatId] = protectedExecute(botMenuFunction.receiveAdministratorSendMessage(stringChatId,
                        textForStartMessage, savedMessageId.size, userRepository, clientRepository))
            }
            else -> {
                val editMessageText: EditMessageText = botMenuFunction.receiveSpecialistEditMessage(longChatId,
                        stringChatId, intMessageId, textForStartMessage, clientRepository)
                protectedExecute(editMessageText)
            }
        }
    }

    // Сообщение для всех пользователей
    private fun messageForAllUsers(updateMessageText: String, stringChatId: String) {
        tempData[stringChatId] = plugText
        if (updateMessageText.length > 3) {
            for (user in userRepository.findAll()){
                val sendMessage = SendMessage(user.chatId.toString(), "$text_fromAdminMessage\n$updateMessageText")
                sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu(okButton, callData_delMessage)
                protectedExecute(sendMessage)
            }
        } else {
            val sendMessage = SendMessage(stringChatId, text_fromAdminMessageNotSend )
            sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu(okButton, callData_delMessage)
            protectedExecute(sendMessage)
        }
    }

    // Проверка валидности имени
    private fun checkFirstname(stringChatId: String, updateMessageText: String) {
        tempData[stringChatId] = plugText
        val firstNameText = updateMessageText.replace(".", "").trim()
        val editMessageText = EditMessageText()
        val savedMessageId: Int = savedMessageId[stringChatId] ?: 0

        if (firstNameText.length > 15) {
            registerPassword[stringChatId] = 0
            editMessageText.putData(stringChatId, savedMessageId, text_nameTooLong)
        } else {
            firstName[stringChatId] = firstNameText
            editMessageText.putData(stringChatId, savedMessageId, text_inputPatronymic)
            tempData[stringChatId] = input_patronymic
        }
        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", callData_mainMenu)
        protectedExecute(editMessageText)
    }

    // Установка отчества имени user
    private  fun setPatronymic(stringChatId: String, longChatId: Long, updateMessageText: String) {
        tempData[stringChatId] = plugText
        val patronymicText = updateMessageText.replace(".", "").trim()
        val savedMessageId: Int = savedMessageId[stringChatId] ?: 0

        if (patronymicText.length > 15) {
            registerPassword[stringChatId] = 0
            val editMessageText = EditMessageText().putData(stringChatId, savedMessageId, text_tooLongPatronymic)
            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню",
                    callData_mainMenu)
            protectedExecute(editMessageText)
        } else {
            patronymic[stringChatId] = patronymicText
            setFullNameInDb(stringChatId, longChatId)
        }
    }

    // Добавление нового пользователя в бд
    private fun setUserDataInDB(stringChatId: String, longChatId: Long, updateMessageText: String) {
        tempData[stringChatId] = plugText
        val admin: User? = userRepository.findAll().find { it.profession == config.adminUser }
        val user: User = userRepository.findById(longChatId).get()
        val editMessageText = EditMessageText()
        val savedMessageId: Int = savedMessageId[stringChatId] ?: 0

        if (updateMessageText == config.adminUser && admin != null || updateMessageText.length > 20) {
            editMessageText.putData(stringChatId, this.savedMessageId[stringChatId]!!, text_wrongProfession)
        } else {
            val date = LocalDate.now()
            val nextPaymentDate = date.plusMonths(config_trialPeriod)

            user.firstName = firstName[stringChatId]!!
            user.secondName = secondName[stringChatId]!!
            user.patronymic = patronymic[stringChatId]!!
            user.profession = updateMessageText.lowercase()
            user.paymentDate = nextPaymentDate.toString()
            userRepository.save(user)
            editMessageText.putData(stringChatId, savedMessageId, text_thanks)
        }
        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                callData_mainMenu)
        protectedExecute(editMessageText)
    }

    // Регистрация клиента после подтверждения им регистрационного кода
    private fun registerClientByPassword(stringChatId: String, longChatId: Long, intMessageId: Int, updateMessageText: String) {
        if (registerPassword.isNotEmpty()){
            for ((key, value) in registerPassword){
                if(value.toString() == updateMessageText) {
                    protectedExecute(DeleteMessage().putData(stringChatId, intMessageId))
                    val sendMessage = SendMessage(stringChatId,  text_passwordOk)
                    sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu(okButton, callData_delMessage)
                    protectedExecute(sendMessage)

                    val savedMessageId: Int = savedMessageId[key] ?: 0
                    val editMessageText = EditMessageText()

                    if (clientIdExistCheck[key].isNullOrEmpty()) { // этот блок для регистрации нового клиента, которого ещё нет в базе
                        editMessageText.putData(key, savedMessageId, text_passwordApprove)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена",
                                callData_mainMenu)
                        tempData[key] = input_clientSecondName
                        savedId[key] = longChatId
                    } else {
                        val clientId = clientIdExistCheck[key]!!.toLong()
                        val client = clientRepository.findById(clientId).get()
                        client.chatId = longChatId
                        clientRepository.save(client)

                        editMessageText.putData(key, savedMessageId, text_passwordClientApprove)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню",
                                callData_mainMenu)
                        clientIdExistCheck[key] = ""
                    }
                    protectedExecute(editMessageText)

                    if (registerPassword.size == 1) registerPassword.clear() else registerPassword[key] = 0
                    return
                }
            }
        }
    }

    // Отправка сообщения при введении команды /help
    private fun sendHelpMessage(stringChatId: String, longChatId: Long) {
        val messageId: Int = savedMessageId[stringChatId] ?: 0
        cleanTemporaryData(stringChatId)
        saveCommonUser(longChatId)

        if (savedMessageId[stringChatId] != null) protectedExecute(DeleteMessage().putData(stringChatId, messageId))
        val user: User = userRepository.findById(longChatId).get()

        if (user.secondName.isEmpty()){
            val sendMessage = SendMessage(stringChatId, text_commonHelpText)
            sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                    callData_mainMenu)
            savedMessageId[stringChatId] = protectedExecute(sendMessage)
        } else {
            val textForMessage = "$text_commonHelpText\n$text_specialistHelpText"
            val sendMessage = SendMessage(stringChatId, textForMessage)
            sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                    callData_mainMenu)
            savedMessageId[stringChatId] = protectedExecute(sendMessage)
        }
    }

    // Удалить данные user
    private fun deleteUserData(stringChatId: String, longChatId: Long) {
        val messageId: Int = savedMessageId[stringChatId] ?: 0
        cleanTemporaryData(stringChatId)
        saveCommonUser(longChatId)
        if (savedMessageId[stringChatId] != null) protectedExecute(DeleteMessage().putData(stringChatId, messageId))
        val sendMessage = SendMessage(stringChatId, text_approveDelData)
        sendMessage.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Отмена",
                callData_mainMenu, "Удалить данные", callData_delAllUserData)
        savedMessageId[stringChatId] = protectedExecute(sendMessage)
    }

    // Удаление записи к специалисту из данных пользователя и отправка уведомления
    private fun cancelClientAppointment(longChatId: Long, stringChatId: String, callBackData: String, intMessageId: Int) {
        val clientId = callBackData.replace(callData_cancelClientAppointment, "").toLong()
        val user = userRepository.findById(longChatId).get()
        val client = clientRepository.findById(clientId).get()
        client.appointmentTime = ""
        client.appointmentDate = ""
        clientRepository.save(client)
        val textForMessage: String

        if (client.chatId > 1){
            val sendMessage = SendMessage(client.chatId.toString(), "Здравствуйте, ${client.firstName} " +
                    "${client.patronymic}, ваша запись у специалиста: ${user.getFullName()} была отменена.")
            sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu(okButton, callData_delMessage)
            protectedExecute(sendMessage)
            textForMessage = text_removeAppointment
        } else {
            textForMessage = text_removeClientAppointment
        }

        val editMessageText = EditMessageText()
        editMessageText.putData(stringChatId, intMessageId, textForMessage)
        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                callData_mainMenu)
        protectedExecute(editMessageText)
    }

    // Сообщение о новой записи для специалиста и сообщение для клиента о предстоящем приеме
    private fun createAppointmentMessages(longChatId: Long, stringChatId: String, intMessageId: Int, callBackData: String) {
        val dataString: String = callBackData.replace(callData_clientData, "")
        val splitDataString = dataString.split("#")
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val clientId = splitDataString[0].toLong()
        val date = splitDataString[1]
        val time = splitDataString[2]
        var textAddition = ""

        val client = clientRepository.findById(clientId).get()
        client.appointmentDate = date
        client.appointmentTime = time
        client.visitAgreement = if (client.chatId > 1) wqSym else qSym
        clientRepository.save(client)

        val user = userRepository.findById(longChatId).get()

        if (client.chatId > 1){
            textAddition = text_newClientAppointment
            val sendMessage = SendMessage(client.chatId.toString(), "$text_messageClientAppointment${user.getFullName()} " +
                    "на ${formatter.format(LocalDate.parse(date))} в $time\n")
            sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu(okButton, callData_delMessage)
            protectedExecute(sendMessage)
        }

        val editMessageText = EditMessageText()
        val textForMessage = "\uD83D\uDD30 ${client.secondName} ${client.firstName} ${client.patronymic} записан на " +
                "${formatter.format(LocalDate.parse(date))} в $time$textAddition"
        editMessageText.putData(stringChatId, intMessageId, textForMessage)
        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню",
                callData_mainMenu)
        protectedExecute(editMessageText)
    }

    // Сообщения для клиента и специалиста в случае отмены визита клиентом
    private fun cancelAppointment(stringChatId: String, intMessageId: Int, callBackData: String) {
        val idData = callBackData.replace(callData_cancelAppointment, "").split("#")
        val userChatId = idData[0]
        val clientId = idData[1].toLong()

        val client = clientRepository.findById(clientId).get()
        client.visitAgreement = xSym
        clientRepository.save(client)

        val specialistEditMessageText = EditMessageText().putData(userChatId, savedMessageId[userChatId]!!,
                "❌  Клиент ${client.secondName} ${client.firstName} ${client.patronymic} отменил запись.")
        specialistEditMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                callData_mainMenu)
        protectedExecute(specialistEditMessageText)

        val clientEditMessageText = EditMessageText().putData(stringChatId, intMessageId, text_clientCanceled)
        clientEditMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(okButton, callData_delMessage)
        protectedExecute(clientEditMessageText)
    }

    // Сообщения для клиента и специалиста в случае подтверждения визита клиентом
    private fun approveAppointment(stringChatId: String, intMessageId: Int, callBackData: String) {
        val idData = callBackData.replace(callData_approveAppointment, "").split("#")
        val userChatId = idData[0]
        val clientId = idData[1].toLong()

        val client = clientRepository.findById(clientId).get()
        client.visitAgreement = okSym
        clientRepository.save(client)

        val specialistEditMessageText = EditMessageText()
        specialistEditMessageText.putData(userChatId, savedMessageId[userChatId]!!, "✅  Клиент" +
                " ${client.secondName} ${client.firstName} ${client.patronymic} подтвердил запись.")
        specialistEditMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                callData_mainMenu)
        protectedExecute(specialistEditMessageText)

        val clientEditMessageText = EditMessageText()
        clientEditMessageText.putData(stringChatId, intMessageId, text_clientAccepted)
        clientEditMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(okButton, callData_delMessage)
        protectedExecute(clientEditMessageText)
    }

    // Добавление в бд новых/отредактированных данных клиента
    private fun setFullNameInDb(stringChatId: String, longChatId: Long) {
        val savedMessageId: Int = savedMessageId[stringChatId] ?: 0
        val existClient = clientRepository.findById(savedId[stringChatId]!!)
        val editMessageText = EditMessageText()

        when {
            existClient.isPresent && existClient.get().chatId != savedId[stringChatId]!! -> {
                val client = existClient.get()
                client.firstName = firstName[stringChatId]!!
                client.secondName = secondName[stringChatId]!!
                client.patronymic = patronymic[stringChatId]!!
                clientRepository.save(client)
                editMessageText.putData(stringChatId, savedMessageId, "✅ ФИО клиента были изменены.")
            }

            savedId[stringChatId] != null && savedId[stringChatId]!! > 0 -> {
                val client = ClientData()
                client.chatId = savedId[stringChatId]!!
                client.firstName = firstName[stringChatId]!!
                client.secondName = secondName[stringChatId]!!
                client.patronymic = patronymic[stringChatId]!!
                client.specialistId = longChatId
                client.visitAgreement = if (savedId[stringChatId]!! > 1) wqSym else qSym
                clientRepository.save(client)
                editMessageText.putData(stringChatId, savedMessageId,"✅ Клиент добавлен в базу данных.")
            }

            savedId[stringChatId] == null || savedId[stringChatId]!! == 0L -> {
                editMessageText.putData(stringChatId, savedMessageId, text_inputProfession)
                tempData[stringChatId] = input_profession
            }
        }
        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню",
                callData_mainMenu)
        protectedExecute(editMessageText)
        savedId[stringChatId] = 0
        registerPassword[stringChatId] = 0
    }

    // Добавление фамилии /ФИО целиком в зависимости от содержимого сообщения
    private fun checkTextContentForRegister(stringChatId: String, longChatId: Long, updateMessageText: String, tempDataText: String) {
        tempData[stringChatId] = plugText
        val editMessageText = EditMessageText()
        val startMessageId: Int = savedMessageId[stringChatId] ?: 0

        when  {
           updateMessageText.split(" ").size == 3 -> {
               when (tempDataText){
                   input_userSecondName ->  takeFullUserNameFromChat(stringChatId, longChatId, updateMessageText)
                   input_clientSecondName -> checkDoubleOfClient(stringChatId, longChatId, updateMessageText)
               }
            }

            updateMessageText.length <= 15 && !updateMessageText.contains(" ") -> {
                val secondNameText = updateMessageText.replace("Ё", "Е").replace("ё", "Е")
                secondName[stringChatId] = secondNameText
                editMessageText.putData(stringChatId, startMessageId, text_setName)
                editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена",
                        callData_mainMenu)
                protectedExecute(editMessageText)
                tempData[stringChatId] = input_firstName
            }

            else -> {
                registerPassword[stringChatId] = 0
                editMessageText.putData(stringChatId, startMessageId, text_tooLongName)
                editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню",
                        callData_mainMenu)
                protectedExecute(editMessageText)
            }
        }
    }

    // Текст из сообщения делится на 3 части составляющих ФИО
    private fun takeFullUserNameFromChat(stringChatId: String, longChatId: Long, updateMessageText: String){
        val splitUpdateMessage = updateMessageText.split(" ")
        val secondNameText = splitUpdateMessage[0].replace(".", "").replace("Ё", "Е").trim()
        val firstNameText = splitUpdateMessage[1].replace(".", "").trim()
        val patronymicText = splitUpdateMessage[2].replace(".", "").trim()
        checkFullNameLength(stringChatId, longChatId, secondNameText, firstNameText, patronymicText)
    }

    // Проверка, совпадает ли ФИО добавляемого клиента с ФИО уже имеющимися в бд
    private fun checkDoubleOfClient(stringChatId: String, longChatId: Long, updateMessageText: String) {
        tempData[stringChatId] = plugText
        val splitUpdateMessage = updateMessageText.split(" ")
        val secondNameText = splitUpdateMessage[0].replace(".", "").replace("Ё", "Е").trim()
        val firstNameText = splitUpdateMessage[1].replace(".", "").trim()
        val patronymicText = splitUpdateMessage[2].replace(".", "").trim()
        val startMessageId: Int = savedMessageId[stringChatId] ?: 0
        val editMessageText = EditMessageText()
        val clients = clientRepository.findAll()

        if (clients.any { it.specialistId == longChatId && it.secondName == secondNameText && it.firstName ==
                        firstNameText && it.patronymic == patronymicText } ) {
            editMessageText.putData(stringChatId, startMessageId, "$text_nameCoincidePartOne$secondNameText " +
                    "$firstNameText $patronymicText$text_nameCoincidePartTwo")
            editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Отмена",
                    callData_mainMenu, "Зарегистрировать", callData_setFullNameInDb)
        } else {
            checkFullNameLength(stringChatId, longChatId, secondNameText, firstNameText, patronymicText)
        }
        protectedExecute(editMessageText)
    }


    // Проверка валидности длины ФИО
    private fun checkFullNameLength(stringChatId: String, longChatId: Long,
                                    secondNameText: String, firstNameText: String, patronymicText: String) {
        val editMessageText = EditMessageText()
        val startMessageId: Int = savedMessageId[stringChatId] ?: 0

        if (secondNameText.length > 15 || firstNameText.length > 15 || patronymicText.length > 15){
            registerPassword[stringChatId] = 0
            editMessageText.putData(stringChatId, startMessageId, text_tooLongSecondName)
            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню",
                    callData_mainMenu)
        } else {
            secondName[stringChatId] = secondNameText
            firstName[stringChatId] = firstNameText
            patronymic[stringChatId] = patronymicText
            setFullNameInDb(stringChatId, longChatId)
        }
        protectedExecute(editMessageText)
    }

    // Меню оплаты подписки
    private fun receiveSubscriptionMenu(longChatId: Long, stringChatId: String, intMessageId: Int) {
        val editMessageText: EditMessageText
        val isPayed: Boolean = LocalDate.now().isBefore(LocalDate.parse(userRepository.findById(longChatId).
        get().paymentDate).minusDays(config_paymentBefore))

        val textForMessage = "$text_paymentTextOne${config_subscriptionDays}$text_paymentTextTwo" +
                "${config_subscriptionPrice}$text_paymentTextThree${config_maxClientsAmount}$text_paymentTextFour" +
                "${config_freeClientsAmount}$text_paymentTextFive${config_paymentBefore}$text_paymentTextSix"

        if (isPayed) {
            editMessageText = EditMessageText().putData(stringChatId, intMessageId, "$textForMessage$text_alreadyPayed")
            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                    callData_mainMenu)
        } else {
            val payLync: String = protectedExecute(botMenuFunction.receiveInvoiceLink(stringChatId, text_receiptDescription))
            editMessageText = botMenuFunction.receivePayMenu(stringChatId, intMessageId, payLync,
                    "$textForMessage$text_canPay")
        }
        protectedExecute(editMessageText)
    }

    // Оплата подписки
    private fun paySubscription(update: Update, preCheckoutQuery: PreCheckoutQuery) {
        val answerPreCheckoutQuery = AnswerPreCheckoutQuery()
        val user: User = userRepository.findById(preCheckoutQuery.invoicePayload.toLong()).get()
        val userPaymentDate: LocalDate = LocalDate.parse(user.paymentDate)
        val localDate: LocalDate = LocalDate.now()
        val editMessageText = EditMessageText()
        val isSuccessfulPayment: Boolean = update.message != null && update.message.successfulPayment != null
        val isPayDate: Boolean = userPaymentDate.minusDays(config_paymentBefore).isBefore(localDate)

        if (isSuccessfulPayment && isPayDate){
            answerPreCheckoutQuery.ok = true
            user.paymentDate = localDate.plusDays(config_subscriptionDays).toString()
            userRepository.save(user)
            editMessageText.putData(preCheckoutQuery.invoicePayload, savedMessageId[preCheckoutQuery.invoicePayload]!!,
                    text_paymentSuccess)
            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                    callData_startMenu)
        } else {
            answerPreCheckoutQuery.ok = false
            editMessageText.putData(preCheckoutQuery.invoicePayload, savedMessageId[preCheckoutQuery.invoicePayload]!!,
                    "$text_paymentNotSuccess$isSuccessfulPayment; pay date: $isPayDate>")
            editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                    callData_startMenu)
        }
        answerPreCheckoutQuery.preCheckoutQueryId = preCheckoutQuery.id
        answerPreCheckoutQuery.errorMessage = text_paymentWrong
        protectedExecute(answerPreCheckoutQuery)
        protectedExecute(editMessageText)
    }

}

/**
 * Строки-константы, добавляемые в Map tempData. В процессе взаимодействия с ботом может понадобиться ввод некоторых
 * данных в чат и до момента отправки этих данных, функция добавляет в Map tempData строку-триггер, в таком случае
 * данные введенные пользователем интерпретируются должным образом.
 */
const val input_remark = "INPUT_REMARK" // добавление заметки для клиента
const val input_findClient = "FIND_CLIENT" // поиск клиента для записи на приём
const val input_password = "INPUT_PASSWORD" // добавить пароль для учётной записи user
const val input_firstName = "INPUT_FIRST_NAME" // проверка валидности имени
const val input_profession = "INPUT_PROFESSION" // запись созданного user в бд
const val input_patronymic = "INPUT_PATRONYMIC" // функция добавления отчества
const val input_changeUser = "INPUT_CHANGE_USER" // изменить данные для учётной записи user
const val input_oldPassword = "INPUT_OLD_PASSWORD" // ввести старый пароль учётной записи user
const val input_uploadBackup = "INPUT_UPLOAD_BACKUP" // выгрузка backup в чат
const val input_messageForAll = "INPUT_MESSAGE_FOR_ALL" // сообщение для всех пользователей
const val input_repairPassword = "INPUT_REPAIR_PASSWORD" // восстановление учётной записи user
const val input_supportMessage = "INPUT_SUPPORT_MESSAGE" // сообщение от user администратору
const val input_messageForUser = "INPUT_MESSAGE_FOR_USER" // отправка сообщения пользователю
const val input_loadUserBackup = "INPUT_LOAD_USER_BACKUP" // восстановить user-сервер из backup-файла в заданной директории
const val input_saveUserBackup = "INPUT_SAVE_USER_BACKUP" // создать backup-файл user в заданной директории
const val input_userSecondName = "INPUT_USER_SECOND_NAME" // применение функций в зависимости от содержимого введённого текста
const val input_saveClientBackup = "INPUT_SAVE_CLIENT_BACKUP" // создать backup-файл client в заданной директории
const val input_loadClientBackup = "INPUT_LOAD_CLIENT_BACKUP" // восстановить client-сервер из backup-файла в заданной директории
const val input_clientSecondName = "INPUT_CLIENT_SECOND_NAME" // применение функций в зависимости от содержимого введённого текста
const val input_loadSettingsPath = "INPUT_LOAD_SETTINGS_PATH" // установка настроек из xml-файла в указанной директории
const val input_textForStartMessage = "INPUT_FOR_START_MESSAGE" // добавить текст в стартовое сообщение
const val input_findClientForSettings = "FIND_CLIENT_FOR_SETTINGS" // поиск клиента для работы с его данными

/**
 * Строки-константы, добавляются как callBackData для клавиш прикреплённой к сообщению клавиатуры.
*/
const val callData_dayUp = "#dayup" // увеличивает количество дней до момента отправки сообщения с уведомлением клиента о приеме у специалиста
const val callData_timeUp = "#timeup" // увеличивает время до момента отправки сообщения с уведомлением клиента о приеме у специалиста
const val callData_zoneUp = "#zoneup" // увеличивает время часового пояса специалиста (от Мск.)
const val callData_myData = "#mydata" // меню с данными специалиста
const val callData_dayDown = "#daydwn" // уменьшает количество дней до момента отправки сообщения с уведомлением клиента о приеме у специалиста
const val callData_changeUser = "#usr" // изменить данные user
const val callData_startBot = "/start" // главное меню
const val callData_myClients = "#mycli" // меню с информацией о клиентах
const val callData_startMenu = "#start" // главное меню
const val callData_appointmentDay = "@" // установка дня записи клиента на прием
const val callData_zoneDown = "#zonedwn" // уменьшает время часового пояса специалиста (от Мск.)
const val callData_helpCommand = "/help" // меню help
const val callData_appointmentHour = "&" // установка часа записи клиента на прием
const val callData_timeDown = "#timedwn" // уменьшает время до момента отправки сообщения с уведомлением клиента о приеме у специалиста
const val callData_registration = "#reg" // запуск процесса регистрации, ввод фамилии/ФИО
const val callData_delMessage = "#delmes" // удаление сообщения
const val callData_allClients = "#allcli" // список всех клиентов специалиста
const val callData_clientData = "#cldata" // callData с прикреплённой информацией для записи на приём: clientId, дата, час, минуты
const val callData_appointmentMin = "#minute" // установка минут для записи клиента на прием
const val callData_deleteClient = "#delcli" // удаление клиента
const val callData_paymentMenu = "#payment" // меню оплаты абонемента
const val callData_getBackup = "#getbackup" // получение файла со списком клиентов для отправки специалисту
const val callData_cleanChat = "#cleanchat" // удаление всех сообщений, id которых находится в Map savedMessageId
const val callData_callBackClientId = "#clid" // callData с прикреплённой информацией для записи на приём: clientId
const val callData_backupMenu = "Backup меню" // backup меню администратора
const val callData_deleteUser = "/deletedata" // меню удаления данных user
const val callData_delClientRemark = "#delrem" // удалить заметку у клиента
const val callData_cancelAppointment = "#disapp" // отмена визита клиентом
const val callData_messageToSupport = "#support" // сообщение в поддержку от специалиста
const val callData_delAllUserData = "#delmydata" // удалить данные пользователя
const val callData_specMenu = "Меню специалиста" // меню специалиста
const val callData_cleanChatMenu = "Очистить чат" // меню удаления всех сообщений, id которых находится в Map savedMessageId
const val callData_clientSettingMenu = "#getmenu" // меню данных клиента
const val callData_approveAppointment = "#approve" // подтверждение визита клиентом
const val callData_clientRemark = "Заметки клиента" // заметки клиента
const val callData_chatBackup = "#defaultchatbackup" // backup-файлов в чат администратора
const val callData_loadBackupUser = "#loadbackupuse" // восстановление из backup сервера user
const val callData_clientForAppointment = "#findcli" // список клиентов для записи к специалисту
const val callData_setPassword = "Установить пароль" // установить пароль специалиста
const val callData_delServerMenu = "❗ Удалить сервер" // меню удаления серверов
const val callData_clientForSettingsMenu = "#finmenu" // список клиентов для меню данных клиента
const val callData_setFullNameInDb = "#acceptregister" // добавление в бд новых/отредактированных данных клиента
const val callData_loadBackupClient = "#loadbackupcli" // загрузить backup client сервера
const val callData_setAppointment = "Записать на приём" // записать клиента на приём
const val callData_myAccount = "⚙  Моя учетная запись" // меню настроек специалиста
const val callData_deleteClientMenu = "Удалить клиента" // меню удаления клиента
const val callData_cancelClientAppointment = "#cancapp" // отменить запись клиента
const val callData_defaultUserBackup = "#createservuse" // создать backup-файл user в заданной директории
const val callData_findClientForSettings = "#getsetmenu" // меню для работы с данными клиента
const val callData_backupInUser = "Backup в user сервер" // меню восстановления из backup сервера user
const val callData_repairAccount = "Восстановить аккаунт" // восстановить аккаунт специалиста
const val callData_defaultClientBackup = "#createservcli" // создание backup-файл client в директории по умолчанию
const val callData_removeAppointment = "Выписать клиента" // меню с подтверждением отмены записи на приём к специалисту
const val callData_editeUsername = "Редактировать мои ФИО" // редактирование ФИО user
const val callData_mainMenu = "\uD83D\uDD19  Назад в меню" // отправка сообщения стартового экрана
const val callData_delUserServer = "❗ Удалить user сервер" // удалить user сервер
const val callData_backupToChat = "Выгрузить backup в чат" // выгрузить backup в чат администратора
const val callData_editeUser = "Редактировать пользователя" // редактировать данные пользователя
const val callData_messageToUser = "Сообщение пользователю" // сообщение для пользователя от администратора
const val callData_messageToMainMenu = "StartMessage текст"  // добавить текст в стартовое сообщение
const val callData_saveUserBackup = "Сохранить user_backup" // создать backup-файл user в директории по умолчанию
const val callData_backupInClient = "Backup в client сервер" // backup для client сервера из директории по умолчанию
const val callData_loadSettings = "Загрузить настройки из XML" // меню загрузки настроек бота из xml файла
const val callData_delClientServer = "❗ Удалить client сервер" // удалить client сервер
const val callData_saveClientBackup = "Сохранить client_backup" // создать backup-файл client в директории по умолчанию
const val callData_appointmentToMe = "Посмотреть запись ко мне" // меню записи клиентов к специалисту
const val callData_editeClientName = "Редактировать ФИО клиента" // редактировать ФИО клиента
const val callData_generateCode = "Генерировать код для клиента" // генерировать код для добавления нового клиента
const val callData_addCommonClient = "Добавить клиента без кода" // Добавить клиента без Telegram
const val callData_appointmentToSpec = "Моя запись к специалисту" //  запись специалиста в качестве клиента к другому специалисту
const val callData_regAsSpec = "Зарегистрироваться как специалист" // регистрация user в качестве специалиста
const val callData_messageToAllUsers = "Сообщение всем пользователям" // сообщение в чат для всех пользователей
const val callData_addNewClient =  "Добавить нового клиента/пациента" // добавление нового клиента
const val callData_clientBaseMenu = "Работа с базой клиентов/пациентов" // меню для работы с данными клиента
const val callData_myAppointment = "\uD83D\uDCC5  Посмотреть мою запись" // меню для просмотра user-ом записи к специалисту
const val callData_sendUserBackupLists = "Отправить backup пользователям" // отправка списка пациентов в чат специалистам

// Символы для использования в системных текстах
const val qSym = " ？"
const val wqSym = "❔"
const val xSym = "✖"
const val okSym = "✔"
const val okButton = "\uD835\uDC0E\uD835\uDC0A"
const val plugText = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
const val monthNotFoundText = "NO_MONTH" // дефолтная строка, в случае если сообщение не содержит названия месяца









