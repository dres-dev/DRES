package dres.data.serializers

import dres.data.model.log.QueryEvent
import dres.data.model.log.QueryEventLog
import dres.data.model.log.QueryResult
import dres.data.model.log.QueryResultLog
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object QueryResultLogSerializer : Serializer<QueryResultLog> {

    override fun serialize(out: DataOutput2, value: QueryResultLog) {
        out.packInt(value.team)
        out.packLong(value.member)
        out.packLong(value.timestamp)

        out.packInt(value.usedCategories.size)
        value.usedCategories.forEach { out.writeUTF(it) }

        out.packInt(value.usedTypes.size)
        value.usedTypes.forEach { out.writeUTF(it) }

        out.packInt(value.sortType.size)
        value.sortType.forEach { out.writeUTF(it) }

        out.writeUTF(value.resultSetAvailability)

        out.packInt(value.results.size)
        value.results.forEach { QueryResultSerializer.serialize(out, it) }

        out.packLong(value.serverTimeStamp)


    }

    override fun deserialize(input: DataInput2, available: Int): QueryResultLog = QueryResultLog(
            input.unpackInt(),
            input.unpackLong(),
            input.unpackLong(),
            (0 until input.unpackInt()).map { input.readUTF() },
            (0 until input.unpackInt()).map { input.readUTF() },
            (0 until input.unpackInt()).map { input.readUTF() },
            input.readUTF(),
            (0 until input.unpackInt()).map { QueryResultSerializer.deserialize(input, available) },
            input.unpackLong()
    )
}

object QueryResultSerializer : Serializer<QueryResult> {

    override fun serialize(out: DataOutput2, value: QueryResult) {
        out.writeUTF(value.video)
        out.packInt(value.shot)
        out.writeDouble(value.score ?: Double.NaN)
        out.packInt(value.rank ?: -1)
    }

    override fun deserialize(input: DataInput2, available: Int): QueryResult = QueryResult(
            input.readUTF(),
            input.unpackInt(),
            input.readDouble().let { if (it.isNaN()) null else it },
            input.unpackInt().let { if (it >= 0) it else null }
    )

}

object QueryEventLogSerializer : Serializer<QueryEventLog> {

    override fun serialize(out: DataOutput2, value: QueryEventLog) {
        out.packInt(value.team)
        out.packLong(value.member)
        out.packLong(value.timestamp)

        out.packInt(value.events.size)
        value.events.forEach { QueryEventSerializer.serialize(out, it) }

        out.packLong(value.serverTimeStamp)

        out.writeUTF(value.type)
    }

    override fun deserialize(input: DataInput2, available: Int): QueryEventLog = QueryEventLog(
            team = input.unpackInt(),
            member = input.unpackLong(),
            timestamp = input.unpackLong(),
            events = (0 until input.unpackInt()).map { QueryEventSerializer.deserialize(input, available) },
            serverTimeStamp = input.unpackLong(),
            type = input.readUTF()
    )

}

object QueryEventSerializer : Serializer<QueryEvent> {

    override fun serialize(out: DataOutput2, value: QueryEvent) {
        out.packLong(value.timestamp)
        out.writeUTF(value.category)
        out.packInt(value.type.size)
        value.type.forEach { out.writeUTF(it) }

        if (value.value != null) {
            out.writeUTF(value.value)
        }
    }

    override fun deserialize(input: DataInput2, available: Int): QueryEvent = QueryEvent(
            input.unpackLong(),
            input.readUTF(),
            (0 until input.unpackInt()).map { input.readUTF() },
            value = if (available > input.pos) {
                input.readUTF()
            } else {
                null
            }
    )


}