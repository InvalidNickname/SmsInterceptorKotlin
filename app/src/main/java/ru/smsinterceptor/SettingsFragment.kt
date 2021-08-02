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
import androidx.preference.*
import ru.smsinterceptor.room.Message
import kotlin.math.roundToInt


/**
 * Фрагмент настроек приложения
 */
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var seekBarUpdater: Handler
    private var changePref = true

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // заполняем настройки
        addPreferencesFromResource(R.xml.prefs)
        PreferenceManager.setDefaultValues(context, R.xml.prefs, false)
    }

    override fun onStart() {
        super.onStart()
        // хендлер для обновления ползунка оставшегося времени без задержки
        seekBarUpdater = Handler(Looper.getMainLooper())
        // запуск монитора
        val monitorIntent = Intent(context, MonitorService::class.java)
        requireContext().startService(monitorIntent)
        // обновление различных настроек
        updatePrefs()
        // запрет на overscroll, без него выглядит лучше
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    /**
     * Обновление различных настроек
     */
    private fun updatePrefs() {
        updateReceiveSmsPermissionButton()
        updateOptimizationButton()
        updateLessSecureAppsButton()
        updateSendAllButton()
        updateVersionText()
        updateIdText()
        updateOtherSettingsButton()
    }

    /**
     * Обновление кнопки получения разрешения на чтение СМС
     */
    private fun updateReceiveSmsPermissionButton() {
        // кнопка получения разрешения на чтение СМС
        val smsPermission = findPreference<Preference>("sms_permission")
        val permissionCheck = ContextCompat.checkSelfPermission(requireContext(), "android.permission.RECEIVE_SMS")
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf("android.permission.RECEIVE_SMS"), 1)
        } else {
            if (smsPermission != null) {
                // если разрешение получено - отключаем кнопку
                smsPermission.setSummary(R.string.sms_permission_granted)
                smsPermission.isEnabled = false
            }
        }
        // слушатель нажатия на кнопку получения разрешения на чтение СМС
        if (smsPermission != null) {
            smsPermission.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                // при нажатии запрашиваем разрешение на чтение СМС
                requestPermissions(arrayOf("android.permission.RECEIVE_SMS"), 1)
                true
            }
        }
    }

    /**
     * Обновление кнопки получения разрешения на игнор оптимизации батареи
     */
    private fun updateOptimizationButton() {
        // кнопка получения разрешения на игнор оптимизации батареи, api >= 23
        val batteryPermission = findPreference<Preference>("battery_permission")
        if (batteryPermission != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // слушатель нажатия на кнопку разрешения на игнор оптимизации батареи
                batteryPermission.setOnPreferenceClickListener {
                    // при нажатии открываем настройки оптимизации батареи
                    val intent = Intent()
                    intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    startActivity(intent)
                    true
                }
            } else {
                // если не подходит по версии, скрываем
                batteryPermission.isVisible = false
            }
        }
    }

    /**
     * Обновление кнопки открытия браузера и разрешения на вход из небезопасных приложений
     */
    private fun updateLessSecureAppsButton() {
        // кнопка открытия браузера и разрешения на вход из небезопасных приложений
        val unsafePermission = findPreference<Preference>("unsafe_permission")
        unsafePermission?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // при нажатии открываем https://myaccount.google.com/lesssecureapps
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://myaccount.google.com/lesssecureapps")
            startActivity(intent)
            true
        }
    }

    /**
     * Обновление кнопки отправки всех сообщений из базы
     */
    private fun updateSendAllButton() {
        // кнопка отправки всех сообщений из базы
        val sendAllAvailable = findPreference<Preference>("send_all_available")
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (sendAllAvailable != null) {
            sendAllAvailable.setOnPreferenceClickListener {
                // при нажатии запускаем AsyncSender
                AsyncSender().execute(requireContext())
                true
            }
            // количество неотправленных сообщений
            sendAllAvailable.summary = String.format(getString(R.string.send_all_available_summary), prefs.getInt("database_size", 0))
        }
    }

    /**
     * Обновление текста версии
     */
    private fun updateVersionText() {
        // установка текста версии
        var versionName = getString(R.string.unknown_version)
        try {
            versionName = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        findPreference<Preference>("version")?.title = String.format(resources.getString(R.string.version_title), versionName)
    }

    /**
     * Обновление настройки идентификатора пользователя
     */
    private fun updateIdText() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (prefs.getString("id", "") == "NOT_SET") {
            prefs.edit().putString("id", Build.MODEL).apply()
        }
        val idPref = findPreference<EditTextPreference>("id")
        idPref?.summary = prefs.getString("id", "")
    }

    /**
     * Обновление кнопки открытия диалога с настройками устройства
     */
    private fun updateOtherSettingsButton() {
        findPreference<Preference>("other_security_permission")?.setOnPreferenceClickListener {
            OtherSettingsDialog().show(parentFragmentManager, "OtherSettings")
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // получено разрешение на чтение СМС
                val smsPermission = findPreference<Preference>("sms_permission")
                if (smsPermission != null) {
                    // делаем кнопку неактивной
                    smsPermission.setSummary(R.string.sms_permission_granted)
                    smsPermission.isEnabled = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        // обновление значения на полоске оставшегося времени без задержки
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
                val timeWoDelay = findPreference<SeekBarPreference>("time_wo_delay")
                timeWoDelay?.value = timeLeft
                // через минуту обновляем снова
                seekBarUpdater.postDelayed(this, 60000L)
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
                // перетянули ползунок оставшегося времени без задержки
                val timeWoDelay = sharedPreferences.getInt("time_wo_delay", 0)
                if (timeWoDelay != 0) {
                    sharedPreferences.edit().putLong("start_immediate_sending", System.currentTimeMillis()).apply()
                    // если установлена отправка оповещений, отправляем
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
                    val from = sharedPreferences.getString("from_temp", "")!!
                    // пустая строка - не менять
                    if (from.isNotEmpty()) {
                        sharedPreferences.edit().putString("from", from).putString("from_temp", "").apply()
                    }
                }
                changePref = !changePref
            }
            "pass_temp" -> {
                // изменение пароля отправителя
                if (changePref) {
                    val pass = sharedPreferences.getString("pass_temp", "")!!
                    // пустая строка - не менять
                    if (pass.isNotEmpty()) {
                        sharedPreferences.edit().putString("pass", pass).putString("pass_temp", "").apply()
                    }
                }
                changePref = !changePref
            }
            "database_size" -> {
                // если изменился размер базы, меняем надпись на кнопке отправки всех сообщений из базы
                val sendPref = findPreference<Preference>("send_all_available")
                sendPref?.summary = String.format(getString(R.string.send_all_available_summary), sharedPreferences.getInt("database_size", 0))
            }
            "enable" -> {
                // переключатель отправки
                if (!sharedPreferences.getBoolean("enable", true)) {
                    sharedPreferences.edit().putBoolean("sender_status", false).apply()
                }
            }
            "id" -> {
                // изменен идентификатор пользователя
                val idPref = findPreference<EditTextPreference>("id")
                idPref?.summary = sharedPreferences.getString("id", "")
            }
        }
    }
}