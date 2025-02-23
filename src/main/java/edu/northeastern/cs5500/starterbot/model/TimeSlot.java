package edu.northeastern.cs5500.starterbot.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Represents the time slot for meeting
 *
 * @author Team Wolf
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class TimeSlot {
    @NonNull private String day;
    @Builder.Default LocalDateTime start = LocalDateTime.now();
    @Builder.Default LocalDateTime end = LocalDateTime.now();
}
