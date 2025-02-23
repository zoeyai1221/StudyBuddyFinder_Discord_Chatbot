package edu.northeastern.cs5500.starterbot.model;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;

/**
 * Represents an online meeting, extending the AbstractMeeting class. This class includes additional
 * information for meetings that occur virtually, such as a link to the online meeting.
 *
 * <p>The class extends AbstractMeeting and inherits attributes such as meeting length, topic,
 * frequency, time slots, and organizer, while adding a meeting link for the virtual location.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class OnlineMeeting extends AbstractMeeting {
    @NonNull private String meetingLink;

    @Builder
    public OnlineMeeting(
            @NonNull ObjectId id,
            @NonNull String topic,
            @NonNull Frequency frequency,
            @NonNull List<TimeSlot> timeSlots,
            @NonNull ObjectId studyGroup,
            @NonNull ObjectId organizer,
            @NonNull String meetingLink,
            @NonNull HashMap<String, Status> participants) {
        super(
                id,
                topic,
                frequency,
                timeSlots,
                studyGroup,
                organizer,
                "OnlineMeeting",
                participants);
        this.meetingLink = meetingLink;
    }
}
