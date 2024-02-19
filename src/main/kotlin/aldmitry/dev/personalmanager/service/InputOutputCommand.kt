package aldmitry.dev.personalmanager.service

import aldmitry.dev.personalmanager.apptexts.*
import aldmitry.dev.personalmanager.backup.BackupCreator
import aldmitry.dev.personalmanager.config.Config
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


val config = Config()

@Component
class InputOutputCommand(@Autowired val clientRepository: ClientDataDao, @Autowired val userRepository: UserDao) :
    TelegramLongPollingBot(config.botToken) {

    init { // Команды меню бота
        val botCommandList: List<BotCommand> = listOf(
            BotCommand(callData_startBot, "Запуск программы"),
           // BotCommand("/star", "Запуск программы"),
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


    private var textForStartMessage: String = ""
    private val botMenuFunction = BotMenuFunction()

    private final val savedId = HashMap<String, Long>()
    private final val tempData = HashMap<String, String>()
    private final val firstName = HashMap<String, String>()
    private final val secondName = HashMap<String, String>()
    private final val patronymic = HashMap<String, String>()
    private final val savedMessageId = HashMap<String, Int>()
    private final val comeBackInfo = HashMap<String, String>()
    private final val registerPassword = HashMap<String, Int>()
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

                when (tempData[stringChatId]) { // если в Map добавляется строка-константа, Update-сообщение (updateMessageText) запускает одну из функций в блоке
                    inputFirstName -> setFirstname(stringChatId, updateMessageText)
                    inputMessageForAll -> messageForAllUsers(updateMessageText, stringChatId)
                    inputMessageForUser -> sendMessageForUser(stringChatId, updateMessageText)
                    inputPatronymic -> setPatronymic(stringChatId, longChatId, updateMessageText)
                    inputProfession -> setUserDataInDB(stringChatId, longChatId, updateMessageText)
                    inputUploadBackup -> uploadServerToChat(longChatId, stringChatId, updateMessageText)
                    inputTextForStartMessage -> setTextIntoStartMessage(stringChatId, updateMessageText)
                    inputSupportMessage -> sendMessageForSupport(stringChatId, longChatId, updateMessageText)
                    inputUserSecondName -> checkTextContentForRegister(stringChatId, longChatId, updateMessageText,
                            inputUserSecondName)
                    inputClientSecondName -> checkTextContentForRegister(stringChatId, longChatId, updateMessageText,
                            inputClientSecondName)
                    inputRemark -> protectedExecute(botMenuFunction.addClientRemark(stringChatId, intMessageId,
                            savedId, updateMessageText, clientRepository))
                    inputLoadClientBackup -> protectedExecute(botMenuFunction.putClientBackupToServer(stringChatId,
                            startMessageId, updateMessageText, tempData, clientRepository))
                    inputPassword -> protectedExecute(botMenuFunction.setUserPassword(stringChatId,
                            startMessageId, longChatId, updateMessageText, userRepository))
                    inputRepairPassword -> protectedExecute(botMenuFunction.repairUserAccount(stringChatId,
                            startMessageId, longChatId, updateMessageText, userRepository, clientRepository))
                    inputChangeUser -> protectedExecute(botMenuFunction.changeUserData(updateMessageText, stringChatId,
                            tempData, startMessageId, userRepository))
                    inputOldPassword -> protectedExecute(botMenuFunction.inputOldUserPassword(stringChatId,
                            startMessageId, longChatId, tempData, updateMessageText, userRepository))
                    inputLoadUserBackup -> protectedExecute(botMenuFunction.putUserBackupToServer(stringChatId,
                            startMessageId, longChatId, updateMessageText, tempData, userRepository))
                    findClient -> protectedExecute(botMenuFunction.receiveFindClientMenu(startMessageId, stringChatId,
                            longChatId, updateMessageText, callData_callBackClientId, clientRepository.findAll()))
                    inputSaveUserBackup -> protectedExecute(botMenuFunction.createBackupInDirectory(stringChatId, startMessageId, config.userXmlGroupTitle,
                            updateMessageText, config.userBackupTitle, userRepository.findAll().flatMap { mutableListOf(it.toString()) }))
                    inputSaveClientBackup -> protectedExecute(botMenuFunction.createBackupInDirectory(stringChatId, startMessageId, config.clientXmlGroupTitle,
                            updateMessageText, config.userBackupTitle, clientRepository.findAll().flatMap { mutableListOf(it.toString()) }))
                }
            }

            registerClientByPassword(stringChatId, longChatId, intMessageId, updateMessageText)

            if (!updateMessageText.contains("/") && tempData[stringChatId].isNullOrEmpty() &&
                    userRepository.findById(longChatId).get().profession.isNotEmpty()) {
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
                    tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
                    protectedExecute(botMenuFunction.createClientAppointment(stringChatId, startMessageId,
                    updateMessageText, month, callData_clientData, clientRepository.findAll()))
                }
                updateMessageText.split(" ").size == 3 -> {
                    savedId[stringChatId] = 1
                    if (isSubscriptionExpire(longChatId)) protectedExecute(botMenuFunction.receiveSubscriptionMessage(
                       startMessageId, stringChatId)) else checkDoubleOfClient(stringChatId, longChatId, updateMessageText)
                }
                !updateMessageText.contains(" ") && updateMessageText.length in 3..14 -> {
                    protectedExecute(botMenuFunction.receiveFindClientMenu(startMessageId, stringChatId, longChatId,
                            updateMessageText, callData_findClient, clientRepository.findAll()))
                }
                updateMessageText.length > 2 && tempData[stringChatId].isNullOrEmpty() -> {
                    protectedExecute(botMenuFunction.receiveSearchClientMenu(stringChatId, startMessageId, longChatId,
                            updateMessageText, clientRepository.findAll()))
                }
              }
            }

            protectedExecute(DeleteMessage().putData(stringChatId, intMessageId))

            when (updateMessageText) { // команды
                "/star" -> sendStartMessage(stringChatId, longChatId)
                callData_startBot -> sendStartMessage(stringChatId, longChatId) // начало работы бота
                callData_helpCommand -> sendHelpMessage(stringChatId, longChatId)
                callData_deleteUser -> deleteUserData(stringChatId, longChatId)
                else -> if (tempData[stringChatId].isNullOrEmpty()) protectedExecute(DeleteMessage().putData(stringChatId, intMessageId)) // TODO удалить: else -> if (tempData[stringChatId].isNullOrEmpty()) protectedExecute(DeleteMessage().putData(stringChatId, intMessageId))
            }

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
                        uploadServerToChat(longChatId, stringChatId, config.defaultDirectory)
                    }

                    callBackData.contains(callData_paymentMenu) -> { // TODO 1111 1111 1111 1026
                        paySubscription(longChatId, stringChatId, intMessageId)
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

                    callBackData == callData_setFullNameInDb -> {
                        savedId[stringChatId] = 1
                        setFullNameInDb(stringChatId, longChatId)
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
                        clientTemporaryData(stringChatId)
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
                                intMessageId, config.defaultDirectory, tempData, clientRepository))
                    }

                    callBackData == callData_loadBackupUser -> {
                        protectedExecute(botMenuFunction.putUserBackupToServer(stringChatId, intMessageId, longChatId,
                                config.defaultDirectory, tempData, userRepository))
                    }

                    callBackData == callData_cleanChat -> {
                        for ((chatId, messageId) in savedMessageId){
                            protectedExecute(DeleteMessage().putData(chatId, messageId))
                        }
                    }

                    callBackData == callData_defaultUserBackup -> {
                        val usersList: List<String> = userRepository.findAll().flatMap { listOf(it.toString()) }
                        protectedExecute(botMenuFunction.createDefaultBackup(stringChatId, intMessageId,
                                config.userBackupTitle, usersList))
                    }

                    callBackData == callData_defaultClientBackup -> {
                        val clientsList: List<String> = clientRepository.findAll().flatMap { listOf(it.toString()) }
                        protectedExecute(botMenuFunction.createDefaultBackup(stringChatId, intMessageId,
                        config.clientBackupTitle, clientsList))
                    }

                    callBackData == callData_clientBaseMenu -> { // и редактировать данные
                        val editMessageText = botMenuFunction.receiveFindClientKeyboard(stringChatId,
                                intMessageId, text_findClient, callData_clientForSettingsMenu)
                        protectedExecute(editMessageText)
                    }


                    callBackData.contains(callData_clientForSettingsMenu) -> {
                        val dataText = callBackData.replace(callData_clientForSettingsMenu, "")
                        protectedExecute(botMenuFunction.receiveFindClientMenu(startMessageId,
                        stringChatId, longChatId, dataText, callData_clientSettingMenu, clientRepository.findAll()))
                    }

                    callBackData.contains(callData_delMessage) -> {
                        val deleteMessage = DeleteMessage()
                        deleteMessage.putData(stringChatId, intMessageId)
                        protectedExecute(deleteMessage)
                    }

                    callBackData == callData_setAppointment -> { // receiveFindClientKeyboard
                        val editMessageText = botMenuFunction.receiveFindClientKeyboard(stringChatId,
                                intMessageId, text_findClientToAppointment, callData_clientForAppointment)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = findClient
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

                    callBackData == callData_messageToUser -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_messageToUser)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Назад в меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputMessageForUser
                    }

                    callBackData == callData_messageToAllUsers -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_messageToAllUsers)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Назад в меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputMessageForAll
                    }

                    callBackData == callData_messageToMainMenu -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_forMainMenuMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Назад в меню", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputTextForStartMessage
                    }

                    callBackData == callData_backupMenu -> {
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, text_backupToChat)
                        val menuList = listOf(callData_sendUserBackupLists, callData_backupToChat,
                        callData_saveClientBackup, callData_saveUserBackup, callData_backupInClient, callData_backupInUser,
                        callData_delUserServer, callData_mainMenu)
                        editMessageText.replyMarkup = botMenuFunction.createButtonSet(menuList)
                        protectedExecute(editMessageText)
                    }

                    callBackData == callData_saveUserBackup -> {
                        val backupDirectory = config.defaultDirectory + config.userBackupTitle
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId,
                                "$text_backupDataPartOne$backupDirectory$text_backupDataPartTwo")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню",
                                callData_mainMenu, "Создать", callData_defaultUserBackup)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputSaveUserBackup
                    }

                    callBackData == callData_saveClientBackup -> {
                        val backupDirectory = config.defaultDirectory + config.clientBackupTitle
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId,
                                "$text_backupDataPartOne$backupDirectory$text_backupDataPartTwo")
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню",
                                callData_mainMenu, "Создать", callData_defaultClientBackup)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputSaveClientBackup
                    }

                    callBackData == callData_backupInClient -> {
                        val backupDirectory = config.defaultDirectory + config.clientBackupTitle
                        val textForMessage = "$text_getBackupPartOne$backupDirectory$text_getBackupPartTwo"
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню",
                        callData_mainMenu, "Загрузить файл", callData_loadBackupClient)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputLoadClientBackup
                    }

                    callBackData ==  callData_backupInUser -> {
                        val backupDirectory = config.defaultDirectory + config.userBackupTitle
                        val textForMessage = "$text_getBackupPartOne$backupDirectory$text_getBackupPartTwo"
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, textForMessage)
                        editMessageText.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  В главное меню",
                                callData_mainMenu, "Загрузить файл", callData_loadBackupUser)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputLoadUserBackup
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
                        tempData[stringChatId] = inputUploadBackup
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
                        tempData[stringChatId] = inputClientSecondName
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
                        tempData[stringChatId] = inputSupportMessage
                    }

                    callBackData.contains(callData_repairAccount) -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_repairAccount)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Отмена", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputRepairPassword
                    }

                    callBackData.contains(callData_editeUsername) -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_inputName)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Отмена", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputUserSecondName
                    }

                    callBackData == callData_registration -> {
                        val editMessageText = EditMessageText()
                        editMessageText.putData(stringChatId, intMessageId, text_inputName)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu(
                                "\uD83D\uDD19  Отмена", callData_mainMenu)
                        protectedExecute(editMessageText)
                        tempData[stringChatId] = inputUserSecondName
                    }

                    callBackData.contains(callData_delAllUserData) -> {
                        val userIdData: String = callBackData.replace(callData_delAllUserData , "")
                        val userChatId: Long = if (userIdData.isNotEmpty()) userIdData.toLong() else longChatId
                        clientRepository.findAll().filter { it.specialistId == userChatId }.forEach { clientRepository.delete(it) }
                        userRepository.deleteById(userChatId)
                        val editMessageText = EditMessageText().putData(stringChatId, intMessageId, text_deletedAllUseData)
                        protectedExecute(editMessageText)
                   }

                    callBackData.contains(callData_clientForAppointment) -> {
                        val dataText = callBackData.replace(callData_clientForAppointment, "")
                        protectedExecute(botMenuFunction.receiveFindClientMenu(startMessageId,
                        stringChatId, longChatId, dataText, callData_callBackClientId, clientRepository.findAll()))
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
                        tempData[stringChatId] = inputClientSecondName
                    }

                    callBackData.contains(callData_getBackup) -> {
                        val userId: Long = callBackData.replace(callData_getBackup, "").toLong()
                        val clientList: String = botMenuFunction.receiveClientList(userId, clientRepository)
                        BackupCreator().createClientListTxt(clientList, "${config.backupListDirectory}$userId")
                        protectedExecute(botMenuFunction.receiveBackupList(stringChatId, "${config.backupListDirectory}$userId"))
                    }
                }

            } else if (update.hasPreCheckoutQuery()) {
            val preCheckoutQuery: PreCheckoutQuery = update.preCheckoutQuery
            val preCheckoutQueryId: String = preCheckoutQuery.id
            paymentApprove(preCheckoutQuery, preCheckoutQueryId)
        }
     }


    @Scheduled(cron = "0 0 23 * * *")
    fun actionEveryDayRepeat() {
        botMenuFunction.saveHistoryOfAppointment(clientRepository)
        botMenuFunction.removeClientsAppointment(clientRepository)
        if (LocalDate.now().dayOfWeek == DayOfWeek.MONDAY) sendBackupListToEachUser()
    }


    @Scheduled(cron = "0 0 * * * *")
    fun actionEveryHourRepeat() {
        createBackup()
        sendBackupToAdmin()
        sendApproveMessage()
        registerPassword.clear()
    }


    private fun clientTemporaryData(stringChatId: String){
        tempData[stringChatId] = ""
        comeBackInfo[stringChatId] = ""
        savedId[stringChatId] = 0
    }


    private fun saveCommonUser(longChatId: Long){
        if (userRepository.findById(longChatId).isEmpty) {
            val user = User()
            user.chatId = longChatId
            userRepository.save(user)
        }
    }


    private fun sendMessageForUser(stringChatId: String, updateMessageText: String) {
        protectedExecute(botMenuFunction.sendMessageToUser(updateMessageText))
        protectedExecute(botMenuFunction.sendMessageToAdminNotification(stringChatId, savedMessageId[stringChatId] ?: 0))
    }


    private fun isSubscriptionExpire(longChatId: Long): Boolean {
        val user: User = userRepository.findById(longChatId).get()
        val clientsAmount = clientRepository.findAll().filter { it.specialistId == longChatId }.size
        return ((LocalDate.now().isAfter(LocalDate.parse(user.paymentDate)) && clientsAmount >= config.freeClientsAmount) ||
                clientsAmount == config.maxClientsAmount)
    }


    private fun uploadServerToChat(longChatId: Long, stringChatId: String, backupDirectory: String) {
        tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
        val userBackupMessage: SendDocument = botMenuFunction.sendBackup(longChatId,
                "ℹ  Файл User backup.", backupDirectory + config.userBackupTitle)
        protectedExecute(userBackupMessage)
        val clientBackupMessage: SendDocument = botMenuFunction.sendBackup(longChatId,
                "ℹ  Файл ClientData backup.", backupDirectory + config.clientBackupTitle)
        protectedExecute(clientBackupMessage)
    }


    private fun sendBackupListToEachUser() {
        val backupCreator = BackupCreator()
        userRepository.findAll().filter { it.secondName.isNotEmpty() }.forEach {
            val clientList: String = botMenuFunction.receiveClientList(it.chatId, clientRepository)
            backupCreator.createClientListTxt(clientList, "${config.backupListDirectory}${it.chatId}")
            protectedExecute(botMenuFunction.receiveBackupList(it.chatId.toString(),
                    "${config.backupListDirectory}${it.chatId}"))
        }
    }


    private fun setTextIntoStartMessage(stringChatId: String, updateMessageText: String) {
        tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
        textForStartMessage = if (updateMessageText.length > 3) "$updateMessageText\n\n" else ""
        val savedMessageId: Int = savedMessageId[stringChatId] ?: 0
        val editMessageText = EditMessageText().putData(stringChatId, savedMessageId,
                "$text_forStartMessage$textForStartMessage")
        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                "\uD83D\uDD19  Назад в меню")
        protectedExecute(editMessageText)
    }


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


    private fun sendBackupToAdmin() {
        if(LocalTime.now().hour == config.createBackupTime){
            val admin: User? = userRepository.findAll().find { it.profession == config.adminUser }
            val adminChatId: Long = admin?.chatId ?: 0

            val userBackupMessage: SendDocument = botMenuFunction.sendBackup(adminChatId,
                    "ℹ  Файл User backup.", config.defaultDirectory + config.userBackupTitle)
            protectedExecute(userBackupMessage)
            val clientBackupMessage: SendDocument = botMenuFunction.sendBackup(adminChatId,
                    "ℹ  Файл ClientData backup.", config.defaultDirectory + config.clientBackupTitle)
            protectedExecute(clientBackupMessage)
        }
    }


    private fun createBackup() {
        if(LocalTime.now().hour == config.createBackupTime){
            val backupCreator = BackupCreator()
            val userList: List<String> = userRepository.findAll().flatMap { listOf(it.toString()) }
            val clientList: List<String> = clientRepository.findAll().flatMap { listOf(it.toString()) }

            val usersBackupFile: String = backupCreator.receiveBackupFile(config.userXmlGroupTitle, userList)
            backupCreator.createBackupXml(usersBackupFile, config.defaultDirectory + config.userBackupTitle)

            val backupFile = backupCreator.receiveBackupFile(config.clientXmlGroupTitle, clientList)
            backupCreator.createBackupXml(backupFile, config.defaultDirectory + config.clientBackupTitle)
        }
    }


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
                                    "$callData_cancelAppointment${user.chatId}#${client.clientId}", "✅  Подтвердить",
                                    "$callData_approveAppointment${user.chatId}#${client.clientId}")
                            protectedExecute(sendMessage)
                        }
                    }
                }
            }
        }
    }


    private fun sendStartMessage(stringChatId: String, longChatId: Long) {
        clientTemporaryData(stringChatId)
        saveCommonUser(longChatId)

        if (savedMessageId[stringChatId] != null) protectedExecute(DeleteMessage().putData(stringChatId, savedMessageId[stringChatId]!!))
        val user: User = userRepository.findById(longChatId).get()

        when{
            user.secondName.isEmpty() -> {
                savedMessageId[stringChatId] = protectedExecute(botMenuFunction.receiveClientMessage(stringChatId))
            }
            user.profession == config.adminUser -> {
                savedMessageId[stringChatId] = protectedExecute(botMenuFunction.receiveAdministratorSendMessage(stringChatId, textForStartMessage,
                        savedMessageId.size, userRepository.findAll(), clientRepository.findAll()))
            }
            else -> {
                val sendMessage: SendMessage = botMenuFunction.receiveSpecialistSendMessage(longChatId, stringChatId,
                        textForStartMessage, clientRepository.findAll())
                savedMessageId[stringChatId] = protectedExecute(sendMessage)
            }
        }
    }


    private fun sendEditStartMessage(stringChatId: String, longChatId: Long, intMessageId: Int) {
        val user: User = userRepository.findById(longChatId).get()

        when{
            user.secondName.isEmpty() -> {
                protectedExecute(botMenuFunction.receiveClientEditMessage (stringChatId, intMessageId))
            }
            user.profession == config.adminUser -> {
                savedMessageId[stringChatId] = protectedExecute(botMenuFunction.receiveAdministratorSendMessage(stringChatId,
                        textForStartMessage, savedMessageId.size, userRepository.findAll(), clientRepository.findAll()))
            }
            else -> {
                val editMessageText: EditMessageText = botMenuFunction.receiveSpecialistEditMessage(longChatId,
                        stringChatId, intMessageId, textForStartMessage, clientRepository.findAll())
                protectedExecute(editMessageText)
            }
        }
    }


    private fun messageForAllUsers(updateMessageText: String, stringChatId: String) {
        tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
        if (updateMessageText.length > 3) {
            for (user in userRepository.findAll()){
                val sendMessage = SendMessage(user.chatId.toString(), "$text_fromAdminMessage\n$updateMessageText")
                sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", callData_delMessage)
                protectedExecute(sendMessage)
            }
        } else {
            val sendMessage = SendMessage(stringChatId, text_fromAdminMessageNotSend )
            sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A", callData_delMessage)
            protectedExecute(sendMessage)
        }
    }


    private fun setFirstname(stringChatId: String, updateMessageText: String) {
        tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
        val firstNameText = updateMessageText.replace(".", "").trim()
        val editMessageText = EditMessageText()
        val savedMessageId: Int = savedMessageId[stringChatId] ?: 0

        if (firstNameText.length > 15) {
            registerPassword[stringChatId] = 0
            editMessageText.putData(stringChatId, savedMessageId, text_nameTooLong)
        } else {
            firstName[stringChatId] = firstNameText
            editMessageText.putData(stringChatId, savedMessageId, text_inputPatronymic)
            tempData[stringChatId] = inputPatronymic
        }
        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена", callData_mainMenu)
        protectedExecute(editMessageText)
    }


    private  fun setPatronymic(stringChatId: String, longChatId: Long, updateMessageText: String) {
        tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
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


    private fun setUserDataInDB(stringChatId: String, longChatId: Long, updateMessageText: String) {
        tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
        val admin: User? = userRepository.findAll().find { it.profession == config.adminUser }
        val user: User = userRepository.findById(longChatId).get()
        val editMessageText = EditMessageText()
        val savedMessageId: Int = savedMessageId[stringChatId] ?: 0

        if (updateMessageText == config.adminUser && admin != null || updateMessageText.length > 20) {
            editMessageText.putData(stringChatId, this.savedMessageId[stringChatId]!!, text_wrongProfession)
        } else {
            val date = LocalDate.now()
            val nextPaymentDate = date.plusMonths(3)

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


    private fun registerClientByPassword(stringChatId: String, longChatId: Long, intMessageId: Int, updateMessageText: String) {
        if (registerPassword.isNotEmpty()){
            for ((key, value) in registerPassword){
                if(value.toString() == updateMessageText) {
                    protectedExecute(DeleteMessage().putData(stringChatId, intMessageId))
                    val sendMessage = SendMessage(stringChatId,  text_passwordOk)
                    sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A",
                            callData_delMessage)
                    protectedExecute(sendMessage)

                    val savedMessageId: Int = savedMessageId[key] ?: 0
                    val editMessageText = EditMessageText()

                    if (clientIdExistCheck[key].isNullOrEmpty()) { // этот блок для регистрации нового клиента, которого ещё нет в базе
                        editMessageText.putData(key, savedMessageId, text_passwordApprove)
                        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена",
                                callData_mainMenu)
                        tempData[key] = inputClientSecondName
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


    private fun sendHelpMessage(stringChatId: String, longChatId: Long) {
        val messageId: Int = savedMessageId[stringChatId] ?: 0
        clientTemporaryData(stringChatId)
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


    private fun deleteUserData(stringChatId: String, longChatId: Long) {
        val messageId: Int = savedMessageId[stringChatId] ?: 0
        clientTemporaryData(stringChatId)
        saveCommonUser(longChatId)
        if (savedMessageId[stringChatId] != null) protectedExecute(DeleteMessage().putData(stringChatId, messageId))
        val sendMessage = SendMessage(stringChatId, text_approveDelData)
        sendMessage.replyMarkup = botMenuFunction.receiveTwoButtonsMenu("\uD83D\uDD19  Отмена",
                callData_mainMenu, "Удалить данные", callData_delAllUserData)
        savedMessageId[stringChatId] = protectedExecute(sendMessage)
    }


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
            sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A",
                    callData_delMessage)
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
            sendMessage.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A",
                    callData_delMessage)
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
        clientEditMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A",
                callData_delMessage)
        protectedExecute(clientEditMessageText)
    }



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
        clientEditMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD835\uDC0E\uD835\uDC0A",
                callData_delMessage)
        protectedExecute(clientEditMessageText)
    }


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
                tempData[stringChatId] = inputProfession
            }
        }
        editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Назад в меню",
                callData_mainMenu)
        protectedExecute(editMessageText)
        savedId[stringChatId] = 0
        registerPassword[stringChatId] = 0
    }

    // добавление клиента из меню
    private fun checkTextContentForRegister(stringChatId: String, longChatId: Long, updateMessageText: String, tempDataText: String) {
        tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
        val editMessageText = EditMessageText()
        val startMessageId: Int = savedMessageId[stringChatId] ?: 0

        when  {
           updateMessageText.split(" ").size == 3 -> {
               when (tempDataText){
                   inputUserSecondName ->  createNewUserFromChat(stringChatId, longChatId, updateMessageText)
                   inputClientSecondName -> checkDoubleOfClient(stringChatId, longChatId, updateMessageText)
               }
            }

            updateMessageText.length <= 15 && !updateMessageText.contains(" ") -> {
                val secondNameText = updateMessageText.replace("Ё", "Е").replace("ё", "Е")
                secondName[stringChatId] = secondNameText
                editMessageText.putData(stringChatId, startMessageId, text_setName)
                editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  Отмена",
                        callData_mainMenu)
                protectedExecute(editMessageText)
                tempData[stringChatId] = inputFirstName
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


    private fun createNewUserFromChat(stringChatId: String, longChatId: Long, updateMessageText: String){
        val splitUpdateMessage = updateMessageText.split(" ")
        val secondNameText = splitUpdateMessage[0].replace(".", "").replace("Ё", "Е").trim()
        val firstNameText = splitUpdateMessage[1].replace(".", "").trim()
        val patronymicText = splitUpdateMessage[2].replace(".", "").trim()
        checkFullNameLength(stringChatId, longChatId, secondNameText, firstNameText, patronymicText)
    }


    private fun checkDoubleOfClient(stringChatId: String, longChatId: Long, updateMessageText: String) {
        tempData[stringChatId] = "TEMP_DATA_TO_AVOID_FIND_CLIENT_BLOCK"
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


    // добавление клиента из меню
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


    private fun paymentApprove(preCheckoutQuery: PreCheckoutQuery, preCheckoutQueryId: String) {
        val answerPreCheckoutQuery = AnswerPreCheckoutQuery()
        val user: User = userRepository.findById(preCheckoutQuery.invoicePayload.toLong()).get()
        val userPaymentDate: LocalDate = LocalDate.parse(user.paymentDate)
        val localDate: LocalDate = LocalDate.now()
        val editMessageText: EditMessageText

        when{
            localDate.isBefore(userPaymentDate.minusDays(config.paymentBefore)) -> answerPreCheckoutQuery.ok = false

            localDate.isAfter(userPaymentDate) -> {
                answerPreCheckoutQuery.ok = true
                user.paymentDate = localDate.plusDays(config.subscriptionDays).toString()
                userRepository.save(user)
                editMessageText = EditMessageText().putData(preCheckoutQuery.invoicePayload,
                        savedMessageId[preCheckoutQuery.invoicePayload]!!, text_paymentSuccess)
                editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                        callData_startMenu)
                protectedExecute(editMessageText)
            }

            else -> {
                answerPreCheckoutQuery.ok = true
                user.paymentDate = userPaymentDate.plusDays(config.subscriptionDays).toString()
                userRepository.save(user)
                editMessageText = EditMessageText().putData(preCheckoutQuery.invoicePayload,
                        savedMessageId[preCheckoutQuery.invoicePayload]!!, text_paymentSuccess)
                editMessageText.replyMarkup = botMenuFunction.receiveOneButtonMenu("\uD83D\uDD19  В главное меню",
                        callData_startMenu)
                protectedExecute(editMessageText)
            }
        }
        answerPreCheckoutQuery.preCheckoutQueryId = preCheckoutQueryId
        answerPreCheckoutQuery.errorMessage = text_paymentWrong
        protectedExecute(answerPreCheckoutQuery)
    }


    private fun paySubscription(longChatId: Long, stringChatId: String, intMessageId: Int) {
        val editMessageText: EditMessageText
        val isPayed: Boolean = LocalDate.now().isBefore(LocalDate.parse(userRepository.findById(longChatId).
        get().paymentDate).minusDays(config.paymentBefore))

        val textForMessage = "$text_paymentTextOne${config.subscriptionDays}$text_paymentTextTwo" +
                "${config.subscriptionPrice}$text_paymentTextThree${config.maxClientsAmount}$text_paymentTextFour" +
                "${config.freeClientsAmount}$text_paymentTextFive${config.paymentBefore}$text_paymentTextSix"

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

/*
    @Scheduled(cron = "0 * * * * *")
    fun testCron() {

    }
 */

}

const val qSym = " ？"
const val wqSym = "❔"
const val xSym = "✖"
const val okSym = "✔"

const val findClient = "FIND_CLIENT"
const val inputRemark = "INPUT_REMARK"
const val inputPassword = "INPUT_PASSWORD"
const val inputFirstName = "INPUT_FIRST_NAME"
const val inputProfession = "INPUT_PROFESSION"
const val inputPatronymic = "INPUT_PATRONYMIC"
const val inputChangeUser = "INPUT_CHANGE_USER"
const val inputOldPassword = "INPUT_OLD_PASSWORD"
const val inputUploadBackup = "INPUT_UPLOAD_BACKUP"
const val inputMessageForAll = "INPUT_MESSAGE_FOR_ALL"
const val inputRepairPassword = "INPUT_REPAIR_PASSWORD"
const val inputSupportMessage = "INPUT_SUPPORT_MESSAGE"
const val inputMessageForUser = "INPUT_MESSAGE_FOR_USER"
const val inputLoadUserBackup = "INPUT_LOAD_USER_BACKUP"
const val inputSaveUserBackup = "INPUT_SAVE_USER_BACKUP"
const val inputUserSecondName = "INPUT_USER_SECOND_NAME"
const val inputSaveClientBackup = "INPUT_SAVE_CLIENT_BACKUP"
const val inputLoadClientBackup = "INPUT_LOAD_CLIENT_BACKUP"
const val inputClientSecondName = "INPUT_CLIENT_SECOND_NAME"
const val inputTextForStartMessage = "INPUT_FOR_START_MESSAGE"

const val callData_dayUp = "#dayup"
const val callData_timeUp = "#timeup"
const val callData_zoneUp = "#zoneup"
const val callData_myData = "#mydata"
const val callData_dayDown = "#daydwn"
const val callData_changeUser = "#usr"
const val callData_startBot = "/start"
const val callData_myClients = "#mycli"
const val callData_startMenu = "#start"
const val callData_appointmentDay = "@"
const val callData_zoneDown = "#zonedwn"
const val callData_helpCommand = "/help"
const val callData_appointmentHour = "&"
const val callData_timeDown = "#timedwn"
const val callData_registration = "#reg"
const val callData_delMessage = "#delmes"
const val callData_allClients = "#allcli"
const val callData_clientData = "#cldata"
const val callData_findClient = "#getmenu"
const val callData_appointmentMin = "#hou"
const val callData_deleteClient = "#delcli"
const val callData_paymentMenu = "#payment"
const val callData_getBackup = "#getbackup"
const val callData_cleanChat = "#cleanchat"
const val callData_callBackClientId = "#clid"
const val callData_backupMenu = "Backup меню"
const val callData_deleteUser = "/deletedata"
const val callData_delClientRemark = "#delrem"
const val callData_cancelAppointment = "#disapp"
const val callData_messageToSupport = "#support"
const val callData_delAllUserData = "#delmydata"
const val callData_specMenu = "Меню специалиста"
const val callData_cleanChatMenu = "Очистить чат"
const val callData_clientSettingMenu = "#getmenu"
const val callData_approveAppointment = "#approve"
const val callData_clientRemark = "Заметки клиента"
const val callData_chatBackup = "#defaultchatbackup"
const val callData_loadBackupUser = "#loadbackupuse"
const val callData_clientForAppointment = "#findcli"
const val callData_setPassword = "Установить пароль"
const val callData_delServerMenu = "❗ Удалить сервер"
const val callData_clientForSettingsMenu = "#finmenu"
const val callData_setFullNameInDb = "#acceptregister"
const val callData_loadBackupClient = "#loadbackupcli"
const val callData_setAppointment = "Записать на приём"
const val callData_myAccount = "⚙  Моя учетная запись"
const val callData_deleteClientMenu = "Удалить клиента"
const val callData_cancelClientAppointment = "#cancapp"
const val callData_defaultUserBackup = "#createservuse"
const val callData_backupInUser = "Backup в user сервер"
const val callData_repairAccount = "Восстановить аккаунт"
const val callData_defaultClientBackup = "#createservcli"
const val callData_removeAppointment = "Выписать клиента"
const val callData_editeUsername = "Редактировать мои ФИО"
const val callData_mainMenu = "\uD83D\uDD19  Назад в меню"
const val callData_delUserServer = "❗ Удалить user сервер"
const val callData_backupToChat = "Выгрузить backup в чат"
const val callData_editeUser = "Редактировать пользователя"
const val callData_messageToUser = "Сообщение пользователю"
const val callData_messageToMainMenu = "StartMessage текст"
const val callData_saveUserBackup = "Сохранить user_backup"
const val callData_backupInClient = "Backup в client сервер"
const val callData_delClientServer = "❗ Удалить client сервер"
const val callData_saveClientBackup = "Сохранить client_backup"
const val callData_appointmentToMe = "Посмотреть запись ко мне"
const val callData_editeClientName = "Редактировать ФИО клиента"
const val callData_generateCode = "Генерировать код для клиента" // "Генерировать код и добавить клиента"
const val callData_addCommonClient = "Добавить клиента без кода" // Добавить клиента без Telegram
const val callData_appointmentToSpec = "Моя запись к специалисту"
const val callData_regAsSpec = "Зарегистрироваться как специалист"
const val callData_messageToAllUsers = "Сообщение всем пользователям"
const val callData_addNewClient =  "Добавить нового клиента/пациента"
const val callData_clientBaseMenu = "Работа с базой клиентов/пациентов"
const val callData_myAppointment = "\uD83D\uDCC5  Посмотреть мою запись"
const val callData_sendUserBackupLists = "Отправить backup пользователям"














