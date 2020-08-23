package io.maxilog.web;

import io.maxilog.dto.TaskDTO;
import io.maxilog.service.impl.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TasksResource {

    private final TaskService taskService;

    public TasksResource(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/me")
    public List<TaskDTO> getMyTasks() {
        return taskService.getMyTasks();
    }

    @PostMapping("/")
    public ResponseEntity<TaskDTO> addTask(TaskDTO taskDTO) throws URISyntaxException {
        return ResponseEntity.created(new URI("/tasks/me")).body(taskService.save(taskDTO));
    }

}
