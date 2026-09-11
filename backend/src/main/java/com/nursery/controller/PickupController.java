package com.nursery.controller;

import com.nursery.config.AuthUser;
import com.nursery.entity.PickupRecord;
import com.nursery.service.PickupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.nursery.controller.ChildController.toUser;

@RestController
@RequestMapping("/api/pickup")
public class PickupController {

    private final PickupService pickupService;

    public PickupController(PickupService pickupService) {
        this.pickupService = pickupService;
    }

    public record VerifyReq(@NotNull(message = "请选择儿童") Long childId,
                            @NotBlank(message = "请输入接送人姓名") String name,
                            String idNumber) {}

    /** 前台核验接送人身份（授权人/临时委托） */
    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('FRONTDESK','DIRECTOR')")
    public Map<String, Object> verify(@Valid @RequestBody VerifyReq req) {
        return pickupService.verify(req.childId(), req.name(), req.idNumber());
    }

    public record RecordReq(@NotNull(message = "请选择儿童") Long childId,
                            @NotBlank(message = "请输入接送人姓名") String pickupPersonName,
                            String pickupPersonIdNumber,
                            @NotNull(message = "请选择核验方式") PickupRecord.VerifyMethod verifyMethod,
                            @NotNull(message = "请选择处理结果") PickupRecord.Result result,
                            String denyReason) {}

    /** 登记接送记录（放行/拒绝），拒绝自动创建未授权到场事件 */
    @PostMapping("/records")
    @PreAuthorize("hasAnyRole('FRONTDESK','DIRECTOR')")
    public PickupRecord record(@AuthenticationPrincipal AuthUser user,
                               @Valid @RequestBody RecordReq req) {
        return pickupService.record(toUser(user), req.childId(), req.pickupPersonName(),
                req.pickupPersonIdNumber(), req.verifyMethod(), req.result(), req.denyReason());
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('FRONTDESK','DIRECTOR')")
    public List<PickupRecord> listRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return pickupService.listRecords(date == null ? LocalDate.now() : date);
    }
}
