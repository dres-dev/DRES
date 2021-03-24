package dev.dres.data.serializers

import dev.dres.data.model.competition.TaskType
import dev.dres.data.model.competition.options.*
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer

object TaskTypeSerializer : Serializer<TaskType> {

    override fun serialize(out: DataOutput2, value: TaskType) {
        out.writeUTF(value.name)
        out.packLong(value.taskDuration)
        //out.packInt(value.targetType.ordinal)
        serializeOption(value.targetType, out)
        out.packInt(value.components.size)
        value.components.forEach {
            //out.packInt(it.ordinal)
            serializeOption(it, out)
        }
        //out.packInt(value.score.ordinal)
        serializeOption(value.score, out)
        out.packInt(value.filter.size)
        value.filter.forEach {
            //out.packInt(it.ordinal)
            serializeOption(it, out)
        }
        out.packInt(value.options.size)
        value.options.forEach {
            //out.packInt(it.ordinal)
            serializeOption(it, out)
        }
    }

    override fun deserialize(input: DataInput2, available: Int): TaskType =
        TaskType(
            input.readUTF(),
            input.unpackLong(),
            ConfiguredOption(TargetOption.values()[input.unpackInt()], deserializeMap(input)),
            (0 until input.unpackInt()).map { ConfiguredOption(QueryComponentOption.values()[input.unpackInt()], deserializeMap(input)) },
            ConfiguredOption(ScoringOption.values()[input.unpackInt()], deserializeMap(input)),
            (0 until input.unpackInt()).map { ConfiguredOption(SubmissionFilterOption.values()[input.unpackInt()], deserializeMap(input)) },
            (0 until input.unpackInt()).map { ConfiguredOption(SimpleOption.values()[input.unpackInt()], deserializeMap(input)) }
        )


    private fun serializeOption(option: ConfiguredOption<*>, out: DataOutput2) {
        out.packInt(option.option.ordinal)
        out.packInt(option.parameters.size)
        option.parameters.forEach { (k, v) ->
            out.writeUTF(k)
            out.writeUTF(v)
        }
    }

    private fun deserializeMap(input: DataInput2): Map<String, String> = (0 until input.unpackInt()).map { input.readUTF() to input.readUTF() }.toMap()
}