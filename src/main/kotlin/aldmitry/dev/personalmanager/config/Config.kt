package aldmitry.dev.personalmanager.config

import lombok.Data
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Data
@Configuration
@EnableScheduling
class Config {

    final val botUsername = "Test" // personal_client_manager_bot  TestDemoUnicNameBot
    final val botToken = "5684975537"
    final val userBackupTitle = "user_backup.xml"
    final val clientBackupTitle = "client_backup.xml"
    final val defaultDirectory = "C:/Users/admit/OneDrive/Рабочий стол/" // "C:/Users/admit/OneDrive/Рабочий стол/"   "/home/dmitry/Documents/Personal_manager/"
    final val adminUser = "admin"
    final val freeClientsAmount = 50
    final val maxClientsAmount = 500
    final val createBackupTime = 22






}