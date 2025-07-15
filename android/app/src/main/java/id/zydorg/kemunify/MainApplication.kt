package id.zydorg.kemunify

import android.app.Application
import id.zydorg.kemunify.data.di.Injection

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        injection = Injection(applicationContext)
    }

    companion object {
        lateinit var injection: Injection
    }
}