package edu.northeastern.cs5500.starterbot.controller;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.northeastern.cs5500.starterbot.model.GroupApplication;
import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

class GroupApplicationControllerTest {
    // Shared repositories
    private final InMemoryRepository<Student> studentRepository = new InMemoryRepository<>();
    private final InMemoryRepository<StudyGroup> studyGroupRepository = new InMemoryRepository<>();
    private final InMemoryRepository<GroupApplication> groupApplicationRepository =
            new InMemoryRepository<>();
    private final InMemoryRepository<Interest> interestRepository = new InMemoryRepository<>();

    // Helper methods to create controllers
    private StudentController getStudentController() {
        return new StudentController(studentRepository, getInterestController());
    }

    private InterestController getInterestController() {
        return new InterestController(interestRepository);
    }

    private StudyGroupController getStudyGroupController() {
        return new StudyGroupController(
                studyGroupRepository, groupApplicationRepository, studentRepository);
    }

    private GroupApplicationController getGroupApplicationController() {
        return new GroupApplicationController(
                groupApplicationRepository, getStudentController(), getStudyGroupController());
    }

    /****************** tests for getApplicationsByLeader() **************/

    /**
     * Test that getApplicationsByLeader() correctly returns applications related to all groups the
     * leader created.
     */
    @Test
    void testGetApplicationsByLeaderSuccessfully() {
        // Create default leader and group and applications
        ObjectId leaderId = new ObjectId();
        Student leader = createDefaultStudent(leaderId, "123456789", "Group Leader");
        ObjectId studyGroupId = new ObjectId();
        StudyGroup group = createDefaultStudyGroup(studyGroupId, leaderId, "Test Group");

        GroupApplication application1 =
                GroupApplication.builder()
                        .id(new ObjectId())
                        .sender("234567890")
                        .receiver(studyGroupId)
                        .timestamp(LocalDateTime.now())
                        .message("First application.")
                        .interestSet(new HashSet<>())
                        .build();

        GroupApplication application2 =
                GroupApplication.builder()
                        .id(new ObjectId())
                        .sender("345678901")
                        .receiver(studyGroupId)
                        .timestamp(LocalDateTime.now().plusMinutes(1))
                        .message("Second application.")
                        .interestSet(new HashSet<>())
                        .build();

        // Add leader and group to repositories
        StudentController studentController = getStudentController();
        studentController.studentRepository.add(leader);

        StudyGroupController studyGroupController = getStudyGroupController();
        studyGroupController.studyGroupRepository.add(group);

        GroupApplicationController groupApplicationController = getGroupApplicationController();
        groupApplicationController.groupApplicationRepository.add(application1);
        groupApplicationController.groupApplicationRepository.add(application2);

        // Fetch applications by leader
        Collection<GroupApplication> applications =
                groupApplicationController.getApplicationsByLeader(leader.getDiscordUserId());

        // Assert correct applications are retrieved
        assertThat(applications).hasSize(2);
        assertThat(applications).containsExactly(application1, application2).inOrder();
    }

    /** Test that getApplicationsByLeader() correctly skips applications not related the leader. */
    @Test
    void testGetApplicationsByLeaderFiltersCorrectly() {
        GroupApplicationController controller = getGroupApplicationController();

        ObjectId leaderId = new ObjectId();
        Student leader = createDefaultStudent(leaderId, "123456789", "Group Leader");
        ObjectId matchingGroupId = new ObjectId();
        ObjectId nonMatchingGroupId = new ObjectId();

        StudyGroup matchingGroup =
                createDefaultStudyGroup(matchingGroupId, leaderId, "Matching Group");
        StudyGroup nonMatchingGroup =
                createDefaultStudyGroup(nonMatchingGroupId, new ObjectId(), "Non-Matching Group");

        GroupApplication matchingApplication =
                GroupApplication.builder()
                        .id(new ObjectId())
                        .sender("applicant1")
                        .receiver(matchingGroupId)
                        .timestamp(LocalDateTime.now())
                        .message("Application to matching group")
                        .interestSet(new HashSet<>())
                        .build();

        GroupApplication nonMatchingApplication =
                GroupApplication.builder()
                        .id(new ObjectId())
                        .sender("applicant2")
                        .receiver(nonMatchingGroupId)
                        .timestamp(LocalDateTime.now().plusMinutes(1))
                        .message("Application to non-matching group")
                        .interestSet(new HashSet<>())
                        .build();

        // Add to repositories
        controller.studentController.studentRepository.add(leader);
        controller.studyGroupController.studyGroupRepository.add(matchingGroup);
        controller.studyGroupController.studyGroupRepository.add(nonMatchingGroup);
        controller.groupApplicationRepository.add(matchingApplication);
        controller.groupApplicationRepository.add(nonMatchingApplication);

        Collection<GroupApplication> applications =
                controller.getApplicationsByLeader(leader.getDiscordUserId());

        // Assert that only applications for the leader's groups are included
        assertThat(applications).hasSize(1);
        assertThat(applications).containsExactly(matchingApplication);
    }

    /**
     * Test that getApplicationsByLeader() correctly returns empty application list if the student
     * has not created any group.
     */
    @Test
    void testGetApplicationsByLeaderWithNoApplications() {
        ObjectId leaderId = new ObjectId();
        Student leader = createDefaultStudent(leaderId, "123456789", "Group Leader");
        StudentController studentController = getStudentController();
        studentController.studentRepository.add(leader);

        GroupApplicationController controller = getGroupApplicationController();

        // Fetch applications by leader
        Collection<GroupApplication> applications =
                controller.getApplicationsByLeader(leader.getDiscordUserId());

        assertThat(applications).isEmpty();
    }

    /****************** tests for accept application **************/
    /** Test that acceptApplication() works correctly. */
    @Test
    void testAcceptApplicationSuccessfully() {
        GroupApplicationController controller = getGroupApplicationController();

        // Create leader, applicant, study group, and application
        ObjectId leaderId = new ObjectId();
        ObjectId applicantId = new ObjectId();
        Student leader = createDefaultStudent(leaderId, "123456789", "Group Leader");
        Student applicant = createDefaultStudent(applicantId, "987654321", "Applicant");

        StudyGroup studyGroup = createDefaultStudyGroup(new ObjectId(), leaderId, "Sample Group");
        GroupApplication application =
                GroupApplication.builder()
                        .id(new ObjectId())
                        .sender(applicant.getDiscordUserId())
                        .receiver(studyGroup.getId())
                        .timestamp(LocalDateTime.now())
                        .message("Test message")
                        .interestSet(new HashSet<>())
                        .build();

        // Add to repositories
        controller.studentController.studentRepository.add(leader);
        controller.studentController.studentRepository.add(applicant);
        controller.studyGroupController.studyGroupRepository.add(studyGroup);
        controller.groupApplicationRepository.add(application);

        // Accept the application
        controller.acceptApplication(application);

        // 1. application is removed
        // 2. applicant is added to the group
        assertThat(controller.groupApplicationRepository.getAll()).isEmpty();
        assertThat(applicant.getGroupList()).contains(studyGroup.getId());
    }

    /****************** tests for decline application **************/
    /** Test that declineApplication() works correctly. */
    @Test
    void testDeclineApplicationSuccessfully() {
        GroupApplicationController controller = getGroupApplicationController();

        // Create leader, study group, and application
        ObjectId leaderId = new ObjectId();
        Student leader = createDefaultStudent(leaderId, "123456789", "Group Leader");
        ObjectId applicantId = new ObjectId();
        Student applicant = createDefaultStudent(applicantId, "987654321", "Applicant");
        StudyGroup studyGroup = createDefaultStudyGroup(new ObjectId(), leaderId, "Sample Group");

        GroupApplication application =
                GroupApplication.builder()
                        .id(new ObjectId())
                        .sender(applicant.getDiscordUserId())
                        .receiver(studyGroup.getId())
                        .timestamp(LocalDateTime.now())
                        .message("Test message")
                        .interestSet(new HashSet<>())
                        .build();

        // Add to repositories
        controller.studentController.studentRepository.add(leader);
        controller.studyGroupController.studyGroupRepository.add(studyGroup);
        controller.groupApplicationRepository.add(application);

        // Decline the application
        controller.declineApplication(application);

        // Assert application is removed
        assertThat(controller.groupApplicationRepository.getAll()).isEmpty();
        assertThat(applicant.getGroupList()).isEmpty();
    }

    /****************** tests for get application by id **************/
    /** Test that getApplicationById() works correctly. */
    @Test
    void testGetApplicationByIdSuccessfully() {
        GroupApplicationController controller = getGroupApplicationController();

        ObjectId applicationId = new ObjectId();
        GroupApplication application =
                GroupApplication.builder()
                        .id(applicationId)
                        .sender("123456789")
                        .receiver(new ObjectId())
                        .timestamp(LocalDateTime.now())
                        .message("Test application")
                        .interestSet(new HashSet<>())
                        .build();

        controller.groupApplicationRepository.add(application);

        // Retrieve the application
        GroupApplication retrievedApplication = controller.getApplicationById(applicationId);

        // Assert application is retrieved correctly
        assertThat(retrievedApplication).isNotNull();
        assertThat(retrievedApplication.getId()).isEqualTo(applicationId);
    }

    /** Test that getApplicationById() throws exception when group application id is invalid. */
    @Test
    void testGetApplicationByIdThrowsExceptionForInvalidId() {
        GroupApplicationController controller = getGroupApplicationController();

        // Invalid application id
        ObjectId invalidId = new ObjectId();

        // Assert: Exception is thrown for invalid application id
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> controller.getApplicationById(invalidId));
        assertThat(exception.getMessage())
                .contains("Group application with ID " + invalidId + " not found.");
    }

    /**
     * Helper function to create a student for testing purpose
     *
     * @param id Student ObjectId
     * @param discordUserId
     * @param displayName
     * @return
     */
    private Student createDefaultStudent(ObjectId id, String discordUserId, String displayName) {
        return Student.builder()
                .id(id)
                .discordUserId(discordUserId)
                .displayName(displayName)
                .email("default@example.com")
                .interestSet(new HashSet<>())
                .availability(new ArrayList<>())
                .groupList(new ArrayList<>())
                .build();
    }

    /**
     * Helper function to create a study group for testing purpose
     *
     * @param id StudyGroup ObjectId
     * @param groupLeaderId
     * @param name
     * @return
     */
    private StudyGroup createDefaultStudyGroup(ObjectId id, ObjectId groupLeaderId, String name) {
        return StudyGroup.builder()
                .id(id)
                .groupLeaderId(groupLeaderId)
                .name(name)
                .description("Default description")
                .interestSet(new HashSet<>())
                .maxMembers(10)
                .autoApprove(false)
                .customCriteria("Default criteria")
                .build();
    }
}
