package com.nursery.controller;

import com.nursery.config.AuthUser;
import com.nursery.entity.DailyMenu;
import com.nursery.entity.MealSubstitution;
import com.nursery.service.MealService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.nursery.controller.ChildController.toUser;

/**
 * 过敏餐临时替换：菜单发布、受影响儿童比对、替换单确认链与出餐/分餐。
 */
@RestController
@RequestMapping("/api/meals")
public class MealController {

    private final MealService mealService;

    public MealController(MealService mealService) {
        this.mealService = mealService;
    }

    // ---------- 每日菜单 ----------

    public record MenuReq(@NotNull(message = "请选择日期") LocalDate date,
                          @NotNull(message = "请选择餐次") DailyMenu.MealType mealType,
                          @NotBlank(message = "请填写菜品") String dishes,
                          String ingredients,
                          String nutritionNotes) {}

    @PostMapping("/menus")
    @PreAuthorize("hasAnyRole('KITCHEN','DIRECTOR')")
    public DailyMenu upsertMenu(@AuthenticationPrincipal AuthUser user,
                                @Valid @RequestBody MenuReq req) {
        return mealService.upsertMenu(toUser(user), req.date(), req.mealType(),
                req.dishes(), req.ingredients(), req.nutritionNotes());
    }

    @GetMapping("/menus")
    public List<DailyMenu> listMenus(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return mealService.listMenus(date);
    }

    /** 平台比对：菜单食材 × 在托儿童过敏档案 → 受影响儿童 */
    @GetMapping("/menus/{id}/affected-children")
    @PreAuthorize("hasAnyRole('KITCHEN','HEALTH','DIRECTOR')")
    public List<Map<String, Object>> affectedChildren(@PathVariable Long id) {
        return mealService.affectedChildren(id);
    }

    // ---------- 替换单 ----------

    public record SubstitutionReq(@NotNull(message = "缺少菜单") Long menuId,
                                  @NotNull(message = "缺少儿童") Long childId,
                                  @NotNull(message = "请选择触发原因") MealSubstitution.Reason reason,
                                  String triggerDetail,
                                  @NotBlank(message = "请填写原菜品") String originalDish,
                                  @NotBlank(message = "请填写替代菜品") String substituteDish,
                                  String substituteIngredients) {}

    @PostMapping("/substitutions")
    @PreAuthorize("hasAnyRole('KITCHEN','DIRECTOR')")
    public MealSubstitution create(@AuthenticationPrincipal AuthUser user,
                                   @Valid @RequestBody SubstitutionReq req) {
        return mealService.createSubstitution(toUser(user), req.menuId(), req.childId(), req.reason(),
                req.triggerDetail(), req.originalDish(), req.substituteDish(), req.substituteIngredients());
    }

    /** 按角色返回替换单：厨房/园长按日期；保健待确认优先；老师本班；家长名下儿童 */
    @GetMapping("/substitutions")
    public List<MealSubstitution> list(@AuthenticationPrincipal AuthUser user,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return mealService.listFor(toUser(user), date);
    }

    /** 老师分餐页：本班儿童 × 当日菜单 × 替换状态 × 人工照护提示 */
    @GetMapping("/serving-board")
    @PreAuthorize("hasRole('TEACHER')")
    public Map<String, Object> servingBoard(@AuthenticationPrincipal AuthUser user,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return mealService.servingBoard(toUser(user), date == null ? LocalDate.now() : date);
    }

    public record ConfirmReq(Boolean approve, String note) {}

    @PostMapping("/substitutions/{id}/health-confirm")
    @PreAuthorize("hasRole('HEALTH')")
    public MealSubstitution healthConfirm(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                          @RequestBody ConfirmReq req) {
        return mealService.healthConfirm(toUser(user), id, req.approve() == null || req.approve(), req.note());
    }

    @PostMapping("/substitutions/{id}/teacher-confirm")
    @PreAuthorize("hasRole('TEACHER')")
    public MealSubstitution teacherConfirm(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                           @RequestBody ConfirmReq req) {
        return mealService.teacherConfirm(toUser(user), id, req.approve() == null || req.approve(), req.note());
    }

    @PostMapping("/substitutions/{id}/parent-confirm")
    @PreAuthorize("hasRole('PARENT')")
    public MealSubstitution parentConfirm(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                          @RequestBody ConfirmReq req) {
        return mealService.parentConfirm(toUser(user), id, req.approve() == null || req.approve(), req.note());
    }

    @PostMapping("/substitutions/{id}/execute")
    @PreAuthorize("hasRole('KITCHEN')")
    public MealSubstitution execute(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return mealService.execute(toUser(user), id);
    }

    @PostMapping("/substitutions/{id}/serve")
    @PreAuthorize("hasRole('TEACHER')")
    public MealSubstitution serve(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return mealService.serve(toUser(user), id);
    }

    public record CancelReq(String note) {}

    @PostMapping("/substitutions/{id}/cancel")
    @PreAuthorize("hasAnyRole('KITCHEN','DIRECTOR')")
    public MealSubstitution cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                   @RequestBody(required = false) CancelReq req) {
        return mealService.cancel(toUser(user), id, req == null ? null : req.note());
    }
}
