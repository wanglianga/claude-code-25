package com.nursery.controller;

import com.nursery.config.AuthUser;
import com.nursery.entity.*;
import com.nursery.service.ChildService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChildController {

    private final ChildService childService;

    public ChildController(ChildService childService) {
        this.childService = childService;
    }

    /** 儿童列表：家长看自己孩子；老师看本班；其余园内角色看全部（可按班级/状态过滤） */
    @GetMapping("/children")
    public List<Child> list(@AuthenticationPrincipal AuthUser user,
                            @RequestParam(required = false) Long classroomId,
                            @RequestParam(required = false) Child.Status status) {
        return childService.listFor(toUser(user), classroomId, status);
    }

    @GetMapping("/children/{id}")
    public Map<String, Object> detail(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        Child child = childService.getAndCheckView(toUser(user), id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("child", child);
        m.put("emergencyContacts", childService.contacts(id));
        m.put("pickupAuthorizations", childService.authorizations(id));
        return m;
    }

    // ---------- 喂药委托 ----------

    public record MedicationReq(@NotBlank(message = "请填写药品名称") String medicineName,
                                @NotBlank(message = "请填写剂量") String dosage,
                                String timePlan,
                                @NotNull(message = "请选择开始日期") LocalDate startDate,
                                @NotNull(message = "请选择结束日期") LocalDate endDate,
                                String parentNote) {}

    @PostMapping("/children/{id}/medication-requests")
    @PreAuthorize("hasRole('PARENT')")
    public MedicationRequest createMedication(@AuthenticationPrincipal AuthUser user,
                                              @PathVariable Long id,
                                              @Valid @RequestBody MedicationReq req) {
        MedicationRequest mr = new MedicationRequest();
        mr.setMedicineName(req.medicineName());
        mr.setDosage(req.dosage());
        mr.setTimePlan(req.timePlan());
        mr.setStartDate(req.startDate());
        mr.setEndDate(req.endDate());
        mr.setParentNote(req.parentNote());
        return childService.createMedicationRequest(toUser(user), id, mr);
    }

    @GetMapping("/children/{id}/medication-requests")
    public List<MedicationRequest> listMedications(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        childService.getAndCheckView(toUser(user), id);
        return childService.medicationRequests(id);
    }

    @PostMapping("/medication-requests/{id}/finish")
    @PreAuthorize("hasRole('PARENT')")
    public MedicationRequest finishMedication(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return childService.finishMedicationRequest(toUser(user), id);
    }

    // ---------- 临时接送委托 ----------

    public record DelegationReq(@NotBlank(message = "请填写被委托人姓名") String delegateName,
                                String delegatePhone,
                                String delegateIdNumber,
                                @NotNull(message = "请选择委托日期") LocalDate validDate,
                                String note) {}

    @PostMapping("/children/{id}/delegations")
    @PreAuthorize("hasRole('PARENT')")
    public TempDelegation createDelegation(@AuthenticationPrincipal AuthUser user,
                                           @PathVariable Long id,
                                           @Valid @RequestBody DelegationReq req) {
        TempDelegation d = new TempDelegation();
        d.setDelegateName(req.delegateName());
        d.setDelegatePhone(req.delegatePhone());
        d.setDelegateIdNumber(req.delegateIdNumber());
        d.setValidDate(req.validDate());
        d.setNote(req.note());
        return childService.createDelegation(toUser(user), id, d);
    }

    @GetMapping("/children/{id}/delegations")
    public List<TempDelegation> listDelegations(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        childService.getAndCheckView(toUser(user), id);
        return childService.delegations(id);
    }

    @GetMapping("/delegations/today")
    @PreAuthorize("hasAnyRole('FRONTDESK','DIRECTOR')")
    public List<TempDelegation> todayDelegations() {
        return childService.todayDelegations();
    }

    // ---------- 接送留言 ----------

    public record PickupMessageReq(@NotBlank(message = "请填写留言内容") String content,
                                   LocalDate date) {}

    @PostMapping("/children/{id}/pickup-messages")
    @PreAuthorize("hasRole('PARENT')")
    public PickupMessage createPickupMessage(@AuthenticationPrincipal AuthUser user,
                                             @PathVariable Long id,
                                             @Valid @RequestBody PickupMessageReq req) {
        return childService.createPickupMessage(toUser(user), id, req.date(), req.content());
    }

    @GetMapping("/children/{id}/pickup-messages")
    public List<PickupMessage> listPickupMessages(@AuthenticationPrincipal AuthUser user,
                                                  @PathVariable Long id,
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        childService.getAndCheckView(toUser(user), id);
        return childService.pickupMessages(id, date);
    }

    /** 由登录主体构造轻量 User（仅 id/role 用于权限判断与关联） */
    static User toUser(AuthUser auth) {
        User u = new User();
        u.setId(auth.getId());
        u.setUsername(auth.getUsername());
        u.setName(auth.getName());
        u.setRole(auth.getRole());
        return u;
    }
}
