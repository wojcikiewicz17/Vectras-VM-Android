package com.vectras.vm.rafaelia

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vectras.vm.R
import com.vectras.vm.databinding.ActivityRafaeliaLogsBinding
import java.io.RandomAccessFile
import java.util.Locale

class RafaeliaLogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRafaeliaLogsBinding
    private val handler = Handler(Looper.getMainLooper())
    private var filePointer = 0L
    private var currentFilter = LogFilter.ALL
    private val logLines: ArrayDeque<String> = ArrayDeque()

    private val pollRunnable = object : Runnable {
        override fun run() {
            readLogIncrementally()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRafaeliaLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.logPath.text = getString(
            R.string.rafaelia_log_path_format,
            RafaeliaSettings.logFile(this).absolutePath
        )

        binding.logFilterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(binding.filterInit.id) -> LogFilter.INIT
                checkedIds.contains(binding.filterTick.id) -> LogFilter.TICK
                checkedIds.contains(binding.filterShutdown.id) -> LogFilter.SHUTDOWN
                checkedIds.contains(binding.filterEntropy.id) -> LogFilter.ENTROPY
                checkedIds.contains(binding.filterCoherence.id) -> LogFilter.COHERENCE
                else -> LogFilter.ALL
            }
            renderLogs()
        }

        binding.btnSnapshotBin.setOnClickListener {
            val file = RafaeliaEventRecorder.snapshot(this)
            val message = file?.absolutePath ?: getString(R.string.rafaelia_snapshot_missing)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

        binding.btnSnapshotJson.setOnClickListener {
            val file = RafaeliaEventRecorder.exportJsonSnapshot(this)
            val message = file?.absolutePath ?: getString(R.string.rafaelia_snapshot_missing)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

        refreshTorusHotfix()
        refreshBenchReport()
    }

    override fun onResume() {
        super.onResume()
        refreshTorusHotfix()
        refreshBenchReport()
        handler.post(pollRunnable)
    }

    private fun refreshTorusHotfix() {
        val p = IntArray(128)
        val i = IntArray(128)
        val o = IntArray(128)
        var s = 0x963
        val g = ((91830L * 16384L) shr 16).toInt()
        for (x in 0 until 128) {
            p[x] = 65536
            i[x] = ((x.toLong() shl 16) / 128L).toInt()
            o[x] = 0
        }
        for (k in 0 until 12) {
            var r = s xor ((k + 1) * 0x9E3779B9.toInt())
            for (x in 0 until 128) {
                r = r * 1103515245 + 12345
                i[x] = r and 0xFFFF
            }
            s = r
            for (x in 0 until 128) {
                val d = i[x] xor ((s xor (x * 0x9E3779B9.toInt())) and 0x0FFF)
                val a = ((p[x].toLong() * 49152L) shr 16).toInt()
                val b = ((d.toLong() * g.toLong()) shr 16).toInt()
                o[x] = a + b
                p[x] = o[x]
                s = s * 1664525 + 1013904223
            }
        }
        var checksum = 12 * 0xA5A5A5A5.toInt()
        for (x in 0 until 128) {
            checksum = checksum xor (o[x] + (x * 17))
            checksum = (checksum shl 3) or (checksum ushr 29)
        }
        binding.torusHotfixValue.text = getString(
            R.string.rafaelia_torus_hotfix_value_format,
            checksum
        )
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(pollRunnable)
    }

    private fun refreshBenchReport() {
        val report = RafaeliaReportStorage.loadBenchReport(this)
        binding.benchReportText.text = report ?: getString(R.string.rafaelia_bench_report_empty)
    }

    private fun readLogIncrementally() {
        val file = RafaeliaSettings.logFile(this)
        if (!file.exists()) {
            binding.logOutput.text = getString(R.string.rafaelia_log_empty)
            return
        }

        RandomAccessFile(file, "r").use { raf ->
            // Maintenance note: if log capture/VM restarts and truncates the file,
            // reset incremental state to avoid seeking past EOF and mixing old/new sessions.
            if (file.length() < filePointer) {
                filePointer = 0L
                logLines.clear()
            }
            raf.seek(filePointer)
            var line = raf.readLine()
            while (line != null) {
                val decoded = line.toByteArray(Charsets.ISO_8859_1).toString(Charsets.UTF_8)
                logLines.add(decoded)
                if (logLines.size > 300) {
                    logLines.removeFirst()
                }
                line = raf.readLine()
            }
            filePointer = raf.filePointer
        }
        renderLogs()
    }

    private fun renderLogs() {
        if (logLines.isEmpty()) {
            binding.logOutput.text = getString(R.string.rafaelia_log_empty)
            return
        }
        val filtered = logLines.filter { filterLine(it) }
        binding.logOutput.text = if (filtered.isEmpty()) {
            getString(R.string.rafaelia_log_filtered_empty)
        } else {
            filtered.joinToString("\n")
        }
    }

    private fun filterLine(line: String): Boolean {
        if (!line.contains("RAFAELIA", ignoreCase = true)) return false
        val lower = line.lowercase(Locale.US)
        return when (currentFilter) {
            LogFilter.ALL -> true
            LogFilter.INIT -> lower.contains("init")
            LogFilter.TICK -> lower.contains("tick")
            LogFilter.SHUTDOWN -> lower.contains("shutdown")
            LogFilter.ENTROPY -> lower.contains("entropy")
            LogFilter.COHERENCE -> lower.contains("coherence")
        }
    }

    enum class LogFilter {
        ALL,
        INIT,
        TICK,
        SHUTDOWN,
        ENTROPY,
        COHERENCE
    }
}
