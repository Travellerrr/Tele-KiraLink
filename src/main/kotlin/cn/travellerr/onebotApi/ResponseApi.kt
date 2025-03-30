package cn.travellerr.onebotApi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface OnebotResponseApi

@Serializable
data class GetVersionInfo(
    @SerialName("app_name")
    val app_name: String,

    @SerialName("app_version")
    val app_version: String,

    @SerialName("protocol_version")
    val protocol_version: String
) : OnebotResponseApi{
    companion object {
        const val API = "/get_version_info"

        fun parse(json: String): GetVersionInfo {
            return Json.decodeFromString(serializer(), json)
        }
    }

    override fun toString() : String {
        print(Json.encodeToString(serializer(), this))
        return Json.encodeToString(serializer(), this)
    }
}

@Serializable
data class GetLoginInfo(
    @SerialName("user_id")
    val user_id: Long,

    @SerialName("nickname")
    val nickname: String
) : OnebotResponseApi{
    companion object {
        const val API = "/get_login_info"

        fun parse(json: String): GetLoginInfo {
            return Json.decodeFromString(serializer(), json)
        }
    }

    override fun toString() : String {
        return Json.encodeToString(serializer(), this)
    }
}

@Serializable
data class GetStatus(
    @SerialName("online")
    val online: Boolean,

    @SerialName("good")
    val good: Boolean
) : OnebotResponseApi{
    companion object {
        const val API = "/get_status"

        fun parse(json: String): GetStatus {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class GetFriendList(
    val friend_list: List<Friend>
) : OnebotResponseApi{
    companion object {
        const val API = "/get_friend_list"

        fun parse(json: String): GetFriendList {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class GetGroupList(
    @SerialName("group_list")
    val groupInfo_list: List<GroupInfo>
) : OnebotResponseApi{
    companion object {
        const val API = "/get_group_list"

        fun parse(json: String): GetGroupList {
            return Json.decodeFromString(serializer(), json)
        }
    }
}


@Serializable
data class GetGroupInfoResponse (
    @SerialName("group_id")
    override val group_id: Long,

    @SerialName("group_name")
    override val group_name: String,

    @SerialName("member_count")
    override val member_count: Int,

    @SerialName("max_member_count")
    override val max_member_count: Int
): OnebotResponseApi, Group {
    companion object {
        const val API = "/get_group_info"

        fun parse(json: String): GetGroupInfoResponse {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class SendMessageResponse(
    @SerialName("message_id")
    val message_id: Long
) : OnebotResponseApi{
    companion object {
        const val API = "/send_msg"

        fun parse(json: String): SendMessageResponse {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class GetGroupMemberInfoResponse(
    @SerialName("group_id")
    override val group_id: Long,

    @SerialName("user_id")
    override val user_id: Long,

    @SerialName("nickname")
    override val nickname: String,

    @SerialName("card")
    override val card: String,

    @SerialName("sex")
    override val sex: String,

    @SerialName("age")
    override val age: Int,

    @SerialName("area")
    override val area: String,

    @SerialName("join_time")
    override val joinTime: Int,

    @SerialName("last_sent_time")
    override val last_sent_time: Int,

    @SerialName("level")
    override val level: String,

    @SerialName("role")
    override val role: String,

    @SerialName("unfriendly")
    override val unfriendly: Boolean,

    @SerialName("title")
    override val title: String,

    @SerialName("title_expire_time")
    override val title_expire_time: Int,

    @SerialName("card_changeable")
    override val card_changeable: Boolean
) : OnebotResponseApi, Member {
    companion object {
        fun parse(json: String): GetGroupMemberInfoResponse {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class GetGroupMemberListResponse(
    @SerialName("member_list")
    val memberList: List<Member>
) : OnebotResponseApi {
    companion object {
        const val API = "/get_group_member_list"

        fun parse(json: String): GetGroupMemberListResponse {
            return Json.decodeFromString(serializer(), json)
        }
    }
}