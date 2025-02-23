package edu.northeastern.cs5500.starterbot.controller;

import static com.google.common.truth.Truth.assertThat;

import com.mongodb.MongoException;
import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

class StudentControllerTest {
    private StudentController getStudentController() {
        return new StudentController(new InMemoryRepository<>(), getInterestController());
    }

    private InterestController getInterestController() {
        return new InterestController(new InMemoryRepository<>());
    }
    /****************** tests for getStudentByStudentId() **************/
    @Test
    void testGetStudentByStudentIdWithValidId() {
        StudentController studentController = getStudentController();

        ObjectId studentId = new ObjectId();
        Student student =
                Student.builder()
                        .id(studentId)
                        .displayName("test student")
                        .email("fake.student@example.com")
                        .discordUserId("123456789123456789")
                        .interestSet(new HashSet<>())
                        .availability(new ArrayList<>())
                        .build();
        studentController.studentRepository.add(student);
        Student retrievedStudent = studentController.getStudentByStudentId(studentId);

        // Verify the retrieved student matches the original
        assertThat(retrievedStudent).isNotNull();
        assertThat(retrievedStudent.getId()).isEqualTo(studentId);
        assertThat(retrievedStudent.getDisplayName()).isEqualTo("test student");
        assertThat(retrievedStudent.getEmail()).isEqualTo("fake.student@example.com");
        assertThat(retrievedStudent.getDiscordUserId()).isEqualTo("123456789123456789");
    }

    @Test
    void testGetStudentByDiscordUserId_whenStudentNotFound_createsNewStudent() {
        StudentController studentController = getStudentController();
        String discordUserId = "123456789";
        // Get student from empty student repository
        Student student = studentController.getStudentByDiscordUserId(discordUserId);

        // Verify that the newly created student has the default values
        assertThat(student).isNotNull();
        assertThat(student.getDiscordUserId()).isEqualTo(discordUserId);
        assertThat(student.getDisplayName()).isEqualTo("");
        assertThat(student.getEmail()).isEqualTo("");
        assertThat(student.getInterestSet()).isNotNull();
        assertThat(student.getInterestSet()).isEmpty();
        assertThat(student.getAvailability()).isNotNull();
        assertThat(student.getAvailability()).isEmpty();
    }

    @Test
    void testGetStudentByStudentIdWithInvalidId() {
        StudentController studentController = getStudentController();

        ObjectId invalidId = new ObjectId();

        // Verify that an IllegalArgumentException is thrown
        try {
            studentController.getStudentByStudentId(invalidId);
        } catch (RuntimeException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage())
                    .contains("Unable to find student with ID: " + invalidId);
            return;
        }

        // Fail the test if no exception is thrown
        assertThat("Expected exception was not thrown").isEmpty();
    }

    @Test
    void testGetStudentByStudentIdWithDatabaseFailure() {
        // Use a repository that simulates a database failure
        InMemoryRepository<Student> failingRepository =
                new InMemoryRepository<Student>() {
                    @Override
                    public Collection<Student> getAll() {
                        throw new MongoException("Simulated database failure");
                    }
                };

        StudentController failingController =
                new StudentController(failingRepository, getInterestController());
        ObjectId studentId = new ObjectId();

        // Verify RuntimeException for MongoException is thrown
        try {
            failingController.getStudentByStudentId(studentId);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Failed to interact with the database.");
            assertThat(e.getCause()).isInstanceOf(MongoException.class);
            return;
        }

        // Fail the test if no exception is thrown
        assertThat("Expected exception was not thrown").isEmpty();
    }

    /****************** tests for clearInterestsForStudent() **************/
    @Test
    void testClearInterestsForStudent_removesSelectedCategories() {
        StudentController studentController = getStudentController();

        ObjectId studentId = new ObjectId();
        Set<Interest> currentInterests = new HashSet<>();
        Interest interest1 =
                Interest.builder()
                        .studentInterest("Java")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();
        Interest interest2 =
                Interest.builder()
                        .studentInterest("Python")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();
        Interest interest3 =
                Interest.builder()
                        .studentInterest("Machine Learning")
                        .category(Interest.Category.COURSE_AI_DATA_SCIENCE)
                        .build();

        currentInterests.add(interest1);
        currentInterests.add(interest2);
        currentInterests.add(interest3);

        Student student =
                Student.builder()
                        .id(studentId)
                        .displayName("test student")
                        .email("fake.student@example.com")
                        .discordUserId("1234567890")
                        .interestSet(currentInterests)
                        .availability(new ArrayList<>())
                        .build();

        studentController.studentRepository.add(student);

        // Set up categories to remove
        Set<Interest.Category> categoriesToRemove = new HashSet<>();
        categoriesToRemove.add(Interest.Category.PROGRAMMING_LANGUAGES);

        studentController.clearInterestsForStudent(student.getDiscordUserId(), categoriesToRemove);
        Set<Interest> updatedInterests =
                studentController.getInterestsForStudent(student.getDiscordUserId());

        // Only "Machine Learning" should remain
        assertThat(updatedInterests).hasSize(1);
        assertThat(updatedInterests).contains(interest3);
        assertThat(updatedInterests).doesNotContain(interest1);
        assertThat(updatedInterests).doesNotContain(interest2);
    }

    @Test
    void testClearInterestsForStudent_withEmptyCategories() {
        StudentController studentController = getStudentController();

        ObjectId studentId = new ObjectId();
        Set<Interest> currentInterests = new HashSet<>();
        Interest interest1 =
                Interest.builder()
                        .studentInterest("Java")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();
        Interest interest2 =
                Interest.builder()
                        .studentInterest("Python")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();
        Interest interest3 =
                Interest.builder()
                        .studentInterest("JavaScript")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();

        currentInterests.add(interest1);
        currentInterests.add(interest2);
        currentInterests.add(interest3);

        Student student =
                Student.builder()
                        .id(studentId)
                        .displayName("test student")
                        .email("fake.student@example.com")
                        .discordUserId("1234567890")
                        .interestSet(currentInterests)
                        .availability(new ArrayList<>())
                        .build();

        studentController.studentRepository.add(student);

        // Empty categories to remove
        Set<Interest.Category> categoriesToRemove = new HashSet<>();

        studentController.clearInterestsForStudent(student.getDiscordUserId(), categoriesToRemove);

        Set<Interest> updatedInterests = student.getInterestSet();

        // No interests should be removed
        assertThat(updatedInterests).hasSize(3);
        assertThat(updatedInterests).contains(interest1);
        assertThat(updatedInterests).contains(interest2);
        assertThat(updatedInterests).contains(interest3);
    }

    /****************** tests for setDisplayNameForStudent() **************/
    @Test
    void testSetDisplayNameForStudent() {
        StudentController studentController = getStudentController();
        String discordUserId = "123456789";
        String newDisplayName = "New Display Name";

        Student student = createStudent(discordUserId);
        studentController.studentRepository.add(student);

        boolean result = studentController.setDisplayNameForStudent(discordUserId, newDisplayName);

        assertThat(result).isTrue();
        assertThat(student.getDisplayName()).isEqualTo(newDisplayName);
    }

    @Test
    void testSetDisplayNameForStudent_withEmptyName() {
        StudentController studentController = getStudentController();
        String discordUserId = "123456789";
        String invalidDisplayName = ""; // Empty name

        Student student = createStudent(discordUserId);
        studentController.studentRepository.add(student);

        boolean result =
                studentController.setDisplayNameForStudent(discordUserId, invalidDisplayName);

        assertThat(result).isFalse();
        assertThat(student.getDisplayName()).isNotEqualTo(invalidDisplayName);
    }

    /****************** tests for setEmailForStudent() **************/
    @Test
    void testSetEmailForStudent() {
        StudentController studentController = getStudentController();
        String discordUserId = "123456789";
        String validEmail = "test@northeastern.edu";

        Student student = createStudent(discordUserId);
        studentController.studentRepository.add(student);

        boolean result = studentController.setEmailForStudent(discordUserId, validEmail);

        assertThat(result).isTrue();
        assertThat(student.getEmail()).isEqualTo(validEmail);
    }

    @Test
    void testSetEmailForStudent_withInvalidEmail() {
        StudentController studentController = getStudentController();
        String discordUserId = "123456789";
        String invalidEmail = "test@student.com"; // Invalid email format

        Student student = createStudent(discordUserId);
        studentController.studentRepository.add(student);

        boolean result = studentController.setEmailForStudent(discordUserId, invalidEmail);

        assertThat(result).isFalse();
        assertThat(student.getEmail()).isNotEqualTo(invalidEmail);
    }

    /****************** tests for getDisplayNameForStudent() **************/
    @Test
    void testGetDisplayNameForStudent() {
        StudentController studentController = getStudentController();
        String discordUserId = "123456789";
        String expectedDisplayName = "Test Student";

        Student student = createStudent(discordUserId);
        student.setDisplayName(expectedDisplayName);
        studentController.studentRepository.add(student);

        String displayName = studentController.getDisplayNameForStudent(discordUserId);

        assertThat(displayName).isEqualTo(expectedDisplayName);
    }

    /****************** tests for getEmailForStudent() **************/
    @Test
    void testGetEmailForStudent() {
        StudentController studentController = getStudentController();
        String discordUserId = "123456789";
        String expectedEmail = "test@student.northeastern.edu";

        Student student = createStudent(discordUserId);
        student.setEmail(expectedEmail);
        studentController.studentRepository.add(student);

        String email = studentController.getEmailForStudent(discordUserId);

        assertThat(email).isEqualTo(expectedEmail);
    }

    /****************** tests for setInterestsForStudent() **************/
    @Test
    void testSetInterestsForStudent_WithSetOfInterestAsParameter() {
        StudentController studentController = getStudentController();
        String discordUserId = "123456789";
        Set<Interest> expectedInterests = new HashSet<>();
        expectedInterests.add(
                Interest.builder()
                        .studentInterest("Java")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build());

        Student student = createStudent(discordUserId);
        studentController.studentRepository.add(student);

        studentController.setInterestsForStudent(discordUserId, expectedInterests);

        Set<Interest> updatedInterests = student.getInterestSet();
        assertThat(updatedInterests).containsExactlyElementsIn(expectedInterests);
    }

    @Test
    void testSetInterestsForStudent_WithSetOfStringAsParameter() {
        StudentController studentController = getStudentController();
        String discordUserId = "123456789";
        Set<String> validInterests = Set.of("Java", "Python");

        // Sample interests
        Interest javaInterest =
                Interest.builder()
                        .studentInterest("Java")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();
        Interest pythonInterest =
                Interest.builder()
                        .studentInterest("Python")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();

        Student student = createStudent(discordUserId);
        studentController.studentRepository.add(student);

        studentController.interestController.interestRepository.add(javaInterest);
        studentController.interestController.interestRepository.add(pythonInterest);

        studentController.setInterestsForStudent(discordUserId, validInterests);

        Set<Interest> updatedInterests = student.getInterestSet();
        assertThat(updatedInterests).hasSize(2);
        assertThat(updatedInterests).contains(javaInterest);
        assertThat(updatedInterests).contains(pythonInterest);
    }

    // Helper method to create a student
    private Student createStudent(String discordUserId) {
        Set<Interest> interestSet = new HashSet<>();
        interestSet.add(
                Interest.builder()
                        .studentInterest("Python")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build());

        return Student.builder()
                .discordUserId(discordUserId)
                .displayName("Test Student")
                .email("test@northeastern.edu")
                .interestSet(interestSet)
                .availability(new ArrayList<>())
                .build();
    }
}
