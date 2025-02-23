package edu.northeastern.cs5500.starterbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;

/**
 * Represents the interest class
 *
 * @author Team Wolf
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Interest implements Model {
    @Builder.Default private ObjectId id = new ObjectId();
    @NonNull String studentInterest;
    @NonNull Category category;

    /**
     * Enum to represent categories of interests Course categories from
     * https://www.khoury.northeastern.edu/masters-program-mscs-program-details/
     */
    public enum Category {
        COURSE_PREREQUISITE,
        COURSE_CORE,
        COURSE_SYSTEM_SOFTWARE,
        COURSE_THEORY_SECURITY,
        COURSE_AI_DATA_SCIENCE,
        PROGRAMMING_LANGUAGES,
        SOFTWARE_PROGRAMMING_SKILLS,
        OTHER_TOPICS
    }
}
