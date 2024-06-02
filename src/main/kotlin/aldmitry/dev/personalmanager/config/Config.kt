package aldmitry.dev.personalmanager.config

import lombok.Data
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import javax.xml.parsers.SAXParserFactory

@Data
@Configuration
@EnableScheduling
class Config : DefaultHandler() {

    final val adminUser = "" // профессия специалиста, дающая права администрирования
    final val botUsername = "" // имя бота
    final val botToken = "5684975537:"  // токен бота

    // Загрузка настроек конфигурации мз xml-файла в заданной директории settingsFileDirectory
    fun loadSettings(directory: String) {
        if (directory.length > 4) settingsFileDirectory = directory
        val factory = SAXParserFactory.newInstance()
        val parser = factory.newSAXParser()
        parser.parse(File(settingsFileDirectory), Config())
    }

    // Настройки конфигурации устанавливаются из xml-файла вместо настроек по умолчанию
    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        if (qName == "config") {
            config_payCard = attributes.getValue("payCard")
            config_payToken = attributes.getValue("payToken")
            config_backupDirectory = attributes.getValue("backupDirectory")
            config_userBackupTitle = attributes.getValue("userBackupTitle")
            config_trialPeriod = attributes.getValue("trialPeriod").toLong()
            config_clientBackupTitle = attributes.getValue("clientBackupTitle")
            config_userXmlGroupTitle = attributes.getValue("userXmlGroupTitle")
            config_paymentBefore = attributes.getValue("paymentBefore").toLong()
            config_clientXmlGroupTitle = attributes.getValue("clientXmlGroupTitle")
            config_backupListDirectory = attributes.getValue("backupListDirectory")
            config_createBackupTime = attributes.getValue("createBackupTime").toInt()
            config_maxClientsAmount = attributes.getValue("maxClientsAmount").toInt()
            config_subscriptionDays = attributes.getValue("subscriptionDays").toLong()
            config_freeClientsAmount = attributes.getValue("freeClientsAmount").toInt()
            config_subscriptionPrice = attributes.getValue("subscriptionPrice").toInt()
        }
    }
}


// Настройки конфигурации по умолчанию
var config_payCard = "5469 5500 7590 371"
var settingsFileDirectory = "/home/dmitry/Documents/Personal_manager/settings.xml" // путь к директории xml-файла с настройками конфигурации приложения
var config_payToken = "381764678" // токен платежной системы
var config_trialPeriod = 6L // срок (в месяцах), в течение которого бот сохраняет полный функционал без внесения абонентской платы
var config_paymentBefore = 7L // срок (в днях) до истечения срока действующего абонемента, когда оплата абонемента на следующий период становится доступной
var config_freeClientsAmount = 50 // количество клиентов, которое можно добавить без оплаты абонемента
var config_maxClientsAmount = 1000 // максимальное количество клиентов, которое можно добавить
var config_subscriptionDays = 30L // срок действия абонемента (в днях)
var config_subscriptionPrice = 50 // стоимость абонемента (в рублях)
var config_createBackupTime = 22 // время в которое администратору отправляется backup (час Мск.)
var config_userBackupTitle = "user_backup.xml" // имя backup-файла сервера user
var config_clientBackupTitle = "client_backup.xml" // имя backup-файла сервера client
var config_userXmlGroupTitle = "users" // тег в backup-файле сервера user
var config_clientXmlGroupTitle = "clients" // тег в backup-файле сервера client
var config_backupDirectory = "/home/dmitry/Documents/Personal_manager/" // директория backup-файла xml. по умолчанию   "C:/Users/admit/OneDrive/Рабочий стол/"   "/home/dmitry/Documents/Personal_manager/"
var config_backupListDirectory = "/home/dmitry/Documents/Personal_manager/" // директория файла-списка пациентов в формате txt. по умолчанию      "C:/Users/admit/OneDrive/Рабочий стол/backup_"    "/home/dmitry/Documents/Personal_manager/"



