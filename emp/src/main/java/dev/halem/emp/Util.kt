package dev.halem.emp

import android.content.Context
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.ext.cronet.CronetDataSource
import com.google.android.exoplayer2.ext.cronet.CronetUtil
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import org.checkerframework.checker.nullness.qual.MonotonicNonNull
import org.chromium.net.CronetEngine
import org.jetbrains.annotations.Nullable
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.Executors

/** Utility methods for the demo app.  */
object Util {

    /**
     * Whether the demo application uses Cronet for networking. Note that Cronet does not provide
     * automatic support for cookies (https://github.com/google/ExoPlayer/issues/5975).
     *
     *
     * If set to false, the platform's default network stack is used with a [CookieManager]
     * configured in [.getHttpDataSourceFactory].
     */
    private const val USE_CRONET_FOR_NETWORKING = true
    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"
    private var dataSourceFactory: @MonotonicNonNull DataSource.Factory? = null
    private var httpDataSourceFactory: @MonotonicNonNull DataSource.Factory? = null

    private var databaseProvider: DatabaseProvider? = null

    private var downloadDirectory: File? = null

    private var downloadCache: Cache? = null

    /** Returns whether extension renderers should be used.  */
    private fun useExtensionRenderers(): Boolean {
//        return BuildConfig.USE_DECODER_EXTENSIONS
        return true
    }

    @JvmStatic
    fun buildRenderersFactory(
        context: Context, preferExtensionRenderer: Boolean
    ): RenderersFactory {
        val extensionRendererMode: @DefaultRenderersFactory.ExtensionRendererMode Int =
            if (useExtensionRenderers()) if (preferExtensionRenderer) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        return DefaultRenderersFactory(context.applicationContext)
            .setExtensionRendererMode(extensionRendererMode)
    }

    @JvmStatic
    @Synchronized
    fun getHttpDataSourceFactory(context: Context): DataSource.Factory {

        if (httpDataSourceFactory == null) {
            if (USE_CRONET_FOR_NETWORKING) {
                @Nullable val cronetEngine: CronetEngine? =
                    CronetUtil.buildCronetEngine(context.applicationContext)
                if (cronetEngine != null) {
                    httpDataSourceFactory =
                        CronetDataSource.Factory(cronetEngine, Executors.newSingleThreadExecutor())
                }
            }
            if (httpDataSourceFactory == null) {
                // We don't want to use Cronet, or we failed to instantiate a CronetEngine.
                val cookieManager = CookieManager()
                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
                CookieHandler.setDefault(cookieManager)
                httpDataSourceFactory = DefaultHttpDataSource.Factory()
            }
        }
        return httpDataSourceFactory as DataSource.Factory
    }

    /** Returns a [DataSource.Factory].  */
    @JvmStatic
    @Synchronized
    fun getDataSourceFactory(context: Context): DataSource.Factory {
        if (dataSourceFactory == null) {
            val upstreamFactory: DefaultDataSource.Factory =
                DefaultDataSource.Factory(
                    context.applicationContext,
                    getHttpDataSourceFactory(context)
                )
            dataSourceFactory =
                buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache(context))
        }
        return dataSourceFactory as @MonotonicNonNull DataSource.Factory
    }


    @Synchronized
    private fun getDownloadCache(context: Context): Cache {
        if (downloadCache == null) {
            val downloadContentDirectory =
                File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY)
            downloadCache = SimpleCache(
                downloadContentDirectory, NoOpCacheEvictor(), getDatabaseProvider(context)
            )
        }
        return downloadCache as Cache
    }


    @Synchronized
    private fun getDatabaseProvider(context: Context): DatabaseProvider {
        if (databaseProvider == null) {
            databaseProvider = StandaloneDatabaseProvider(context)
        }
        return databaseProvider as DatabaseProvider
    }

    @Synchronized
    private fun getDownloadDirectory(context: Context): File? {
        if (downloadDirectory == null) {
            downloadDirectory = context.getExternalFilesDir( /* p0 = type */null)
            if (downloadDirectory == null) {
                downloadDirectory = context.filesDir
            }
        }
        return downloadDirectory
    }

    private fun buildReadOnlyCacheDataSource(
        upstreamFactory: DataSource.Factory, cache: Cache
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}