package dres.data.serializers

import dres.data.model.admin.HashedPassword
import dres.data.model.admin.Role
import dres.data.model.admin.User
import dres.data.model.admin.UserName
import dres.utilities.extensions.UID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object UserSerializer : Serializer<User> {
    override fun serialize(out: DataOutput2, value: User) {
        out.writeUTF(value.id.string)
        out.writeUTF(value.username.name)
        out.writeUTF(value.password.hash)
        out.writeInt(value.role.ordinal)
    }

    override fun deserialize(input: DataInput2, available: Int): User = User(
            input.readUTF().UID(),
            UserName(input.readUTF()),
            HashedPassword(input.readUTF()),
            Role.values()[input.readInt()]
    )
}