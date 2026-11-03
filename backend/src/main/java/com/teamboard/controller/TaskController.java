package com.teamboard.controller;

import com.teamboard.dto.Dtos.CommentRequest;
import com.teamboard.dto.Dtos.CommentResponse;
import com.teamboard.dto.Dtos.StatusRequest;
import com.teamboard.dto.Dtos.TaskRequest;
import com.teamboard.dto.Dtos.TaskResponse;
import com.teamboard.security.AuthUser;
import com.teamboard.service.CommentService;
import com.teamboard.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;
    private final CommentService commentService;

    public TaskController(TaskService taskService, CommentService commentService) {
        this.taskService = taskService;
        this.commentService = commentService;
    }

    /** Open tasks assigned to me. */
    @GetMapping("/mine")
    public List<TaskResponse> mine(@AuthenticationPrincipal AuthUser me) {
        return taskService.mine(me);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return taskService.get(me, id);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.update(me, id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                     @Valid @RequestBody StatusRequest request) {
        return taskService.updateStatus(me, id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        taskService.delete(me, id);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return commentService.list(me, id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                      @Valid @RequestBody CommentRequest request) {
        return commentService.add(me, id, request);
    }
}
