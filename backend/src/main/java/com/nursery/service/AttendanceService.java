package com.nursery.service;

import com.nursery.config.BizException;
import com.nursery.entity.*;
import com.nursery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 晨检与在园照护记录。
 * 晨检结果影响能否入班/隔离观察/通知家长接回；发热、药品漏带自动创建协同事件。
 */
@Service
public class AttendanceService {

    /** 发热阈值（℃） */
    public static final BigDecimal FEVER_THRESHOLD = new BigDecimal("37.3");

    private final MorningCheckRepository morningCheckRepo;
    private final CareRecordRepository careRecordRepo;
    private final ChildRepository childRepo;
    private final MedicationRequestRepository medicationRepo;
    private final PickupRecordRepository pickupRecordRepo;
    private final PickupMessageRepository pickupMessageRepo;
    private final NurseryEventRepository eventRepo;
    private final AccessService accessService;
    private final EventService eventService;
    private final AlertService alertService;

    public AttendanceService(MorningCheckRepository morningCheckRepo,
                             CareRecordRepository careRecordRepo,
                             ChildRepository childRepo,
                             MedicationRequestRepository medicationRepo,
                             PickupRecordRepository pickupRecordRepo,
                             PickupMessageRepository pickupMessageRepo,
                             NurseryEventRepository eventRepo,
                             AccessService accessService,
                             EventService eventService,
                             AlertService alertService) {
        this.morningCheckRepo = morningCheckRepo;
        this.careRecordRepo = careRecordRepo;
        this.childRepo = childRepo;
        this.medicationRepo = medicationRepo;
        this.pickupRecordRepo = pickupRecordRepo;
        this.pickupMessageRepo = pickupMessageRepo;
        this.eventRepo = eventRepo;
        this.accessService = accessService;
        this.eventService = eventService;
        this.alertService = alertService;
    }

    /**
     * 老师本班儿童名单 + 当日晨检状态 + 当日是否有喂药委托
     */
    public List<Map<String, Object>> classChildren(User teacher, LocalDate date) {
        List<Child> children = childRepo.findAll().stream()
                .filter(c -> c.getStatus() == Child.Status.ENROLLED || c.getStatus() == Child.Status.ASSIGNED)
                .filter(c -> accessService.isTeacherOf(teacher, c))
                .sorted(Comparator.comparing(Child::getId))
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Child c : children) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("child", c);
            m.put("morningCheck", morningCheckRepo.findByChildIdAndCheckDate(c.getId(), date).orElse(null));
            m.put("hasMedicationToday", hasActiveMedication(c.getId(), date));
            result.add(m);
        }
        return result;
    }

    public boolean hasActiveMedication(Long childId, LocalDate date) {
        return medicationRepo.findByChildIdAndStatus(childId, MedicationRequest.Status.ACTIVE).stream()
                .anyMatch(r -> (r.getStartDate() == null || !r.getStartDate().isAfter(date))
                        && (r.getEndDate() == null || !r.getEndDate().isBefore(date)));
    }

    /**
     * 登记/更新晨检。同一儿童同一天仅一条记录（重复提交为更新）。
     */
    @Transactional
    public MorningCheck upsertMorningCheck(User teacher, Long childId, LocalDate date, BigDecimal temperature,
                                           String skinStatus, String mood, String diet, String carriedItems,
                                           Boolean medicationBrought, MorningCheck.Result result, String note) {
        Child child = childRepo.findById(childId).orElseThrow(() -> BizException.notFound("儿童不存在"));
        accessService.checkTeacherOperate(teacher, child);

        MorningCheck check = morningCheckRepo.findByChildIdAndCheckDate(childId, date).orElseGet(MorningCheck::new);
        boolean isNew = check.getId() == null;
        check.setChild(child);
        check.setCheckDate(date);
        check.setTeacher(teacher);
        check.setTemperature(temperature);
        check.setSkinStatus(skinStatus);
        check.setMood(mood);
        check.setDiet(diet);
        check.setCarriedItems(carriedItems);
        check.setMedicationBrought(medicationBrought);
        check.setResult(result);
        check.setNote(note);
        morningCheckRepo.save(check);

        if (isNew) {
            handleMorningCheckConsequences(child, check, teacher);
        }
        return check;
    }

    private void handleMorningCheckConsequences(Child child, MorningCheck check, User teacher) {
        if (check.getResult() == MorningCheck.Result.ENTER_CLASS) {
            // 正常入班仍需核对药品漏带
            maybeMedicationMissingEvent(child, check, teacher);
            return;
        }
        boolean fever = check.getTemperature() != null
                && check.getTemperature().compareTo(FEVER_THRESHOLD) >= 0;
        String resultLabel = check.getResult() == MorningCheck.Result.ISOLATION ? "隔离观察" : "通知家长接回";
        String title = fever
                ? "晨检发热 " + check.getTemperature() + "℃（" + resultLabel + "）"
                : "晨检异常（" + resultLabel + "）";
        String desc = String.format("晨检结果：%s。体温：%s；皮肤：%s；情绪：%s；饮食：%s；携带物品：%s。备注：%s",
                resultLabel,
                check.getTemperature() == null ? "未测" : check.getTemperature() + "℃",
                nullToDash(check.getSkinStatus()), nullToDash(check.getMood()),
                nullToDash(check.getDiet()), nullToDash(check.getCarriedItems()),
                nullToDash(check.getNote()));
        eventService.createEvent(
                fever ? NurseryEvent.EventType.FEVER : NurseryEvent.EventType.OTHER,
                child, title, desc, teacher);
        maybeMedicationMissingEvent(child, check, teacher);
    }

    private void maybeMedicationMissingEvent(Child child, MorningCheck check, User teacher) {
        if (Boolean.FALSE.equals(check.getMedicationBrought())
                && hasActiveMedication(child.getId(), check.getCheckDate())) {
            eventService.createEvent(NurseryEvent.EventType.MEDICATION_MISSING, child,
                    "药品漏带提醒",
                    "该儿童今日有在服喂药委托，但晨检时未携带药品，请家长尽快补送或确认暂停喂药。",
                    teacher);
        }
    }

    private String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    public List<MorningCheck> listMorningChecks(LocalDate date, Long classroomId) {
        if (classroomId != null) {
            return morningCheckRepo.findByCheckDateAndChildClassroomIdOrderByIdAsc(date, classroomId);
        }
        return morningCheckRepo.findByCheckDateOrderByIdAsc(date);
    }

    /**
     * 添加在园照护记录（午间照护/喂药/如厕/睡眠/活动伤情）。
     * 伤情需关注或严重时自动创建协同事件（抓咬 → 抓咬事件）。
     */
    @Transactional
    public CareRecord addCareRecord(User teacher, Long childId, LocalDate date, CareRecord.CareType type,
                                    String detail, CareRecord.Severity severity, CareRecord.InjuryType injuryType) {
        Child child = childRepo.findById(childId).orElseThrow(() -> BizException.notFound("儿童不存在"));
        accessService.checkTeacherOperate(teacher, child);

        CareRecord record = new CareRecord();
        record.setChild(child);
        record.setRecordDate(date);
        record.setType(type);
        record.setDetail(detail);
        record.setSeverity(severity);
        record.setInjuryType(type == CareRecord.CareType.INJURY ? injuryType : null);
        record.setRecordedBy(teacher);
        careRecordRepo.save(record);

        if (type == CareRecord.CareType.INJURY && severity != CareRecord.Severity.NORMAL) {
            boolean bite = injuryType == CareRecord.InjuryType.SCRATCH_BITE;
            eventService.createEvent(
                    bite ? NurseryEvent.EventType.BITE_INCIDENT : NurseryEvent.EventType.INJURY,
                    child,
                    (bite ? "班级抓咬事件" : "活动伤情") + "（" + severityLabel(severity) + "）",
                    "照护记录：" + nullToDash(detail), teacher);
        } else if (severity == CareRecord.Severity.SERIOUS) {
            eventService.createEvent(NurseryEvent.EventType.OTHER, child,
                    "照护异常（严重）", "照护记录：" + nullToDash(detail), teacher);
        } else {
            alertService.checkChild(child);
        }
        return record;
    }

    private String severityLabel(CareRecord.Severity s) {
        return switch (s) {
            case NORMAL -> "一般";
            case ATTENTION -> "需关注";
            case SERIOUS -> "严重";
        };
    }

    /**
     * 儿童某日完整在园记录：晨检 + 照护记录 + 喂药委托 + 接送记录 + 当日事件。
     */
    public Map<String, Object> dailyRecord(Long childId, LocalDate date) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", date.toString());
        m.put("morningCheck", morningCheckRepo.findByChildIdAndCheckDate(childId, date).orElse(null));
        m.put("careRecords", careRecordRepo.findByChildIdAndRecordDateOrderByCreatedAtAsc(childId, date));
        m.put("medicationRequests", medicationRepo.findByChildIdAndStatus(childId, MedicationRequest.Status.ACTIVE)
                .stream().filter(r -> (r.getStartDate() == null || !r.getStartDate().isAfter(date))
                        && (r.getEndDate() == null || !r.getEndDate().isBefore(date)))
                .collect(Collectors.toList()));
        m.put("pickupRecords", pickupRecordRepo.findByChildIdAndPickupDate(childId, date));
        m.put("pickupMessages", pickupMessageRepo.findByChildIdAndMsgDateOrderByCreatedAtAsc(childId, date));
        m.put("events", eventRepo.findByChildIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                childId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()));
        return m;
    }
}
