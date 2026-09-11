package com.nursery.controller;

import com.nursery.entity.Classroom;
import com.nursery.entity.User;
import com.nursery.repository.UserRepository;
import com.nursery.service.ClassService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClassroomController {

    private final ClassService classService;
    private final UserRepository userRepo;

    public ClassroomController(ClassService classService, UserRepository userRepo) {
        this.classService = classService;
        this.userRepo = userRepo;
    }

    /** 班级列表（含容量/在托/师幼比），园内角色均可查看 */
    @GetMapping("/classrooms")
    @PreAuthorize("hasAnyRole('DIRECTOR','HEALTH','TEACHER','FRONTDESK')")
    public List<Map<String, Object>> list() {
        return classService.listWithStats();
    }

    public record ClassroomReq(@NotBlank(message = "请填写班级名称") String name,
                               @NotNull(message = "请填写容量") Integer capacity,
                               String ageRange,
                               String description) {}

    @PostMapping("/classrooms")
    @PreAuthorize("hasRole('DIRECTOR')")
    public Classroom create(@Valid @RequestBody ClassroomReq req) {
        return classService.create(req.name(), req.capacity(), req.ageRange(), req.description());
    }

    public record AssignTeacherReq(@NotNull(message = "请选择老师") Long teacherId) {}

    @PostMapping("/classrooms/{id}/teachers")
    @PreAuthorize("hasRole('DIRECTOR')")
    public Classroom assignTeacher(@PathVariable Long id, @Valid @RequestBody AssignTeacherReq req) {
        return classService.assignTeacher(id, req.teacherId());
    }

    @DeleteMapping("/classrooms/{id}/teachers/{teacherId}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public Classroom removeTeacher(@PathVariable Long id, @PathVariable Long teacherId) {
        return classService.removeTeacher(id, teacherId);
    }

    /** 用户列表（园长用于分配老师等） */
    @GetMapping("/users")
    @PreAuthorize("hasRole('DIRECTOR')")
    public List<User> users(@RequestParam(required = false) User.Role role) {
        return role == null ? userRepo.findAll() : userRepo.findByRole(role);
    }
}
