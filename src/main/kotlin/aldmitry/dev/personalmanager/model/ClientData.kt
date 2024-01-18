package aldmitry.dev.personalmanager.model

import jakarta.persistence.*

@Entity(name = "client_data")
open class ClientData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "client_id")
    open var clientId: Long = 0

    @Column(name = "chat_id")
    open var chatId: Long = 0

    @Column(name = "specialist_id")
    open var specialistId: Long = 0

    @Column(name = "first_name")
    open var firstName: String = ""

    @Column(name = "second_name")
    open var secondName: String = ""

    @Column(name = "patronymic")
    open var patronymic: String = ""

    @Column(name = "phone_number")
    open var phoneNumber: String = ""

    @Column(name = "visit_agreement")
    open var visitAgreement: String = "❔"

    @Column(name = "appointment_date")
    open var appointmentDate: String = ""

    @Column(name = "appointment_time")
    open var appointmentTime: String = ""

    @Column(name = "remark", columnDefinition = "text")
    open var remark: String = ""



}