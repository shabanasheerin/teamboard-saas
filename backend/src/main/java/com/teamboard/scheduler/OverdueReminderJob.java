package com.teamboard.scheduler;

import com.teamboard.entity.Task;
import com.teamboard.entity.TaskStatus;
import com.teamboard.repository.TaskRepository;
import com.teamboard.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Every morning: tells each assignee about their overdue tasks (live, in the app, and by email if enabled). */
@Component
public class OverdueReminderJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueReminderJob.class);

    private final TaskRepository tasks;
    private final NotificationService notifications;

    public OverdueReminderJob(TaskRepository tasks, NotificationService notifications) {
        this.tasks = tasks;
        this.notifications = notifications;
    }

    @Scheduled(cron = "${app.reminders.cron}")
    @Transactional
    public void remindOverdueTasks() {
        List<Task> overdue = tasks.findByDueDateBeforeAndStatusNotAndAssigneeIsNotNull(LocalDate.now(), TaskStatus.DONE);
        for (Task t : overdue) {
            notifications.notify(t.getAssignee(), "Overdue: " + t.displayKey() + " " + t.getTitle(),
                    t.getId(), t.getProject().getId());
        }
        log.info("Overdue reminder job finished, {} task(s) overdue", overdue.size());
    }
}
