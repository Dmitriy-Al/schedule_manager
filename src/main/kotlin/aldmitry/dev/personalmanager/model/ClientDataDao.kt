package aldmitry.dev.personalmanager.model

import org.springframework.data.repository.CrudRepository

interface ClientDataDao : CrudRepository<ClientData, Long?> {

}