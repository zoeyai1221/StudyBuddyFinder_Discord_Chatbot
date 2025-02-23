package edu.northeastern.cs5500.starterbot.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;

/**
 * Represents an abstract meeting, which serves as a blueprint for different types of meetings. This
 * class includes information such as the meeting length, topic, frequency, potential time slots,
 * and organizer details.
 *
 * <p>The class is marked as abstract and should be extended by specific meeting types that provide
 * additional functionality or meeting-specific attributes.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = OnlineMeeting.class, name = "OnlineMeeting")})
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractMeeting implements Model {
    @NonNull private ObjectId id = new ObjectId();
    @NonNull private String topic;
    @NonNull private Frequency frequency;
    @NonNull private List<TimeSlot> timeSlots;
    @NonNull private ObjectId studyGroup;
    @NonNull private ObjectId organizer;
    @NonNull private String type;
    @NonNull private HashMap<String, Status> participants; // student : status

    /** Meeting status */
    public enum Status {
        ACCEPT,
        DECLINE,
        TENTATIVE
    }
}
