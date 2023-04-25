package io.customer.sdk.queue.taskdata

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MergeProfileQueueTaskData(
    val primaryIdentifier: String,
    val secondaryIdentifier: String
)
