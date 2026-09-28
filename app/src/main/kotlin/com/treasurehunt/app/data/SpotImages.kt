package com.treasurehunt.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject

/**
 * Curated spot photos bundled with the APK (assets/kolodko-images.json plus
 * the JPEGs under assets/kolodko-images/).
 *
 * Spots are matched by their coordinates rounded to 6 decimal places rather
 * than by name, so the English and Chinese kolodko lists — identical
 * coordinates, translated names — resolve to the same photos, and a
 * user-imported list happens to get photos for any spot placed exactly on a
 * curated coordinate. The mapping file is read once per process.
 */
object SpotImages {

    data class Entry(val files: List<String>, val credits: List<String>)

    private var cache: Map<String, Entry>? = null

    /** Warm the cache off the main thread (called once at app start). */
    fun preload(context: Context) {
        if (cache != null) return
        cache = parse(context)
    }

    fun forSpot(context: Context, lat: Double, lon: Double): Entry? {
        val map = cache ?: run {
            val parsed = parse(context)
            cache = parsed
            parsed
        }
        return map["%.6f,%.6f".format(java.util.Locale.US, lat, lon)]
    }

    private fun parse(context: Context): Map<String, Entry> {
        val text = context.assets.open("kolodko-images.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val map = HashMap<String, Entry>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = root.getJSONObject(key)
            val files = ArrayList<String>()
            val fileArray = obj.getJSONArray("files")
            for (i in 0 until fileArray.length()) files.add(fileArray.getString(i))
            if (files.isEmpty()) continue
            val credits = ArrayList<String>()
            val creditArray = obj.getJSONArray("credits")
            for (i in 0 until creditArray.length()) credits.add(creditArray.getString(i))
            map[key] = Entry(files, credits)
        }
        return map
    }
}

/**
 * Decodes bundled asset images with a small LRU cache, so a spot's photos
 * stay in memory while the user flips between them. [maxDim] is the decoded
 * bitmap's maximum dimension; decoding is done on whatever dispatcher the
 * caller runs this on.
 */
object AssetBitmaps {

    private const val MAX_CACHE = 16

    private val cache = LinkedHashMap<String, Bitmap>()

    fun decode(context: Context, assetPath: String, maxDim: Int): Bitmap? {
        val key = assetPath + "@" + maxDim
        synchronized(cache) { cache[key]?.let { return it } }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null
        synchronized(cache) {
            cache[key] = bitmap
            while (cache.size > MAX_CACHE) {
                val oldest = cache.keys.first()
                cache.remove(oldest)
            }
        }
        return bitmap
    }
}
