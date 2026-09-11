package com.nursery.controller;

import com.nursery.config.AuthUser;
import com.nursery.entity.CareRecord;
import com.nursery.entity.Child;
import com.nursery.entity.MorningCheck;
import com.nursery.service.AccessService;
import com.nursery.service.AttendanceService;
import com.nursery.service.ChildService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.nursery.controller.ChildController.toUser;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final ChildService childService;
    private final AccessService accessService;

    public AttendanceController(AttendanceService attendanceService,
                                ChildService childService,
                                AccessService accessService) {
        this.attendanceService = attendanceService;
        this.childService = childService;
        this.accessService = accessService;
    }

    /** 老师本班儿童 + 当日晨检状态 */
    @GetMapping("/class-children")
    @PreAuthorize("hasRole('TEACHER')")
    public List<Map<String, Object>> classChildren(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.classChildren(toUser(user), date == null ? LocalDate.now() : date);
    }

    public record MorningCheckReq(@NotNull(message = "缺少儿童") Long childId,
                                  @NotNull(message = "请选择日期") LocalDate date,
                                  BigDecimal temperature,
                                  String skinStatus,
                                  String mood,
                                  String diet,
                                  String carriedItems,
                                  Boolean medicationBrought,
                                  @NotNull(message = "请选择晨检结果") MorningCheck.Result result,
                                  String note) {}

    @PostMapping("/morning-checks")
    @PreAuthorize("hasAnyRole('TEACHER','HEALTH','DIRECTOR')")
    public MorningCheck upsertMorningCheck(@AuthenticationPrincipal AuthUser user,
                                           @Valid @RequestBody MorningCheckReq req) {
        return attendanceService.upsertMorningCheck(toUser(user), req.childId(), req.date(),
                req.temperature(), req.skinStatus(), req.mood(), req.diet(), req.carriedItems(),
                req.medicationBrought(), req.result(), req.note());
    }

    /** 某日晨检列表（可按班级过滤），园内角色可查 */
    @GetMapping("/morning-checks")
    @PreAuthorize("hasAnyRole('TEACHER','HEALTH','DIRECTOR','FRONTDESK')")
    public List<MorningCheck> listMorningChecks(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long classroomId) {
        return attendanceService.listMorningChecks(date, classroomId);
    }

    public record CareRecordReq(@NotNull(message = "缺少儿童") Long childId,
                                @NotNull(message = "请选择日期") LocalDate date,
                                @NotNull(message = "请选择记录类型") CareRecord.CareType type,
                                String detail,
                                @NotNull(message = "请选择关注程度") CareRecord.Severity severity,
                                CareRecord.InjuryType injuryType) {}

    @PostMapping("/care-records")
    @PreAuthorize("hasAnyRole('TEACHER','HEALTH','DIRECTOR')")
    public CareRecord addCareRecord(@AuthenticationPrincipal AuthUser user,
                                    @Valid @RequestBody CareRecordReq req) {
        return attendanceService.addCareRecord(toUser(user), req.childId(), req.date(), req.type(),
                req.detail(), req.severity(), req.injuryType());
    }

    /** 儿童某日完整在园记录（晨检+照护+喂药+接送+事件） */
    @GetMapping("/daily")
    public Map<String, Object> daily(@AuthenticationPrincipal AuthUser user,
                                     @RequestParam Long childId,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Child child = childService.get(childId);
        accessService.checkViewChild(toUser(user), child);
        return attendanceService.dailyRecord(childId, date);
    }
}
