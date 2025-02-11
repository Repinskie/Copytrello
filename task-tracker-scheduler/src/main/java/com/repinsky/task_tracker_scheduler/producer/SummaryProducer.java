package com.repinsky.task_tracker_scheduler.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repinsky.task_tracker_scheduler.utils.FormatTaskUtil;
import com.repinsky.task_tracker_scheduler.dto.StatisticDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryProducer {
    private static final String TOPIC = "EMAIL_SENDING_TASKS";

    private static final String JSON_CONVERSION_ERROR = "Error converting message to JSON";
    private static final String UNFINISHED_TASKS_MESSAGE = """
            Dear %s,

            You have %d unfinished tasks:
            %s

            Best regards,
            Copytrello Support Team
            """;
    private static final String FINISHED_TASKS_MESSAGE = """
            Dear %s,

            Today you have %d completed tasks:
            %s

            Best regards,
            Copytrello Support Team
            """;
    private static final String ALL_TASKS_MESSAGE = """
            Dear %s,

            Today you have %d unfinished tasks and %d completed tasks:
            Unfinished tasks: %s
            Completed tasks: %s

            Best regards,
            Copytrello Support Team
            """;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private void send(Message message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(TOPIC, jsonMessage);
        } catch (JsonProcessingException e) {
            log.error(JSON_CONVERSION_ERROR, e);
        }
    }

    public void sendUnfinishedTasks(StatisticDto dto) {
        log.info("Preparing to send unfinished tasks email to '{}'. Number of tasks: {}", dto.getEmail(), dto.getCountUnFinishedTasks());

        String formatted = String.format(UNFINISHED_TASKS_MESSAGE, dto.getEmail(),
                dto.getCountUnFinishedTasks(), FormatTaskUtil.formatTasks(dto.getUnFinishedTasks()));
        send(new Message(dto.getEmail(), "In progress tasks", formatted));

        log.info("Unfinished tasks email sent successfully to '{}'.", dto.getEmail());
    }

    public void sendCompletedTasks(StatisticDto dto) {
        log.info("Preparing to send completed tasks email to '{}'. Number of tasks: {}", dto.getEmail(), dto.getCountCompletedTasks());

        String formatted = String.format(FINISHED_TASKS_MESSAGE, dto.getEmail(),
                dto.getCountCompletedTasks(), FormatTaskUtil.formatTasks(dto.getCompletedTasks()));
        send(new Message(dto.getEmail(), "Completed tasks", formatted));

        log.info("Completed tasks email sent successfully to '{}'.", dto.getEmail());
    }

    public void sendAllTasks(StatisticDto dto) {
        log.info("Preparing to send unfinished and completed tasks email to '{}'. Number of unfinished tasks: '{}'. Number of completed tasks: '{}'", dto.getEmail(), dto.getCountUnFinishedTasks(), dto.getCountUnFinishedTasks());

        String allTaskMessageFormatted = String.format(ALL_TASKS_MESSAGE, dto.getEmail(), dto.getCountUnFinishedTasks(),
                dto.getCountCompletedTasks(),
                FormatTaskUtil.formatTasks(dto.getUnFinishedTasks()), FormatTaskUtil.formatTasks(dto.getCompletedTasks()));
        send(new Message(dto.getEmail(), "All tasks statistic", allTaskMessageFormatted));

        log.info("Unfinished and completed tasks email sent successfully to '{}'.", dto.getEmail());
    }

    @Data
    @AllArgsConstructor
    private static class Message {
        private String recipient;
        private String subject;
        private String text;
    }
}
