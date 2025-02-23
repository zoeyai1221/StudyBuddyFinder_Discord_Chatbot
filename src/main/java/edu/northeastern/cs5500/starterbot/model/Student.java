package edu.northeastern.cs5500.starterbot.model;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;

/**
 * Represents the student class
 *
 * @author Team Wolf
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Student implements Model {
    @Builder.Default private ObjectId id = new ObjectId();
    @NonNull private String displayName;
    @NonNull private String email;
    @NonNull private List<TimeSlot> availability;
    @NonNull private String discordUserId;
    // List of study groups that student joined
    @Builder.Default private List<ObjectId> groupList = new ArrayList<>();
    // Interest that associated with the student
    @NonNull private Set<Interest> interestSet;
    private Integer reminderTimeInMin;
}
