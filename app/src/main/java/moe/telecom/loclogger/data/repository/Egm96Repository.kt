package moe.telecom.loclogger.data.repository

import android.content.Context
import android.net.Uri
import moe.telecom.loclogger.util.Egm96
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** EGM96 数据文件状态 */
sealed interface Egm96State {
    data object NotInstalled : Egm96State
    data object Downloading : Egm96State
    data class Progress(val percent: Int) : Egm96State
    data object Ready : Egm96State
    data class Error(val message: String) : Egm96State
}

@Singleton
class Egm96Repository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val FILE_NAME = "WW15MGH.DAC"
        const val EXPECTED_SIZE = 2076480L
        const val DOWNLOAD_URL = "https://cdn.jsdelivr.net/gh/jleppert/egm96@master/WW15MGH.DAC"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<Egm96State>(Egm96State.NotInstalled)
    val state: StateFlow<Egm96State> = _state.asStateFlow()

    private val dataFile: File get() = File(context.filesDir, FILE_NAME)

    val isInstalled: Boolean
        get() = dataFile.exists() && dataFile.length() == EXPECTED_SIZE

    /** 有本地数据则载入内存，供录制时实时查询 */
    fun ensureLoaded() {
        if (Egm96.isLoaded) return
        if (isInstalled) runCatching { Egm96.load(dataFile.readBytes()) }
    }

    /** 根据本地文件刷新状态（界面打开时调用） */
    fun refreshState() {
        if (_state.value is Egm96State.Downloading) return
        _state.value = if (isInstalled) Egm96State.Ready else Egm96State.NotInstalled
    }

    /** 一键下载 EGM96 数据文件 */
    fun download() {
        if (_state.value is Egm96State.Downloading) return
        _state.value = Egm96State.Downloading
        scope.launch {
            var tmp: File? = null
            try {
                val conn = URL(DOWNLOAD_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.connect()
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    _state.value = Egm96State.Error("下载失败 HTTP " + conn.responseCode)
                    return@launch
                }
                val total = conn.contentLengthLong
                tmp = File.createTempFile("egm96", ".tmp", context.cacheDir)
                conn.inputStream.buffered().use { input ->
                    tmp!!.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) _state.value = Egm96State.Progress(((done * 100) / total).toInt())
                        }
                    }
                }
                if (tmp!!.length() != EXPECTED_SIZE) {
                    _state.value = Egm96State.Error("数据文件大小不正确")
                    return@launch
                }
                dataFile.parentFile?.mkdirs()
                tmp.copyTo(dataFile, overwrite = true)
                Egm96.load(dataFile.readBytes())
                _state.value = Egm96State.Ready
            } catch (e: Exception) {
                _state.value = Egm96State.Error(e.message ?: "下载失败")
            } finally {
                tmp?.delete()
            }
        }
    }

    /** 手动导入数据文件（SAF） */
    fun importUri(uri: Uri) {
        scope.launch {
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: run {
                        _state.value = Egm96State.Error("无法读取所选文件")
                        return@launch
                    }
                dataFile.parentFile?.mkdirs()
                input.use { source ->
                    dataFile.outputStream().use { out -> source.copyTo(out) }
                }
                if (dataFile.length() != EXPECTED_SIZE) {
                    dataFile.delete()
                    _state.value = Egm96State.Error("文件大小不正确，请选择 WW15MGH.DAC")
                    return@launch
                }
                Egm96.load(dataFile.readBytes())
                _state.value = Egm96State.Ready
            } catch (e: Exception) {
                _state.value = Egm96State.Error(e.message ?: "导入失败")
            }
        }
    }
}
