package edu.northeastern.cs5500.starterbot.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;

/**
 * Represents the reminder class
 *
 * @author Team Wolf
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Reminder implements Model {
    @Builder.Default private ObjectId id = new ObjectId();
    private LocalDateTime reminderDateTime;
    @NonNull private String message;
    @NonNull private Integer reminderTimeInMin;
    @NonNull private ObjectId meetingId;
    @NonNull private Student student;

    // Override setter for reminderDateTime to store time in UTC
    public void setReminderDateTime(LocalDateTime reminderDateTime) {
        if (reminderDateTime != null) {
            ZonedDateTime utcDateTime =
                    reminderDateTime
                            .atZone(ZoneId.systemDefault())
                            .withZoneSameInstant(ZoneId.of("UTC"));
            this.reminderDateTime = utcDateTime.toLocalDateTime();
        } else {
            this.reminderDateTime = null;
        }
    }

    // Getter to convert UTC back to the system's local time
    public LocalDateTime getReminderDateTimeInLocal() {
        return reminderDateTime
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
