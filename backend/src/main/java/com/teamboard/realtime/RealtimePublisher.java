package com.teamboard.realtime;

import com.teamboard.dto.Dtos.NotificationPush;
import com.teamboard.dto.Dtos.TaskEvent;
import com.teamboard.util.AfterCommit;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Sends live updates to browsers, but only after the database change is committed. */
@Component
public class RealtimePublisher {

    private final SimpMessagingTemplate template;

    public RealtimePublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    /** Everyone in the organization gets this, so open boards refresh themselves. */
    public void taskChanged(Long orgId, String type, Long taskId, Long projectId) {
        AfterCommit.run(() ->
                template.convertAndSend("/topic/org/" + orgId + "/tasks", new TaskEvent(type, taskId, projectId)));
    }

    /** Only this one person gets this. */
    public void toUser(Long userId, NotificationPush payload) {
        AfterCommit.run(() ->
                template.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", payload));
    }
}
