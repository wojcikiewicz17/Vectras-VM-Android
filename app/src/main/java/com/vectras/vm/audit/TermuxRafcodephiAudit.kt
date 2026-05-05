package com.vectras.vm.audit

enum class EvidenceStatus {
    DOCUMENTED,
    STATIC_CHECKED,
    CI_CHECKED,
    DEVICE_PENDING,
    DEVICE_VALIDATED,
    EXPERIMENTAL,
    NOT_CERTIFIED
}

data class BenchmarkMetric(
    val id: String,
    val name: String,
    val category: String,
    val unit: String,
    val value: String?,
    val status: EvidenceStatus,
    val source: String,
    val notes: String
)

data class TermuxRafcodephiAudit(
    val packageName: String = "com.termux.rafacodephi",
    val versionName: String = "unknown",
    val versionCode: Int = -1,
    val minSdk: Int = -1,
    val targetSdk: Int = -1,
    val compileSdk: Int = -1,
    val ndkVersion: String = "unknown",
    val supportedAbis: List<String> = emptyList(),
    val bootstrapStatus: EvidenceStatus = EvidenceStatus.DOCUMENTED,
    val bootstrapRuntimeReady: EvidenceStatus = EvidenceStatus.DEVICE_PENDING,
    val bootstrapBlake3Status: EvidenceStatus = EvidenceStatus.DOCUMENTED,
    val pageSizeSupport16kb: EvidenceStatus = EvidenceStatus.DOCUMENTED,
    val rmrPureCoreAvailable: EvidenceStatus = EvidenceStatus.DOCUMENTED,
    val rmrNoMallocMode: EvidenceStatus = EvidenceStatus.DOCUMENTED,
    val rmrNoHeapMode: EvidenceStatus = EvidenceStatus.DOCUMENTED,
    val rmrNoLibmMode: EvidenceStatus = EvidenceStatus.DOCUMENTED,
    val jniDirectAvailable: EvidenceStatus = EvidenceStatus.DOCUMENTED,
    val terminalLifecycleGuards: EvidenceStatus = EvidenceStatus.STATIC_CHECKED,
    val processGroupKillEnabled: EvidenceStatus = EvidenceStatus.STATIC_CHECKED,
    val waitForEintrHandlingEnabled: EvidenceStatus = EvidenceStatus.STATIC_CHECKED,
    val benchmarkStatus: EvidenceStatus = EvidenceStatus.CI_CHECKED,
    val deviceRuntimeStatus: EvidenceStatus = EvidenceStatus.DEVICE_PENDING,
    val isoClaimStatus: String = "NOT_CERTIFIED_INTERNAL_REFERENCE_ONLY",
    val benchmarkSummary: Map<String, String> = emptyMap(),
    val benchmarkMetrics: List<BenchmarkMetric> = emptyList(),
    val runtimeDeviceStatus: String = "DEVICE_PENDING",
    val evidenceStatus: Map<String, EvidenceStatus> = emptyMap()
) {
    fun exportAuditJson(): String {
        val metricsJson = benchmarkMetrics.joinToString(prefix = "[", postfix = "]") {
            """{"id":"${it.id}","name":"${it.name}","category":"${it.category}","unit":"${it.unit}","value":${it.value?.let { v -> "\"$v\"" } ?: "null"},"status":"${it.status}","source":"${it.source}","notes":"${it.notes}"}"""
        }
        val summaryJson = benchmarkSummary.entries.joinToString(prefix = "{", postfix = "}") { (k, v) -> "\"$k\":\"$v\"" }
        val evidenceJson = evidenceStatus.entries.joinToString(prefix = "{", postfix = "}") { (k, v) -> "\"$k\":\"$v\"" }
        return """
            {
              "package_name": "$packageName",
              "version_name": "$versionName",
              "version_code": $versionCode,
              "min_sdk": $minSdk,
              "target_sdk": $targetSdk,
              "compile_sdk": $compileSdk,
              "ndk_version": "$ndkVersion",
              "supported_abis": ${supportedAbis.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")},
              "benchmark_summary": $summaryJson,
              "benchmark_metrics": $metricsJson,
              "runtime_device_status": "$runtimeDeviceStatus",
              "iso_notice": "Internal checklist only. No ISO certification or formal compliance claim is made.",
              "iso_claim_status": "$isoClaimStatus",
              "evidence_status": $evidenceJson
            }
        """.trimIndent()
    }
}
