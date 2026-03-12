package com.openclaw.bilitv.data.model

import com.google.gson.annotations.SerializedName

data class SearchSuggestResponse(
    @SerializedName("code") val code: Int = -1,
    @SerializedName("result") val result: Map<String, List<SearchSuggestItem>> = emptyMap()
)

data class SearchSuggestItem(
    @SerializedName("value") val value: String? = null,
    @SerializedName("term") val term: String? = null,
    @SerializedName("name") val name: String? = null
)
