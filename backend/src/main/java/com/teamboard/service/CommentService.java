package com.teamboard.service;

import com.teamboard.dto.Dtos.CommentRequest;
import com.teamboard.dto.Dtos.CommentResponse;
import com.teamboard.entity.Comment;
import com.teamboard.entity.Task;
import com.teamboard.entity.User;
import com.teamboard.exception.ApiException;
import com.teamboard.repository.CommentRepository;
import com.teamboard.repository.TaskRepository;
import com.teamboard.repository.UserRepository;
import com.teamboard.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository comments;
    private final TaskRepository tasks;
    private final UserRepository users;
    private final NotificationService notifications;
    private final AuditService audit;

    public CommentService(CommentRepository comments, TaskRepository tasks, UserRepository users,
                          NotificationService notifications, AuditService audit) {
        this.comments = comments;
        this.tasks = tasks;
        this.users = users;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(AuthUser me, Long taskId) {
        Task task = findTask(me, taskId);
        return comments.findByTaskIdOrderByCreatedAtAsc(task.getId()).stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse add(AuthUser me, Long taskId, CommentRequest request) {
        Task task = findTask(me, taskId);
        User author = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("User"));

        Comment c = new Comment();
        c.setTask(task);
        c.setAuthor(author);
        c.setText(request.text().trim());
        c = comments.save(c);

        audit.log(me, "TASK", task.getId(), "COMMENT_ADDED", "Comment on " + task.displayKey());
        notifications.notifyParticipants(me, task, me.name() + " commented on " + task.displayKey());
        return CommentResponse.from(c);
    }

    private Task findTask(AuthUser me, Long taskId) {
        return tasks.findByIdAndOrganizationId(taskId, me.orgId()).orElseThrow(() -> ApiException.notFound("Task"));
    }
}
