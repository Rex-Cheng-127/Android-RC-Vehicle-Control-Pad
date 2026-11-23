package com.example.btvehiclecontrol

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var btController: BluetoothController

    private lateinit var tvStatus: TextView
    private lateinit var btnConnect: Button

    // Android 13 權限請求 Callback (使用標準 RequestPermission)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            connectToHC05()
        } else {
            Toast.makeText(this, "需要藍芽權限才能連接遙控車！", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 取得藍芽介面卡
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        tvStatus = findViewById(R.id.tvStatus)
        btnConnect = findViewById(R.id.btnConnect)

        // 初始化藍芽控制器
        btController = BluetoothController { isConnected, message ->
            runOnUiThread {
                tvStatus.text = "狀態：$message"
                tvStatus.setTextColor(if (isConnected) Color.parseColor("#2E7D32") else Color.RED)
            }
        }

        // 點擊「連線至 HC-05」按鈕
        btnConnect.setOnClickListener {
            checkAndConnect()
        }

        // 設定控制方向按鈕
        setupControlButtons()
    }

    // 檢查 Android 13 權限
    private fun checkAndConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                // 尚未取得權限，跳窗請求
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
        }
        connectToHC05()
    }

    // 搜尋已配對的 HC-05 並發起連線
    @SuppressLint("MissingPermission")
    private fun connectToHC05() {
        val pairedDevices = btController.getPairedDevices(bluetoothAdapter)
        val hc05Device = pairedDevices.find { it.name?.contains("HC-05", ignoreCase = true) == true || it.name?.contains( other = "REXCHENGHQ", ignoreCase = true) == true }
        if (hc05Device != null) {
            btController.connect(hc05Device)
        } else {
            Toast.makeText(this, "找不到已配對的 HC-05！請先在手機系統設定中配對", Toast.LENGTH_LONG).show()
        }
    }

    // 設定操控按鈕 (按住前進/後退/轉彎，放開自動煞車)
    @SuppressLint("ClickableViewAccessibility")
    private fun setupControlButtons() {
        val btnForward = findViewById<Button>(R.id.btnForward)
        val btnBackward = findViewById<Button>(R.id.btnBackward)
        val btnLeft = findViewById<Button>(R.id.btnLeft)
        val btnRight = findViewById<Button>(R.id.btnRight)
        val btnStop = findViewById<Button>(R.id.btnStop)

        // 前進
        btnForward.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> btController.sendCommand('F')
                MotionEvent.ACTION_UP -> btController.sendCommand('S')
            }
            true
        }

        // 後退
        btnBackward.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> btController.sendCommand('B')
                MotionEvent.ACTION_UP -> btController.sendCommand('S')
            }
            true
        }

        // 左轉
        btnLeft.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> btController.sendCommand('L')
                MotionEvent.ACTION_UP -> btController.sendCommand('S')
            }
            true
        }

        // 右轉
        btnRight.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> btController.sendCommand('R')
                MotionEvent.ACTION_UP -> btController.sendCommand('S')
            }
            true
        }

        // 單擊手動煞車按鈕
        btnStop.setOnClickListener {
            btController.sendCommand('S')
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        btController.disconnect()
    }
}