package aldmitry.dev.personalmanager.config

import lombok.Data
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Data
@Configuration
@EnableScheduling
class Config {

    final val botUsername = ""
    final val botToken = "" // - тест
    final val userBackupTitle = "user_backup.xml"
    final val userXmlGroupTitle = "users"
    final val clientBackupTitle = "client_backup.xml"
    final val clientXmlGroupTitle = "clients"
    final val defaultDirectory = "C:/Users/admit/OneDrive/Рабочий стол/" // "C:/Users/admit/OneDrive/Рабочий стол/"   "/home/dmitry/Documents/Personal_manager/"
    final val adminUser = "admin"
    final val paymentBefore = 7L
    final val freeClientsAmount = 4
    final val createBackupTime = 22
    final val maxClientsAmount = 500
    final val subscriptionDays = 30L
    final val subscriptionPrice = 100
    final val payToken = ""
    final val backupListDirectory = "C:/Users/admit/OneDrive/Рабочий стол/backup_"  //  "C:/Users/admit/OneDrive/Рабочий стол/backup_"    "/home/dmitry/Documents/Personal_manager/"
    final val bill = "{\"Текст\": \"Текст\",\"Число\": 12345}" // JSON-сериализованные данные о счете-фактуре, которые будут переданы поставщику платежей.



}