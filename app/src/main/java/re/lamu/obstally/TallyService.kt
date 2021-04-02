@file:Suppress("DEPRECATION")

package re.lamu.obstally

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.AsyncTask
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets

class TallyService : Service() {

    private var endpoint: String? = null
    private var cam: String? = null
    private var task: RMQTask? = null

    override fun onBind(p0: Intent?): IBinder? {
        Log.d(TAG, "onBind")
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
        notificationManager?.createNotificationChannel(channel)
        setForeground()
    }

    private fun setForeground() {
        Log.d(TAG, "setForeground")
        val notification = NotificationCompat.Builder(this, channelId)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setContentTitle("Tally is Live")
                .setContentText("Using MQ").build()
        startForeground(1, notification)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            Log.d(TAG, "FOREGROUND SERVICE TYPE: $foregroundServiceType")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        endpoint = intent?.extras?.getString("endpoint")
        cam = intent?.extras?.getString("cam")
        if (endpoint != null) {

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            tallyView = LinearLayout(this)
            val lparams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lparams.setMargins(10, 10, 10, 10)
            lparams.width = 100
            lparams.height = 100
            tallyView?.layoutParams = lparams
            tallyView?.setBackgroundColor(Color.parseColor("#888888"))

            tallyText = TextView(this)
            tallyText?.textSize = 25.0F
            tallyText?.setTextColor(Color.WHITE)
            tallyText?.layoutParams = lparams
            tallyText?.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL

            task = RMQTask(this)
            task!!.execute(endpoint)

            tallyText?.text = "0"

            val wparams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            )
            wparams.gravity = Gravity.TOP or Gravity.START
            wparams.x = 100
            wparams.y = 100

            tallyView?.addView(tallyText)
            windowManager?.addView(tallyView, wparams)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        if (task != null) {
            task?.cancel(true)
        }
        if (tallyView != null) {
            windowManager?.removeView(tallyView)
        }
    }

    companion object {
        private val TAG = "OBS Tally Service"
        private val channelId = "tallyChannel"
        private var windowManager: WindowManager? = null
        private var tallyText: TextView? = null
        private var tallyView: LinearLayout? = null
        private var notificationManager: NotificationManager? = null
        private var currentScene: String? = null
        private var currentPreviewScene: String? = null
    }

    private class RMQTask(context: TallyService) : AsyncTask<String, String, String>() {

        private val activityReference: WeakReference<TallyService> = WeakReference(context)

        override fun doInBackground(vararg params: String): String {
            val endpoint = params[0]
            val factory = ConnectionFactory()
            factory.host = endpoint
            factory.username = BuildConfig.amqp_username
            factory.password = BuildConfig.amqp_password
            factory.isAutomaticRecoveryEnabled = true
            val connection = factory.newConnection()
            val channel = connection.createChannel()
            val queue = channel.queueDeclare("", true, true, true, null).queue
            channel.queueBind(queue, "cam", "")
            Log.d(TAG, "[$queue] Waiting for messages...")
            val deliverCallback = DeliverCallback { consumerTag: String?, delivery: Delivery ->
                val message = String(delivery.body, StandardCharsets.UTF_8)
                Log.d(TAG, "[$queue] Received message: '$message'")
                publishProgress(message)
            }
            val cancelCallback = CancelCallback { consumerTag: String? ->
                Log.d(TAG, "[$queue] was canceled")
            }
            channel.basicConsume(queue, true, "", deliverCallback, cancelCallback)
            return ""
        }

        override fun onProgressUpdate(vararg values: String) {
            val activity = activityReference.get()
            if (activity != null) {
                val message = values[0]
                if ("^[0-9]$".toRegex().containsMatchIn(message)) {
                    currentScene = message
                    tallyText?.text = message
                } else if ("^p_[0-9]$".toRegex().containsMatchIn(message))
                    currentPreviewScene = message

                if (currentScene == activity.cam)
                    tallyView?.setBackgroundColor(Color.parseColor("#CC0000"))
                else if (currentPreviewScene == "p_" + activity.cam)
                    tallyView?.setBackgroundColor(Color.parseColor("#00CC00"))
                else
                    tallyView?.setBackgroundColor(Color.parseColor("#888888"))
            }

        }
    }
}