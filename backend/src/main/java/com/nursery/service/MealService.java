package com.nursery.service;

import com.nursery.config.BizException;
import com.nursery.entity.*;
import com.nursery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 过敏餐临时替换：
 * 厨房发布每日菜单后，平台比对在托儿童过敏档案与菜单食材，找出受影响儿童；
 * 厨房发起替换单（食材缺货 / 过敏源风险），平台核验替代食材并生成营养比对说明；
 * 替换方案依次经保健老师 → 班级老师 → 家长确认；
 * 家长确认后厨房出餐、老师分餐，当日饮食记录（照护记录-用餐）同步更新；
 * 家长未确认或任一环节拒绝时，分餐页给出人工照护提示，避免误食。
 */
@Service
public class MealService {

    /** 常见过敏源关键词（用于档案 ↔ 食材比对） */
    private static final List<String> ALLERGEN_KEYWORDS = List.of(
            "花生", "牛奶", "奶粉", "蛋", "虾", "蟹", "海鲜", "鱼", "芒果", "菠萝",
            "坚果", "核桃", "大豆", "麸质", "小麦", "芝麻", "乳糖", "草莓", "猕猴桃");

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final DailyMenuRepository menuRepo;
    private final MealSubstitutionRepository subRepo;
    private final ChildRepository childRepo;
    private final CareRecordRepository careRecordRepo;
    private final AccessService accessService;

    public MealService(DailyMenuRepository menuRepo,
                       MealSubstitutionRepository subRepo,
                       ChildRepository childRepo,
                       CareRecordRepository careRecordRepo,
                       AccessService accessService) {
        this.menuRepo = menuRepo;
        this.subRepo = subRepo;
        this.childRepo = childRepo;
        this.careRecordRepo = careRecordRepo;
        this.accessService = accessService;
    }

    // ---------- 每日菜单 ----------

    /** 厨房发布/更新某日某餐次菜单（同一日期+餐次仅一份，重复提交为更新） */
    @Transactional
    public DailyMenu upsertMenu(User kitchen, LocalDate date, DailyMenu.MealType mealType,
                                String dishes, String ingredients, String nutritionNotes) {
        DailyMenu menu = menuRepo.findByMenuDateAndMealType(date, mealType).orElseGet(DailyMenu::new);
        menu.setMenuDate(date);
        menu.setMealType(mealType);
        menu.setDishes(dishes);
        menu.setIngredients(ingredients);
        menu.setNutritionNotes(nutritionNotes);
        if (menu.getId() == null) {
            menu.setCreatedBy(kitchen);
        }
        menu.setUpdatedAt(LocalDateTime.now());
        return menuRepo.save(menu);
    }

    public List<DailyMenu> listMenus(LocalDate date) {
        return menuRepo.findByMenuDateOrderByMealTypeAsc(date);
    }

    public DailyMenu getMenu(Long menuId) {
        return menuRepo.findById(menuId).orElseThrow(() -> BizException.notFound("菜单不存在"));
    }

    // ---------- 平台比对：受影响儿童 ----------

    /** 从儿童过敏史/特殊照护需求中提取命中的过敏源关键词 */
    public List<String> extractAllergens(Child child) {
        String profile = (nullToEmpty(child.getAllergyHistory()) + " "
                + nullToEmpty(child.getSpecialCareNeeds())).toLowerCase();
        // 「无过敏」类档案直接跳过
        if (profile.matches("^(无|none)\\s*$") || profile.isBlank()) return List.of();
        List<String> hits = new ArrayList<>();
        for (String kw : ALLERGEN_KEYWORDS) {
            if (profile.contains(kw.toLowerCase())) hits.add(kw);
        }
        return hits;
    }

    /**
     * 比对菜单食材与在托儿童过敏档案，返回受影响儿童及命中过敏源。
     * 同时带出该儿童当日是否已有替换单，避免厨房重复发起。
     */
    public List<Map<String, Object>> affectedChildren(Long menuId) {
        DailyMenu menu = getMenu(menuId);
        String ingredients = nullToEmpty(menu.getIngredients()) + " " + nullToEmpty(menu.getDishes());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Child c : childRepo.findAll()) {
            if (c.getStatus() != Child.Status.ENROLLED) continue;
            List<String> allergens = extractAllergens(c);
            List<String> matched = allergens.stream().filter(ingredients::contains).toList();
            boolean allergyMeal = Boolean.TRUE.equals(c.getAllergyMealRequired());
            if (matched.isEmpty() && !allergyMeal) continue; // 无命中且无需过敏餐的儿童不受菜单影响
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("child", c);
            m.put("matchedAllergens", matched);
            m.put("allergyMealRequired", allergyMeal);
            m.put("allergenProfile", allergens);
            List<MealSubstitution> existing = subRepo.findByChildIdAndMealDate(c.getId(), menu.getMenuDate())
                    .stream()
                    .filter(s -> s.getMenu().getId().equals(menuId))
                    .filter(s -> s.getStatus() != MealSubstitution.Status.CANCELLED
                            && s.getStatus() != MealSubstitution.Status.REJECTED
                            && s.getStatus() != MealSubstitution.Status.SERVED)
                    .toList();
            m.put("existingSubstitution", existing.isEmpty() ? null : existing.get(0));
            result.add(m);
        }
        return result;
    }

    // ---------- 替换单流转 ----------

    /**
     * 厨房发起替换单。平台自动比对儿童档案与替代食材：
     * 替代食材仍含过敏源时直接拒绝；通过时生成营养比对说明。
     */
    @Transactional
    public MealSubstitution createSubstitution(User kitchen, Long menuId, Long childId,
                                               MealSubstitution.Reason reason, String triggerDetail,
                                               String originalDish, String substituteDish,
                                               String substituteIngredients) {
        DailyMenu menu = getMenu(menuId);
        Child child = childRepo.findById(childId).orElseThrow(() -> BizException.notFound("儿童不存在"));
        if (child.getStatus() != Child.Status.ENROLLED) {
            throw new BizException("仅在托儿童可发起替换餐");
        }
        // 同一儿童同一菜单仅允许一张进行中的替换单（已分餐/已拒绝/已取消为终态，可再次发起）
        boolean inFlight = subRepo.findByChildIdAndMealDate(childId, menu.getMenuDate()).stream()
                .anyMatch(s -> s.getMenu().getId().equals(menuId)
                        && s.getStatus() != MealSubstitution.Status.CANCELLED
                        && s.getStatus() != MealSubstitution.Status.REJECTED
                        && s.getStatus() != MealSubstitution.Status.SERVED);
        if (inFlight) {
            throw new BizException("该儿童在此菜单下已有进行中的替换单");
        }

        List<String> allergens = extractAllergens(child);
        String menuIngredients = nullToEmpty(menu.getIngredients()) + " " + nullToEmpty(menu.getDishes());
        List<String> matched = allergens.stream().filter(menuIngredients::contains).toList();

        // 平台核验替代食材：不得含有该儿童任一过敏源
        String subIngredients = nullToEmpty(substituteIngredients) + " " + nullToEmpty(substituteDish);
        List<String> dangerous = allergens.stream().filter(subIngredients::contains).toList();
        if (!dangerous.isEmpty()) {
            throw new BizException("替代食材仍含有过敏源「" + String.join("、", dangerous)
                    + "」，平台比对未通过，请更换替代食材");
        }

        MealSubstitution sub = new MealSubstitution();
        sub.setMenu(menu);
        sub.setChild(child);
        sub.setMealDate(menu.getMenuDate());
        sub.setReason(reason);
        sub.setTriggerDetail(triggerDetail);
        sub.setOriginalDish(originalDish);
        sub.setSubstituteDish(substituteDish);
        sub.setSubstituteIngredients(substituteIngredients);
        sub.setMatchedAllergy(matched.isEmpty() ? null : String.join("、", matched));
        sub.setNutritionCheck(buildNutritionCheck(child, menu, matched, allergens, substituteIngredients));
        sub.setCreatedBy(kitchen);
        return subRepo.save(sub);
    }

    /** 平台比对说明：儿童档案 × 菜单食材 × 替代食材 × 营养要求 */
    private String buildNutritionCheck(Child child, DailyMenu menu, List<String> matched,
                                       List<String> allergens, String substituteIngredients) {
        StringBuilder sb = new StringBuilder("平台比对：");
        sb.append("儿童档案过敏源「").append(allergens.isEmpty() ? "未登记" : String.join("、", allergens)).append("」；");
        if (matched.isEmpty()) {
            sb.append("本次为食材缺货触发的临时替换，菜单未直接命中过敏源；");
        } else {
            sb.append("当日菜单食材命中过敏源「").append(String.join("、", matched)).append("」，须替换；");
        }
        sb.append("替代食材「").append(nullToEmpty(substituteIngredients).isBlank() ? "见替代菜品" : substituteIngredients)
                .append("」经核验不含该儿童过敏源；");
        sb.append("替代餐保留同等主食与优质蛋白/蔬果搭配，符合当日营养要求");
        if (menu.getNutritionNotes() != null && !menu.getNutritionNotes().isBlank()) {
            sb.append("（菜单营养基准：").append(menu.getNutritionNotes()).append("）");
        }
        sb.append("。");
        return sb.toString();
    }

    public MealSubstitution get(Long id) {
        return subRepo.findById(id).orElseThrow(() -> BizException.notFound("替换单不存在"));
    }

    /** 保健老师确认（拒绝则终态，进入人工照护） */
    @Transactional
    public MealSubstitution healthConfirm(User health, Long id, boolean approve, String note) {
        MealSubstitution sub = get(id);
        requireStatus(sub, MealSubstitution.Status.PENDING_HEALTH, "保健老师");
        sub.setHealthConfirmedBy(health);
        sub.setHealthConfirmedAt(LocalDateTime.now());
        sub.setHealthNote(note);
        sub.setStatus(approve ? MealSubstitution.Status.PENDING_TEACHER : MealSubstitution.Status.REJECTED);
        sub.setUpdatedAt(LocalDateTime.now());
        return subRepo.save(sub);
    }

    /** 班级老师确认（仅本班老师） */
    @Transactional
    public MealSubstitution teacherConfirm(User teacher, Long id, boolean approve, String note) {
        MealSubstitution sub = get(id);
        requireStatus(sub, MealSubstitution.Status.PENDING_TEACHER, "班级老师");
        accessService.checkTeacherOperate(teacher, sub.getChild());
        sub.setTeacherConfirmedBy(teacher);
        sub.setTeacherConfirmedAt(LocalDateTime.now());
        sub.setTeacherNote(note);
        sub.setStatus(approve ? MealSubstitution.Status.PENDING_PARENT : MealSubstitution.Status.REJECTED);
        sub.setUpdatedAt(LocalDateTime.now());
        return subRepo.save(sub);
    }

    /** 家长确认。确认后厨房方可出餐；拒绝/不确认则分餐页进入人工照护提示。 */
    @Transactional
    public MealSubstitution parentConfirm(User parent, Long id, boolean approve, String note) {
        MealSubstitution sub = get(id);
        requireStatus(sub, MealSubstitution.Status.PENDING_PARENT, "家长");
        if (!sub.getChild().getParent().getId().equals(parent.getId())) {
            throw BizException.forbidden("仅该儿童的家长可确认");
        }
        sub.setParentConfirmedAt(LocalDateTime.now());
        sub.setParentNote(note);
        sub.setStatus(approve ? MealSubstitution.Status.CONFIRMED : MealSubstitution.Status.REJECTED);
        sub.setUpdatedAt(LocalDateTime.now());
        return subRepo.save(sub);
    }

    /** 厨房出餐（家长已确认）。同步写入当日饮食记录（照护记录-用餐）。 */
    @Transactional
    public MealSubstitution execute(User kitchen, Long id) {
        MealSubstitution sub = get(id);
        requireStatus(sub, MealSubstitution.Status.CONFIRMED, "厨房出餐前需家长确认");
        sub.setKitchenExecutedBy(kitchen);
        sub.setKitchenExecutedAt(LocalDateTime.now());
        sub.setStatus(MealSubstitution.Status.EXECUTED);
        sub.setUpdatedAt(LocalDateTime.now());
        subRepo.save(sub);

        saveMealCareRecord(sub, kitchen, "替代餐出餐");
        return sub;
    }

    /** 班级老师分餐（厨房已出餐）。同步写入当日饮食记录。 */
    @Transactional
    public MealSubstitution serve(User teacher, Long id) {
        MealSubstitution sub = get(id);
        requireStatus(sub, MealSubstitution.Status.EXECUTED, "班级分餐前需厨房出餐");
        accessService.checkTeacherOperate(teacher, sub.getChild());
        sub.setServedBy(teacher);
        sub.setServedAt(LocalDateTime.now());
        sub.setStatus(MealSubstitution.Status.SERVED);
        sub.setUpdatedAt(LocalDateTime.now());
        subRepo.save(sub);

        saveMealCareRecord(sub, teacher, "替代餐已分餐");
        return sub;
    }

    /** 厨房取消（仅未出餐前） */
    @Transactional
    public MealSubstitution cancel(User kitchen, Long id, String note) {
        MealSubstitution sub = get(id);
        if (sub.getStatus() == MealSubstitution.Status.EXECUTED
                || sub.getStatus() == MealSubstitution.Status.SERVED) {
            throw new BizException("已出餐的替换单不可取消");
        }
        if (sub.getStatus() == MealSubstitution.Status.CANCELLED
                || sub.getStatus() == MealSubstitution.Status.REJECTED) {
            throw new BizException("替换单已终结");
        }
        sub.setStatus(MealSubstitution.Status.CANCELLED);
        if (note != null && !note.isBlank()) {
            sub.setTriggerDetail(sub.getTriggerDetail() + "（取消原因：" + note + "）");
        }
        sub.setUpdatedAt(LocalDateTime.now());
        return subRepo.save(sub);
    }

    private void requireStatus(MealSubstitution sub, MealSubstitution.Status expected, String who) {
        if (sub.getStatus() != expected) {
            throw new BizException("当前状态不允许该操作（" + who + "）："
                    + statusLabel(sub.getStatus()));
        }
    }

    /** 出餐/分餐同步写入当日饮食记录（照护记录-用餐），供在园记录与时间轴核对 */
    private void saveMealCareRecord(MealSubstitution sub, User operator, String action) {
        CareRecord r = new CareRecord();
        r.setChild(sub.getChild());
        r.setRecordDate(sub.getMealDate());
        r.setType(CareRecord.CareType.MEAL);
        r.setSeverity(CareRecord.Severity.NORMAL);
        r.setRecordedBy(operator);
        String parentTime = sub.getParentConfirmedAt() == null ? "—" : sub.getParentConfirmedAt().format(HM);
        r.setDetail(String.format("%s：「%s」（原「%s」，%s）。家长确认时间 %s；保健老师 %s、班级老师 %s 已审核。",
                action, sub.getSubstituteDish(), sub.getOriginalDish(),
                sub.getReason() == MealSubstitution.Reason.INGREDIENT_SHORTAGE ? "食材缺货" : "过敏源风险",
                parentTime,
                sub.getHealthConfirmedBy() == null ? "—" : sub.getHealthConfirmedBy().getName(),
                sub.getTeacherConfirmedBy() == null ? "—" : sub.getTeacherConfirmedBy().getName()));
        careRecordRepo.save(r);
    }

    // ---------- 查询 ----------

    /** 按角色返回替换单列表 */
    public List<MealSubstitution> listFor(User user, LocalDate date) {
        return switch (user.getRole()) {
            case KITCHEN, DIRECTOR -> date != null
                    ? subRepo.findByMealDateOrderByCreatedAtDesc(date)
                    : subRepo.findAll().stream()
                            .sorted(Comparator.comparing(MealSubstitution::getCreatedAt).reversed())
                            .limit(100).toList();
            case HEALTH -> {
                List<MealSubstitution> all = new ArrayList<>(subRepo.findByStatusOrderByCreatedAtDesc(
                        MealSubstitution.Status.PENDING_HEALTH));
                Set<Long> ids = all.stream().map(MealSubstitution::getId).collect(Collectors.toSet());
                subRepo.findAll().stream()
                        .filter(s -> !ids.contains(s.getId()))
                        .sorted(Comparator.comparing(MealSubstitution::getCreatedAt).reversed())
                        .limit(50)
                        .forEach(all::add);
                yield all;
            }
            case TEACHER -> (date != null ? subRepo.findByMealDateOrderByCreatedAtDesc(date)
                    : subRepo.findAll().stream()
                            .sorted(Comparator.comparing(MealSubstitution::getCreatedAt).reversed())
                            .limit(100).toList())
                    .stream()
                    .filter(s -> accessService.isTeacherOf(user, s.getChild()))
                    .toList();
            case PARENT -> subRepo.findByChildParentIdOrderByCreatedAtDesc(user.getId());
            default -> List.of();
        };
    }

    /**
     * 老师分餐页：本班在托儿童 × 当日菜单 × 替换单状态。
     * 家长未确认（PENDING_PARENT）或已拒绝（REJECTED）的儿童标记 manualCare=true，
     * 提示人工照护、禁止发放常规餐，避免误食。
     */
    public Map<String, Object> servingBoard(User teacher, LocalDate date) {
        List<Child> children = childRepo.findAll().stream()
                .filter(c -> c.getStatus() == Child.Status.ENROLLED)
                .filter(c -> accessService.isTeacherOf(teacher, c))
                .sorted(Comparator.comparing(Child::getId))
                .toList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Child c : children) {
            List<MealSubstitution> subs = subRepo.findByChildIdAndMealDate(c.getId(), date).stream()
                    .filter(s -> s.getStatus() != MealSubstitution.Status.CANCELLED)
                    .toList();
            boolean manualCare = subs.stream().anyMatch(s ->
                    s.getStatus() == MealSubstitution.Status.PENDING_PARENT
                            || s.getStatus() == MealSubstitution.Status.REJECTED);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("child", c);
            row.put("allergens", extractAllergens(c));
            row.put("substitutions", subs);
            row.put("manualCare", manualCare);
            rows.add(row);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", date.toString());
        m.put("menus", listMenus(date));
        m.put("children", rows);
        return m;
    }

    public static String statusLabel(MealSubstitution.Status s) {
        return switch (s) {
            case PENDING_HEALTH -> "待保健老师确认";
            case PENDING_TEACHER -> "待班级老师确认";
            case PENDING_PARENT -> "待家长确认";
            case CONFIRMED -> "待厨房出餐";
            case EXECUTED -> "已出餐待分餐";
            case SERVED -> "已分餐";
            case REJECTED -> "已拒绝";
            case CANCELLED -> "已取消";
        };
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
