package com.nursery.service;

import com.nursery.config.BizException;
import com.nursery.entity.*;
import com.nursery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 入托评估流程：家长提交 → 生成评估任务 → 园长分班 → 保健老师健康评估 → 入托。
 */
@Service
public class ApplicationService {

    private final EnrollmentApplicationRepository applicationRepo;
    private final ChildRepository childRepo;
    private final EmergencyContactRepository contactRepo;
    private final PickupAuthorizationRepository authRepo;
    private final ClassroomRepository classroomRepo;
    private final ClassAssignmentRepository assignmentRepo;
    private final HealthAssessmentRepository assessmentRepo;

    public ApplicationService(EnrollmentApplicationRepository applicationRepo,
                              ChildRepository childRepo,
                              EmergencyContactRepository contactRepo,
                              PickupAuthorizationRepository authRepo,
                              ClassroomRepository classroomRepo,
                              ClassAssignmentRepository assignmentRepo,
                              HealthAssessmentRepository assessmentRepo) {
        this.applicationRepo = applicationRepo;
        this.childRepo = childRepo;
        this.contactRepo = contactRepo;
        this.authRepo = authRepo;
        this.classroomRepo = classroomRepo;
        this.assignmentRepo = assignmentRepo;
        this.assessmentRepo = assessmentRepo;
    }

    /**
     * 家长提交儿童资料，生成入托评估任务（待分班）。
     */
    @Transactional
    public EnrollmentApplication submit(User parent, Child childInfo,
                                        List<EmergencyContact> contacts,
                                        List<PickupAuthorization> authorizations,
                                        String parentExpectations) {
        childInfo.setId(null);
        childInfo.setParent(parent);
        childInfo.setStatus(Child.Status.ASSESSING);
        childRepo.save(childInfo);

        for (EmergencyContact c : contacts) {
            c.setId(null);
            c.setChild(childInfo);
            contactRepo.save(c);
        }
        for (PickupAuthorization a : authorizations) {
            a.setId(null);
            a.setChild(childInfo);
            a.setActive(true);
            authRepo.save(a);
        }

        EnrollmentApplication app = new EnrollmentApplication();
        app.setChild(childInfo);
        app.setParentExpectations(parentExpectations);
        app.setStatus(EnrollmentApplication.Status.PENDING_CLASS);
        return applicationRepo.save(app);
    }

    public List<EnrollmentApplication> listMy(User parent) {
        return applicationRepo.findByChildParentIdOrderByCreatedAtDesc(parent.getId());
    }

    public List<EnrollmentApplication> listAll(EnrollmentApplication.Status status) {
        return status == null
                ? applicationRepo.findAllByOrderByCreatedAtDesc()
                : applicationRepo.findByStatusOrderByCreatedAtAsc(status);
    }

    public EnrollmentApplication get(Long id) {
        return applicationRepo.findById(id).orElseThrow(() -> BizException.notFound("入托申请不存在"));
    }

    /**
     * 园长分班：校验班级容量，结合师幼比/特殊照护/家长期望记录备注。
     */
    @Transactional
    public EnrollmentApplication assignClass(User director, Long applicationId, Long classroomId, String note) {
        EnrollmentApplication app = get(applicationId);
        if (app.getStatus() != EnrollmentApplication.Status.PENDING_CLASS) {
            throw new BizException("该申请当前不在待分班状态");
        }
        Classroom classroom = classroomRepo.findById(classroomId)
                .orElseThrow(() -> BizException.notFound("班级不存在"));
        long enrolled = childRepo.countByClassroomIdAndStatusIn(classroomId,
                List.of(Child.Status.ASSIGNED, Child.Status.ENROLLED));
        if (enrolled >= classroom.getCapacity()) {
            throw new BizException("班级「" + classroom.getName() + "」已满（" + enrolled + "/" + classroom.getCapacity() + "），请选择其他班级");
        }

        ClassAssignment assignment = new ClassAssignment();
        assignment.setApplication(app);
        assignment.setClassroom(classroom);
        assignment.setAssignedBy(director);
        assignment.setNote(note);
        assignmentRepo.save(assignment);

        Child child = app.getChild();
        child.setClassroom(classroom);
        child.setStatus(Child.Status.ASSIGNED);
        childRepo.save(child);

        app.setStatus(EnrollmentApplication.Status.PENDING_HEALTH);
        app.setUpdatedAt(LocalDateTime.now());
        return applicationRepo.save(app);
    }

    /**
     * 保健老师健康评估：确认是否需要过敏餐、临时观察或家长补充资料。
     */
    @Transactional
    public EnrollmentApplication healthAssess(User assessor, Long applicationId, HealthAssessment.Result result,
                                              Boolean allergyMealRequired, Boolean observationRequired,
                                              String supplementRequest, String note) {
        EnrollmentApplication app = get(applicationId);
        if (app.getStatus() != EnrollmentApplication.Status.PENDING_HEALTH) {
            throw new BizException("该申请当前不在待健康评估状态");
        }
        if (result == HealthAssessment.Result.NEED_SUPPLEMENT
                && (supplementRequest == null || supplementRequest.isBlank())) {
            throw new BizException("要求家长补充资料时必须填写补充说明");
        }

        HealthAssessment assessment = new HealthAssessment();
        assessment.setApplication(app);
        assessment.setAssessor(assessor);
        assessment.setResult(result);
        assessment.setAllergyMealRequired(Boolean.TRUE.equals(allergyMealRequired));
        assessment.setObservationRequired(Boolean.TRUE.equals(observationRequired));
        assessment.setSupplementRequest(supplementRequest);
        assessment.setNote(note);
        assessmentRepo.save(assessment);

        Child child = app.getChild();
        if (result == HealthAssessment.Result.NEED_SUPPLEMENT) {
            app.setSupplementRequest(supplementRequest);
            app.setStatus(EnrollmentApplication.Status.NEED_SUPPLEMENT);
        } else {
            app.setStatus(EnrollmentApplication.Status.COMPLETED);
            child.setStatus(Child.Status.ENROLLED);
            child.setAllergyMealRequired(result == HealthAssessment.Result.ALLERGY_MEAL
                    || Boolean.TRUE.equals(allergyMealRequired));
            child.setHealthObservation(result == HealthAssessment.Result.TEMP_OBSERVATION
                    || Boolean.TRUE.equals(observationRequired));
            childRepo.save(child);
        }
        app.setUpdatedAt(LocalDateTime.now());
        return applicationRepo.save(app);
    }

    /**
     * 家长补充资料，申请重新回到待健康评估。
     */
    @Transactional
    public EnrollmentApplication supplement(User parent, Long applicationId, String supplementNote) {
        EnrollmentApplication app = get(applicationId);
        if (!app.getChild().getParent().getId().equals(parent.getId())) {
            throw BizException.forbidden("只能补充自己孩子的申请");
        }
        if (app.getStatus() != EnrollmentApplication.Status.NEED_SUPPLEMENT) {
            throw new BizException("该申请当前无需补充资料");
        }
        app.setSupplementNote(supplementNote);
        app.setStatus(EnrollmentApplication.Status.PENDING_HEALTH);
        app.setUpdatedAt(LocalDateTime.now());
        return applicationRepo.save(app);
    }
}
