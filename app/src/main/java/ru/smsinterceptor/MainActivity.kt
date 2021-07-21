package ru.smsinterceptor

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

open class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction().add(SettingsFragment(), "settings")
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }
}