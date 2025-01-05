package com.example.musicalike

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class MusicBrainzService {

    private val client = OkHttpClient()

    fun searchRecordingsByTag(tag: String, callback: (List<Recording>?) -> Unit) {
        val url = "https://musicbrainz.org/ws/2/recording/?query=tag:${URLEncoder.encode(tag, "UTF-8")}&fmt=json"

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "YourAppName/1.0 (your-email@example.com)")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(null)
                    return
                }

                val responseBody = response.body?.string()
                responseBody?.let {
                    val json = JSONObject(it)
                    val recordingsArray = json.getJSONArray("recordings")
                    val recordings = mutableListOf<Recording>()
                    for (i in 0 until recordingsArray.length()) {
                        val recordingJson = recordingsArray.getJSONObject(i)
                        val recording = Recording(
                            id = recordingJson.getString("id"),
                            title = recordingJson.getString("title"),
                            artist = recordingJson.getJSONArray("artist-credit").getJSONObject(0).getJSONObject("artist").getString("name"),
                            tags = if (recordingJson.has("tags")) {
                                val tagsArray = recordingJson.getJSONArray("tags")
                                (0 until tagsArray.length()).map { tagsArray.getJSONObject(it).getString("name") }
                            } else {
                                emptyList()
                            }
                        )
                        recordings.add(recording)
                    }
                    callback(recordings)
                } ?: run {
                    callback(null)
                }
            }
        })
    }
}

data class Recording(
    val id: String,
    val title: String,
    val artist: String,
    val tags: List<String>
)