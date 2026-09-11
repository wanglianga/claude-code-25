package com.nursery.repository;

import com.nursery.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ChildRepository extends JpaRepository<Child, Long> {
    List<Child> findByParentIdOrderByIdAsc(Long parentId);
    List<Child> findByClassroomId(Long classroomId);
    List<Child> findByClassroomIdAndStatusIn(Long classroomId, Collection<Child.Status> statuses);
    long countByClassroomIdAndStatusIn(Long classroomId, Collection<Child.Status> statuses);
    List<Child> findByStatusIn(Collection<Child.Status> statuses);
}
