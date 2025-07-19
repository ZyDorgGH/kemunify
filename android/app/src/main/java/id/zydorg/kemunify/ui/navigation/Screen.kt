package id.zydorg.kemunify.ui.navigation

import android.net.Uri

sealed class Screen(val route: String){
    object Home : Screen("home")
    object DetailCustomer: Screen("home/{customer}") {
        fun createRoute(customer: String) = "home/$customer"
    }
    object Login : Screen("login")
    object AddWaste : Screen("add")
    object Camera : Screen("camera")
    object Detect : Screen("detect?photo={photo}"){
        fun takePhoto(photo: kotlin.String) = "detect?photo=${Uri.encode(photo)}"
    }
    object Profile : Screen("profile")
}