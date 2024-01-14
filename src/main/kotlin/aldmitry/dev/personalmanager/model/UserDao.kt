package aldmitry.dev.personalmanager.model

import org.springframework.data.repository.CrudRepository

interface UserDao : CrudRepository<User, Long?> {

}