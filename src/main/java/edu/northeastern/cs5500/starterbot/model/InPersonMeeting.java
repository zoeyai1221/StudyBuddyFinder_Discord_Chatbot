package edu.northeastern.cs5500.starterbot.model;

import com.mongodb.lang.Nullable;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;

/**
 * Represents an in-person meeting, extending the AbstractMeeting class. This class includes
 * additional information for meetings that occur at a physical location, such as a booking for the
 * meeting space.
 *
 * <p>The class extends AbstractMeeting and inherits attributes such as meeting length, topic,
 * frequency, time slots, and organizer, while adding a booking detail for the location.
 *
 * @author Team Wolf
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class InPersonMeeting extends AbstractMeeting {
    @Nullable private Booking booking;

    @Builder
    public InPersonMeeting(
            @NonNull ObjectId id,
            @NonNull String topic,
            @NonNull Frequency frequency,
            @NonNull List<TimeSlot> timeSlots,
            @NonNull ObjectId studyGroup,
            @NonNull ObjectId organizer,
            @Nullable Booking booking,
            @NonNull HashMap<String, Status> participants) {
        super(
                id,
                topic,
                frequency,
                timeSlots,
                studyGroup,
                organizer,
                "InPersonMeeting",
                participants);
        this.booking = booking;
    }
}
