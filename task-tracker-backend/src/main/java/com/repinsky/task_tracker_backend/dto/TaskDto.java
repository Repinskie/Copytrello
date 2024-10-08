package com.repinsky.task_tracker_backend.dto;

import java.sql.Timestamp;

public record TaskDto(
        String title,
        String description,
        String ownerEmail,
        String status,
        Timestamp completedAt
) {
}
