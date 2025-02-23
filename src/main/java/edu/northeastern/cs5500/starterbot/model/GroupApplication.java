package edu.northeastern.cs5500.starterbot.model;

import java.time.LocalDateTime;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;

/**
 * Represents the group application class
 *
 * @author Team Wolf
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class GroupApplication implements Model {
    @Builder.Default private ObjectId id = new ObjectId();
    // The discord user id
    @NonNull private String sender;
    // The study group
    @NonNull private ObjectId receiver;
    @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();
    @NonNull private Set<Interest> interestSet;
    @NonNull private String message;
}
