package com.nursery.controller;

import com.nursery.config.AuthUser;
import com.nursery.entity.Child;
import com.nursery.entity.NurseryEvent;
import com.nursery.service.AccessService;
import com.nursery.service.ChildService;
import com.nursery.service.EventService;
import com.nursery.service.TimelineService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.nursery.controller.ChildController.toUser;

@RestController
@RequestMapping("/api/timeline")
public class TimelineController {

    private final TimelineService timelineService;
    private final ChildService childService;
    private final EventService eventService;
    private final AccessService accessService;

    public TimelineController(TimelineService timelineService,
                              ChildService childService,
                              EventService eventService,
                              AccessService accessService) {
        this.timelineService = timelineService;
        this.childService = childService;
        this.eventService = eventService;
        this.accessService = accessService;
    }

    /** 儿童时间轴：家长（自己孩子）/ 园内角色 */
    @GetMapping("/child/{id}")
    public List<TimelineService.TimelineItem> child(@AuthenticationPrincipal AuthUser user,
                                                    @PathVariable Long id) {
        Child child = childService.get(id);
        accessService.checkViewChild(toUser(user), child);
        return timelineService.childTimeline(id);
    }

    /** 班级时间轴（园内角色） */
    @GetMapping("/class/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR','HEALTH','TEACHER','FRONTDESK')")
    public List<TimelineService.TimelineItem> classroom(@PathVariable Long id) {
        return timelineService.classTimeline(id);
    }

    /** 事件时间轴：事件 + 沟通 + 当日环节记录 */
    @GetMapping("/event/{id}")
    public List<TimelineService.TimelineItem> event(@AuthenticationPrincipal AuthUser user,
                                                    @PathVariable Long id) {
        NurseryEvent event = eventService.get(id);
        eventService.checkAccess(toUser(user), event);
        return timelineService.eventTimeline(event);
    }

    /** 家长时间轴（自己所有孩子） */
    @GetMapping("/parent")
    @PreAuthorize("hasRole('PARENT')")
    public List<TimelineService.TimelineItem> parent(@AuthenticationPrincipal AuthUser user) {
        return timelineService.parentTimeline(user.getId());
    }
}
