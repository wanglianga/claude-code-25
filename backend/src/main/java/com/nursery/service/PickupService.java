package com.nursery.service;

import com.nursery.config.BizException;
import com.nursery.entity.*;
import com.nursery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 接送核验：授权人 / 临时委托匹配，人脸或证件核验方式留痕，
 * 未授权人员到场自动创建协同事件。
 */
@Service
public class PickupService {

    private final PickupAuthorizationRepository authRepo;
    private final TempDelegationRepository delegationRepo;
    private final PickupRecordRepository recordRepo;
    private final ChildRepository childRepo;
    private final EventService eventService;

    public PickupService(PickupAuthorizationRepository authRepo,
                         TempDelegationRepository delegationRepo,
                         PickupRecordRepository recordRepo,
                         ChildRepository childRepo,
                         EventService eventService) {
        this.authRepo = authRepo;
        this.delegationRepo = delegationRepo;
        this.recordRepo = recordRepo;
        this.childRepo = childRepo;
        this.eventService = eventService;
    }

    /**
     * 核验接送人身份：先匹配固定授权人，再匹配当日临时委托。
     */
    public Map<String, Object> verify(Long childId, String name, String idNumber) {
        Child child = childRepo.findById(childId).orElseThrow(() -> BizException.notFound("儿童不存在"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("child", child);

        if (name == null || name.isBlank()) {
            result.put("matched", false);
            result.put("message", "请输入接送人姓名");
            return result;
        }
        String trimmed = name.trim();

        for (PickupAuthorization a : authRepo.findByChildIdAndActiveTrue(childId)) {
            if (a.getName().equals(trimmed) && idMatches(a.getIdNumber(), idNumber)) {
                result.put("matched", true);
                result.put("authType", PickupRecord.AuthType.AUTHORIZED.name());
                result.put("authorization", a);
                result.put("message", "已匹配固定授权人：" + a.getName() + "（" + a.getRelation() + "）");
                return result;
            }
        }
        for (TempDelegation d : delegationRepo.findByChildIdAndValidDateAndStatus(
                childId, LocalDate.now(), TempDelegation.Status.ACTIVE)) {
            if (d.getDelegateName().equals(trimmed) && idMatches(d.getDelegateIdNumber(), idNumber)) {
                result.put("matched", true);
                result.put("authType", PickupRecord.AuthType.TEMP_DELEGATION.name());
                result.put("delegation", d);
                result.put("message", "已匹配今日临时委托：" + d.getDelegateName());
                return result;
            }
        }
        result.put("matched", false);
        result.put("message", "未匹配到授权人或当日临时委托，请勿放行并上报事件");
        return result;
    }

    /** 证件号匹配：登记了证件号则必须一致（前台可只核对后4位） */
    private boolean idMatches(String registered, String provided) {
        if (registered == null || registered.isBlank()) return true;
        if (provided == null || provided.isBlank()) return false;
        String reg = registered.trim();
        String pro = provided.trim();
        return reg.equalsIgnoreCase(pro)
                || (pro.length() >= 4 && reg.endsWith(pro))
                || (reg.length() >= 4 && pro.endsWith(reg.substring(reg.length() - 4)));
    }

    /**
     * 登记接送记录。放行必须已通过核验；拒绝必须填写原因并自动创建未授权到场事件。
     */
    @Transactional
    public PickupRecord record(User operator, Long childId, String personName, String personIdNumber,
                               PickupRecord.VerifyMethod verifyMethod, PickupRecord.Result result,
                               String denyReason) {
        Child child = childRepo.findById(childId).orElseThrow(() -> BizException.notFound("儿童不存在"));

        Map<String, Object> verify = verify(childId, personName, personIdNumber);
        boolean matched = Boolean.TRUE.equals(verify.get("matched"));

        PickupRecord record = new PickupRecord();
        record.setChild(child);
        record.setPickupDate(LocalDate.now());
        record.setPickupPersonName(personName);
        record.setPickupPersonIdNumber(personIdNumber);
        record.setVerifyMethod(verifyMethod);
        record.setOperator(operator);

        if (result == PickupRecord.Result.SUCCESS) {
            if (!matched) {
                throw new BizException("未匹配到授权人，不能放行；如需拒绝请选择「拒绝并上报」");
            }
            record.setResult(PickupRecord.Result.SUCCESS);
            record.setAuthType(PickupRecord.AuthType.valueOf((String) verify.get("authType")));
            // 临时委托使用后失效
            Object delegation = verify.get("delegation");
            if (delegation instanceof TempDelegation d) {
                d.setStatus(TempDelegation.Status.USED);
                delegationRepo.save(d);
            }
        } else {
            if (denyReason == null || denyReason.isBlank()) {
                throw new BizException("拒绝接送必须填写原因");
            }
            record.setResult(PickupRecord.Result.DENIED);
            record.setDenyReason(denyReason);
            recordRepo.save(record);
            eventService.createEvent(NurseryEvent.EventType.UNAUTHORIZED_PICKUP, child,
                    "未授权人员到场接孩子",
                    "接送人「" + personName + "」未通过授权核验，前台已拒绝放行。原因：" + denyReason
                            + "。核验方式：" + (verifyMethod == PickupRecord.VerifyMethod.FACE ? "人脸核验" : "证件核验"),
                    operator);
            return record;
        }
        return recordRepo.save(record);
    }

    public List<PickupRecord> listRecords(LocalDate date) {
        return recordRepo.findByPickupDateOrderByCreatedAtAsc(date);
    }
}
