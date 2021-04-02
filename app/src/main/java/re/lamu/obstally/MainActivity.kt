package re.lamu.obstally

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = "OBS Tally MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 0)
        } else {
            setContentView(R.layout.activity_main)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val sharedPreferences =
                    this.getSharedPreferences("OBS Tally SharedPreferences", Context.MODE_PRIVATE)
            var endpointString = sharedPreferences.getString("endpoint", "")
            var camString = sharedPreferences.getString("cam", "")
            endpoint.setText(endpointString)
            cam.setText(camString)
            b_start_stop.setOnClickListener {
                Log.d(TAG, "bStartStop.setOnClickListener")
                if (isMyServiceRunning(TallyService::class.java)) {
                    stopService(Intent(applicationContext, TallyService::class.java))
                    b_start_stop.setText(R.string.start_button)
                } else {
                    endpointString = endpoint.text.toString()
                    camString = cam.text.toString()
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                    editor.putString("endpoint", endpointString)
                    editor.putString("cam", camString)
                    editor.apply()
                    editor.commit()
                    if (endpointString.isNullOrBlank() or camString.isNullOrBlank()) {
                        Toast.makeText(this, "Invalid endpoint or cam", Toast.LENGTH_SHORT).show()
                    } else {
                        Intent(applicationContext, TallyService::class.java).also { intent ->
                            intent.putExtra("endpoint", endpoint.text.toString())
                            intent.putExtra("cam", cam.text.toString())
                            startService(intent)
                            b_start_stop.setText(R.string.stop_button)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 0)
        } else {
            if (isMyServiceRunning(TallyService::class.java)) {
                b_start_stop.setText(R.string.stop_button)
            } else {
                b_start_stop.setText(R.string.start_button)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        Log.d(TAG, "isMyServiceRunning")
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                Log.d(TAG, "isMyServiceRunning => True")
                return true
            }
        }
        Log.d(TAG, "isMyServiceRunning => False")
        return false
    }
}