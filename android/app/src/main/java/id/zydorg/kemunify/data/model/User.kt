package id.zydorg.kemunify.data.model

data class User(
    val fullName: String,
    val email: String,
    val profile: String,
    val isLogin: Boolean = false
)
