package com.nursery.service;

import com.nursery.config.BizException;
import com.nursery.entity.*;
import com.nursery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ChildService {

    private final ChildRepository childRepo;
    private final EmergencyContactRepository contactRepo;
    private final PickupAuthorizationRepository authRepo;
    private final MedicationRequestRepository medicationRepo;
    private final TempDelegationRepository delegationRepo;
    private final PickupMessageRepository messageRepo;
    private final AccessService accessService;

    public ChildService(ChildRepository childRepo,
                        EmergencyContactRepository contactRepo,
                        PickupAuthorizationRepository authRepo,
                        MedicationRequestRepository medicationRepo,
                        TempDelegationRepository delegationRepo,
                        PickupMessageRepository messageRepo,
                        AccessService accessService) {
        this.childRepo = childRepo;
        this.contactRepo = contactRepo;
        this.authRepo = authRepo;
        this.medicationRepo = medicationRepo;
        this.delegationRepo = delegationRepo;
        this.messageRepo = messageRepo;
        this.accessService = accessService;
    }

    public Child get(Long id) {
        return childRepo.findById(id).orElseThrow(() -> BizException.notFound("儿童不存在"));
    }

    public Child getAndCheckView(User user, Long id) {
        Child child = get(id);
        accessService.checkViewChild(user, child);
        return child;
    }

    public List<Child> listFor(User user, Long classroomId, Child.Status status) {
        if (user.getRole() == User.Role.PARENT) {
            return childRepo.findByParentIdOrderByIdAsc(user.getId());
        }
        if (user.getRole() == User.Role.TEACHER) {
            // 老师仅看自己班级的儿童
            return childRepo.findAll().stream()
                    .filter(c -> accessService.isTeacherOf(user, c))
                    .toList();
        }
        if (classroomId != null) {
            return childRepo.findByClassroomId(classroomId);
        }
        if (status != null) {
            return childRepo.findByStatusIn(List.of(status));
        }
        return childRepo.findAll();
    }

    public List<EmergencyContact> contacts(Long childId) {
        return contactRepo.findByChildIdOrderByPriorityAsc(childId);
    }

    public List<PickupAuthorization> authorizations(Long childId) {
        return authRepo.findByChildId(childId);
    }

    // ---------- 喂药委托 ----------

    @Transactional
    public MedicationRequest createMedicationRequest(User parent, Long childId, MedicationRequest req) {
        Child child = get(childId);
        checkOwn(parent, child);
        req.setId(null);
        req.setChild(child);
        req.setStatus(MedicationRequest.Status.ACTIVE);
        return medicationRepo.save(req);
    }

    public List<MedicationRequest> medicationRequests(Long childId) {
        return medicationRepo.findByChildIdOrderByCreatedAtDesc(childId);
    }

    @Transactional
    public MedicationRequest finishMedicationRequest(User parent, Long id) {
        MedicationRequest req = medicationRepo.findById(id)
                .orElseThrow(() -> BizException.notFound("喂药委托不存在"));
        checkOwn(parent, req.getChild());
        req.setStatus(MedicationRequest.Status.FINISHED);
        return medicationRepo.save(req);
    }

    // ---------- 临时接送委托 ----------

    @Transactional
    public TempDelegation createDelegation(User parent, Long childId, TempDelegation req) {
        Child child = get(childId);
        checkOwn(parent, child);
        req.setId(null);
        req.setChild(child);
        req.setStatus(TempDelegation.Status.ACTIVE);
        return delegationRepo.save(req);
    }

    public List<TempDelegation> delegations(Long childId) {
        return delegationRepo.findByChildIdOrderByValidDateDesc(childId);
    }

    public List<TempDelegation> todayDelegations() {
        return delegationRepo.findByValidDateOrderByIdAsc(LocalDate.now());
    }

    // ---------- 接送留言 ----------

    @Transactional
    public PickupMessage createPickupMessage(User parent, Long childId, LocalDate date, String content) {
        Child child = get(childId);
        checkOwn(parent, child);
        PickupMessage msg = new PickupMessage();
        msg.setChild(child);
        msg.setMsgDate(date == null ? LocalDate.now() : date);
        msg.setContent(content);
        msg.setCreatedBy(parent);
        return messageRepo.save(msg);
    }

    public List<PickupMessage> pickupMessages(Long childId, LocalDate date) {
        if (date != null) {
            return messageRepo.findByChildIdAndMsgDateOrderByCreatedAtAsc(childId, date);
        }
        return messageRepo.findByChildIdOrderByCreatedAtDesc(childId);
    }

    private void checkOwn(User parent, Child child) {
        if (!child.getParent().getId().equals(parent.getId())) {
            throw BizException.forbidden("只能操作自己孩子的资料");
        }
    }
}
