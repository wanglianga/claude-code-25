package com.nursery.service;

import com.nursery.config.BizException;
import com.nursery.entity.*;
import com.nursery.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 异常预警：同一儿童在统计窗口内异常（晨检异常 + 需关注照护记录 + 事件）达到阈值，
 * 自动生成预警并给出处置建议。
 */
@Service
public class AlertService {

    private final AnomalyAlertRepository alertRepo;
    private final MorningCheckRepository morningCheckRepo;
    private final CareRecordRepository careRecordRepo;
    private final NurseryEventRepository eventRepo;

    private final int windowDays;
    private final int threshold;

    public AlertService(AnomalyAlertRepository alertRepo,
                        MorningCheckRepository morningCheckRepo,
                        CareRecordRepository careRecordRepo,
                        NurseryEventRepository eventRepo,
                        @Value("${app.anomaly.window-days}") int windowDays,
                        @Value("${app.anomaly.threshold}") int threshold) {
        this.alertRepo = alertRepo;
        this.morningCheckRepo = morningCheckRepo;
        this.careRecordRepo = careRecordRepo;
        this.eventRepo = eventRepo;
        this.windowDays = windowDays;
        this.threshold = threshold;
    }

    /**
     * 重新统计儿童近期异常并在达到阈值时生成预警（已有未处理预警时不重复生成）。
     * 使用独立事务，避免主流程回滚时丢失预警。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkChild(Child child) {
        LocalDate since = LocalDate.now().minusDays(windowDays);
        long abnormalChecks = morningCheckRepo.countByChildIdAndCheckDateAfterAndResultNot(
                child.getId(), since, MorningCheck.Result.ENTER_CLASS);
        long attentionCares = careRecordRepo.countByChildIdAndRecordDateAfterAndSeverityNot(
                child.getId(), since, CareRecord.Severity.NORMAL);
        long events = eventRepo.countByChildIdAndCreatedAtAfter(child.getId(), since.atStartOfDay());
        long total = abnormalChecks + attentionCares + events;

        if (total < threshold) return;
        if (alertRepo.existsByChildIdAndStatus(child.getId(), AnomalyAlert.Status.OPEN)) return;

        AnomalyAlert alert = new AnomalyAlert();
        alert.setChild(child);
        alert.setAnomalyCount((int) total);
        alert.setWindowDays(windowDays);
        alert.setReason(String.format("近%d天：晨检异常%d次，需关注照护记录%d条，协同事件%d起",
                windowDays, abnormalChecks, attentionCares, events));
        alert.setSuggestion(suggest(abnormalChecks, attentionCares, events, child));
        alertRepo.save(alert);
    }

    private AnomalyAlert.Suggestion suggest(long abnormalChecks, long attentionCares, long events, Child child) {
        // 健康异常为主 → 增加保健观察；伤情/抓咬为主 → 考虑调整班级；其余 → 约谈家长
        if (abnormalChecks >= 2 || Boolean.TRUE.equals(child.getHealthObservation())) {
            return AnomalyAlert.Suggestion.HEALTH_OBSERVATION;
        }
        if (attentionCares >= 2) {
            return AnomalyAlert.Suggestion.CLASS_ADJUST;
        }
        return AnomalyAlert.Suggestion.PARENT_MEETING;
    }
}
