package edu.northeastern.cs5500.starterbot.model;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;

/**
 * Represents the booking class
 *
 * @author Team Wolf
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Booking implements Model {
    @Builder.Default private ObjectId id = new ObjectId();
    boolean isConfirmed;
    @NonNull private ObjectId roomId;
    @NonNull private ObjectId inPersonMeetingId;
    @NonNull private ObjectId studentId;
}
