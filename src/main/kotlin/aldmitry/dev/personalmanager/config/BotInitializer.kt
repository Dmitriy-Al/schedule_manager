package aldmitry.dev.personalmanager.config

import aldmitry.dev.personalmanager.service.InputOutputCommand
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Component
class BotInitializer(@Autowired var inputOutputCommand: InputOutputCommand){

    // Коннект с Telegram
    @EventListener(ContextRefreshedEvent::class)
    fun init() {
        try{
            val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
            telegramBotsApi.registerBot(inputOutputCommand)
        } catch (e: TelegramApiException){
            val logger = LoggerFactory.getLogger("BotInitializer <init>")
            logger.error(e.message)
        }
    }
}