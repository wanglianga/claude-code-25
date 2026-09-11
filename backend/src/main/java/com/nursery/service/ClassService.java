package com.nursery.service;

import com.nursery.config.BizException;
import com.nursery.entity.Child;
import com.nursery.entity.Classroom;
import com.nursery.entity.User;
import com.nursery.repository.ChildRepository;
import com.nursery.repository.ClassroomRepository;
import com.nursery.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClassService {

    private final ClassroomRepository classroomRepo;
    private final ChildRepository childRepo;
    private final UserRepository userRepo;

    public ClassService(ClassroomRepository classroomRepo, ChildRepository childRepo, UserRepository userRepo) {
        this.classroomRepo = classroomRepo;
        this.childRepo = childRepo;
        this.userRepo = userRepo;
    }

    /**
     * 班级列表（含在托人数与师幼比，供园长分班决策）
     */
    public List<Map<String, Object>> listWithStats() {
        return classroomRepo.findAllByOrderByIdAsc().stream().map(c -> {
            long enrolled = childRepo.countByClassroomIdAndStatusIn(c.getId(),
                    List.of(Child.Status.ASSIGNED, Child.Status.ENROLLED));
            int teachers = c.getTeachers().size();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("capacity", c.getCapacity());
            m.put("ageRange", c.getAgeRange());
            m.put("description", c.getDescription());
            m.put("teachers", c.getTeachers());
            m.put("enrolledCount", enrolled);
            m.put("remaining", c.getCapacity() - enrolled);
            m.put("ratio", teachers == 0 ? "未配教师" : "1:" + (enrolled == 0 ? 0 : Math.round((double) enrolled / teachers)));
            return m;
        }).toList();
    }

    @Transactional
    public Classroom create(String name, Integer capacity, String ageRange, String description) {
        Classroom c = new Classroom();
        c.setName(name);
        c.setCapacity(capacity);
        c.setAgeRange(ageRange);
        c.setDescription(description);
        return classroomRepo.save(c);
    }

    @Transactional
    public Classroom assignTeacher(Long classroomId, Long teacherId) {
        Classroom c = classroomRepo.findById(classroomId)
                .orElseThrow(() -> BizException.notFound("班级不存在"));
        User teacher = userRepo.findById(teacherId)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        if (teacher.getRole() != User.Role.TEACHER) {
            throw new BizException("只能分配老师角色的用户");
        }
        c.getTeachers().add(teacher);
        return classroomRepo.save(c);
    }

    @Transactional
    public Classroom removeTeacher(Long classroomId, Long teacherId) {
        Classroom c = classroomRepo.findById(classroomId)
                .orElseThrow(() -> BizException.notFound("班级不存在"));
        c.getTeachers().removeIf(t -> t.getId().equals(teacherId));
        return classroomRepo.save(c);
    }
}
