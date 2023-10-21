package server

data class Character(
    val id: Int,
    val userId: Int,
    val name: String,
    val species: String,
    val properties: Map<String, Any>,
    var x: Long,
    var y: Long,
    var status: Boolean,
    //val coordinates: Pair<Double, Double>
)