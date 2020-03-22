package dres

import dres.data.dbo.DAO
import dres.data.model.Config
import dres.data.model.admin.PlainPassword
import dres.data.model.admin.Role
import dres.data.model.admin.User
import dres.data.model.admin.UserName
import dres.data.serializers.UserSerializer
import org.mindrot.jbcrypt.BCrypt
import java.nio.file.Paths

object Playground {

    @JvmStatic
    fun main(args: Array<String>) {

        val config = Config()

        val dao = DAO(Paths.get(config.dataPath + "/users.db"), UserSerializer)

        val user =  User(username = UserName("testuser"), password = PlainPassword("password").hash(), role = Role.ADMIN)

        dao.append(user)

        dao.close()
    }

}