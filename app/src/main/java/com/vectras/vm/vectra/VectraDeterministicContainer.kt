package com.vectras.vm.vectra

import kotlin.math.min

private const val CONTAINER_BASE60_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwx"
private const val CONTAINER_STATE_COUNT = 10
private const val CONTAINER_MATRIX_GRANULARITY = 1024
private const val CONTAINER_SATURATION_IOPS_THRESHOLD = 4096L
private const val CONTAINER_RECONSOLIDATION_IOPS_DELTA = 192L

data class VectraContainerEntry(
    val path: String,
    val normalizedPath: String,
    val byteSize: Int,
    val chunkSize: Int,
    val chunkCount: Int,
    val layer: Int,
    val lane: Int,
    val matrixCell: Int,
    val waveState: Int,
    val pathVectorBase60: String,
    val entryTag: Long
)

data class VectraContainerManifest(
    val entries: List<VectraContainerEntry>,
    val totalBytes: Long,
    val totalChunks: Int,
    val totalLayers: Int,
    val deterministicTag: Long,
    val writeIops: Long,
    val readIops: Long,
    val cycleStateSn: String,
    val cycleRoutesRn: String,
    val cyclePoliciesPn: String,
    val cycleClosureC: String,
    val cyclePhase: String,
    val saturationSignal: Boolean,
    val reconsolidationSignal: Boolean,
    val troubleshootingSignals: List<String>
)

/**
 * Deterministic layered container for OS-like file-path construction.
 *
 * - Layered storage (zip-like layout) without decompression dependency.
 * - Deterministic path canonicalization and manifest ordering.
 * - Base-60 path vectors + deterministic state index (0..9).
 */
class VectraDeterministicContainer(
    private val chunkSize: Int = 4096,
    private val laneCount: Int = 4,
    private val state: VectraState? = null
) {
    private data class EntryRecord(
        val path: String,
        val normalizedPath: String,
        val layer: Int,
        val lane: Int,
        val matrixCell: Int,
        val byteSize: Int,
        val chunkIds: LongArray,
        val entryTag: Long,
        val waveState: Int,
        val pathVectorBase60: String
    )

    private val chunkStore = LinkedHashMap<Long, ByteArray>()
    private val entries = LinkedHashMap<String, EntryRecord>()
    private var manifestSequence = 0L
    private var writeOps = 0L
    private var readOps = 0L
    private var lastManifestTag = 0L

    fun put(path: String, payload: ByteArray, preferredLayer: Int = 0): VectraContainerEntry {
        val normalized = normalizePath(path)
        val pathHash = stablePathHash(normalized)
        val lane = ((pathHash ushr 1) and 0x7FFFFFFF).toInt() % laneCount
        val matrixCell = matrixCell(pathHash, preferredLayer, lane)
        val pathVector60 = toBase60(pathHash)

        val chunks = splitChunks(payload)
        val chunkIds = LongArray(chunks.size)
        var rolling = pathHash xor payload.size.toLong()

        for (index in chunks.indices) {
            val chunkId = chunkKey(pathHash, index, lane, preferredLayer)
            val chunkCopy = chunks[index]
            chunkStore[chunkId] = chunkCopy
            chunkIds[index] = chunkId
            writeOps++
            rolling = mix64(rolling xor chunkId xor CRC32C.update(0, chunkCopy).toLong())
        }

        val wave = waveState(pathHash, chunks.size, payload.size, matrixCell)
        val tag = mix64(rolling xor wave.toLong() xor (preferredLayer.toLong() shl 32))

        val record = EntryRecord(
            path = path,
            normalizedPath = normalized,
            layer = preferredLayer,
            lane = lane,
            matrixCell = matrixCell,
            byteSize = payload.size,
            chunkIds = chunkIds,
            entryTag = tag,
            waveState = wave,
            pathVectorBase60 = pathVector60
        )

        entries[normalized] = record
        state?.stageCounters?.let {
            it[2] += payload.size.toLong()
            it[4] = tag
        }

        return toPublicEntry(record)
    }

    fun read(path: String): ByteArray? {
        val normalized = normalizePath(path)
        val record = entries[normalized] ?: return null
        val out = ByteArray(record.byteSize)
        var offset = 0
        for (chunkId in record.chunkIds) {
            val chunk = chunkStore[chunkId] ?: return null
            val count = min(chunk.size, out.size - offset)
            System.arraycopy(chunk, 0, out, offset, count)
            offset += count
            readOps++
        }
        return out
    }

    fun buildManifest(): VectraContainerManifest {
        val ordered = entries.values.sortedBy { it.normalizedPath }
        var totalBytes = 0L
        var totalChunks = 0
        var manifestTag = 0x5645435452414D41L

        val outEntries = ArrayList<VectraContainerEntry>(ordered.size)
        for (record in ordered) {
            totalBytes += record.byteSize
            totalChunks += record.chunkIds.size
            manifestTag = mix64(manifestTag xor record.entryTag xor stablePathHash(record.normalizedPath))
            outEntries.add(toPublicEntry(record))
        }

        manifestTag = mix64(manifestTag xor manifestSequence xor totalBytes)
        val cycle = deriveCycleMetrics(
            totalChunks = totalChunks,
            totalLayers = (ordered.maxOfOrNull { it.layer } ?: -1) + 1,
            deterministicTag = manifestTag
        )
        manifestSequence++
        lastManifestTag = manifestTag

        return VectraContainerManifest(
            entries = outEntries,
            totalBytes = totalBytes,
            totalChunks = totalChunks,
            totalLayers = cycle.totalLayers,
            deterministicTag = manifestTag,
            writeIops = writeOps,
            readIops = readOps,
            cycleStateSn = cycle.stateSn,
            cycleRoutesRn = cycle.routesRn,
            cyclePoliciesPn = cycle.policiesPn,
            cycleClosureC = cycle.closureC,
            cyclePhase = cycle.phase,
            saturationSignal = cycle.saturationSignal,
            reconsolidationSignal = cycle.reconsolidationSignal,
            troubleshootingSignals = cycle.troubleshootingSignals
        )
    }


    private data class CycleMetrics(
        val totalLayers: Int,
        val stateSn: String,
        val routesRn: String,
        val policiesPn: String,
        val closureC: String,
        val phase: String,
        val saturationSignal: Boolean,
        val reconsolidationSignal: Boolean,
        val troubleshootingSignals: List<String>
    )

    private fun deriveCycleMetrics(
        totalChunks: Int,
        totalLayers: Int,
        deterministicTag: Long
    ): CycleMetrics {
        val ioTotal = writeOps + readOps
        val ioDelta = kotlin.math.abs(writeOps - readOps)
        val saturationSignal = ioTotal >= CONTAINER_SATURATION_IOPS_THRESHOLD
        val reconsolidationSignal = !saturationSignal && ioDelta <= CONTAINER_RECONSOLIDATION_IOPS_DELTA

        val phase = when {
            saturationSignal -> "SATURA"
            reconsolidationSignal -> "RECOLAPSA"
            else -> "CRESCE"
        }

        val stateSn = "S_${manifestSequence}"
        val routesRn = "R_${totalLayers}L_${totalChunks}C"
        val policiesPn = "P_${laneCount}x${chunkSize}_io(${writeOps}:${readOps})"
        val closureC = "C(${stateSn},${routesRn},${policiesPn})=${toBase60(deterministicTag xor lastManifestTag xor ioTotal)}"

        val signals = ArrayList<String>(3)
        if (saturationSignal) {
            signals.add("saturacao_iops>=${CONTAINER_SATURATION_IOPS_THRESHOLD}")
        }
        if (reconsolidationSignal) {
            signals.add("reconsolidacao_delta<=${CONTAINER_RECONSOLIDATION_IOPS_DELTA}")
        }
        if (!saturationSignal && !reconsolidationSignal) {
            signals.add("crescimento_estavel")
        }

        return CycleMetrics(
            totalLayers = totalLayers,
            stateSn = stateSn,
            routesRn = routesRn,
            policiesPn = policiesPn,
            closureC = closureC,
            phase = phase,
            saturationSignal = saturationSignal,
            reconsolidationSignal = reconsolidationSignal,
            troubleshootingSignals = signals
        )
    }

    private fun toPublicEntry(record: EntryRecord): VectraContainerEntry {
        return VectraContainerEntry(
            path = record.path,
            normalizedPath = record.normalizedPath,
            byteSize = record.byteSize,
            chunkSize = chunkSize,
            chunkCount = record.chunkIds.size,
            layer = record.layer,
            lane = record.lane,
            matrixCell = record.matrixCell,
            waveState = record.waveState,
            pathVectorBase60 = record.pathVectorBase60,
            entryTag = record.entryTag
        )
    }

    private fun splitChunks(payload: ByteArray): List<ByteArray> {
        if (payload.isEmpty()) return listOf(ByteArray(0))
        val result = ArrayList<ByteArray>((payload.size + chunkSize - 1) / chunkSize)
        var offset = 0
        while (offset < payload.size) {
            val end = min(offset + chunkSize, payload.size)
            val chunk = ByteArray(end - offset)
            System.arraycopy(payload, offset, chunk, 0, chunk.size)
            result.add(chunk)
            offset = end
        }
        return result
    }

    private fun normalizePath(path: String): String {
        val parts = path.replace('\\', '/').trim().split('/')
        val stack = ArrayList<String>(parts.size)
        for (part in parts) {
            if (part.isBlank() || part == ".") continue
            if (part == "..") {
                if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
                continue
            }
            stack.add(part)
        }
        return stack.joinToString("/").lowercase()
    }

    private fun stablePathHash(path: String): Long {
        var hash = 0xcbf29ce484222325uL.toLong()
        for (i in path.indices) {
            hash = hash xor path[i].code.toLong()
            hash *= 0x100000001b3uL.toLong()
        }
        return mix64(hash)
    }

    private fun matrixCell(pathHash: Long, layer: Int, lane: Int): Int {
        val folded = (pathHash xor (pathHash ushr 32)).toInt()
        val base = folded xor (layer shl 11) xor (lane shl 6)
        return (base and 0x7FFFFFFF) % CONTAINER_MATRIX_GRANULARITY
    }

    private fun waveState(pathHash: Long, chunks: Int, bytes: Int, matrixCell: Int): Int {
        val folded = (pathHash xor (pathHash ushr 33)).toInt() and 0x7FFFFFFF
        return (folded + (chunks * 257) + (bytes * 7) + (matrixCell * 13)) % CONTAINER_STATE_COUNT
    }

    private fun chunkKey(pathHash: Long, index: Int, lane: Int, layer: Int): Long {
        val header = (lane.toLong() shl 56) xor (layer.toLong() shl 48) xor index.toLong()
        return mix64(pathHash xor header xor (index.toLong() shl 17))
    }

    private fun toBase60(value: Long): String {
        var x = if (value < 0) -value else value
        if (x == 0L) return "0"
        val out = StringBuilder(16)
        while (x > 0) {
            val digit = (x % 60L).toInt()
            out.append(CONTAINER_BASE60_ALPHABET[digit])
            x /= 60L
        }
        return out.reverse().toString()
    }

    private fun mix64(value: Long): Long {
        var x = value + 0x9E3779B97F4A7C15uL.toLong()
        x = (x xor (x ushr 30)) * -0x61c8864680b583ebL
        x = (x xor (x ushr 27)) * -0x4498517a7b3558c5L
        return x xor (x ushr 31)
    }
}
