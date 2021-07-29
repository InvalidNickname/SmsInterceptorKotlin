package ru.smsinterceptor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Главная и единственная активность приложения. При создании запускает фрагмент настроек
 */
open class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction().add(SettingsFragment(), "settings")
    }
}