package cn.travellerr.onebotApi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
class TranslateResponse : ArrayList<TranslateResponseItem>() {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(jsonStr: String): TranslateResponse {
            return json
                .decodeFromString(serializer(), jsonStr)
        }
    }
}
@Serializable
data class TranslateResponseItem(
    val detectedLanguage: DetectedLanguage,
    val translations: List<Translation>,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(jsonStr: String): TranslateResponseItem {
            return json
                .decodeFromString(serializer(), jsonStr)
        }
    }
}
@Serializable
data class DetectedLanguage(
    val language: String
)
@Serializable
data class Translation(
    val text: String,
    val to: String
)