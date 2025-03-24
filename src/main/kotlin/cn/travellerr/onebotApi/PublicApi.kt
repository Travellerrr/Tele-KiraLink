package cn.travellerr.onebotApi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

val Json = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

interface OnebotPublicApi {
    companion object {
        const val API = ""

        fun parse(json: String): OnebotPublicApi {
            return Json.decodeFromString(serializer(), json)
        }
    }
}


@Serializable
data class GetGroupInfo(
    @SerialName("group_id")
    val groupId: Long,

    @SerialName("no_cache")
    val noCache: Boolean
) : OnebotPublicApi{
    companion object {
        const val API = "/get_group_info"

        fun parse(json: String): GetGroupInfo {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class SendMessage(
    @SerialName("message_type")
    val messageType: String,

    @SerialName("user_id")
    val userId: Long? = null,

    @SerialName("group_id")
    val groupId: Long? = null,

    @SerialName("message")
    val message: String

) : OnebotPublicApi{
    companion object {
        const val API = "/send_msg"

        fun parse(json: String): SendMessage {
            return Json.decodeFromString(serializer(), json)
        }
    }
}


@Serializable
data class GetGroupMemberInfo(
    @SerialName("group_id")
    val groupId: Long,

    @SerialName("user_id")
    val userId: Long,

    @SerialName("no_cache")
    val noCache: Boolean
) : OnebotPublicApi{
    companion object {
        const val API = "/get_group_member_info"

        fun parse(json: String): GetGroupMemberInfo {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class GetGroupMemberList(
    @SerialName("group_id")
    val groupId: Long
) : OnebotPublicApi{
    companion object {
        const val API = "/get_group_member_list"

        fun parse(json: String): GetGroupMemberList {
            return Json.decodeFromString(serializer(), json)
        }
    }
}