package io.customer.sdk.util

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Abstract way to deserialize JSON strings without thinking about the library used.
 *
 * This is an object instead of a class through dependency injection because when we run tests, we like to use the real instance of this class in our tests. That is usually a sign it doesn't need to be injected.
 */
@OptIn(InternalSerializationApi::class)
class JsonAdapter internal constructor(val json: Json) {

    /**
     * Note: This class must treat arrays and not arrays differently (below we call them Lists but we mean arrays because that's what we call them in Json).
     *
     * We are not talking about embedded arrays:
     * ```
     * {
     *   "nested": []
     * }
     * ```
     * Nesting works fine with the regular functions.
     *
     * We mean:
     * ```
     * {
     *   "not_array_json": ""
     * }
     * ```
     * vs
     * ```
     * [
     *   {
     *     "not_array_json": ""
     *   }
     * ]
     * ```
     *
     * Moshi handles arrays differently: https://github.com/square/moshi#parse-json-arrays
     *
     * Parses data string to single objects. Using this method directly is
     * discouraged as parsing json incorrectly can lead to crashes on client
     * apps. Prefer safe parsing instead by using [fromJsonOrNull].
     *
     * @see [fromJsonOrNull] for safe parsing
     */
    @Throws(Exception::class)
    inline fun <reified T : Any> fromJson(data: String): T {
        val trimmedData = data.trim()

        if (trimmedData.isNotEmpty() && trimmedData[0] == '[') throw IllegalArgumentException("String is a list. Use `fromJsonList` instead.")

        return json.decodeFromString(T::class.serializer(), trimmedData)
    }

    /**
     * Parses data string to a single objects or null if there is any exception
     * e.g. malformed json, missing adapter, etc.
     */
    inline fun <reified T : Any> fromJsonOrNull(json: String): T? = try {
        fromJson(json)
    } catch (ex: Exception) {
        null
    }

    /**
     * Parses data string to list of objects. Using this method directly is
     * discouraged as parsing json incorrectly can lead to crashes on client
     * apps. Prefer safe parsing instead by using [fromJsonListOrNull].
     *
     * @see [fromJsonListOrNull] for safe parsing
     */
    @Throws(Exception::class)
    inline fun <reified T : Any> fromJsonList(data: String): List<T> {
        val trimmedData = data.trim()

        if (trimmedData.isNotEmpty() && trimmedData[0] != '[') throw IllegalArgumentException("String is not a list. Use `fromJson` instead.")

        val listSerializer = ListSerializer(T::class.serializer())
        return json.decodeFromString(listSerializer, trimmedData)
    }

    /**
     * Parses data string to list of objects or null if there is any exception
     * e.g. malformed json, missing adapter, etc.
     */
    inline fun <reified T : Any> fromJsonListOrNull(json: String): List<T>? = try {
        fromJsonList(json)
    } catch (ex: Exception) {
        null
    }

    inline fun <reified T : Any> toJson(data: T): String {
        return json.encodeToString(T::class.serializer(), data)
    }

    inline fun <reified T : Any> toJson(data: List<T>): String {
        val listSerializer = ListSerializer(T::class.serializer())

        return json.encodeToString(listSerializer, data)
    }
}
