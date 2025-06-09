package com.aniapps.locationtracker

import com.google.gson.annotations.SerializedName

data class UserSummary(

	@field:SerializedName("names")
	val names: List<NamesItem?>? = null,

	@field:SerializedName("dates")
	val dates: List<DatesItem?>? = null,

	@field:SerializedName("status")
	val status: String? = null
)

data class NamesItem(

	@field:SerializedName("device_id")
	val deviceId: String? = null,

	@field:SerializedName("user")
	val user: String? = null,

	@field:SerializedName("device")
	val device: String? = null
)

data class DatesItem(

	@field:SerializedName("date")
	val date: String? = null,

	@field:SerializedName("count")
	val count: String? = null
)
