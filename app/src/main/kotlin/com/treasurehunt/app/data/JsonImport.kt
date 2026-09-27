package com.treasurehunt.app.data

import android.content.Context
import com.treasurehunt.app.R
import org.json.JSONArray
import org.json.JSONObject

data class LocationDraft(
    val name: String,
    val description: String,
    val lat: Double,
    val lon: Double,
)

class HuntImportException(message: String) : Exception(message)

/**
 * Parses a treasure hunt JSON file. Accepted shapes:
 *
 *  { "name": "Harbor Hunt", "locations": [ { "name", "description", "lat", "lon" } ] }
 *  or a bare JSON array of locations (list name is taken from the file name).
 *
 * Coordinate keys: lat/latitude and lon/lng/long/longitude, or
 * "coordinates"/"coords"/"geo"/"position" as a [lat, lon] array.
 */
object JsonImport {

    data class ParsedList(val name: String, val locations: List<LocationDraft>)

    private val locationArrayKeys = arrayOf("locations", "points", "items", "treasures", "stops", "spots")
    private val nameKeys = arrayOf("name", "title", "label", "spot")
    private val descKeys = arrayOf("description", "desc", "details", "hint", "clue")
    private val latKeys = arrayOf("lat", "latitude")
    private val lonKeys = arrayOf("lon", "lng", "long", "longitude")
    private val coordArrayKeys = arrayOf("coordinates", "coords", "geo", "position", "ll")

    fun parse(context: Context, text: String, defaultListName: String): ParsedList {
        var obj: JSONObject? = null
        var arr: JSONArray? = null
        try {
            obj = JSONObject(text)
        } catch (e: Exception) {
            try {
                arr = JSONArray(text)
            } catch (e2: Exception) {
                throw HuntImportException(context.getString(R.string.import_invalid_json))
            }
        }

        if (obj != null) {
            val listName = firstString(obj, arrayOf("name", "title")) ?: defaultListName
            var found = false
            val locations = ArrayList<LocationDraft>()
            for (key in locationArrayKeys) {
                if (obj.has(key) && obj.get(key) is JSONArray) {
                    found = true
                    val array = obj.getJSONArray(key)
                    for (i in 0 until array.length()) {
                        locations += parseLocation(context, array.getJSONObject(i), i)
                    }
                }
            }
            if (!found) throw HuntImportException(context.getString(R.string.import_no_locations))
            return ParsedList(listName, locations)
        }

        val locations = ArrayList<LocationDraft>()
        for (i in 0 until arr!!.length()) {
            locations += parseLocation(context, arr.getJSONObject(i), i)
        }
        return ParsedList(defaultListName, locations)
    }

    private fun parseLocation(context: Context, obj: JSONObject, index: Int): LocationDraft {
        val name = firstString(obj, nameKeys)
            ?: throw HuntImportException(context.getString(R.string.import_missing_name, index + 1))
        val description = firstString(obj, descKeys).orEmpty()

        var lat = firstDouble(obj, latKeys)
        var lon = firstDouble(obj, lonKeys)
        if (lat == null || lon == null) {
            for (key in coordArrayKeys) {
                if (obj.has(key) && obj.get(key) is JSONArray) {
                    val pair = obj.getJSONArray(key)
                    if (pair.length() >= 2) {
                        lat = pair.getDouble(0)
                        lon = pair.getDouble(1)
                        break
                    }
                }
            }
        }
        if (lat == null || lon == null) {
            throw HuntImportException(context.getString(R.string.import_missing_coordinates, index + 1, name))
        }
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
            throw HuntImportException(context.getString(R.string.import_coordinates_out_of_range, index + 1, name))
        }
        return LocationDraft(name.trim(), description.trim(), lat, lon)
    }

    private fun firstString(obj: JSONObject, keys: Array<String>): String? {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                obj.optString(key).takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return null
    }

    private fun firstDouble(obj: JSONObject, keys: Array<String>): Double? {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                obj.optDouble(key, Double.NaN).takeIf { !it.isNaN() }?.let { return it }
            }
        }
        return null
    }
}
