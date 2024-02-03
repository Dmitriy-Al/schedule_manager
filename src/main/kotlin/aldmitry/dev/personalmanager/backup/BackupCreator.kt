package aldmitry.dev.personalmanager.backup

import java.io.FileOutputStream

class BackupCreator {

    fun receiveBackupFile(groupTitle: String, attributes: List<String>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<$groupTitle>")
        attributes.forEach { stringBuilder.append("\n   $it") }
        stringBuilder.append("\n</$groupTitle>")
        return stringBuilder.toString()
    }


    fun createBackupXml(backupFile: String, directory: String) {
            val fileOutputStream = FileOutputStream(directory)
            fileOutputStream.write(backupFile.toByteArray())
            fileOutputStream.flush()
            fileOutputStream.close()
    }

}

