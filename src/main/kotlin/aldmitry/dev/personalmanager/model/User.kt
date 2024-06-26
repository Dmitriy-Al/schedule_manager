package aldmitry.dev.personalmanager.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id

// Любой пользователь бота автоматически становится user; user может авторизоваться и получить права специалиста
@Entity(name = "user_data")
open class User {

    @Id
    @Column(name = "chat_id")
    open var chatId: Long = 0

    @Column(name = "send_time")
    open var sendTime: Int = 12

    @Column(name = "time_zone")
    open var timeZone: Long = 0

    @Column(name = "password")
    open var password: String = ""

    @Column(name = "profession")
    open var profession: String = ""

    @Column(name = "first_name")
    open var firstName: String = ""

    @Column(name = "second_name")
    open var secondName: String = ""

    @Column(name = "patronymic")
    open var patronymic: String = ""

    @Column(name = "payment_date")
    open var paymentDate: String = ""

    @Column(name = "send_before_days")
    open var sendBeforeDays: Long = 1


    fun getFullName(): String {
        return "$secondName $firstName $patronymic"
    }


    override fun toString(): String {
        return "<user chatId=\"$chatId\" secondName=\"$secondName\" firstName=\"$firstName\" patronymic=\"$patronymic\" " +
                "password=\"$password\" profession=\"$profession\" sendTime=\"$sendTime\" timeZone=\"$timeZone\" " +
                "paymentDate=\"$paymentDate\" sendBeforeDays=\"$sendBeforeDays\" />"
    }


}