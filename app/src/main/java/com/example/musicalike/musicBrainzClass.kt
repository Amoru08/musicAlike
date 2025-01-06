package com.example.musicalike

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.*



class SpotifyService(private val clientId: String, private val clientSecret: String) {

    private val client = OkHttpClient()
    private var accessToken: String? = null

    // Método para obtener el token de acceso
    private fun getAccessToken(callback: (String?) -> Unit) {
        if (accessToken != null) {
            callback(accessToken)
            return
        }

        val url = "https://accounts.spotify.com/api/token"
        val authHeader = "Basic " + Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())
        val requestBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", authHeader)
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
                    accessToken = json.getString("access_token")
                    callback(accessToken)
                } ?: run {
                    callback(null)
                }
            }
        })
    }

    // Método para obtener los géneros de un artista
    fun getArtistGenres(artistName: String, callback: (List<String>?) -> Unit) {
        getAccessToken { accessToken ->
            if (accessToken == null) {
                callback(null)
                return@getAccessToken
            }

            val searchUrl = "https://api.spotify.com/v1/search?q=${URLEncoder.encode(artistName, "UTF-8")}&type=artist"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            client.newCall(searchRequest).enqueue(object : Callback {
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
                        val artists = json.getJSONObject("artists").getJSONArray("items")
                        if (artists.length() > 0) {
                            val artist = artists.getJSONObject(0)
                            val genres = artist.getJSONArray("genres")
                            val genreList = (0 until genres.length()).map { genres.getString(it) }
                            callback(genreList)
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

    // Método para buscar canciones por tags
    fun searchSongsByTags(tags: List<String>, callback: (List<Song>?) -> Unit) {
        if (tags.isEmpty()) {
            callback(emptyList())
            return
        }

        val tagsQuery = tags.joinToString(",") { URLEncoder.encode(it, "UTF-8") }
        val url = "http://ws.audioscrobbler.com/2.0/?method=tag.getTopTracks&tag=$tagsQuery&api_key=YOUR_LAST_FM_API_KEY&format=json"

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
                        val tracksArray = json.getJSONObject("toptracks").getJSONArray("track")
                        val songs = mutableListOf<Song>()

                        for (i in 0 until tracksArray.length()) {
                            val track = tracksArray.getJSONObject(i)
                            val name = track.getString("name")
                            val artist = track.getJSONObject("artist").getString("name")
                            val tags = track.optJSONArray("toptags")?.let { tagArray ->
                                (0 until tagArray.length()).map { tagArray.getJSONObject(it).getString("name") }
                            } ?: listOf()

                            songs.add(Song(name, artist, tags))
                        }

                        callback(songs)
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