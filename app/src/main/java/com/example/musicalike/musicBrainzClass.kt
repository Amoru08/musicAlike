package com.example.musicalike

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class LastFmService {

    private val client = OkHttpClient()
    private val apiKey = "ef00b8f621f08d051b3e443f5acbc424" // Reemplaza con tu API Key de Last.fm

    fun searchTagsBySongAndArtist(songTitle: String, artistName: String, callback: (List<String>?) -> Unit) {
        val url = "http://ws.audioscrobbler.com/2.0/?method=track.gettoptags" +
                "&track=${URLEncoder.encode(songTitle, "UTF-8")}" +
                "&artist=${URLEncoder.encode(artistName, "UTF-8")}" +
                "&api_key=$apiKey&format=json"

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "MusicAlike/1.0 (musicalikesoporte@gmail.com)")
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
                    try {
                        val json = JSONObject(it)
                        val tagsObject = json.getJSONObject("toptags")
                        val tagsArray = tagsObject.optJSONArray("tag")

                        if (tagsArray != null && tagsArray.length() > 0) {
                            val tags = (0 until tagsArray.length())
                                .map { tagsArray.getJSONObject(it).getString("name") }
                            Log.d("LastFmService", "Tags: $tags")
                            callback(tags)
                        } else {
                            Log.d("LastFmService", "No tags found")
                            callback(emptyList())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback(null)
                    }
                } ?: run {
                    callback(null)
                }
            }
        })
    }
}