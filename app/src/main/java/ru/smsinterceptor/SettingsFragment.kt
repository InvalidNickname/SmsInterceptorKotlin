package ru.smsinterceptor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var newContext: Context? = null

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
            smsPermission.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference? ->
                requestPermissions(arrayOf("android.permission.RECEIVE_SMS"), 1)
                true
            }
        }
        // кнопка открытия браузера и разрешения на вход из небезопасных приложений
        val unsafePermission: Preference? = findPreference("unsafe_permission")
        if (unsafePermission != null) {
            unsafePermission.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference? ->
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
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if ("disable_delay" == key) {
            if (sharedPreferences.getBoolean("disable_delay", false)) {
                sharedPreferences.edit().putLong("start_immediate_sending", System.currentTimeMillis()).apply()
            }
        }
    }
}