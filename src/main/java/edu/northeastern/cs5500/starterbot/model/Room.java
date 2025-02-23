package edu.northeastern.cs5500.starterbot.model;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;

/**
 * Represents the room class
 *
 * @author Team Wolf
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Room implements Model {
    @Builder.Default private ObjectId id = new ObjectId();
    @NonNull private String location;
    @NonNull private Set<TimeSlot> bookedSlots;
    @NonNull private Integer capacity;
}
