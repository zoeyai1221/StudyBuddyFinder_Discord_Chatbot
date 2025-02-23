package edu.northeastern.cs5500.starterbot.model;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;

/**
 * Represents the calendar class
 *
 * @author Team Wolf
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Calendar implements Model {
    @Builder.Default private ObjectId id = new ObjectId();
    @NonNull private Student student;
    @NonNull private List<AbstractMeeting> meetings;
}
