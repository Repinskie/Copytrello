package com.repinsky.task_tracker_backend.services;

import com.repinsky.task_tracker_backend.converters.TaskConverter;
import com.repinsky.task_tracker_backend.dto.TaskDto;
import com.repinsky.task_tracker_backend.exceptions.BadRequestException;
import com.repinsky.task_tracker_backend.exceptions.TaskNotFoundException;
import com.repinsky.task_tracker_backend.exceptions.UserNotFoundException;
import com.repinsky.task_tracker_backend.models.Task;
import com.repinsky.task_tracker_backend.models.User;
import com.repinsky.task_tracker_backend.repositories.TaskRepository;
import com.repinsky.task_tracker_backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskConverter taskConverter;
    private final UserRepository userRepository;
    private final ApplicationContext applicationContext;

    public String createNewTask(String title, String description, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UserNotFoundException("User with email '" + ownerEmail + "' not found"));

        if (taskRepository.findTaskByTitleAndUserEmail(title, ownerEmail).isPresent()) {
            throw new BadRequestException("Task with title '" + title + "' already exists");
        }

        Task newTask = Task.builder()
                .title(title)
                .description(description)
                .owner(owner)
                .status("in progress")
                .build();

        TaskService proxy = applicationContext.getBean(TaskService.class);
        proxy.saveTaskToDb(newTask);

        return "Task created successfully";
    }

    public List<TaskDto> getAllUserTasks(String userEmail) {
        return taskRepository.findAllByUserEmail(userEmail).orElseThrow(
                        () -> new TaskNotFoundException("Tasks for user '" + userEmail + "' not found")
                )
                .stream()
                .map(taskConverter::entityToDto)
                .toList();
    }

    public String deleteTaskByTitle(String taskTitle, String userEmail) {
        Task task = taskRepository.findTaskByTitleAndUserEmail(taskTitle, userEmail).orElseThrow(
                () -> new TaskNotFoundException("Task with title '" + taskTitle + "' not found")
        );

        TaskService proxy = applicationContext.getBean(TaskService.class);
        proxy.deleteTaskFromDb(task);

        return "Task deleted successfully";
    }

    public String deleteAllTasks(String currentUserEmail) {
        List<Task> tasks = taskRepository.findAllByUserEmail(currentUserEmail).orElseThrow(
                () -> new TaskNotFoundException("Tasks for user '" + currentUserEmail + "' not found")
        );

        TaskService proxy = applicationContext.getBean(TaskService.class);
        proxy.deleteAllUserTasks(tasks);

        return "All tasks deleted successfully";
    }

    public TaskDto getTaskById(Long id, String currentUserEmail) {
        /* The user has access only to their own tasks.
         * The task with the current ID may exist, but it belongs to another user. */
        Task task = taskRepository.findTaskByUserEmailAndId(currentUserEmail, id).orElseThrow(
                () -> new TaskNotFoundException("Task with id '" + id + "' for user '" + currentUserEmail + "' does not exist")
        );

        return taskConverter.entityToDto(task);
    }

    public List<TaskDto> getTaskByTitle(String title, String currentUserEmail) {
        Task task = taskRepository.findTaskByTitleAndUserEmail(title, currentUserEmail).orElseThrow(
                () -> new TaskNotFoundException("Task with title '" + title + "' does not exist")
        );

        return List.of(taskConverter.entityToDto(task));
    }

    public String deleteTaskById(Long id, String currentUserEmail) {
        Task task = taskRepository.findTaskByUserEmailAndId(currentUserEmail, id).orElseThrow(
                () -> new TaskNotFoundException("Task with id '" + id + "' for user '" + currentUserEmail + "' does not exist")
        );

        TaskService proxy = applicationContext.getBean(TaskService.class);
        proxy.deleteTaskFromDb(task);

        return "Task deleted successfully";
    }

    public List<TaskDto> getTasksWithStatus(String status, String currentUserEmail) {
        List<Task> tasks = taskRepository.findTaskByStatusAndUserEmail(status, currentUserEmail).orElseThrow(
                () -> new TaskNotFoundException("You do not have tasks with status '" + status + "'")
        );

        return tasks.stream()
                .map(taskConverter::entityToDto)
                .collect(Collectors.toList());
    }

    public String patchTaskByTitle(String title, String currentUserEmail, Map<String, Object> updates) {
        Task task = taskRepository.findTaskByTitleAndUserEmail(title, currentUserEmail).orElseThrow(
                () -> new TaskNotFoundException("Task with title '" + title + "' not found")
        );

        return applyUpdatesToTask(currentUserEmail, updates, task);
    }

    public String updateTaskById(Long id, String currentUserEmail, Map<String, Object> updates) {
        Task task = taskRepository.findTaskByUserEmailAndId(currentUserEmail, id).orElseThrow(
                () -> new TaskNotFoundException("Task with id '" + id + "' for user '" + currentUserEmail + "' does not exist")
        );

        return applyUpdatesToTask(currentUserEmail, updates, task);
    }

    private String applyUpdatesToTask(String userEmail, Map<String, Object> updates, Task task) {
        updates.forEach((key, value) -> {
            switch (key) {
                case "title":
                    List<Task> tasks = taskRepository.findAllTasksByEmailAndTitle(userEmail, (String) value);

                    if (!tasks.isEmpty() && !tasks.contains(task)) {
                        throw new BadRequestException("Task with title '" + value + "' already exists");
                    }
                    task.setTitle((String) value);
                    break;

                case "description":
                    task.setDescription((String) value);
                    break;

                case "status":
                    task.setStatus((String) value);
                    break;

                default:
                    throw new BadRequestException("Invalid field: " + key);
            }
        });

        if ("completed".equals(task.getStatus())) {
            Timestamp completed = new Timestamp(System.currentTimeMillis());
            task.setCompletedAt(completed);
        }

        TaskService proxy = applicationContext.getBean(TaskService.class);
        proxy.saveTaskToDb(task);

        return "Task updated successfully";
    }

    public String updateTaskByTitle(String title, String currentUserEmail, String newTitle, String newDescription, String newStatus) {
        Task task = taskRepository.findTaskByTitleAndUserEmail(title, currentUserEmail).orElseThrow(
                () -> new TaskNotFoundException("Task with title '" + title + "' does not exist")
        );

        return updateTaskWithNewValues(currentUserEmail, newTitle, newDescription, newStatus, task);
    }

    public String patchTaskById(Long id, String currentUserEmail, String newTitle, String newDescription, String newStatus) {
        Task task = taskRepository.findTaskByUserEmailAndId(currentUserEmail, id).orElseThrow(
                () -> new TaskNotFoundException("Task with id'" + id + "' for user '" + currentUserEmail + "' does not exist")
        );

        return updateTaskWithNewValues(currentUserEmail, newTitle, newDescription, newStatus, task);
    }

    private String updateTaskWithNewValues(String userEmail, String newTitle, String newDescription, String newStatus, Task task) {
        if (newTitle == null || newStatus == null) {
            throw new TaskNotFoundException("Title and status for new task cannot be be empty");
        }

        List<Task> tasks = taskRepository.findAllTasksByEmailAndTitle(userEmail, newTitle);

        if (!tasks.isEmpty() && !tasks.contains(task)) {
            throw new BadRequestException("Task with title '" + newTitle + "' already exists");
        }
        task.setTitle(newTitle);
        task.setDescription(newDescription);
        task.setStatus(newStatus);
        if (task.getStatus().equals("completed")) {
            Timestamp completed = new Timestamp(System.currentTimeMillis());
            task.setCompletedAt(completed);
        }

        TaskService proxy = applicationContext.getBean(TaskService.class);
        proxy.saveTaskToDb(task);

        return "Task updated successfully";
    }

    @Transactional
    protected void deleteTaskFromDb(Task task) {
        taskRepository.delete(task);
    }

    @Transactional
    protected void deleteAllUserTasks(List<Task> tasks) {
        taskRepository.deleteAll(tasks);
    }

    @Transactional
    protected void saveTaskToDb(Task task) {
        taskRepository.save(task);
    }
}
