package ru.smsinterceptor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.preference.*
import androidx.room.Room
import ru.smsinterceptor.room.Database
import ru.smsinterceptor.room.Message
import kotlin.math.roundToInt


class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var newContext: Context? = null
    private var seekBarUpdater: Handler? = null
    private var changePref = true

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs)
        PreferenceManager.setDefaultValues(newContext, R.xml.prefs, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.newContext = context
    }

    override fun onStart() {
        super.onStart()
        seekBarUpdater = Handler()
        // кнопка получения разрешения на чтение СМС
        val smsPermission: Preference? = findPreference("sms_permission")
        val permissionCheck = ContextCompat.checkSelfPermission(newContext!!, "android.permission.RECEIVE_SMS")
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
        // маски пароля для акка отправителя
        findPreference<EditTextPreference?>("from")?.setOnBindEditTextListener { editText: EditText -> disableCopying(editText) }
        findPreference<EditTextPreference?>("pass")?.setOnBindEditTextListener { editText: EditText -> disableCopying(editText) }
        // переключатель мгновенной пересылки
        val prefs = PreferenceManager.getDefaultSharedPreferences(newContext)
        val timeUntilDelay = prefs.getLong("start_immediate_sending", 0) + prefs.getInt("time_wo_delay", 0) * 60 * 1000
        if (System.currentTimeMillis() > timeUntilDelay) {
            // время закончилось
            prefs.edit().putBoolean("disable_delay", false).apply()
        }
        // кнопка отправки всех сообщений из базы
        val sendAllAvailable = findPreference<Preference>("send_all_available")
        if (sendAllAvailable != null) {
            sendAllAvailable.setOnPreferenceClickListener {
                AsyncSender().execute(context)
                true
            }
            // количество неотправленных сообщений
            object : AsyncTask<Void, Void?, Void?>() {
                var rowCount = 0
                override fun doInBackground(vararg voids: Void): Void? {
                    val db: Database = Room.databaseBuilder(context!!, Database::class.java, "messages").build()
                    rowCount = db.messageDao()?.getRowCount() ?: 0
                    return null
                }

                override fun onPostExecute(result: Void?) {
                    sendAllAvailable.summary = String.format(getString(R.string.send_all_available_summary), rowCount)
                    super.onPostExecute(result)
                }
            }.execute()
        }
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

    private fun disableCopying(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        editText.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(actionMode: ActionMode, item: MenuItem): Boolean {
                return false
            }

            override fun onDestroyActionMode(actionMode: ActionMode) {}
        }
        editText.setTextIsSelectable(false)
        editText.isLongClickable = false
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        seekBarUpdater!!.post(object : Runnable {
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
                seekBarUpdater!!.postDelayed(this, (60 * 1000).toLong())
            }
        })
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        seekBarUpdater!!.removeCallbacksAndMessages(null)
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
                        AsyncDb(Message(from, to, password, notifSubj, body, System.currentTimeMillis())).execute(context)
                    }
                }
            }
            "from_temp" -> {
                // изменение гугл адреса отправителя
                if (changePref) {
                    changePref = false
                    val from = sharedPreferences.getString("from_temp", "")
                    // пустая строка - не менять
                    if (from!!.isEmpty()) return
                    sharedPreferences.edit().putString("from", from).apply()
                    val fromPref = findPreference<EditTextPreference>("from_temp")
                    if (fromPref != null) {
                        fromPref.text = ""
                    }
                } else {
                    changePref = true
                }
            }
            "pass_temp" -> {
                // изменение пароля отправителя
                if (changePref) {
                    changePref = false
                    val pass = sharedPreferences.getString("pass_temp", "")
                    // пустая строка - не менять
                    if (pass!!.isEmpty()) return
                    sharedPreferences.edit().putString("pass", pass).apply()
                    val passPref = findPreference<EditTextPreference>("pass_temp")
                    if (passPref != null) {
                        passPref.text = ""
                    }
                } else {
                    changePref = true
                }
            }
        }
    }
}