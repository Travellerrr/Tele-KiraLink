package cn.travellerr.onebotApi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMessage(
    @SerialName("time")
    val time: Long,

    @SerialName("self_id")
    val self_id: Long,

    @SerialName("post_type")
    val post_type: String,

    @SerialName("message_type")
    val message_type: String,

    @SerialName("sub_type")
    val sub_type: String,

    @SerialName("message_id")
    val message_id: Int,

    @SerialName("group_id")
    val group_id: Long,

    @SerialName("user_id")
    val user_id: Long,

    @SerialName("anonymous")
    val anonymous: Anonymous?,


    @SerialName("raw_message")
    val raw_message: String,

    @SerialName("font")
    val font: Int,

    @SerialName("sender")
    val sender: Sender? = null
)

@Serializable
data class PrivateMessage(
    @SerialName("time")
    val time: Long,

    @SerialName("self_id")
    val self_id: Long,

    @SerialName("post_type")
    val post_type: String,

    @SerialName("message_type")
    val message_type: String,

    @SerialName("sub_type")
    val sub_type: String,

    @SerialName("message_id")
    val message_id: Int,

    @SerialName("user_id")
    val user_id: Long,

    @SerialName("raw_message")
    val raw_message: String,

    @SerialName("font")
    val font: Int,

    @SerialName("sender")
    val sender: Sender? = null,

)

@Serializable
data class Data(
    val echo: String,
    val message: String? = null,
    val retcode: Int = 0,
    val status: String = "ok",
    val wording: String? = null
) {
    constructor(echo: String) : this(echo, null, 0, "ok", null)

    constructor(echo: String, status: Boolean) : this(echo, null, 0, if (status) "ok" else "failed", null)

    companion object {

        fun parse(json: String): Data {
            return Json.decodeFromString(serializer(), json)
        }
    }

    override fun toString() : String {
        return Json.encodeToString(serializer(), this)
    }
}


interface ArrayMessage{
    val type: String
}

@Serializable
data class Text(
    override val type: String = "text",
    val data: TextData
) : ArrayMessage {
    constructor(text: String) : this("text", TextData(text))
    override fun toString() : String {
        return Json.encodeToString(serializer(), this)
    }
}

@Serializable
data class At(
    override val type: String = "at",
    val data: AtData
) : ArrayMessage {
    constructor(userId: Long) : this("at", AtData(userId))
}

@Serializable
data class Image(
    override val type: String = "image",
    val data: File
) : ArrayMessage {
    constructor(file: String) : this("image", File(file))
}

@Serializable
data class Reply(
    override val type: String = "reply",
    val data: Id
) : ArrayMessage {
    constructor(messageId: Long) : this("reply", Id(messageId))
}