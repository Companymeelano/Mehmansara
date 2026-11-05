package ir.ahvaz.mehra.core.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.components.SingletonComponent
import ir.ahvaz.mehra.core.di.ApiClient
import ir.ahvaz.mehra.core.security.ServerUrlProvider
import ir.ahvaz.mehra.core.security.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads authenticated images (fault photos, guest documents) without a third-party
 * image library: the app already owns an OkHttp client and the JWT lives in memory.
 *
 * Bitmaps are kept in a small LRU cache; failures return null so the UI can show a
 * neutral placeholder instead of pretending an image exists.
 */
@Singleton
class RemoteImageLoader @Inject constructor(
    @ApiClient private val client: OkHttpClient,
    private val sessionManager: SessionManager,
    private val serverUrlProvider: ServerUrlProvider,
) {

    private val cache = object : LruCache<String, Bitmap>(maxSize()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    suspend fun load(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        cache.get(url)?.let { return@withContext it }
        val absolute = absoluteUrl(url) ?: return@withContext null
        val request = Request.Builder()
            .url(absolute)
            .apply {
                sessionManager.accessToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val bytes = response.body?.bytes() ?: return@use null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()?.also { bitmap -> cache.put(url, bitmap) }
    }

    private fun absoluteUrl(url: String): String? {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        val base = serverUrlProvider.current()
        if (base.isBlank()) return null
        return base.trimEnd('/') + "/" + url.trimStart('/')
    }

    private fun maxSize(): Int {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        return maxKb / 8
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImageLoaderEntryPoint {
    fun remoteImageLoader(): RemoteImageLoader
}

fun imageLoaderFrom(context: android.content.Context): RemoteImageLoader =
    EntryPoints.get(
        context.applicationContext,
        ImageLoaderEntryPoint::class.java,
    ).remoteImageLoader()
