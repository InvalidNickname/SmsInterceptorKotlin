package ru.smsinterceptor

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*

class SendService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
            if (prefs.getBoolean("enable", true)) {
                var send = false
                var sms_from = intent.extras!!.getString("sms_from")
                val body = intent.extras!!.getString("sms_body")

                // если получена СМС с управляющего номера
                if (sms_from == prefs.getString("control_number", "")) {
                    val timeZone = TimeZone.getTimeZone("UTC")
                    val c = Calendar.getInstance(timeZone)
                    val simpleDateFormat = SimpleDateFormat("HH", Locale.US)
                    simpleDateFormat.timeZone = timeZone
                    var dayOfWeek = c[Calendar.DAY_OF_WEEK] - 1
                    if (dayOfWeek == 0) {
                        dayOfWeek = 7
                    }
                    val hours = simpleDateFormat.format(c.time).toInt() + 20
                    if (body == "#!$dayOfWeek$hours") {
                        // и соответствует содержание, включаем пересылку без задержки
                        prefs.edit()
                            .putLong("start_immediate_sending", System.currentTimeMillis())
                            .putBoolean("disable_delay", true)
                            .apply()
                        return START_STICKY
                    }
                }
                val list = prefs.getString("list", "")!!.split(",".toRegex()).toTypedArray()
                when (prefs.getString("type", "all")) {
                    "all" ->                         // пересылать всё
                        send = true
                    "black_list" -> {
                        // чёрный список
                        send = true
                        for (number in list) {
                            if (sms_from == number.trim { it <= ' ' }) {
                                send = false
                                break
                            }
                        }
                    }
                    "white_list" -> {
                        // белый список
                        send = false
                        for (number in list) {
                            if (sms_from == number.trim { it <= ' ' }) {
                                send = true
                                break
                            }
                        }
                    }
                }
                if (send) {
                    val to = prefs.getString("to", "")
                    val from = prefs.getString("from", "")
                    val id = prefs.getString("id", "")
                    val password = prefs.getString("pass", "")
                    if (!id!!.isEmpty()) {
                        sms_from += getString(R.string.from) + id
                    }
                    if (!to!!.isEmpty() && !from!!.isEmpty() && !password!!.isEmpty()) {
                        // время, до которого нет задержки отправки
                        val timeUntilDelay = prefs.getLong("start_immediate_sending", 0) + prefs.getInt("time_wo_delay", 0) * 60 * 1000
                        // если была включена пересылка без задержки и её время ещё не закончилось
                        val woDelayTimerActive = prefs.getBoolean("disable_delay", false) && System.currentTimeMillis() <= timeUntilDelay
                        if (System.currentTimeMillis() > timeUntilDelay) {
                            // время закончилось
                            prefs.edit().putBoolean("disable_delay", false).apply()
                        }
                        if (prefs.getInt("delay", 0) == 0 || woDelayTimerActive) {
                            // пересылка без задержки
                            AsyncSender().execute(from, to, password, sms_from, body)
                        } else {
                            // пересылка с задержкой
                            val manager = getSystemService(ALARM_SERVICE) as AlarmManager
                            if (manager != null) {
                                val alarmIntent = Intent(this, AlarmReceiver::class.java)
                                alarmIntent.putExtra("from", from)
                                alarmIntent.putExtra("to", to)
                                alarmIntent.putExtra("pass", password)
                                alarmIntent.putExtra("sms_from", sms_from)
                                alarmIntent.putExtra("body", body)
                                alarmIntent.action = System.currentTimeMillis().toString()
                                val pi = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                                val timeWoDelay = SystemClock.elapsedRealtime() + prefs.getInt("delay", 1) * 60 * 1000
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, timeWoDelay, pi)
                                } else {
                                    manager.setExact(AlarmManager.ELAPSED_REALTIME, timeWoDelay, pi)
                                }
                            }
                        }
                    }
                }
            }
        }
        return START_STICKY
    }
}