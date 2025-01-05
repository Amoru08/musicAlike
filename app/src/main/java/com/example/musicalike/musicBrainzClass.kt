package com.example.musicalike

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class MusicBrainzService {

    private val client = OkHttpClient()

    fun searchTagsBySongAndArtist(songTitle: String, artistName: String, callback: (List<String>?) -> Unit) {
        val query = "${URLEncoder.encode(songTitle, "UTF-8")} AND artist:${URLEncoder.encode(artistName, "UTF-8")}"
        val url = "https://musicbrainz.org/ws/2/recording/?query=${query}&fmt=json"

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
                    if (recordingsArray.length() > 0) {
                        val recordingJson = recordingsArray.getJSONObject(0)
                        val tags = if (recordingJson.has("tags")) {
                            val tagsArray = recordingJson.getJSONArray("tags")
                            (0 until tagsArray.length()).map { tagsArray.getJSONObject(it).getString("name") }
                        } else {
                            emptyList()
                        }
                        callback(tags)
                    } else {
                        callback(null)
                    }
                } ?: run {
                    callback(null)
                }
            }
        })
    }
}