package com.nursery.controller;

import com.nursery.config.AuthUser;
import com.nursery.entity.*;
import com.nursery.service.ApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.nursery.controller.ChildController.toUser;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public record ContactDto(@NotBlank(message = "请填写紧急联系人姓名") String name,
                             @NotBlank(message = "请填写与儿童关系") String relation,
                             @NotBlank(message = "请填写紧急联系人电话") String phone,
                             Integer priority) {}

    public record AuthDto(@NotBlank(message = "请填写接送授权人姓名") String name,
                          @NotBlank(message = "请填写与儿童关系") String relation,
                          String phone,
                          String idNumber) {}

    /** 家长提交入托申请：儿童资料 + 紧急联系人 + 接送授权人 + 家长期望 */
    public record SubmitReq(@NotBlank(message = "请填写儿童姓名") String childName,
                            @NotBlank(message = "请选择性别") String gender,
                            @NotNull(message = "请选择出生日期") @Past(message = "出生日期不正确") LocalDate birthDate,
                            String allergyHistory,
                            String vaccinationStatus,
                            String napHabit,
                            String toiletAbility,
                            String medications,
                            String specialCareNeeds,
                            String parentExpectations,
                            @NotEmpty(message = "至少填写一名紧急联系人") List<@Valid ContactDto> emergencyContacts,
                            @NotEmpty(message = "至少填写一名接送授权人") List<@Valid AuthDto> pickupAuthorizations) {}

    public record AssignReq(@NotNull(message = "请选择班级") Long classroomId, String note) {}

    public record HealthAssessReq(@NotNull(message = "请选择评估结论") HealthAssessment.Result result,
                                  Boolean allergyMealRequired,
                                  Boolean observationRequired,
                                  String supplementRequest,
                                  String note) {}

    public record SupplementReq(@NotBlank(message = "请填写补充资料") String supplementNote) {}

    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    public EnrollmentApplication submit(@AuthenticationPrincipal AuthUser user,
                                        @Valid @RequestBody SubmitReq req) {
        Child child = new Child();
        child.setName(req.childName());
        child.setGender(req.gender());
        child.setBirthDate(req.birthDate());
        child.setAllergyHistory(req.allergyHistory());
        child.setVaccinationStatus(req.vaccinationStatus());
        child.setNapHabit(req.napHabit());
        child.setToiletAbility(req.toiletAbility());
        child.setMedications(req.medications());
        child.setSpecialCareNeeds(req.specialCareNeeds());

        List<EmergencyContact> contacts = req.emergencyContacts().stream().map(c -> {
            EmergencyContact e = new EmergencyContact();
            e.setName(c.name());
            e.setRelation(c.relation());
            e.setPhone(c.phone());
            e.setPriority(c.priority() == null ? 1 : c.priority());
            return e;
        }).toList();

        List<PickupAuthorization> auths = req.pickupAuthorizations().stream().map(a -> {
            PickupAuthorization p = new PickupAuthorization();
            p.setName(a.name());
            p.setRelation(a.relation());
            p.setPhone(a.phone());
            p.setIdNumber(a.idNumber());
            return p;
        }).toList();

        return applicationService.submit(toUser(user), child, contacts, auths, req.parentExpectations());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PARENT')")
    public List<EnrollmentApplication> my(@AuthenticationPrincipal AuthUser user) {
        return applicationService.listMy(toUser(user));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR','HEALTH')")
    public List<EnrollmentApplication> list(@RequestParam(required = false) EnrollmentApplication.Status status) {
        return applicationService.listAll(status);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('DIRECTOR')")
    public EnrollmentApplication assign(@AuthenticationPrincipal AuthUser user,
                                        @PathVariable Long id,
                                        @Valid @RequestBody AssignReq req) {
        return applicationService.assignClass(toUser(user), id, req.classroomId(), req.note());
    }

    @PostMapping("/{id}/health-assessment")
    @PreAuthorize("hasRole('HEALTH')")
    public EnrollmentApplication healthAssess(@AuthenticationPrincipal AuthUser user,
                                              @PathVariable Long id,
                                              @Valid @RequestBody HealthAssessReq req) {
        return applicationService.healthAssess(toUser(user), id, req.result(),
                req.allergyMealRequired(), req.observationRequired(), req.supplementRequest(), req.note());
    }

    @PostMapping("/{id}/supplement")
    @PreAuthorize("hasRole('PARENT')")
    public EnrollmentApplication supplement(@AuthenticationPrincipal AuthUser user,
                                            @PathVariable Long id,
                                            @Valid @RequestBody SupplementReq req) {
        return applicationService.supplement(toUser(user), id, req.supplementNote());
    }
}
