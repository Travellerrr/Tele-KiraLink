package cn.travellerr.onebotApi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

interface OnebotSubClassApi {
    companion object {
        fun parse(json: String): OnebotSubClassApi {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

interface Group {
    val group_id: Long
    val group_name: String
    val member_count: Int
    val max_member_count: Int
}

interface Member {
    val group_id: Long
    val user_id: Long
    val nickname: String
    val card: String
    val sex: String
    val age: Int
    val area: String
    val joinTime: Int
    val last_sent_time: Int
    val level: String
    val role: String
    val unfriendly: Boolean
    val title: String
    val title_expire_time: Int
    val card_changeable: Boolean
}

@Serializable
data class Friend (
    @SerialName("user_id")
    val userId: Long,

    @SerialName("nickname")
    val nickname: String,

    @SerialName("remark")
    val remark: String
) : OnebotSubClassApi {
    companion object {
        fun parse(json: String): Friend {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class GroupInfo (
    @SerialName("group_id")
    override val group_id: Long,

    @SerialName("group_name")
    override val group_name: String,

    @SerialName("member_count")
    override val member_count: Int,

    @SerialName("max_member_count")
    override val max_member_count: Int
) : OnebotSubClassApi, Group {
    companion object {
        fun parse(json: String): GroupInfo {
            return Json.decodeFromString(serializer(), json)
        }
    }
}


@Serializable
data class MemberInfo (
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
) : OnebotSubClassApi, Member {
    companion object {
        fun parse(json: String): MemberInfo {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class Sender (
    @SerialName("user_id")
    val user_id: Long,

    @SerialName("nickname")
    val nickname: String,

    @SerialName("card")
    val card: String? = null,

    @SerialName("sex")
    val sex: String,

    @SerialName("age")
    val age: Int,

    @SerialName("area")
    val area: String? = null,

    @SerialName("level")
    val level: String? = null,

    @SerialName("role")
    val role: String? = null,

    @SerialName("title")
    val title: String? = null
)  {

    companion object {
        fun parse(json: String): Sender {
            return Json.decodeFromString(serializer(), json)
        }
    }

    override fun toString(): String {
        return Json.encodeToString(serializer(), this)
    }
}


@Serializable
data class Anonymous(
    @SerialName("id")
    val id: Long,

    @SerialName("name")
    val name: String,

    @SerialName("flag")
    val flag: String
) {
    companion object {
        fun parse(json: String): Anonymous {
            return Json.decodeFromString(serializer(), json)
        }
    }
}

@Serializable
data class TextData(
    val text: String
)

@Serializable
data class AtData(
    val qq: Long,
)

@Serializable
data class File(
    val file: String
)