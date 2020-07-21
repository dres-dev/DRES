package dres.utilities.extensions

import dres.data.model.UID
import org.mapdb.DataInput2
import org.mapdb.DataOutput2

fun DataOutput2.writeUID(uid: UID) = this.writeUTF(uid.string)

fun DataInput2.readUID() : UID = this.readUTF().UID()