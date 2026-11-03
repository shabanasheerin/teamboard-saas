package com.teamboard;

import com.teamboard.dto.Dtos.TaskRequest;
import com.teamboard.entity.*;
import com.teamboard.exception.ApiException;
import com.teamboard.realtime.RealtimePublisher;
import com.teamboard.repository.ProjectRepository;
import com.teamboard.repository.TaskRepository;
import com.teamboard.repository.UserRepository;
import com.teamboard.security.AuthUser;
import com.teamboard.service.AuditService;
import com.teamboard.service.NotificationService;
import com.teamboard.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    TaskRepository tasks;
    @Mock
    ProjectRepository projects;
    @Mock
    UserRepository users;
    @Mock
    AuditService audit;
    @Mock
    NotificationService notifications;
    @Mock
    RealtimePublisher realtime;

    @InjectMocks
    TaskService service;

    private AuthUser member(Long id, Long orgId) {
        return new AuthUser(id, orgId, "u" + id + "@x.com", "User " + id, Role.MEMBER, true, false);
    }

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setName("User " + id);
        return u;
    }

    @Test
    void taskFromAnotherOrganizationIsNotFound() {
        AuthUser fromOtherCompany = member(5L, 2L);
        // the repository is always asked with the caller's own organization id
        when(tasks.findByIdAndOrganizationId(100L, 2L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.get(fromOtherCompany, 100L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void memberCannotMoveSomeoneElsesTask() {
        Task task = new Task();
        task.setId(100L);
        task.setReporter(user(1L));
        task.setAssignee(user(2L));
        when(tasks.findByIdAndOrganizationId(100L, 1L)).thenReturn(Optional.of(task));

        AuthUser stranger = member(3L, 1L);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateStatus(stranger, 100L, TaskStatus.DONE));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void memberCannotDeleteTasks() {
        AuthUser member = member(3L, 1L);
        ApiException ex = assertThrows(ApiException.class, () -> service.delete(member, 100L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void memberCannotEditTaskTheyDidNotCreate() {
        Task task = new Task();
        task.setId(100L);
        task.setReporter(user(1L));
        when(tasks.findByIdAndOrganizationId(100L, 1L)).thenReturn(Optional.of(task));

        AuthUser member = member(3L, 1L);
        TaskRequest request = new TaskRequest("New title", null, Priority.LOW, null, null);
        ApiException ex = assertThrows(ApiException.class, () -> service.update(member, 100L, request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }
}
