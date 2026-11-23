package com.example.btvehiclecontrol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothController(
    private val onStatusChange: (Boolean, String) -> Unit
) {
    // 標準 Bluetooth SPP (Serial Port Profile) 通用 UUID
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // 取得已配對裝置
    @SuppressLint("MissingPermission")
    fun getPairedDevices(adapter: BluetoothAdapter?): Set<BluetoothDevice> {
        return adapter?.bondedDevices ?: emptySet()
    }

    // 於背景 Thread 發起連線
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        Thread {
            try {
                onStatusChange(false, "正在連線至 ${device.name}...")
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
                outputStream = socket?.outputStream

                // 連線成功
                onStatusChange(true, "已成功連線至 ${device.name}")
            } catch (e: IOException) {
                e.printStackTrace()
                disconnect()
                onStatusChange(false, "連線失敗，請檢查 HC-05 電源！")
            }
        }.start()
    }

    // 發送字元指令給 STM32 (如 'F', 'B', 'L', 'R', 'S')
    fun sendCommand(cmd: Char) {
        // 加上這行 Log，即使沒連線也能在 Android Studio 看到指令觸發
        android.util.Log.d("CarDebug", ">>> [遙控指令]: $cmd")

        Thread {
            try {
                outputStream?.write(cmd.code)
                outputStream?.flush() // 即時送出，避免緩衝區延遲
            } catch (e: IOException) {
                e.printStackTrace()
                onStatusChange(false, "連線中斷！")
            }
        }.start()
    }

    // 斷開連線
    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
            socket = null
            outputStream = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}