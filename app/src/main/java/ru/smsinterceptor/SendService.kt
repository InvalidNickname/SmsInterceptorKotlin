package ru.smsinterceptor

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.preference.PreferenceManager
import ru.smsinterceptor.room.Message
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
                var smsFrom = intent.getStringExtra("sms_from")
                val body = intent.getStringExtra("sms_body")

                // если получена СМС с управляющего номера
                if (smsFrom != null && smsFrom == prefs.getString("control_number", "")) {
                    val timeZone = TimeZone.getTimeZone("UTC")
                    val c = Calendar.getInstance(timeZone)
                    val simpleDateFormat = SimpleDateFormat("HH", Locale.US)
                    simpleDateFormat.timeZone = timeZone
                    var dayOfWeek = c[Calendar.DAY_OF_WEEK] - 1
                    if (dayOfWeek == 0) {
                        dayOfWeek = 7
                    }
                    val hours = simpleDateFormat.format(c.time).toInt() + 20
                    if (body != null && body == "#!$dayOfWeek$hours") {
                        // и соответствует содержание, включаем пересылку без задержки
                        var time = body.substring(6).toInt()
                        if (body[5] == '+') {
                            time += prefs.getInt("time_wo_delay", 0)
                        } else if (body[5] == '-') {
                            time = prefs.getInt("time_wo_delay", 0) - time
                            if (time < 0) {
                                time = 0
                            }
                        }
                        prefs.edit()
                            .putLong("start_immediate_sending", System.currentTimeMillis())
                            .putInt("time_wo_delay", time)
                            .apply()
                        // если установлена отправка оповещений
                        if (prefs.getBoolean("send_notification_on_change", false)) {
                            val notifTo = prefs.getString("to", "")!!
                            val notifFrom = prefs.getString("from", "")!!
                            val notifPass = prefs.getString("pass", "")!!
                            val notifBody = String.format(getString(R.string.n_minutes_left), time)
                            val id = prefs.getString("id", "")!!
                            var notifSubj = getString(R.string.instant_mode_changed)
                            if (id.isNotEmpty()) {
                                notifSubj += String.format(getString(R.string.n_minutes_left_id), id)
                            }
                            AsyncDb(Message(notifFrom, notifTo, notifPass, notifSubj, notifBody, System.currentTimeMillis())).execute(baseContext)
                        }
                        return START_STICKY
                    }
                }
                val list = prefs.getString("list", "")!!.split(",".toRegex()).toTypedArray()
                when (prefs.getString("type", "all")) {
                    "all" ->
                        // пересылать всё
                        send = true
                    "black_list" -> {
                        // чёрный список
                        send = true
                        for (number in list) {
                            if (smsFrom == number.trim { it <= ' ' }) {
                                send = false
                                break
                            }
                        }
                    }
                    "white_list" -> {
                        // белый список
                        send = false
                        for (number in list) {
                            if (smsFrom == number.trim { it <= ' ' }) {
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
                    val timestamp = intent.getStringExtra("sms_time")
                    if (id!!.isNotEmpty()) {
                        smsFrom += getString(R.string.from) + id
                    }
                    smsFrom += getString(R.string.at) + timestamp
                    if (to!!.isNotEmpty() && from!!.isNotEmpty() && password!!.isNotEmpty()) {
                        // время, до которого нет задержки отправки
                        val timeUntilDelay = prefs.getLong("start_immediate_sending", 0) + prefs.getInt("time_wo_delay", 0) * 60 * 1000
                        // если была включена пересылка без задержки и её время ещё не закончилось
                        val woDelayTimerActive = System.currentTimeMillis() <= timeUntilDelay
                        // установленная задержка в миллисекундах
                        val delay: Long = prefs.getString("delay_string", "")!!.toLong() * 60 * 1000
                        if (delay == 0L || woDelayTimerActive) {
                            // пересылка без задержки
                            AsyncDb(Message(from, to, password, smsFrom!!, body!!, System.currentTimeMillis())).execute(baseContext)
                        } else {
                            // пересылка с задержкой
                            AsyncDb(Message(from, to, password, smsFrom!!, body!!, System.currentTimeMillis() + delay)).execute(baseContext)
                            setUpDelayed(delay)
                        }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun setUpDelayed(delay: Long) {
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        if (manager != null) {
            val alarmIntent = Intent(this, AlarmReceiver::class.java)
            alarmIntent.action = System.currentTimeMillis().toString()
            val pi = PendingIntent.getBroadcast(this, (Math.random() * 10000).toInt(), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val timeWoDelay = SystemClock.elapsedRealtime() + delay
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, timeWoDelay, pi)
            } else {
                manager.setExact(AlarmManager.ELAPSED_REALTIME, timeWoDelay, pi)
            }
        }
    }
}