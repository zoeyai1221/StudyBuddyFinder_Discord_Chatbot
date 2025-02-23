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
 * Represents the study group class
 *
 * @author Team Wolf
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class StudyGroup implements Model {
    @Builder.Default private ObjectId id = new ObjectId();
    @NonNull private String name;
    @NonNull private Set<Interest> interestSet;
    @NonNull private String description;
    private boolean autoApprove;
    @Builder.Default private LocalDateTime lastActiveTime = LocalDateTime.now();
    @NonNull private Integer maxMembers;
    @NonNull private ObjectId groupLeaderId;
    @NonNull private String customCriteria;
    private String channelId;
}
