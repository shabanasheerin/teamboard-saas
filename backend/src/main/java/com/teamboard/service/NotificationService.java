package com.teamboard.service;

import com.teamboard.dto.Dtos.NotificationPush;
import com.teamboard.dto.Dtos.NotificationResponse;
import com.teamboard.entity.Notification;
import com.teamboard.entity.Task;
import com.teamboard.entity.User;
import com.teamboard.exception.ApiException;
import com.teamboard.realtime.RealtimePublisher;
import com.teamboard.repository.NotificationRepository;
import com.teamboard.security.AuthUser;
import com.teamboard.util.AfterCommit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final RealtimePublisher realtime;
    private final ObjectProvider<EmailService> emailService;

    public NotificationService(NotificationRepository repository, RealtimePublisher realtime,
                               ObjectProvider<EmailService> emailService) {
        this.repository = repository;
        this.realtime = realtime;
        this.emailService = emailService;
    }

    /** Saves the notification, pushes it live over WebSocket, and emails it when email is enabled. */
    @Transactional
    public void notify(User to, String message, Long taskId, Long projectId) {
        String text = message.length() > 300 ? message.substring(0, 300) : message;
        Notification n = new Notification();
        n.setUser(to);
        n.setMessage(text);
        n.setTaskId(taskId);
        n.setProjectId(projectId);
        n = repository.save(n);

        realtime.toUser(to.getId(),
                new NotificationPush(n.getId(), text, taskId, projectId, n.getCreatedAt().toString()));
        AfterCommit.run(() -> emailService.ifAvailable(mail ->
                mail.send(to.getEmail(), "TeamBoard: " + text, text + "\n\nOpen TeamBoard to see the details.")));
    }

    /** Tells the reporter and the assignee of a task (except the person who did the action). */
    @Transactional
    public void notifyParticipants(AuthUser actor, Task task, String message) {
        Map<Long, User> people = new LinkedHashMap<>();
        people.put(task.getReporter().getId(), task.getReporter());
        if (task.getAssignee() != null) {
            people.put(task.getAssignee().getId(), task.getAssignee());
        }
        people.remove(actor.id());
        for (User person : people.values()) {
            notify(person, message, task.getId(), task.getProject().getId());
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(AuthUser me) {
        return repository.findTop50ByUserIdOrderByCreatedAtDesc(me.id()).stream()
                .map(NotificationResponse::from).toList();
    }

    @Transactional
    public void markSeen(AuthUser me, Long id) {
        Notification n = repository.findById(id).orElseThrow(() -> ApiException.notFound("Notification"));
        if (!n.getUser().getId().equals(me.id())) {
            throw ApiException.notFound("Notification");
        }
        n.setSeen(true);
    }

    @Transactional
    public void markAllSeen(AuthUser me) {
        repository.markAllSeen(me.id());
    }
}
