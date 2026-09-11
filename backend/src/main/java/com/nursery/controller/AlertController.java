package com.nursery.controller;

import com.nursery.config.AuthUser;
import com.nursery.config.BizException;
import com.nursery.entity.AnomalyAlert;
import com.nursery.entity.User;
import com.nursery.repository.AnomalyAlertRepository;
import com.nursery.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AnomalyAlertRepository alertRepo;
    private final UserRepository userRepo;

    public AlertController(AnomalyAlertRepository alertRepo, UserRepository userRepo) {
        this.alertRepo = alertRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','HEALTH')")
    public List<AnomalyAlert> list(@RequestParam(required = false) AnomalyAlert.Status status) {
        return status == null ? alertRepo.findAllByOrderByCreatedAtDesc()
                : alertRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    public record HandleReq(@NotBlank(message = "请填写处理说明") String handleNote) {}

    /** 园长处理预警：调整班级 / 增加保健观察 / 约谈家长 的处理记录 */
    @PostMapping("/{id}/handle")
    @PreAuthorize("hasRole('DIRECTOR')")
    public AnomalyAlert handle(@AuthenticationPrincipal AuthUser user,
                               @PathVariable Long id,
                               @Valid @RequestBody HandleReq req) {
        AnomalyAlert alert = alertRepo.findById(id)
                .orElseThrow(() -> BizException.notFound("预警不存在"));
        if (alert.getStatus() == AnomalyAlert.Status.HANDLED) {
            throw new BizException("该预警已处理");
        }
        User handler = userRepo.findById(user.getId()).orElseThrow();
        alert.setStatus(AnomalyAlert.Status.HANDLED);
        alert.setHandledBy(handler);
        alert.setHandleNote(req.handleNote());
        alert.setHandledAt(LocalDateTime.now());
        return alertRepo.save(alert);
    }
}
