package aldmitry.dev.personalmanager.backup

import java.io.FileOutputStream

/**
 * Создание backup-файлов
 */
class BackupCreator {

    // Конфигурация backup-файла xml.
    fun receiveBackupFile(groupTitle: String, attributes: List<String>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<$groupTitle>")
        attributes.forEach { stringBuilder.append("\n   $it") }
        stringBuilder.append("\n</$groupTitle>")
        return stringBuilder.toString()
    }

    // Создание в указанной директории backup-файла xml.
    fun createBackupXml(backupFile: String, directory: String) {
            val fileOutputStream = FileOutputStream(directory)
            fileOutputStream.write(backupFile.toByteArray())
            fileOutputStream.flush()
            fileOutputStream.close()
    }

    // Создание в указанной директории файла со списка клиентов в формате txt.
    fun createClientListTxt(backupFile: String, directory: String) {
        val fileOutputStream = FileOutputStream(directory)
        fileOutputStream.write(backupFile.toByteArray())
        fileOutputStream.flush()
        fileOutputStream.close()
    }

}

