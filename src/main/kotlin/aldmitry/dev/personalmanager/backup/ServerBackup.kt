package aldmitry.dev.personalmanager.backup

import aldmitry.dev.personalmanager.model.ClientData
import aldmitry.dev.personalmanager.model.User
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import javax.xml.parsers.SAXParserFactory

private val clientsDataList = mutableListOf<ClientData>()
private val usersList = mutableListOf<User>()

class ServerBackup : DefaultHandler() {

    fun startBackup(directory: String) {
        val factory = SAXParserFactory.newInstance()
        val parser = factory.newSAXParser()
        val serverBackup = ServerBackup()
        parser.parse(File(directory), serverBackup) // TODO посмотреть File
    }


    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        when (qName){
            "client" -> {
                val clientData = ClientData()
                clientData.clientId = attributes.getValue("clientId").toLong()
                clientData.chatId = attributes.getValue("chatId").toLong()
                clientData.specialistId = attributes.getValue("specialistId").toLong()
                clientData.firstName = attributes.getValue("firstName")
                clientData.secondName = attributes.getValue("secondName")
                clientData.patronymic = attributes.getValue("patronymic")
                clientData.visitAgreement = attributes.getValue("visitAgreement")
                clientData.appointmentDate = attributes.getValue("appointmentDate")
                clientData.appointmentTime = attributes.getValue("appointmentTime")
                clientData.remark = attributes.getValue("remark")
                clientData.visitHistory = attributes.getValue("visitHistory")
                clientsDataList.add(clientData)
            }

            "user" -> {
                val user = User()
                user.chatId = attributes.getValue("chatId").toLong()
                user.password = attributes.getValue("password")
                user.profession = attributes.getValue("profession")
                user.firstName = attributes.getValue("firstName")
                user.secondName = attributes.getValue("secondName")
                user.patronymic = attributes.getValue("patronymic")
                user.sendTime = attributes.getValue("sendTime").toInt()
                user.timeZone = attributes.getValue("timeZone").toLong()
                user.paymentDate = attributes.getValue("paymentDate")
                user.sendBeforeDays = attributes.getValue("sendBeforeDays").toLong()
                usersList.add(user)
            }
        }
    }


    fun receiveClientsBackup(clients: MutableList<ClientData>) : MutableList<ClientData> {
        clients.addAll(clientsDataList)
        return clients
    }

    fun receiveUsersBackup(users: MutableList<User>) : MutableList<User> {
        users.addAll(usersList)
        return users
    }

}