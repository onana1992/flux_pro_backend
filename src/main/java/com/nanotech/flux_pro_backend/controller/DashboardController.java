package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.response.DashboardSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.DelayByTypeResponse;
import com.nanotech.flux_pro_backend.dto.response.MyActivityResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationRankingResponse;
import com.nanotech.flux_pro_backend.dto.response.OverdueFileResponse;
import com.nanotech.flux_pro_backend.dto.response.WorkloadEntryResponse;
import com.nanotech.flux_pro_backend.common.DashboardException;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.DashboardExportService;
import com.nanotech.flux_pro_backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Tableaux de bord et reporting (DSH — CDC §7.6 et §11, cf. docs/SPEC-DSH.md). */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardExportService dashboardExportService;
    private final SecurityUtils securityUtils;

    @GetMapping("/summary")
    @RequiresPermission(RbacPermissions.DASHBOARD_READ)
    public DashboardSummaryResponse summary(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String fileTypeCode) {
        return dashboardService.summary(securityUtils.currentUser(), organizationId, fileTypeCode);
    }

    /** DSH-01 — jamais soumis à un `organizationId` : toujours l'activité de l'appelant. */
    @GetMapping("/my-activity")
    @RequiresPermission(RbacPermissions.DASHBOARD_READ)
    public MyActivityResponse myActivity() {
        return dashboardService.myActivity(securityUtils.currentUser());
    }

    @GetMapping("/workload")
    @RequiresPermission(RbacPermissions.DASHBOARD_READ)
    public List<WorkloadEntryResponse> workload(@RequestParam(required = false) UUID organizationId) {
        return dashboardService.workload(securityUtils.currentUser(), organizationId);
    }

    @GetMapping("/overdue-files")
    @RequiresPermission(RbacPermissions.DASHBOARD_READ)
    public List<OverdueFileResponse> overdueFiles(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(defaultValue = "10") int limit) {
        return dashboardService.overdueFiles(securityUtils.currentUser(), organizationId, limit);
    }

    @GetMapping("/delay-by-type")
    @RequiresPermission(RbacPermissions.DASHBOARD_READ)
    public List<DelayByTypeResponse> delayByType(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(defaultValue = "30") int windowDays) {
        return dashboardService.delayByType(securityUtils.currentUser(), organizationId, windowDays);
    }

    @GetMapping("/compliance-ranking")
    @RequiresPermission(RbacPermissions.DASHBOARD_READ)
    public List<OrganizationRankingResponse> complianceRanking(
            @RequestParam(defaultValue = "DIRECTORATE") String groupByTypeCode,
            @RequestParam(defaultValue = "90") int windowDays) {
        return dashboardService.complianceRanking(securityUtils.currentUser(), groupByTypeCode, windowDays);
    }

    /** DSH-08 — export des jeux de données ci-dessus. Seul le CSV est disponible (PDF : cf. SPEC-DSH.md §9.2). */
    @GetMapping("/export")
    @RequiresPermission(RbacPermissions.DASHBOARD_EXPORT)
    public ResponseEntity<byte[]> export(
            @RequestParam String dataset,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(defaultValue = "90") int windowDays,
            @RequestParam(defaultValue = "DIRECTORATE") String groupByTypeCode) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw DashboardException.badRequest(
                    "DASHBOARD_FORMAT_UNSUPPORTED", "Format non supporté à ce jour : " + format + " (PDF à venir)",
                    format);
        }
        byte[] csv = dashboardExportService.exportCsv(
                dataset, securityUtils.currentUser(), organizationId, windowDays, groupByTypeCode);
        String filename = "dashboard-" + dataset + "-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }
}
