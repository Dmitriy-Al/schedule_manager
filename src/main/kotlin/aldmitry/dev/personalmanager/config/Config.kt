package aldmitry.dev.personalmanager.config

import lombok.Data
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Data
@Configuration
@EnableScheduling
class Config {

    final val botUsername = "TestDemoUnicNameBot" // personal_client_manager_bot  TestDemoUnicNameBot
    final val botToken = "5684975537:AAHNI1ulaYG9U0ifSlOet3r6DClVoPWlgUk"  // "6866836519:AAGiOazP_Sh9iqIFkT_41GHsYQjbuRf2Xp8"  5684975537:AAHNI1ulaYG9U0ifSlOet3r6DClVoPWlgUk - тест
    final val userBackupTitle = "user_backup.xml"
    final val userXmlGroupTitle = "users"
    final val clientBackupTitle = "client_backup.xml"
    final val clientXmlGroupTitle = "clients"
    final val defaultDirectory = "C:/Users/admit/OneDrive/Рабочий стол/" // "C:/Users/admit/OneDrive/Рабочий стол/"   "/home/dmitry/Documents/Personal_manager/"
    final val adminUser = "admin"
    final val freeClientsAmount = 4
    final val maxClientsAmount = 500
    final val createBackupTime = 22




}