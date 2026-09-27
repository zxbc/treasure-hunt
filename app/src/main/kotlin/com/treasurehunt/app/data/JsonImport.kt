package com.treasurehunt.app.data

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

    fun parse(text: String, defaultListName: String): ParsedList {
        val obj: JSONObject?
        val arr: JSONArray?
        try {
            obj = JSONObject(text)
            arr = null
        } catch (e: Exception) {
            try {
                arr = JSONArray(text)
                obj = null
            } catch (e2: Exception) {
                throw HuntImportException("File is not valid JSON.")
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
                        locations += parseLocation(array.getJSONObject(i), i)
                    }
                }
            }
            if (!found) throw HuntImportException("No 'locations' array found in the JSON object.")
            return ParsedList(listName, locations)
        }

        val locations = ArrayList<LocationDraft>()
        for (i in 0 until arr!!.length()) {
            locations += parseLocation(arr.getJSONObject(i), i)
        }
        return ParsedList(defaultListName, locations)
    }

    private fun parseLocation(obj: JSONObject, index: Int): LocationDraft {
        val name = firstString(obj, nameKeys)
            ?: throw HuntImportException("Location #" + (index + 1) + " is missing a 'name'.")
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
            throw HuntImportException("Location #" + (index + 1) + " ('" + name + "') is missing coordinates.")
        }
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
            throw HuntImportException("Location #" + (index + 1) + " ('" + name + "') has coordinates out of range.")
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
