package com.aniapps.locationtracker

import com.google.gson.annotations.SerializedName

data class UserRecords(

	@field:SerializedName("records")
	val records: List<RecordsItem?>? = null,

	@field:SerializedName("status")
	val status: String? = null
)

data class RecordsItem(

	@field:SerializedName("sequence")
	val sequence: String? = null,

	@field:SerializedName("usertime")
	val usertime: String? = null,

	@field:SerializedName("servertime")
	val servertime: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("lat")
	val lat: String? = null,

	@field:SerializedName("long")
	val jsonMemberLong: String? = null
)
