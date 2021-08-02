package ru.smsinterceptor

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.util.*

class OtherSettingsDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_other_settings, null)
        builder.setView(view)
        return builder.create()
    }

    override fun onResume() {
        super.onResume()
        val title = dialog?.findViewById<TextView>(R.id.dialog_title)
        val body = dialog?.findViewById<TextView>(R.id.main_text)
        when (Build.MANUFACTURER.toLowerCase(Locale.ROOT)) {
            "huawei" -> {
                title?.text = String.format(getString(R.string.dialog_current_device), getString(R.string.manufacturer_huawei))
                body?.text = getText(R.string.manufacturer_huawei_other)
            }
            "xiaomi" -> {
                title?.text = String.format(getString(R.string.dialog_current_device), getString(R.string.manufacturer_xiaomi))
                body?.text = getText(R.string.manufacturer_xiaomi_other)
            }
            "samsung" -> {
                title?.text = String.format(getString(R.string.dialog_current_device), getString(R.string.manufacturer_samsung))
                body?.text = getText(R.string.manufacturer_samsung_other)
            }
            "meizu" -> {
                title?.text = String.format(getString(R.string.dialog_current_device), getString(R.string.manufacturer_meizu))
                body?.text = getText(R.string.manufacturer_meizu_other)
            }
            "oppo" -> {
                title?.text = String.format(getString(R.string.dialog_current_device), getString(R.string.manufacturer_oppo))
                body?.text = getText(R.string.manufacturer_oppo_other)
            }
            "vivo" -> {
                title?.text = String.format(getString(R.string.dialog_current_device), getString(R.string.manufacturer_vivo))
                body?.text = getText(R.string.manufacturer_vivo_other)
            }
            "leeco" -> {
                title?.text = String.format(getString(R.string.dialog_current_device), getString(R.string.manufacturer_leeco))
                body?.text = getText(R.string.manufacturer_leeco_other)
            }
            "smartisan" -> {
                title?.text = String.format(getString(R.string.dialog_current_device), getString(R.string.manufacturer_smartisan))
                body?.text = getText(R.string.manufacturer_smartisan_other)
            }
            else -> {
                title?.text = String.format(getString(R.string.dialog_current_device), Build.MANUFACTURER)
                body?.text = getText(R.string.manufacturer_default_other)
            }
        }
    }
}