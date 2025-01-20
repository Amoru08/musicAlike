package com.example.musicalike

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
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

    // Método para buscar canciones por logs
    fun searchSongsByLogs(logs: String, callback: (List<Song>?) -> Unit) {
        getAccessToken { accessToken ->
            if (accessToken == null) {
                callback(null)
                return@getAccessToken
            }

            val searchUrl = "https://api.spotify.com/v1/search?q=$logs&type=track&limit=20"
            val request = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $accessToken")
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
                        val tracks = json.getJSONObject("tracks").getJSONArray("items")
                        val songs = mutableListOf<Song>()

                        for (i in 0 until tracks.length()) {
                            val track = tracks.getJSONObject(i)
                            val song = Song(
                                name = track.getString("name"),
                                artist = track.getJSONArray("artists").getJSONObject(0).getString("name"),
                                tags = listOf() // No tags, assuming tags come from the search query
                            )
                            songs.add(song)
                        }

                        callback(songs)
                    } ?: run {
                        callback(null)
                    }
                }
            })
        }
    }

    fun getArtistGenres(artistName: String, callback: (List<String>?) -> Unit) {
        getAccessToken { accessToken ->
            if (accessToken == null) {
                callback(null)
                return@getAccessToken
            }

            val searchUrl = "https://api.spotify.com/v1/search?q=$artistName&type=artist"
            val request = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $accessToken")
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
                        val artists = json.getJSONObject("artists").getJSONArray("items")
                        if (artists.length() > 0) {
                            val artistId = artists.getJSONObject(0).getString("id")
                            getGenresByArtistId(artistId, callback)
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
    private fun getGenresByArtistId(artistId: String, callback: (List<String>?) -> Unit) {
        val url = "https://api.spotify.com/v1/artists/$artistId"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
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
                    val genres = json.getJSONArray("genres")
                    val genreList = mutableListOf<String>()

                    for (i in 0 until genres.length()) {
                        genreList.add(genres.getString(i))
                    }

                    callback(genreList)
                } ?: run {
                    callback(null)
                }
            }
        })
    }
    fun searchSongsByTags(tags: List<String>, callback: (List<Song>?) -> Unit) {
        getAccessToken { accessToken ->
            if (accessToken == null) {
                callback(null)
                return@getAccessToken
            }

            val tagsJoined = tags.joinToString(" ")
            val searchUrl = "https://api.spotify.com/v1/search?q=$tagsJoined&type=track&limit=20"
            val request = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer $accessToken")
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
                        val tracks = json.getJSONObject("tracks").getJSONArray("items")
                        val songs = mutableListOf<Song>()

                        for (i in 0 until tracks.length()) {
                            val track = tracks.getJSONObject(i)
                            val song = Song(
                                name = track.getString("name").replace(Regex("\\s*\\([^)]*\\)\\s*"), "").trim(), // Limpiar el nombre de la canción
                                artist = track.getJSONArray("artists").getJSONObject(0).getString("name"),
                                tags = listOf() // Assuming tags are not directly available from the API
                            )
                            songs.add(song)
                        }

                        callback(songs)
                    } ?: run {
                        callback(null)
                    }
                }
            })
        }
    }
}