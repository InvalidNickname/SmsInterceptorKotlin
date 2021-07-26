package ru.smsinterceptor

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import ru.smsinterceptor.room.Message
import kotlin.math.roundToInt


class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var seekBarUpdater: Handler
    private var changePref = true

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs)
        PreferenceManager.setDefaultValues(context, R.xml.prefs, false)
    }

    override fun onStart() {
        super.onStart()
        seekBarUpdater = Handler(Looper.getMainLooper())
        // запуск монитора
        val monitorIntent = Intent(context, MonitorService::class.java)
        requireContext().startService(monitorIntent)
        // кнопка получения разрешения на чтение СМС
        val smsPermission: Preference? = findPreference("sms_permission")
        val permissionCheck = ContextCompat.checkSelfPermission(requireContext(), "android.permission.RECEIVE_SMS")
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf("android.permission.RECEIVE_SMS"), 1)
        } else {
            if (smsPermission != null) {
                smsPermission.setSummary(R.string.permission_granted)
                smsPermission.isEnabled = false
            }
        }
        // слушатель нажатия на кнопку получения разрешения на чтение СМС
        if (smsPermission != null) {
            smsPermission.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                requestPermissions(arrayOf("android.permission.RECEIVE_SMS"), 1)
                true
            }
        }
        // кнопка получения разрешения на игнор оптимизации батареи, api >= 23
        val batteryPermission = findPreference<Preference>("battery_permission")
        if (batteryPermission != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // слушатель нажатия на кнопку разрешения на игнор оптимизации батареи
            batteryPermission.setOnPreferenceClickListener {
                val intent = Intent()
                intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                startActivity(intent)
                true
            }
        } else if (batteryPermission != null) {
            batteryPermission.isVisible = false
        }
        // кнопка открытия браузера и разрешения на вход из небезопасных приложений
        val unsafePermission: Preference? = findPreference("unsafe_permission")
        if (unsafePermission != null) {
            unsafePermission.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://myaccount.google.com/lesssecureapps")
                startActivity(intent)
                true
            }
        }
        // переключатель мгновенной пересылки
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val timeUntilDelay = prefs.getLong("start_immediate_sending", 0) + prefs.getInt("time_wo_delay", 0) * 60 * 1000
        if (System.currentTimeMillis() > timeUntilDelay) {
            // время закончилось
            prefs.edit().putBoolean("disable_delay", false).apply()
        }
        // кнопка отправки всех сообщений из базы
        val sendAllAvailable = findPreference<Preference>("send_all_available")
        if (sendAllAvailable != null) {
            sendAllAvailable.setOnPreferenceClickListener {
                AsyncSender().execute(requireContext())
                true
            }
            // количество неотправленных сообщений
            sendAllAvailable.summary = String.format(getString(R.string.send_all_available_summary), prefs.getInt("database_size", 0))
        }
        // установка текста версии
        var versionName: String? = "unknown"
        try {
            versionName = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        findPreference<Preference>("version")?.title = String.format(resources.getString(R.string.pref_version), versionName)
        // запрет на overscroll, без него выглядит лучше
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val smsPermission: Preference? = findPreference("sms_permission")
                if (smsPermission != null) {
                    smsPermission.setSummary(R.string.permission_granted)
                    smsPermission.isEnabled = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        seekBarUpdater.post(object : Runnable {
            override fun run() {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val millis = prefs.getLong("start_immediate_sending", 0) + prefs.getInt("time_wo_delay", 0) * 60 * 1000 - System.currentTimeMillis()
                var timeLeft = (millis / 60000.0).roundToInt()
                if (timeLeft < 0) {
                    timeLeft = 0
                }
                prefs.edit()
                    .putInt("time_wo_delay", timeLeft)
                    .putLong("start_immediate_sending", System.currentTimeMillis())
                    .apply()
                val timeWoDelay: SeekBarPreference? = findPreference("time_wo_delay")
                if (timeWoDelay != null) {
                    timeWoDelay.value = timeLeft
                }
                seekBarUpdater.postDelayed(this, (60 * 1000).toLong())
            }
        })
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        seekBarUpdater.removeCallbacksAndMessages(null)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "time_wo_delay" -> {
                val timeWoDelay = sharedPreferences.getInt("time_wo_delay", 0)
                if (timeWoDelay != 0) {
                    sharedPreferences.edit().putLong("start_immediate_sending", System.currentTimeMillis()).apply()
                    // если установлена отправка оповещений
                    if (sharedPreferences.getBoolean("send_notification_on_change", false)) {
                        val to = sharedPreferences.getString("to", "")!!
                        val from = sharedPreferences.getString("from", "")!!
                        val password = sharedPreferences.getString("pass", "")!!
                        val body = String.format(getString(R.string.n_minutes_left), timeWoDelay)
                        val id = sharedPreferences.getString("id", "")!!
                        var notifSubj = getString(R.string.instant_mode_changed)
                        if (id.isNotEmpty()) {
                            notifSubj += String.format(getString(R.string.n_minutes_left_id), id)
                        }
                        AsyncDb(Message(from, to, password, notifSubj, body, System.currentTimeMillis())).execute(requireContext())
                    }
                }
            }
            "from_temp" -> {
                // изменение гугл адреса отправителя
                if (changePref) {
                    changePref = false
                    val from = sharedPreferences.getString("from_temp", "")!!
                    // пустая строка - не менять
                    if (from.isNotEmpty()) {
                        sharedPreferences.edit().putString("from", from).putString("from_temp", "").apply()
                    }
                } else {
                    changePref = true
                }
            }
            "pass_temp" -> {
                // изменение пароля отправителя
                if (changePref) {
                    changePref = false
                    val pass = sharedPreferences.getString("pass_temp", "")!!
                    // пустая строка - не менять
                    if (pass.isNotEmpty()) {
                        sharedPreferences.edit().putString("pass", pass).putString("pass_temp", "").apply()
                    }
                } else {
                    changePref = true
                }
            }
            "database_size" -> {
                findPreference<Preference>("send_all_available")?.summary =
                    String.format(getString(R.string.send_all_available_summary), sharedPreferences.getInt("database_size", 0))
            }
            "enable" -> {
                if (!sharedPreferences.getBoolean("enable", true)) {
                    sharedPreferences.edit().putBoolean("sender_status", false).apply()
                }
            }
        }
    }
}