package com.nursery.controller;

import com.nursery.config.AuthUser;
import com.nursery.entity.Child;
import com.nursery.entity.EventMessage;
import com.nursery.entity.EventParticipant;
import com.nursery.entity.NurseryEvent;
import com.nursery.service.ChildService;
import com.nursery.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.nursery.controller.ChildController.toUser;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final ChildService childService;

    public EventController(EventService eventService, ChildService childService) {
        this.eventService = eventService;
        this.childService = childService;
    }

    public record CreateReq(@NotNull(message = "请选择事件类型") NurseryEvent.EventType type,
                            @NotNull(message = "请选择儿童") Long childId,
                            @NotBlank(message = "请填写标题") String title,
                            String description) {}

    /** 园内角色手动上报事件（如退费争议、其他异常） */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','HEALTH','DIRECTOR','FRONTDESK')")
    public NurseryEvent create(@AuthenticationPrincipal AuthUser user,
                               @Valid @RequestBody CreateReq req) {
        Child child = childService.get(req.childId());
        return eventService.createEvent(req.type(), child, req.title(), req.description(), toUser(user));
    }

    @GetMapping
    public List<NurseryEvent> list(@AuthenticationPrincipal AuthUser user,
                                   @RequestParam(required = false) NurseryEvent.Status status,
                                   @RequestParam(required = false) NurseryEvent.EventType type) {
        return eventService.listFor(toUser(user), status, type);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        NurseryEvent event = eventService.get(id);
        eventService.checkAccess(toUser(user), event);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("event", event);
        List<EventParticipant> participants = eventService.participants(id);
        m.put("participants", participants);
        List<EventMessage> messages = eventService.messages(id);
        m.put("messages", messages);
        m.put("participant", participants.stream().anyMatch(p -> p.getUser().getId().equals(user.getId())));
        return m;
    }

    public record MessageReq(@NotBlank(message = "请输入内容") String content) {}

    @PostMapping("/{id}/messages")
    public EventMessage addMessage(@AuthenticationPrincipal AuthUser user,
                                   @PathVariable Long id,
                                   @Valid @RequestBody MessageReq req) {
        return eventService.addMessage(toUser(user), id, req.content());
    }

    public record StatusReq(@NotNull(message = "缺少目标状态") NurseryEvent.Status status,
                            String note) {}

    /** 事件状态流转（园内角色） */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TEACHER','HEALTH','DIRECTOR','FRONTDESK')")
    public NurseryEvent updateStatus(@AuthenticationPrincipal AuthUser user,
                                     @PathVariable Long id,
                                     @Valid @RequestBody StatusReq req) {
        return eventService.updateStatus(toUser(user), id, req.status(), req.note());
    }
}
