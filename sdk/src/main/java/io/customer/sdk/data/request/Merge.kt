package io.customer.sdk.data.request

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Merge(
    val primary: MergeIdentifier,
    val secondary: MergeIdentifier
)

@JsonClass(generateAdapter = true)
data class MergeIdentifier(val id: String)
