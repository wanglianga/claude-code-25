package com.nursery.service;

import com.nursery.entity.*;
import com.nursery.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 各角色工作台统计
 */
@Service
public class DashboardService {

    private final EnrollmentApplicationRepository applicationRepo;
    private final AnomalyAlertRepository alertRepo;
    private final NurseryEventRepository eventRepo;
    private final EventParticipantRepository participantRepo;
    private final MorningCheckRepository morningCheckRepo;
    private final PickupRecordRepository pickupRecordRepo;
    private final TempDelegationRepository delegationRepo;
    private final ChildRepository childRepo;
    private final ClassroomRepository classroomRepo;
    private final AccessService accessService;

    public DashboardService(EnrollmentApplicationRepository applicationRepo,
                            AnomalyAlertRepository alertRepo,
                            NurseryEventRepository eventRepo,
                            EventParticipantRepository participantRepo,
                            MorningCheckRepository morningCheckRepo,
                            PickupRecordRepository pickupRecordRepo,
                            TempDelegationRepository delegationRepo,
                            ChildRepository childRepo,
                            ClassroomRepository classroomRepo,
                            AccessService accessService) {
        this.applicationRepo = applicationRepo;
        this.alertRepo = alertRepo;
        this.eventRepo = eventRepo;
        this.participantRepo = participantRepo;
        this.morningCheckRepo = morningCheckRepo;
        this.pickupRecordRepo = pickupRecordRepo;
        this.delegationRepo = delegationRepo;
        this.childRepo = childRepo;
        this.classroomRepo = classroomRepo;
        this.accessService = accessService;
    }

    public Map<String, Object> build(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        long openEvents = eventRepo.countByStatus(NurseryEvent.Status.OPEN)
                + eventRepo.countByStatus(NurseryEvent.Status.PROCESSING);

        switch (user.getRole()) {
            case DIRECTOR -> {
                m.put("pendingClassApps", applicationRepo.countByStatus(EnrollmentApplication.Status.PENDING_CLASS));
                m.put("pendingHealthApps", applicationRepo.countByStatus(EnrollmentApplication.Status.PENDING_HEALTH));
                m.put("openAlerts", alertRepo.countByStatus(AnomalyAlert.Status.OPEN));
                m.put("openEvents", openEvents);
                m.put("totalChildren", childRepo.count());
                m.put("totalClasses", classroomRepo.count());
            }
            case HEALTH -> {
                m.put("pendingHealthApps", applicationRepo.countByStatus(EnrollmentApplication.Status.PENDING_HEALTH));
                m.put("todayAbnormalChecks", morningCheckRepo.countByCheckDateAndResultNot(today, MorningCheck.Result.ENTER_CLASS));
                m.put("openEvents", openEvents);
            }
            case TEACHER -> {
                List<Child> mine = childRepo.findAll().stream()
                        .filter(c -> c.getStatus() == Child.Status.ENROLLED || c.getStatus() == Child.Status.ASSIGNED)
                        .filter(c -> accessService.isTeacherOf(user, c))
                        .toList();
                long checked = mine.stream()
                        .filter(c -> morningCheckRepo.findByChildIdAndCheckDate(c.getId(), today).isPresent())
                        .count();
                m.put("myClassChildren", mine.size());
                m.put("todayChecked", checked);
                m.put("todayUnchecked", mine.size() - checked);
                m.put("myOpenEvents", myOpenEvents(user));
            }
            case FRONTDESK -> {
                m.put("todayPickups", pickupRecordRepo.countByPickupDate(today));
                m.put("todayDenied", pickupRecordRepo.countByPickupDateAndResult(today, PickupRecord.Result.DENIED));
                m.put("todayDelegations", delegationRepo.findByValidDateOrderByIdAsc(today).size());
                m.put("openEvents", openEvents);
            }
            case PARENT -> {
                List<Child> children = childRepo.findByParentIdOrderByIdAsc(user.getId());
                m.put("childrenCount", children.size());
                m.put("myOpenEvents", myOpenEvents(user));
                long needSupplement = children.stream()
                        .flatMap(c -> applicationRepo.findByChildIdOrderByCreatedAtDesc(c.getId()).stream())
                        .filter(a -> a.getStatus() == EnrollmentApplication.Status.NEED_SUPPLEMENT)
                        .count();
                m.put("needSupplement", needSupplement);
            }
        }
        return m;
    }

    private long myOpenEvents(User user) {
        return participantRepo.findByUserId(user.getId()).stream()
                .map(EventParticipant::getEvent)
                .filter(e -> e.getStatus() == NurseryEvent.Status.OPEN
                        || e.getStatus() == NurseryEvent.Status.PROCESSING)
                .count();
    }
}
