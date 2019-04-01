package com.gottlicher.pedal4me

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.coroutines.onClick
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.shawnlin.numberpicker.NumberPicker
import com.shawnlin.numberpicker.NumberPicker.OnScrollListener.SCROLL_STATE_IDLE


class MainActivity : AppCompatActivity() {

    val PREF_NAME:String = "MAIN"
    val PREF_IP:String = "IP"
    val PREF_PIN:String = "PIN"

    lateinit var api:PedalerApi
    var isRunning:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton.onClick { startStopClick() }
        guideButton.onClick { openPinGuide() }
        scanButton.onClick { goToScanner() }
        val prefs = getPrefs()
        ipTextView.setText(prefs.first)
        bcmPinText.setText(prefs.second.toString())
        val listener = PickerListener()
        rpmPicker.setOnValueChangedListener (listener)
        rpmPicker.setOnScrollListener(listener)
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch(Dispatchers.Main) { checkStatus() }
    }

    private suspend fun checkStatus(){
        api = makeRetrofit(ipTextView.text.toString())
        startButton.isEnabled = false
        try {
            val response = api.statusAsync().await()

            if (!response.isSuccessful){
                Toast.makeText(this, "Error ${response.errorBody().toString()}", Toast.LENGTH_LONG).show()
                return
            }
            val running = response.body()!!.isRunning
            setButtonWheenState(running)
            if (running) {
                bcmPinText.setText(response.body()!!.bcmPin.toString())
                rpmPicker.value = response.body()!!.rpm
            }
        } catch (e:Exception) {
            Toast.makeText(this, "Error $e", Toast.LENGTH_LONG).show()
            return
        } finally {
            startButton.isEnabled = true

        }
    }

    private suspend fun onRpmChanged() {
        if (!isRunning) return
        rpmPicker.isEnabled = false
        try {
            start()
        } finally {
            rpmPicker.isEnabled = true
        }
    }

    private suspend fun startStopClick() {
        savePrefs()
        api = makeRetrofit(ipTextView.text.toString())
        try {
            if (isRunning) {
                stop()
            } else {
                start()
            }
        } catch (e:Exception) {
            Toast.makeText(this, "Error $e", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun start(){
        startButton.isEnabled = false
        val response = api.setAsync(SetParams(bcmPinText.text.toString().toInt(), rpmPicker.value)).await()
        startButton.isEnabled = true
        if (response.isSuccessful) {
            setButtonWheenState(true)
        } else {
            Toast.makeText(this, "Error ${response.errorBody().toString()}", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun stop(){
        startButton.isEnabled = false
        val response = api.stopAsync().await()
        startButton.isEnabled = true
        if (response.isSuccessful) {
            setButtonWheenState(false)
        } else {
            Toast.makeText(this, "Error ${response.errorBody().toString()}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setButtonWheenState (running:Boolean) {
        isRunning = running;
        if (isRunning) {
            startButton.text = "Stop"
            startButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorStop)
            val anim = AnimationUtils.loadAnimation(this, R.anim.rotation)
            wheelImage.startAnimation(anim)
        } else {
            startButton.text = "Start"
            startButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorGo)
            wheelImage.animation?.cancel()
        }
    }

    private fun goToScanner() {
        startActivityForResult(Intent(this, ScannerActivity::class.java), 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == 1) {
            val ip = data?.getStringExtra("IP")
            ipTextView.setText(ip)
        }
    }

    private fun makeRetrofit(url:String): PedalerApi {

        val retrofit = Retrofit.Builder()
            .baseUrl("http://$url/")
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(PedalerApi::class.java)
    }

    private fun getPrefs():Pair<String,Int>{
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val ip = prefs.getString(PREF_IP, "192.168.1.1")
        val pin = prefs.getInt(PREF_PIN, 21)
        return Pair(ip, pin)
    }

    private fun savePrefs(){
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(PREF_IP, ipTextView.text.toString())
        editor.putInt(PREF_PIN, bcmPinText.text.toString().toInt())
        editor.apply()
    }

    private fun openPinGuide(){
        val url = "https://pinout.xyz/"
        CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
    }

    private inner class PickerListener : NumberPicker.OnScrollListener, NumberPicker.OnValueChangeListener {
        private var scrollState = 0
        override fun onScrollStateChange(view: NumberPicker, scrollState: Int) {
            this.scrollState = scrollState
            if (scrollState == SCROLL_STATE_IDLE) {
                GlobalScope.launch(Dispatchers.Main) { onRpmChanged() }
            }
        }

        override fun onValueChange(picker: NumberPicker, oldVal: Int, newVal: Int) {
            if (scrollState == 0) {
                GlobalScope.launch(Dispatchers.Main) { onRpmChanged() }
            }
        }
    }
}
