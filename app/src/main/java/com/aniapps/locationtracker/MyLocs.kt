data class MyLocs(
	val locations: List<Location>,
)

data class Location(
	val name: String,
	val dates: List<DateData>
)

data class DateData(
	val date: String,
	val data: List<LocData>
)

data class LocData(
	val lat: Double,
	val long: Double,
	val timestamp: String
)



