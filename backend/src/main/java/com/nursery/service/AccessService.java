package com.nursery.service;

import com.nursery.config.BizException;
import com.nursery.entity.Child;
import com.nursery.entity.Classroom;
import com.nursery.entity.User;
import org.springframework.stereotype.Service;

/**
 * 数据访问权限规则：
 * - 园长 / 保健老师 / 前台：可查看全部儿童
 * - 老师：仅本班儿童
 * - 家长：仅自己的孩子
 */
@Service
public class AccessService {

    public boolean canViewChild(User user, Child child) {
        return switch (user.getRole()) {
            case DIRECTOR, HEALTH, FRONTDESK -> true;
            case PARENT -> child.getParent().getId().equals(user.getId());
            case TEACHER -> isTeacherOf(user, child);
        };
    }

    public void checkViewChild(User user, Child child) {
        if (!canViewChild(user, child)) {
            throw BizException.forbidden("无权查看该儿童信息");
        }
    }

    public boolean isTeacherOf(User user, Child child) {
        Classroom classroom = child.getClassroom();
        if (classroom == null) return false;
        return classroom.getTeachers().stream().anyMatch(t -> t.getId().equals(user.getId()));
    }

    /** 老师对本班儿童的操作权限（晨检、照护记录） */
    public void checkTeacherOperate(User user, Child child) {
        if (user.getRole() == User.Role.DIRECTOR || user.getRole() == User.Role.HEALTH) return;
        if (user.getRole() != User.Role.TEACHER || !isTeacherOf(user, child)) {
            throw BizException.forbidden("仅本班老师可执行该操作");
        }
    }
}
