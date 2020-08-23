package io.maxilog.service.impl;

import io.maxilog.domain.Task;
import io.maxilog.dto.TaskDTO;
import io.maxilog.mapper.TaskMapper;
import io.maxilog.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    public List<TaskDTO> getMyTasks() {
        return taskRepository.findByCreatedBy("admin")
                .stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    public TaskDTO save(TaskDTO taskDTO) {
        Task task = taskMapper.toEntity(taskDTO);
        //task.setCreatedBy(identity.getPrincipal().getName());
        taskRepository.save(task);
        return taskMapper.toDto(task);
    }
}
