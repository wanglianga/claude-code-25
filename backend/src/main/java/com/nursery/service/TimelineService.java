package com.nursery.service;

import com.nursery.entity.*;
import com.nursery.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 时间轴：按儿童 / 班级 / 家长 / 事件聚合晨检、照护、接送、评估与沟通记录，
 * 用于纠纷处理时核对各环节是否连续。
 */
@Service
public class TimelineService {

    public record TimelineItem(LocalDateTime time, String category, String title, String detail, Long refId) {}

    private final EnrollmentApplicationRepository applicationRepo;
    private final ClassAssignmentRepository assignmentRepo;
    private final HealthAssessmentRepository assessmentRepo;
    private final MorningCheckRepository morningCheckRepo;
    private final CareRecordRepository careRecordRepo;
    private final PickupRecordRepository pickupRecordRepo;
    private final NurseryEventRepository eventRepo;
    private final EventMessageRepository messageRepo;
    private final AnomalyAlertRepository alertRepo;
    private final ChildRepository childRepo;
    private final MealSubstitutionRepository mealSubRepo;

    public TimelineService(EnrollmentApplicationRepository applicationRepo,
                           ClassAssignmentRepository assignmentRepo,
                           HealthAssessmentRepository assessmentRepo,
                           MorningCheckRepository morningCheckRepo,
                           CareRecordRepository careRecordRepo,
                           PickupRecordRepository pickupRecordRepo,
                           NurseryEventRepository eventRepo,
                           EventMessageRepository messageRepo,
                           AnomalyAlertRepository alertRepo,
                           ChildRepository childRepo,
                           MealSubstitutionRepository mealSubRepo) {
        this.applicationRepo = applicationRepo;
        this.assignmentRepo = assignmentRepo;
        this.assessmentRepo = assessmentRepo;
        this.morningCheckRepo = morningCheckRepo;
        this.careRecordRepo = careRecordRepo;
        this.pickupRecordRepo = pickupRecordRepo;
        this.eventRepo = eventRepo;
        this.messageRepo = messageRepo;
        this.alertRepo = alertRepo;
        this.childRepo = childRepo;
        this.mealSubRepo = mealSubRepo;
    }

    public List<TimelineItem> childTimeline(Long childId) {
        List<TimelineItem> items = new ArrayList<>();
        Child child = childRepo.findById(childId).orElse(null);
        if (child == null) return items;

        for (EnrollmentApplication app : applicationRepo.findByChildIdOrderByCreatedAtDesc(childId)) {
            items.add(new TimelineItem(app.getCreatedAt(), "APPLICATION",
                    "提交入托申请", "家长期望：" + dash(app.getParentExpectations()), app.getId()));
            if (app.getSupplementNote() != null && !app.getSupplementNote().isBlank()) {
                items.add(new TimelineItem(app.getUpdatedAt(), "APPLICATION",
                        "家长补充资料", app.getSupplementNote(), app.getId()));
            }
        }
        for (ClassAssignment a : assignmentRepo.findByApplicationChildIdOrderByCreatedAtDesc(childId)) {
            items.add(new TimelineItem(a.getCreatedAt(), "ASSIGNMENT",
                    "园长分班 → " + a.getClassroom().getName(),
                    "考量备注：" + dash(a.getNote()), a.getId()));
        }
        for (HealthAssessment h : assessmentRepo.findByApplicationChildIdOrderByCreatedAtDesc(childId)) {
            items.add(new TimelineItem(h.getCreatedAt(), "HEALTH",
                    "健康评估：" + healthResultLabel(h.getResult()),
                    buildHealthDetail(h), h.getId()));
        }
        for (MorningCheck mc : morningCheckRepo.findByChildIdOrderByCheckDateDesc(childId)) {
            items.add(new TimelineItem(mc.getCreatedAt(), "MORNING_CHECK",
                    "晨检：" + morningResultLabel(mc.getResult()),
                    String.format("体温 %s｜皮肤 %s｜情绪 %s｜饮食 %s｜携带 %s",
                            mc.getTemperature() == null ? "未测" : mc.getTemperature() + "℃",
                            dash(mc.getSkinStatus()), dash(mc.getMood()), dash(mc.getDiet()),
                            dash(mc.getCarriedItems())), mc.getId()));
        }
        for (CareRecord c : careRecordRepo.findByChildIdOrderByRecordDateDescCreatedAtDesc(childId)) {
            items.add(new TimelineItem(c.getCreatedAt(), "CARE",
                    "照护[" + careTypeLabel(c.getType()) + "]" + severitySuffix(c.getSeverity()),
                    dash(c.getDetail()), c.getId()));
        }
        for (PickupRecord p : pickupRecordRepo.findByChildIdOrderByCreatedAtDesc(childId)) {
            String title = p.getResult() == PickupRecord.Result.SUCCESS
                    ? "接送放行：" + p.getPickupPersonName()
                    : "接送拒绝：" + p.getPickupPersonName();
            String detail = "核验方式：" + (p.getVerifyMethod() == PickupRecord.VerifyMethod.FACE ? "人脸" : "证件")
                    + (p.getAuthType() != null
                        ? "｜授权类型：" + (p.getAuthType() == PickupRecord.AuthType.AUTHORIZED ? "固定授权人" : "临时委托")
                        : "")
                    + (p.getDenyReason() != null ? "｜原因：" + p.getDenyReason() : "");
            items.add(new TimelineItem(p.getCreatedAt(), "PICKUP", title, detail, p.getId()));
        }
        for (NurseryEvent e : eventRepo.findByChildIdOrderByCreatedAtDesc(childId)) {
            items.add(new TimelineItem(e.getCreatedAt(), "EVENT",
                    "事件[" + eventTypeLabel(e.getType()) + "] " + e.getTitle(),
                    "状态：" + eventStatusLabel(e.getStatus()) + "｜" + dash(e.getDescription()), e.getId()));
        }
        for (EventMessage msg : messageRepo.findByEventChildIdOrderByCreatedAtAsc(childId)) {
            items.add(new TimelineItem(msg.getCreatedAt(), "COMMUNICATION",
                    "事件沟通 · " + msg.getSender().getName(), msg.getContent(), msg.getEvent().getId()));
        }
        for (AnomalyAlert a : alertRepo.findByChildIdOrderByCreatedAtDesc(childId)) {
            items.add(new TimelineItem(a.getCreatedAt(), "ALERT",
                    "异常预警：" + suggestionLabel(a.getSuggestion()),
                    a.getReason() + (a.getHandleNote() != null ? "｜处理：" + a.getHandleNote() : ""), a.getId()));
        }
        for (MealSubstitution s : mealSubRepo.findByChildIdOrderByCreatedAtDesc(childId)) {
            items.add(new TimelineItem(s.getCreatedAt(), "MEAL",
                    "过敏餐替换[" + mealSubStatusLabel(s.getStatus()) + "] 「" + s.getOriginalDish()
                            + "」→「" + s.getSubstituteDish() + "」",
                    (s.getReason() == MealSubstitution.Reason.INGREDIENT_SHORTAGE ? "食材缺货" : "过敏源风险")
                            + (s.getMatchedAllergy() != null ? "（命中：" + s.getMatchedAllergy() + "）" : "")
                            + (s.getParentConfirmedAt() != null
                                ? "｜家长确认 " + s.getParentConfirmedAt().toLocalTime().withNano(0) : "")
                            + (s.getServedAt() != null ? "｜已分餐 " + s.getServedAt().toLocalTime().withNano(0) : ""),
                    s.getId()));
        }
        items.sort(Comparator.comparing(TimelineItem::time).reversed());
        return items;
    }

    /**
     * 班级时间轴：本班儿童近 7 天动态（最多 200 条）
     */
    public List<TimelineItem> classTimeline(Long classroomId) {
        List<Child> children = childRepo.findByClassroomId(classroomId);
        LocalDateTime since = LocalDate.now().minusDays(7).atStartOfDay();
        List<TimelineItem> all = new ArrayList<>();
        for (Child c : children) {
            for (TimelineItem item : childTimeline(c.getId())) {
                if (item.time().isAfter(since)) {
                    all.add(new TimelineItem(item.time(), item.category(),
                            c.getName() + "｜" + item.title(), item.detail(), item.refId()));
                }
            }
        }
        all.sort(Comparator.comparing(TimelineItem::time).reversed());
        return all.stream().limit(200).collect(Collectors.toList());
    }

    /**
     * 家长时间轴：名下所有儿童动态合并
     */
    public List<TimelineItem> parentTimeline(Long parentId) {
        List<TimelineItem> all = new ArrayList<>();
        for (Child c : childRepo.findByParentIdOrderByIdAsc(parentId)) {
            for (TimelineItem item : childTimeline(c.getId())) {
                all.add(new TimelineItem(item.time(), item.category(),
                        c.getName() + "｜" + item.title(), item.detail(), item.refId()));
            }
        }
        all.sort(Comparator.comparing(TimelineItem::time).reversed());
        return all;
    }

    /**
     * 事件时间轴：事件本身 + 事件内沟通 + 儿童当日相关记录（核对处理是否连续）
     */
    public List<TimelineItem> eventTimeline(NurseryEvent event) {
        List<TimelineItem> items = new ArrayList<>();
        Long childId = event.getChild().getId();
        LocalDate day = event.getCreatedAt().toLocalDate();

        items.add(new TimelineItem(event.getCreatedAt(), "EVENT",
                "事件创建[" + eventTypeLabel(event.getType()) + "] " + event.getTitle(),
                dash(event.getDescription()), event.getId()));
        for (EventMessage msg : messageRepo.findByEventIdOrderByCreatedAtAsc(event.getId())) {
            items.add(new TimelineItem(msg.getCreatedAt(), "COMMUNICATION",
                    "事件沟通 · " + msg.getSender().getName(), msg.getContent(), event.getId()));
        }
        // 当日该儿童的晨检 / 照护 / 接送，便于核对事件前后环节
        morningCheckRepo.findByChildIdAndCheckDate(childId, day).ifPresent(mc ->
                items.add(new TimelineItem(mc.getCreatedAt(), "MORNING_CHECK",
                        "当日晨检：" + morningResultLabel(mc.getResult()),
                        "体温 " + (mc.getTemperature() == null ? "未测" : mc.getTemperature() + "℃"), mc.getId())));
        for (CareRecord c : careRecordRepo.findByChildIdAndRecordDateOrderByCreatedAtAsc(childId, day)) {
            items.add(new TimelineItem(c.getCreatedAt(), "CARE",
                    "当日照护[" + careTypeLabel(c.getType()) + "]", dash(c.getDetail()), c.getId()));
        }
        for (PickupRecord p : pickupRecordRepo.findByChildIdAndPickupDate(childId, day)) {
            items.add(new TimelineItem(p.getCreatedAt(), "PICKUP",
                    (p.getResult() == PickupRecord.Result.SUCCESS ? "当日接送放行：" : "当日接送拒绝：")
                            + p.getPickupPersonName(),
                    p.getDenyReason() != null ? p.getDenyReason() : "", p.getId()));
        }
        if (event.getClosedAt() != null) {
            items.add(new TimelineItem(event.getClosedAt(), "EVENT", "事件关闭",
                    dash(event.getResolutionNote()), event.getId()));
        }
        items.sort(Comparator.comparing(TimelineItem::time));
        return items;
    }

    // ---------- 标签 ----------

    private String dash(String s) { return s == null || s.isBlank() ? "—" : s; }

    private String healthResultLabel(HealthAssessment.Result r) {
        return switch (r) {
            case PASS -> "通过";
            case ALLERGY_MEAL -> "通过（需过敏餐）";
            case TEMP_OBSERVATION -> "通过（需临时观察）";
            case NEED_SUPPLEMENT -> "需家长补充资料";
        };
    }

    private String buildHealthDetail(HealthAssessment h) {
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(h.getAllergyMealRequired())) sb.append("需过敏餐；");
        if (Boolean.TRUE.equals(h.getObservationRequired())) sb.append("需临时观察；");
        if (h.getSupplementRequest() != null && !h.getSupplementRequest().isBlank())
            sb.append("补充资料要求：").append(h.getSupplementRequest()).append("；");
        if (h.getNote() != null && !h.getNote().isBlank()) sb.append("备注：").append(h.getNote());
        return sb.length() == 0 ? "—" : sb.toString();
    }

    private String morningResultLabel(MorningCheck.Result r) {
        return switch (r) {
            case ENTER_CLASS -> "正常入班";
            case ISOLATION -> "隔离观察";
            case PARENT_PICKUP -> "通知家长接回";
        };
    }

    private String careTypeLabel(CareRecord.CareType t) {
        return switch (t) {
            case NOON_CARE -> "午间照护";
            case MEDICATION -> "喂药";
            case TOILET -> "如厕";
            case SLEEP -> "睡眠";
            case INJURY -> "活动伤情";
            case MEAL -> "用餐";
        };
    }

    private String mealSubStatusLabel(MealSubstitution.Status s) {
        return MealService.statusLabel(s);
    }

    private String severitySuffix(CareRecord.Severity s) {
        return switch (s) {
            case NORMAL -> "";
            case ATTENTION -> "（需关注）";
            case SERIOUS -> "（严重）";
        };
    }

    private String eventTypeLabel(NurseryEvent.EventType t) {
        return switch (t) {
            case UNAUTHORIZED_PICKUP -> "未授权接送";
            case FEVER -> "发热";
            case MEDICATION_MISSING -> "药品漏带";
            case BITE_INCIDENT -> "抓咬事件";
            case INJURY -> "活动伤情";
            case REFUND_DISPUTE -> "退费争议";
            case OTHER -> "其他";
        };
    }

    private String eventStatusLabel(NurseryEvent.Status s) {
        return switch (s) {
            case OPEN -> "待处理";
            case PROCESSING -> "处理中";
            case RESOLVED -> "已解决";
            case CLOSED -> "已关闭";
        };
    }

    private String suggestionLabel(AnomalyAlert.Suggestion s) {
        return switch (s) {
            case CLASS_ADJUST -> "建议调整班级";
            case HEALTH_OBSERVATION -> "建议增加保健观察";
            case PARENT_MEETING -> "建议约谈家长";
        };
    }
}
