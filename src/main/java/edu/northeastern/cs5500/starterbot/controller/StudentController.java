package edu.northeastern.cs5500.starterbot.controller;

import com.mongodb.MongoException;
import com.mongodb.lang.Nullable;
import edu.northeastern.cs5500.starterbot.model.*;
import edu.northeastern.cs5500.starterbot.repository.GenericRepository;
import edu.northeastern.cs5500.starterbot.service.FakeOpenTelemetryService;
import edu.northeastern.cs5500.starterbot.service.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import java.util.*;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

/**
 * Controller for managing student. This class provides methods for setting and getting fields in
 * student profile.
 */
@Slf4j
public class StudentController {
    GenericRepository<Student> studentRepository;
    InterestController interestController;
    @Inject OpenTelemetry openTelemetry;
    static final String EMPTY_STRING = "";
    static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@northeastern\\.edu$");

    /**
     * The MeetingController constructor
     *
     * @param studentRepository repo for managing the student entities
     * @param interestController interst controller
     */
    @Inject
    public StudentController(
            GenericRepository<Student> studentRepository, InterestController interestController) {
        this.studentRepository = studentRepository;
        this.interestController = interestController;

        if (studentRepository.count() == 0) {
            HashSet<Interest> interests = new HashSet<>();
            Interest interest = new Interest();
            interest.setStudentInterest("Java");
            interest.setCategory(Interest.Category.PROGRAMMING_LANGUAGES);
            interests.add(interest);
            // vanessa's discord id is used for testing, you can change to yours
            Student student =
                    Student.builder()
                            .displayName("Fake Student")
                            .email("fake.student@example.com")
                            .discordUserId("905314236824162344")
                            .interestSet(interests)
                            .availability(new ArrayList<>())
                            .build();
            studentRepository.add(student);
            // leader of fake groups
            Student student2 =
                    Student.builder()
                            .displayName("Fake Student2")
                            .email("fake.student@example.com")
                            .discordUserId("123456789123456789")
                            .interestSet(interests)
                            .availability(new ArrayList<>())
                            .build();
            studentRepository.add(student2);
        }
        openTelemetry = new FakeOpenTelemetryService();
    }

    /**
     * Set the display name for student
     *
     * @param discordUserId the discord user id
     * @param displayName the display name
     * @return true if the display name have been updated
     */
    public boolean setDisplayNameForStudent(String discordUserId, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return false; // Invalid display name
        }

        var span = openTelemetry.span("setDisplayNameForStudent");
        span.setAttribute("discordUserId", discordUserId);
        span.setAttribute("displayName", displayName);
        try (Scope scope = span.makeCurrent()) {
            Student student = getStudentByDiscordUserId(discordUserId);
            student.setDisplayName(displayName);
            studentRepository.update(student);
            return true; // Successfully updated
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Get display name for student
     *
     * @param discordUserId the discord user id
     * @return the name for student
     */
    @Nullable
    public String getDisplayNameForStudent(String discordUserId) {
        var span = openTelemetry.span("getDisplayNameForStudent");
        span.setAttribute("discordUserId", discordUserId);

        try (Scope scope = span.makeCurrent()) {
            Student student = getStudentByDiscordUserId(discordUserId);
            return student.getDisplayName();
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Set the email for student
     *
     * @param discordUserId the discord user id
     * @param email the email
     * @return true if the email have been updated
     */
    public boolean setEmailForStudent(String discordUserId, String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return false; // Invalid email input
        }
        var span = openTelemetry.span("setEmailForStudent");
        span.setAttribute("discordUserId", discordUserId);
        span.setAttribute("email", email);

        try (Scope scope = span.makeCurrent()) {
            Student student = getStudentByDiscordUserId(discordUserId);
            student.setEmail(email);
            studentRepository.update(student);
            return true;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Get the email for student
     *
     * @param discordUserId the discord user id
     * @return the email
     */
    @Nullable
    public String getEmailForStudent(String discordUserId) {
        var span = openTelemetry.span("getEmailForStudent");
        span.setAttribute("discordUserId", discordUserId);

        try (Scope scope = span.makeCurrent()) {
            Student student = getStudentByDiscordUserId(discordUserId);
            return student.getEmail();
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Set the interests for student
     *
     * @param discordUserId the discord user id
     * @param interests a coillection of interests
     */
    public void setInterestsForStudent(String discordUserId, Collection<String> interests) {
        var span = openTelemetry.span("setInterestsForStudent");
        span.setAttribute("discordUserId", discordUserId);
        span.setAttribute("interests", interests.toString());

        try (Scope scope = span.makeCurrent()) {
            Student student = getStudentByDiscordUserId(discordUserId);
            Set<Interest> interestSet = new HashSet<>();
            for (String interestName : interests) {
                Interest interest = interestController.getInterestByInterestName(interestName);
                if (interest != null) {
                    interestSet.add(interest);
                } else {
                    log.warn("Interest '{}' not found in the database.", interestName);
                }
            }
            // Update the student's interest set
            student.setInterestSet(interestSet);
            studentRepository.update(student);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Set the interests for student
     *
     * @param discordUserId the discord user id
     * @param interests a set of intersts
     */
    public void setInterestsForStudent(String discordUserId, Set<Interest> interests) {
        var span = openTelemetry.span("setInterestsForStudent");
        span.setAttribute("discordUserId", discordUserId);
        span.setAttribute("interests", interests.toString());

        try (Scope scope = span.makeCurrent()) {
            // Retrieve the student by discord user ID
            Student student = getStudentByDiscordUserId(discordUserId);

            // Update the student's interest set with the provided interests
            student.setInterestSet(interests);
            studentRepository.update(student);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Get interests for student
     *
     * @param discordUserId the discord user id
     * @return a set of interests user selected
     */
    @Nullable
    public Set<Interest> getInterestsForStudent(String discordUserId) {
        var span = openTelemetry.span("getInterestsForStudent");
        span.setAttribute("discordUserId", discordUserId);

        try (Scope scope = span.makeCurrent()) {
            Student student = getStudentByDiscordUserId(discordUserId);
            return student.getInterestSet();
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Removes interests related to certain categories from student profile
     *
     * @param discordUserId
     * @param categoriesToRemove Set that contains category enums, interets under these categories
     *     will be removed from the user's interests
     */
    public void clearInterestsForStudent(
            String discordUserId, Set<Interest.Category> categoriesToRemove) {
        var span = openTelemetry.span("clearInterestsForStudent");
        span.setAttribute("discordUserId", discordUserId);
        span.setAttribute("categoriesToRemove", categoriesToRemove.toString());

        try (Scope scope = span.makeCurrent()) {
            Student student = getStudentByDiscordUserId(discordUserId);
            Set<Interest> currentInterests = student.getInterestSet();
            Set<Interest> filteredInterests = new HashSet<>();

            for (Interest interest : currentInterests) {
                // If the interest's category is not in the categoriesToRemove set, keep it
                if (!categoriesToRemove.contains(interest.getCategory())) {
                    filteredInterests.add(interest);
                }
            }
            student.setInterestSet(filteredInterests);
            studentRepository.update(student);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Set the availability for student
     *
     * @param discordUserId the discord user id
     * @param availability the student availability
     */
    public void setAvailabilityForStudent(String discordUserId, List<TimeSlot> availability) {
        var span = openTelemetry.span("setAvailabilityForStudent");
        span.setAttribute("discordUserId", discordUserId);
        span.setAttribute("availability", availability.toString());

        try (Scope scope = span.makeCurrent()) {
            Student student = getStudentByDiscordUserId(discordUserId);
            student.setAvailability(availability);
            studentRepository.update(student);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Get the availability for student
     *
     * @param discordUserId the discord user id
     * @return a list of time slot uer selected
     */
    @Nullable
    public List<TimeSlot> getAvailabilityForStudent(String discordUserId) {
        var span = openTelemetry.span("getAvailabilityForStudent");
        span.setAttribute("discordUserId", discordUserId);

        try (Scope scope = span.makeCurrent()) {
            Student student = getStudentByDiscordUserId(discordUserId);
            return student.getAvailability();
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Get student by the discord user id
     *
     * @param discordUserId the discord user id
     * @return the student object
     */
    @Nonnull
    public Student getStudentByDiscordUserId(String discordUserId) {
        try {
            Collection<Student> students = studentRepository.getAll();
            for (Student student : students) {
                if (student.getDiscordUserId().equals(discordUserId)) {
                    return student;
                }
            }
            // Create new student if not in database
            Student newStudent =
                    Student.builder()
                            .displayName(EMPTY_STRING)
                            .email(EMPTY_STRING)
                            .discordUserId(discordUserId)
                            .interestSet(new HashSet<>())
                            .availability(new ArrayList<>())
                            .build();
            studentRepository.add(newStudent);
            return newStudent;
        } catch (MongoException e) {
            throw new RuntimeException("Failed to interact with the database.", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred:", e);
        }
    }

    /**
     * Retrieves a {@link Student} by their unique student ObjectId.
     *
     * <p>This method searches the student repository for a student with the given ID. If the
     * student is found, it returns the corresponding {@link Student} object. If the student ID is
     * not found in the repository, it throws an {@link IllegalArgumentException}. If a
     * database-related issue occurs, a {@link RuntimeException} wrapping the {@link MongoException}
     * is thrown.
     *
     * @param studentId The {@link ObjectId} representing the unique ID of the student.
     * @return The {@link Student} object corresponding to the given ID.
     * @throws IllegalArgumentException if the student with the specified ID cannot be found.
     * @throws RuntimeException if a database error or unexpected error occurs during the operation.
     * @throws NullPointerException if {@code studentId} is {@code null}.
     */
    @Nonnull
    public Student getStudentByStudentId(ObjectId studentId) {
        try {
            Collection<Student> students = studentRepository.getAll();
            for (Student student : students) {
                if (student.getId().equals(studentId)) {
                    return student;
                }
            }
            throw new IllegalArgumentException("Unable to find student with ID: " + studentId);
        } catch (MongoException e) {
            throw new RuntimeException("Failed to interact with the database.", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred:", e);
        }
    }

    /**
     * Update the student
     *
     * @param student the student
     */
    public void updateStudent(Student student) {
        var span = openTelemetry.span("updateStudent");
        span.setAttribute("discordUserId", student.getDiscordUserId());

        try (Scope scope = span.makeCurrent()) {
            studentRepository.update(student);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
