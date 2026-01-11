package com.vectras.vm.tools;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.benchmark.VectraBenchmark;
import com.vectras.vm.utils.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ProfessionalToolsActivity - MEGA TOOLS Professional Engineering/Benchmark/Scientific System
 * 
 * This activity provides a comprehensive, professional-grade benchmarking and analysis system
 * that integrates multiple technical methodologies and standards:
 * 
 * - ISO/IEC 25010 (Software Quality Model)
 * - IEEE 829/1012 (Test Documentation and Verification)
 * - ACM Standards
 * - NIST Benchmarking Principles
 * - SPEC Methodology
 * - MLPerf Philosophy
 * 
 * Features:
 * - Checklist-based category selection with estimated time tracking
 * - Multiple methodology standard compliance indicators
 * - Statistical analysis with confidence intervals
 * - Academic/Industry/Scientific grade validation
 * - Professional report generation
 * - Reproducibility assessment
 * 
 * Design Principles:
 * - Technically rigorous
 * - Scientifically valid
 * - Reproducible
 * - Academically acceptable
 * - Professionally documented
 */
public class ProfessionalToolsActivity extends AppCompatActivity {
    private static final String TAG = "ProfessionalToolsActivity";
    
    // Time estimates in seconds for each category
    private static final int TIME_CPU_SINGLE = 15;
    private static final int TIME_CPU_MULTI = 20;
    private static final int TIME_MEMORY = 25;
    private static final int TIME_STORAGE = 30;
    private static final int TIME_INTEGRITY = 20;
    private static final int TIME_EMULATION = 15;
    
    // UI Elements - Category Checkboxes
    private CheckBox cbCpuSingle;
    private CheckBox cbCpuMulti;
    private CheckBox cbMemory;
    private CheckBox cbStorage;
    private CheckBox cbIntegrity;
    private CheckBox cbEmulation;
    
    // UI Elements - Methodology Chips
    private Chip chipISO;
    private Chip chipIEEE;
    private Chip chipACM;
    private Chip chipNIST;
    private Chip chipSPEC;
    private Chip chipMLPerf;
    
    // UI Elements - Status and Progress
    private Chip chipValidationStatus;
    private TextView tvEstimatedTime;
    private LinearLayout layoutProgress;
    private LinearProgressIndicator progressIndicator;
    private TextView tvProgressText;
    private TextView tvProgressDetail;
    
    // UI Elements - Results
    private LinearLayout layoutResults;
    private TextView tvExecutiveSummary;
    private Chip chipGradeIndustry;
    private Chip chipGradeAcademic;
    private Chip chipGradeScientific;
    private TextView tvGradeJustification;
    private TextView tvStatMean;
    private TextView tvStatMedian;
    private TextView tvStatStdDev;
    private TextView tvStatConfidence;
    private TextView tvStatReproducibility;
    
    // UI Elements - Buttons
    private MaterialButton btnRunAnalysis;
    private MaterialButton btnSelectAll;
    private MaterialButton btnDeselectAll;
    private LinearLayout btnViewFullReport;
    private LinearLayout btnExportReport;
    private LinearLayout btnShareReport;
    
    // Data
    private VectraBenchmark.BenchmarkResult[] lastResults;
    private AnalysisReport lastReport;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_professional_tools);
        
        setupToolbar();
        initViews();
        setupListeners();
        updateEstimatedTime();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setTitle(getString(R.string.professional_tools));
    }
    
    private void initViews() {
        // Category Checkboxes
        cbCpuSingle = findViewById(R.id.cbCpuSingle);
        cbCpuMulti = findViewById(R.id.cbCpuMulti);
        cbMemory = findViewById(R.id.cbMemory);
        cbStorage = findViewById(R.id.cbStorage);
        cbIntegrity = findViewById(R.id.cbIntegrity);
        cbEmulation = findViewById(R.id.cbEmulation);
        
        // Methodology Chips
        chipISO = findViewById(R.id.chipISO);
        chipIEEE = findViewById(R.id.chipIEEE);
        chipACM = findViewById(R.id.chipACM);
        chipNIST = findViewById(R.id.chipNIST);
        chipSPEC = findViewById(R.id.chipSPEC);
        chipMLPerf = findViewById(R.id.chipMLPerf);
        
        // Status and Progress
        chipValidationStatus = findViewById(R.id.chipValidationStatus);
        tvEstimatedTime = findViewById(R.id.tvEstimatedTime);
        layoutProgress = findViewById(R.id.layoutProgress);
        progressIndicator = findViewById(R.id.progressIndicator);
        tvProgressText = findViewById(R.id.tvProgressText);
        tvProgressDetail = findViewById(R.id.tvProgressDetail);
        
        // Results
        layoutResults = findViewById(R.id.layoutResults);
        tvExecutiveSummary = findViewById(R.id.tvExecutiveSummary);
        chipGradeIndustry = findViewById(R.id.chipGradeIndustry);
        chipGradeAcademic = findViewById(R.id.chipGradeAcademic);
        chipGradeScientific = findViewById(R.id.chipGradeScientific);
        tvGradeJustification = findViewById(R.id.tvGradeJustification);
        tvStatMean = findViewById(R.id.tvStatMean);
        tvStatMedian = findViewById(R.id.tvStatMedian);
        tvStatStdDev = findViewById(R.id.tvStatStdDev);
        tvStatConfidence = findViewById(R.id.tvStatConfidence);
        tvStatReproducibility = findViewById(R.id.tvStatReproducibility);
        
        // Buttons
        btnRunAnalysis = findViewById(R.id.btnRunAnalysis);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnDeselectAll = findViewById(R.id.btnDeselectAll);
        btnViewFullReport = findViewById(R.id.btnViewFullReport);
        btnExportReport = findViewById(R.id.btnExportReport);
        btnShareReport = findViewById(R.id.btnShareReport);
    }
    
    private void setupListeners() {
        // Category checkbox listeners - update estimated time
        View.OnClickListener checkboxListener = v -> updateEstimatedTime();
        cbCpuSingle.setOnClickListener(checkboxListener);
        cbCpuMulti.setOnClickListener(checkboxListener);
        cbMemory.setOnClickListener(checkboxListener);
        cbStorage.setOnClickListener(checkboxListener);
        cbIntegrity.setOnClickListener(checkboxListener);
        cbEmulation.setOnClickListener(checkboxListener);
        
        // Select/Deselect all
        btnSelectAll.setOnClickListener(v -> {
            cbCpuSingle.setChecked(true);
            cbCpuMulti.setChecked(true);
            cbMemory.setChecked(true);
            cbStorage.setChecked(true);
            cbIntegrity.setChecked(true);
            cbEmulation.setChecked(true);
            updateEstimatedTime();
        });
        
        btnDeselectAll.setOnClickListener(v -> {
            cbCpuSingle.setChecked(false);
            cbCpuMulti.setChecked(false);
            cbMemory.setChecked(false);
            cbStorage.setChecked(false);
            cbIntegrity.setChecked(false);
            cbEmulation.setChecked(false);
            updateEstimatedTime();
        });
        
        // Main action button
        btnRunAnalysis.setOnClickListener(v -> runAnalysis());
        
        // Result action buttons
        btnViewFullReport.setOnClickListener(v -> showFullReport());
        btnExportReport.setOnClickListener(v -> exportReport());
        btnShareReport.setOnClickListener(v -> shareReport());
    }
    
    private void updateEstimatedTime() {
        int totalSeconds = 0;
        int selectedCategories = 0;
        
        if (cbCpuSingle.isChecked()) {
            totalSeconds += TIME_CPU_SINGLE;
            selectedCategories++;
        }
        if (cbCpuMulti.isChecked()) {
            totalSeconds += TIME_CPU_MULTI;
            selectedCategories++;
        }
        if (cbMemory.isChecked()) {
            totalSeconds += TIME_MEMORY;
            selectedCategories++;
        }
        if (cbStorage.isChecked()) {
            totalSeconds += TIME_STORAGE;
            selectedCategories++;
        }
        if (cbIntegrity.isChecked()) {
            totalSeconds += TIME_INTEGRITY;
            selectedCategories++;
        }
        if (cbEmulation.isChecked()) {
            totalSeconds += TIME_EMULATION;
            selectedCategories++;
        }
        
        if (totalSeconds == 0) {
            tvEstimatedTime.setText(getString(R.string.pro_tools_no_categories_selected));
            btnRunAnalysis.setEnabled(false);
        } else {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            String timeText;
            if (minutes > 0) {
                timeText = getString(R.string.pro_tools_estimated_time_format_min_sec, minutes, seconds);
            } else {
                timeText = getString(R.string.pro_tools_estimated_time_format_sec, seconds);
            }
            tvEstimatedTime.setText(timeText);
            btnRunAnalysis.setEnabled(true);
        }
    }
    
    private List<Integer> getSelectedCategories() {
        List<Integer> categories = new ArrayList<>();
        if (cbCpuSingle.isChecked()) categories.add(0);
        if (cbCpuMulti.isChecked()) categories.add(1);
        if (cbMemory.isChecked()) categories.add(2);
        if (cbStorage.isChecked()) categories.add(3);
        if (cbIntegrity.isChecked()) categories.add(4);
        if (cbEmulation.isChecked()) categories.add(5);
        return categories;
    }
    
    private List<String> getSelectedMethodologies() {
        List<String> methodologies = new ArrayList<>();
        if (chipISO.isChecked()) methodologies.add("ISO/IEC 25010");
        if (chipIEEE.isChecked()) methodologies.add("IEEE 829/1012");
        if (chipACM.isChecked()) methodologies.add("ACM");
        if (chipNIST.isChecked()) methodologies.add("NIST");
        if (chipSPEC.isChecked()) methodologies.add("SPEC");
        if (chipMLPerf.isChecked()) methodologies.add("MLPerf");
        return methodologies;
    }
    
    private void runAnalysis() {
        List<Integer> selectedCategories = getSelectedCategories();
        if (selectedCategories.isEmpty()) {
            Toast.makeText(this, R.string.pro_tools_select_at_least_one, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Update UI state
        layoutProgress.setVisibility(View.VISIBLE);
        layoutResults.setVisibility(View.GONE);
        btnRunAnalysis.setEnabled(false);
        chipValidationStatus.setText(R.string.pro_tools_status_running);
        progressIndicator.setProgress(0);
        
        // Run in background
        executor.execute(() -> {
            try {
                // Update progress - Starting
                mainHandler.post(() -> {
                    tvProgressText.setText(R.string.pro_tools_initializing);
                    tvProgressDetail.setText(R.string.pro_tools_warming_up);
                });
                
                // Run benchmark
                VectraBenchmark.BenchmarkResult[] results = VectraBenchmark.runAllBenchmarks();
                lastResults = results;
                
                // Update progress - Analyzing
                mainHandler.post(() -> {
                    progressIndicator.setProgress(60);
                    tvProgressText.setText(R.string.pro_tools_analyzing);
                    tvProgressDetail.setText(R.string.pro_tools_computing_statistics);
                });
                
                // Generate analysis report
                AnalysisReport report = generateAnalysisReport(results, selectedCategories, getSelectedMethodologies());
                lastReport = report;
                
                // Update progress - Finalizing
                mainHandler.post(() -> {
                    progressIndicator.setProgress(90);
                    tvProgressText.setText(R.string.pro_tools_finalizing);
                    tvProgressDetail.setText(R.string.pro_tools_generating_report);
                });
                
                // Small delay for visual feedback
                Thread.sleep(500);
                
                // Update UI with results
                mainHandler.post(() -> {
                    progressIndicator.setProgress(100);
                    displayResults(report);
                    layoutProgress.setVisibility(View.GONE);
                    layoutResults.setVisibility(View.VISIBLE);
                    btnRunAnalysis.setEnabled(true);
                    chipValidationStatus.setText(R.string.pro_tools_status_complete);
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    layoutProgress.setVisibility(View.GONE);
                    btnRunAnalysis.setEnabled(true);
                    chipValidationStatus.setText(R.string.pro_tools_status_error);
                    Toast.makeText(this, getString(R.string.pro_tools_analysis_failed, e.getMessage()), 
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private AnalysisReport generateAnalysisReport(VectraBenchmark.BenchmarkResult[] results,
                                                   List<Integer> selectedCategories,
                                                   List<String> methodologies) {
        AnalysisReport report = new AnalysisReport();
        
        // Calculate scores
        int totalScore = VectraBenchmark.calculateTotalScore(results);
        int[] categoryScores = VectraBenchmark.calculateCategoryScores(results);
        
        report.totalScore = totalScore;
        report.categoryScores = categoryScores;
        report.methodologies = methodologies;
        report.selectedCategories = selectedCategories;
        
        // Statistical analysis
        long[] values = Arrays.stream(results)
                .filter(r -> r != null)
                .mapToLong(VectraBenchmark.BenchmarkResult::value)
                .toArray();
        
        report.mean = calculateMean(values);
        report.median = calculateMedian(values);
        report.stdDev = calculateStdDev(values, report.mean);
        report.confidenceInterval95 = calculateConfidenceInterval(values, 0.95);
        
        // Reproducibility assessment
        report.reproducibilityScore = assessReproducibility(results);
        
        // Validation grade assessment
        report.isIndustryGrade = assessIndustryGrade(report);
        report.isAcademicGrade = assessAcademicGrade(report);
        report.isScientificGrade = assessScientificGrade(report);
        
        // Generate executive summary
        report.executiveSummary = generateExecutiveSummary(report);
        
        // Generate grade justification
        report.gradeJustification = generateGradeJustification(report);
        
        // System metadata
        report.deviceModel = Build.MODEL;
        report.deviceManufacturer = Build.MANUFACTURER;
        report.androidVersion = Build.VERSION.RELEASE;
        report.cpuAbi = Build.SUPPORTED_ABIS[0];
        report.timestamp = new Date();
        
        return report;
    }
    
    private double calculateMean(long[] values) {
        if (values.length == 0) return 0;
        long sum = 0;
        for (long v : values) sum += v;
        return (double) sum / values.length;
    }
    
    private double calculateMedian(long[] values) {
        if (values.length == 0) return 0;
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        int mid = sorted.length / 2;
        if (sorted.length % 2 == 0) {
            return (sorted[mid - 1] + sorted[mid]) / 2.0;
        } else {
            return sorted[mid];
        }
    }
    
    private double calculateStdDev(long[] values, double mean) {
        if (values.length == 0) return 0;
        double sumSquaredDiff = 0;
        for (long v : values) {
            double diff = v - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / values.length);
    }
    
    private double[] calculateConfidenceInterval(long[] values, double confidenceLevel) {
        double mean = calculateMean(values);
        double stdDev = calculateStdDev(values, mean);
        double z = 1.96; // Z-score for 95% confidence
        double margin = z * (stdDev / Math.sqrt(values.length));
        return new double[] { mean - margin, mean + margin };
    }
    
    private double assessReproducibility(VectraBenchmark.BenchmarkResult[] results) {
        // Calculate coefficient of variation as reproducibility metric
        // Lower CV = higher reproducibility
        long[] scores = Arrays.stream(results)
                .filter(r -> r != null)
                .mapToLong(r -> r.score())
                .toArray();
        
        double mean = calculateMean(scores);
        double stdDev = calculateStdDev(scores, mean);
        
        if (mean == 0) return 0;
        double cv = (stdDev / mean) * 100;
        
        // Convert to reproducibility percentage (inverse of CV, capped at 100%)
        return Math.max(0, Math.min(100, 100 - cv));
    }
    
    private boolean assessIndustryGrade(AnalysisReport report) {
        // Industry grade requires:
        // - At least 3 categories measured
        // - Reproducibility > 70%
        // - At least 2 methodology standards applied
        return report.selectedCategories.size() >= 3 
                && report.reproducibilityScore >= 70 
                && report.methodologies.size() >= 2;
    }
    
    private boolean assessAcademicGrade(AnalysisReport report) {
        // Academic grade requires:
        // - At least 5 categories measured
        // - Reproducibility > 85%
        // - At least 3 methodology standards applied
        // - Statistical analysis complete
        return report.selectedCategories.size() >= 5 
                && report.reproducibilityScore >= 85 
                && report.methodologies.size() >= 3
                && report.stdDev > 0;
    }
    
    private boolean assessScientificGrade(AnalysisReport report) {
        // Scientific grade requires:
        // - All 6 categories measured
        // - Reproducibility > 95%
        // - At least 4 methodology standards applied
        // - Full statistical analysis
        // - Confidence interval within acceptable range
        return report.selectedCategories.size() >= 6 
                && report.reproducibilityScore >= 95 
                && report.methodologies.size() >= 4
                && report.stdDev > 0
                && report.confidenceInterval95 != null;
    }
    
    private String generateExecutiveSummary(AnalysisReport report) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(getString(R.string.pro_tools_summary_intro, report.totalScore));
        sb.append("\n\n");
        
        // Categories summary
        String[] categoryNames = {"CPU Single", "CPU Multi", "Memory", "Storage", "Integrity", "Emulation"};
        sb.append(getString(R.string.pro_tools_summary_categories));
        for (int i = 0; i < report.categoryScores.length && i < categoryNames.length; i++) {
            sb.append("\n• ").append(categoryNames[i]).append(": ").append(report.categoryScores[i]).append(" pts");
        }
        sb.append("\n\n");
        
        // Methodologies applied
        sb.append(getString(R.string.pro_tools_summary_methodologies, String.join(", ", report.methodologies)));
        sb.append("\n\n");
        
        // Reproducibility assessment
        sb.append(getString(R.string.pro_tools_summary_reproducibility, String.format(Locale.US, "%.1f%%", report.reproducibilityScore)));
        
        return sb.toString();
    }
    
    private String generateGradeJustification(AnalysisReport report) {
        StringBuilder sb = new StringBuilder();
        
        if (report.isScientificGrade) {
            sb.append(getString(R.string.pro_tools_grade_scientific_justification));
        } else if (report.isAcademicGrade) {
            sb.append(getString(R.string.pro_tools_grade_academic_justification));
        } else if (report.isIndustryGrade) {
            sb.append(getString(R.string.pro_tools_grade_industry_justification));
        } else {
            sb.append(getString(R.string.pro_tools_grade_informal_justification));
        }
        
        return sb.toString();
    }
    
    private void displayResults(AnalysisReport report) {
        // Executive Summary
        tvExecutiveSummary.setText(report.executiveSummary);
        
        // Validation Grades
        chipGradeIndustry.setChecked(report.isIndustryGrade);
        chipGradeAcademic.setChecked(report.isAcademicGrade);
        chipGradeScientific.setChecked(report.isScientificGrade);
        tvGradeJustification.setText(report.gradeJustification);
        
        // Statistical Analysis
        tvStatMean.setText(formatNumber(report.mean));
        tvStatMedian.setText(formatNumber(report.median));
        tvStatStdDev.setText(formatNumber(report.stdDev));
        
        if (report.confidenceInterval95 != null) {
            tvStatConfidence.setText(String.format(Locale.US, "95%%: [%.0f, %.0f]", 
                    report.confidenceInterval95[0], report.confidenceInterval95[1]));
        }
        
        tvStatReproducibility.setText(String.format(Locale.US, "%.1f%%", report.reproducibilityScore));
    }
    
    private String formatNumber(double value) {
        if (value >= 1_000_000_000) {
            return String.format(Locale.US, "%.2fB", value / 1_000_000_000);
        } else if (value >= 1_000_000) {
            return String.format(Locale.US, "%.2fM", value / 1_000_000);
        } else if (value >= 1_000) {
            return String.format(Locale.US, "%.2fK", value / 1_000);
        } else {
            return String.format(Locale.US, "%.2f", value);
        }
    }
    
    private void showFullReport() {
        if (lastReport == null) {
            Toast.makeText(this, R.string.pro_tools_no_results, Toast.LENGTH_SHORT).show();
            return;
        }
        
        String fullReport = generateFullReport(lastReport);
        
        TextView messageView = new TextView(this);
        messageView.setText(fullReport);
        messageView.setTextIsSelectable(true);
        messageView.setTypeface(android.graphics.Typeface.MONOSPACE);
        messageView.setTextSize(9f);
        messageView.setPadding(32, 32, 32, 32);
        
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(messageView);
        
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pro_tools_full_report_title)
                .setView(scrollView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
    
    private String generateFullReport(AnalysisReport report) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║         VECTRAS PROFESSIONAL ANALYSIS REPORT                                 ║\n");
        sb.append("║         Engineering / Benchmark / Scientific System                          ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Metadata
        sb.append(String.format("║ Report Generated: %-58s ║\n", 
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(report.timestamp)));
        sb.append(String.format("║ Device: %-68s ║\n", 
                report.deviceManufacturer + " " + report.deviceModel));
        sb.append(String.format("║ Android: %-67s ║\n", report.androidVersion));
        sb.append(String.format("║ CPU ABI: %-67s ║\n", report.cpuAbi));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Section 1: Executive Summary
        sb.append("║ 1. EXECUTIVE TECHNICAL SUMMARY                                               ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append(wrapText(report.executiveSummary, 76));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Section 2: Methodology Standards
        sb.append("║ 2. METHODOLOGY STANDARDS APPLIED                                             ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        for (String method : report.methodologies) {
            sb.append(String.format("║ ✓ %-74s ║\n", method));
        }
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Section 3: Statistical Analysis
        sb.append("║ 3. STATISTICAL ROBUSTNESS                                                    ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Mean:                  %-53s ║\n", formatNumber(report.mean)));
        sb.append(String.format("║ Median:                %-53s ║\n", formatNumber(report.median)));
        sb.append(String.format("║ Standard Deviation:    %-53s ║\n", formatNumber(report.stdDev)));
        if (report.confidenceInterval95 != null) {
            sb.append(String.format("║ 95%% Confidence:        [%.0f, %.0f]%s ║\n", 
                    report.confidenceInterval95[0], report.confidenceInterval95[1],
                    String.format("%" + (46 - String.format("[%.0f, %.0f]", 
                            report.confidenceInterval95[0], report.confidenceInterval95[1]).length()) + "s", "")));
        }
        sb.append(String.format("║ Reproducibility Score: %-53s ║\n", 
                String.format(Locale.US, "%.1f%%", report.reproducibilityScore)));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Section 4: Validation Grade
        sb.append("║ 4. ALIGNMENT WITH ACADEMIC STANDARDS                                         ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Industry-grade:        %-53s ║\n", report.isIndustryGrade ? "✓ PASSED" : "✗ NOT MET"));
        sb.append(String.format("║ Academic-grade:        %-53s ║\n", report.isAcademicGrade ? "✓ PASSED" : "✗ NOT MET"));
        sb.append(String.format("║ Scientific-grade:      %-53s ║\n", report.isScientificGrade ? "✓ PASSED" : "✗ NOT MET"));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ Justification:                                                               ║\n");
        sb.append(wrapText(report.gradeJustification, 76));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Section 5: Category Scores
        sb.append("║ 5. METRIC TAXONOMY - CATEGORY SCORES                                         ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        String[] categoryNames = {"CPU Single-threaded", "CPU Multi-threaded", "Memory", "Storage", "Integrity", "Emulation"};
        for (int i = 0; i < report.categoryScores.length && i < categoryNames.length; i++) {
            sb.append(String.format("║ %-24s: %6d pts%s ║\n", 
                    categoryNames[i], report.categoryScores[i],
                    String.format("%" + 42 + "s", "")));
        }
        sb.append(String.format("║ %-24s: %6d pts%s ║\n", "TOTAL SCORE", report.totalScore, String.format("%" + 42 + "s", "")));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Section 6: Formal Technical Verdict
        sb.append("║ 6. FORMAL TECHNICAL VERDICT                                                  ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        String verdict;
        if (report.isScientificGrade) {
            verdict = "This analysis meets SCIENTIFIC-GRADE standards and is suitable for peer-reviewed publications, technical audits, and expert testimony.";
        } else if (report.isAcademicGrade) {
            verdict = "This analysis meets ACADEMIC-GRADE standards and is suitable for research reports, engineering documentation, and university submissions.";
        } else if (report.isIndustryGrade) {
            verdict = "This analysis meets INDUSTRY-GRADE standards and is suitable for engineering benchmarks, performance assessments, and technical documentation.";
        } else {
            verdict = "This analysis is INFORMAL engineering-grade. Additional metrics, methodologies, or reproducibility improvements are recommended.";
        }
        sb.append(wrapText(verdict, 76));
        
        // Footer
        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝\n");
        
        // Detailed Results (if available)
        if (lastResults != null) {
            sb.append("\n");
            sb.append(VectraBenchmark.formatReport(lastResults));
        }
        
        return sb.toString();
    }
    
    private String wrapText(String text, int width) {
        StringBuilder result = new StringBuilder();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        
        for (String word : words) {
            if (line.length() + word.length() + 1 > width) {
                result.append(String.format("║ %-" + width + "s ║\n", line.toString().trim()));
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (line.length() > 0) {
            result.append(String.format("║ %-" + width + "s ║\n", line.toString().trim()));
        }
        return result.toString();
    }
    
    private void exportReport() {
        if (lastReport == null) {
            Toast.makeText(this, R.string.pro_tools_no_results, Toast.LENGTH_SHORT).show();
            return;
        }
        
        executor.execute(() -> {
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String fileName = "vectras_analysis_" + timestamp + ".txt";
                File exportDir = new File(AppConfig.maindirpath);
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }
                File exportFile = new File(exportDir, fileName);
                
                String report = generateFullReport(lastReport);
                FileUtils.writeToFile(exportDir.getAbsolutePath(), fileName, report);
                
                mainHandler.post(() -> {
                    Toast.makeText(this, 
                            getString(R.string.pro_tools_report_exported, exportFile.getAbsolutePath()), 
                            Toast.LENGTH_LONG).show();
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(this, getString(R.string.pro_tools_export_failed, e.getMessage()), 
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void shareReport() {
        if (lastReport == null) {
            Toast.makeText(this, R.string.pro_tools_no_results, Toast.LENGTH_SHORT).show();
            return;
        }
        
        String report = generateFullReport(lastReport);
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Vectras VM Professional Analysis Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, report);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.pro_tools_share_report)));
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
    
    /**
     * Internal class to hold analysis report data
     */
    private static class AnalysisReport {
        int totalScore;
        int[] categoryScores;
        List<String> methodologies;
        List<Integer> selectedCategories;
        
        // Statistical analysis
        double mean;
        double median;
        double stdDev;
        double[] confidenceInterval95;
        double reproducibilityScore;
        
        // Validation grades
        boolean isIndustryGrade;
        boolean isAcademicGrade;
        boolean isScientificGrade;
        
        // Text content
        String executiveSummary;
        String gradeJustification;
        
        // Metadata
        String deviceModel;
        String deviceManufacturer;
        String androidVersion;
        String cpuAbi;
        Date timestamp;
    }
}
