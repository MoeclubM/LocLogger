package moe.telecom.loclogger.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 开机后检查是否有未完成的记录
            // 实际应用中可以检查数据库是否有 isRecording=1 的轨迹
            // 这里暂不自动恢复，避免开机就启动服务
        }
    }
}
