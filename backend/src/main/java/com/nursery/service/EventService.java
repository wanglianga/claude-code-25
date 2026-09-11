package com.nursery.service;

import com.nursery.config.BizException;
import com.nursery.entity.*;
import com.nursery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 协同事件：把老师、保健老师、园长、家长拉入同一事件处理，
 * 支持事件内沟通、状态流转与处理结论（风险复盘）。
 */
@Service
public class EventService {

    private final NurseryEventRepository eventRepo;
    private final EventParticipantRepository participantRepo;
    private final EventMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final AlertService alertService;

    public EventService(NurseryEventRepository eventRepo,
                        EventParticipantRepository participantRepo,
                        EventMessageRepository messageRepo,
                        UserRepository userRepo,
                        AlertService alertService) {
        this.eventRepo = eventRepo;
        this.participantRepo = participantRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.alertService = alertService;
    }

    /**
     * 创建事件并自动拉入相关方：儿童家长、本班老师、全体保健老师、全体园长、创建人。
     */
    @Transactional
    public NurseryEvent createEvent(NurseryEvent.EventType type, Child child, String title,
                                    String description, User creator) {
        NurseryEvent event = new NurseryEvent();
        event.setType(type);
        event.setChild(child);
        event.setClassroom(child.getClassroom());
        event.setTitle(title);
        event.setDescription(description);
        event.setCreatedBy(creator);
        eventRepo.save(event);

        // 按用户 id 去重（创建人可能同时是本班老师）
        Map<Long, User> participants = new LinkedHashMap<>();
        participants.put(child.getParent().getId(), child.getParent());
        if (child.getClassroom() != null) {
            for (User t : child.getClassroom().getTeachers()) {
                participants.putIfAbsent(t.getId(), t);
            }
        }
        for (User u : userRepo.findByRole(User.Role.HEALTH)) {
            participants.putIfAbsent(u.getId(), u);
        }
        for (User u : userRepo.findByRole(User.Role.DIRECTOR)) {
            participants.putIfAbsent(u.getId(), u);
        }
        if (creator != null) {
            participants.putIfAbsent(creator.getId(), creator);
        }
        for (User u : participants.values()) {
            participantRepo.save(new EventParticipant(event, u));
        }
        saveSystemMessage(event, creator, "事件已创建，已通知家长、班级老师、保健老师与园长共同处理。");
        alertService.checkChild(child);
        return event;
    }

    public void saveSystemMessage(NurseryEvent event, User sender, String content) {
        EventMessage msg = new EventMessage();
        msg.setEvent(event);
        msg.setSender(sender != null ? sender : event.getCreatedBy());
        msg.setContent(content);
        messageRepo.save(msg);
    }

    public List<NurseryEvent> listFor(User user, NurseryEvent.Status status, NurseryEvent.EventType type) {
        if (user.getRole() == User.Role.PARENT) {
            List<Long> eventIds = participantRepo.findByUserId(user.getId()).stream()
                    .map(p -> p.getEvent().getId()).toList();
            if (eventIds.isEmpty()) return List.of();
            return eventRepo.findAllById(eventIds).stream()
                    .filter(e -> status == null || e.getStatus() == status)
                    .filter(e -> type == null || e.getType() == type)
                    .sorted(Comparator.comparing(NurseryEvent::getCreatedAt).reversed())
                    .collect(Collectors.toList());
        }
        List<NurseryEvent> all;
        if (status != null && type != null) all = eventRepo.findByStatusAndTypeOrderByCreatedAtDesc(status, type);
        else if (status != null) all = eventRepo.findByStatusOrderByCreatedAtDesc(status);
        else if (type != null) all = eventRepo.findByTypeOrderByCreatedAtDesc(type);
        else all = eventRepo.findAllByOrderByCreatedAtDesc();
        return all;
    }

    public NurseryEvent get(Long id) {
        return eventRepo.findById(id).orElseThrow(() -> BizException.notFound("事件不存在"));
    }

    public void checkAccess(User user, NurseryEvent event) {
        if (user.getRole() != User.Role.PARENT) return; // 园内角色均可查看处理
        if (!participantRepo.existsByEventIdAndUserId(event.getId(), user.getId())) {
            throw BizException.forbidden("您未参与该事件");
        }
    }

    @Transactional
    public EventMessage addMessage(User user, Long eventId, String content) {
        NurseryEvent event = get(eventId);
        checkAccess(user, event);
        if (event.getStatus() == NurseryEvent.Status.CLOSED) {
            throw new BizException("事件已关闭，无法留言");
        }
        EventMessage msg = new EventMessage();
        msg.setEvent(event);
        msg.setSender(user);
        msg.setContent(content);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepo.save(event);
        return messageRepo.save(msg);
    }

    @Transactional
    public NurseryEvent updateStatus(User user, Long eventId, NurseryEvent.Status target, String note) {
        NurseryEvent event = get(eventId);
        NurseryEvent.Status current = event.getStatus();
        boolean forward = (current == NurseryEvent.Status.OPEN && target == NurseryEvent.Status.PROCESSING)
                || (current == NurseryEvent.Status.PROCESSING && target == NurseryEvent.Status.RESOLVED)
                || (target == NurseryEvent.Status.CLOSED && current != NurseryEvent.Status.CLOSED);
        if (!forward) {
            throw new BizException("事件状态只能按 待处理→处理中→已解决→已关闭 流转");
        }
        if (target == NurseryEvent.Status.RESOLVED && (note == null || note.isBlank())) {
            throw new BizException("标记解决时必须填写处理结论");
        }
        event.setStatus(target);
        event.setUpdatedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            event.setResolutionNote((event.getResolutionNote() == null ? "" : event.getResolutionNote() + "\n")
                    + "[" + user.getName() + "] " + note);
        }
        if (target == NurseryEvent.Status.CLOSED) {
            event.setClosedAt(LocalDateTime.now());
        }
        eventRepo.save(event);
        String label = switch (target) {
            case PROCESSING -> "开始处理";
            case RESOLVED -> "标记解决";
            case CLOSED -> "关闭事件";
            default -> "更新状态";
        };
        saveSystemMessage(event, user, user.getName() + " 将事件标记为「" + label + "」"
                + (note != null && !note.isBlank() ? "：" + note : ""));
        return event;
    }

    public List<EventParticipant> participants(Long eventId) {
        return participantRepo.findByEventIdOrderByJoinedAtAsc(eventId);
    }

    public List<EventMessage> messages(Long eventId) {
        return messageRepo.findByEventIdOrderByCreatedAtAsc(eventId);
    }
}
