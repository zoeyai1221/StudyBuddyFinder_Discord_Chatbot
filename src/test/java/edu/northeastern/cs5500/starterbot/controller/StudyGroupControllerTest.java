package edu.northeastern.cs5500.starterbot.controller;

import static com.google.common.truth.Truth.assertThat;

import com.mongodb.MongoException;
import edu.northeastern.cs5500.starterbot.model.GroupApplication;
import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.annotation.Nonnull;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

class StudyGroupControllerTest {
    InMemoryRepository<Student> studentRepository = new InMemoryRepository<>();

    private StudyGroupController getStudyGroupController() {
        return new StudyGroupController(
                new InMemoryRepository<>(), new InMemoryRepository<>(), studentRepository);
    }

    private StudentController getStudentController() {
        return new StudentController(studentRepository, getInterestController());
    }

    private InterestController getInterestController() {
        return new InterestController(new InMemoryRepository<>());
    }

    private BookingController getBookingController() {
        return new BookingController(new InMemoryRepository<>(), new InMemoryRepository<>());
    }

    private ReminderController getReminderController() {
        return new ReminderController(
                new InMemoryRepository<>(),
                getStudentController(),
                getMeetingController(),
                getStudyGroupController(),
                null);
    }

    private MeetingController getMeetingController() {
        return new MeetingController(new InMemoryRepository<>(), new InMemoryRepository<>());
    }

    /****************** tests for getMemberListOfStudyGroup() **************/
    @Test
    void testGetMemberListOfStudyGroupWithMembers() {
        // Create a study group

        ObjectId studyGroupId = new ObjectId();
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(studyGroupId);

        // Create students
        Student student1 = new Student();
        student1.setId(new ObjectId());
        student1.setGroupList(List.of(studyGroupId)); // Add to group

        Student student2 = new Student();
        student2.setId(new ObjectId());
        student2.setGroupList(List.of()); // Not in the group

        Student student3 = new Student();
        student3.setId(new ObjectId());
        student3.setGroupList(List.of(studyGroupId)); // Add to group

        // Add students to repository
        StudyGroupController studyGroupController = getStudyGroupController();
        studyGroupController.studentRepository.add(student1);
        studyGroupController.studentRepository.add(student2);
        studyGroupController.studentRepository.add(student3);

        // Call the method
        List<Student> members = studyGroupController.getMemberListOfStudyGroup(studyGroup);

        // Assert results
        assertThat(members).isNotNull();
        assertThat(members).hasSize(2);
    }

    @Test
    void testGetMemberListOfStudyGroupWithNoMembers() {
        // Create a study group
        ObjectId studyGroupId = new ObjectId();
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(studyGroupId);

        // Call the method
        StudyGroupController studyGroupController = getStudyGroupController();
        List<Student> members = studyGroupController.getMemberListOfStudyGroup(studyGroup);

        // Assertions
        assertThat(members).isNotNull();
        assertThat(members).isEmpty();
    }

    @Test
    void testExceptionHandlingInGetMemberListOfStudyGroup() {
        // Create a study group
        ObjectId studyGroupId = new ObjectId();
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(studyGroupId);

        // Simulate an exception by overriding getAll() to throw an exception
        StudyGroupController studyGroupController = getStudyGroupController();
        studyGroupController.studentRepository =
                new InMemoryRepository<Student>() {
                    @Override
                    public Collection<Student> getAll() {
                        throw new RuntimeException("Simulated repository exception");
                    }
                };

        try {
            studyGroupController.getMemberListOfStudyGroup(studyGroup);
        } catch (RuntimeException e) {
            // Assert that an exception was thrown and contains the expected message
            assertThat(e).isNotNull();
            return;
        }

        // Fail the test if no exception is thrown
        assertThat("Expected exception was not thrown").isEmpty();
    }

    /****************** tests for matchedGroup() **************/
    @Test
    void testMatchedGroupWithMatchingInterest() {
        StudyGroupController studyGroupController = getStudyGroupController();

        // create a interest
        HashSet<Interest> interests = new HashSet<>();
        Interest interest = new Interest();
        interest.setStudentInterest("CS5500:Foundations of Software Engineering");
        interest.setCategory(Interest.Category.COURSE_SYSTEM_SOFTWARE);
        interests.add(interest);

        // create a student
        Student student = new Student();
        student.setId(new ObjectId());
        student.setInterestSet(interests);
        student.setGroupList(List.of());
        studyGroupController.studentRepository.add(student);

        // create a matching group
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setInterestSet(interests);
        studyGroup.setMaxMembers(10);

        // Verify the matching group match for the student
        assertThat(studyGroupController.matchedGroup(student, studyGroup)).isTrue();
    }

    @Test
    void testMatchedGroupWithNoMatchingInterest() {
        StudyGroupController studyGroupController = getStudyGroupController();

        // create a interest for student
        HashSet<Interest> studentInterests = new HashSet<>();
        Interest interest = new Interest();
        interest.setStudentInterest("CS5520:Mobile Application Development");
        interest.setCategory(Interest.Category.COURSE_SYSTEM_SOFTWARE);
        studentInterests.add(interest);

        HashSet<Interest> groupInterests = new HashSet<>();
        Interest interest2 = new Interest();
        interest2.setStudentInterest("CS5500:Foundations of Software Engineering");
        interest2.setCategory(Interest.Category.COURSE_SYSTEM_SOFTWARE);
        groupInterests.add(interest2);

        // create a student
        Student student = new Student();
        student.setId(new ObjectId());
        student.setInterestSet(studentInterests);
        student.setGroupList(List.of());
        studyGroupController.studentRepository.add(student);

        // create a matching group
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setInterestSet(groupInterests);
        studyGroup.setMaxMembers(10);

        // Verify the matching group match for the student
        assertThat(studyGroupController.matchedGroup(student, studyGroup)).isFalse();
    }

    @Test
    void testMatchedGroupWithFullGroup() {
        StudyGroupController studyGroupController = getStudyGroupController();
        // create a interest
        HashSet<Interest> interests = new HashSet<>();
        Interest interest = new Interest();
        interest.setStudentInterest("CS5500:Foundations of Software Engineering");
        interest.setCategory(Interest.Category.COURSE_SYSTEM_SOFTWARE);
        interests.add(interest);

        // create a student
        Student student = new Student();
        student.setId(new ObjectId());
        student.setInterestSet(interests);
        student.setGroupList(List.of());
        studyGroupController.studentRepository.add(student);

        // create a matching group that is full
        StudyGroup fullStudyGroup = new StudyGroup();
        ObjectId studyGroupId = new ObjectId();
        fullStudyGroup.setId(studyGroupId);
        fullStudyGroup.setInterestSet(interests);
        fullStudyGroup.setMaxMembers(3);
        // add 3 members to full group
        for (int i = 0; i < 3; i++) {
            Student member = new Student();
            member.setId(new ObjectId());
            member.setGroupList(List.of(fullStudyGroup.getId()));
            studyGroupController.studentRepository.add(member);
        }

        // Verify the full group is not matched for the student
        assertThat(studyGroupController.matchedGroup(student, fullStudyGroup)).isFalse();
    }

    @Test
    void testMatchedGroupAlreadyJoined() {
        StudyGroupController studyGroupController = getStudyGroupController();

        // create a interest
        HashSet<Interest> interests = new HashSet<>();
        Interest interest = new Interest();
        interest.setStudentInterest("CS5500:Foundations of Software Engineering");
        interest.setCategory(Interest.Category.COURSE_SYSTEM_SOFTWARE);
        interests.add(interest);

        // create a student
        Student student = new Student();
        student.setId(new ObjectId());
        student.setInterestSet(interests);
        student.setGroupList(List.of());
        studyGroupController.studentRepository.add(student);

        // create a matching group
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setInterestSet(interests);
        studyGroup.setMaxMembers(10);

        // add the student to the group
        student.setGroupList(List.of(studyGroup.getId()));

        // Verify the matching group match for the student
        assertThat(studyGroupController.matchedGroup(student, studyGroup)).isFalse();
    }

    /****************** tests for addStudyGroup() **************/
    /**
     * Tests that a student is successfully added to a study group when slots are available. Ensures
     * that the study group ID is added to the student's group list when the group has capacity.
     */
    @Test
    void testAddStudyGroupWhenSlotsAreAvailable() {
        StudyGroupController studyGroupController = getStudyGroupController();
        StudentController studentController = getStudentController();

        // create a study group with member
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(new ObjectId());
        studyGroup.setMaxMembers(5);
        Student member = new Student();
        member.setGroupList(List.of(studyGroup.getId()));
        studyGroupController.studentRepository.add(member);

        // create the student to be added
        Student student = new Student();
        student.setId(new ObjectId());
        student.setGroupList(new ArrayList<>());

        // act
        studyGroupController.addStudyGroup(student, studyGroup, studentController);

        // verify that the study group list is added to student's group list
        assertThat(student.getGroupList()).contains(studyGroup.getId());
    }

    /**
     * Tests that a student is not added to a study group when the group is full. Ensures that the
     * study group ID is not added to the student's group list when the maximum capacity is reached.
     */
    @Test
    void testAddStudyGroupWhenGroupIsFull() {
        StudyGroupController studyGroupController = getStudyGroupController();
        StudentController studentController = getStudentController();

        // create a full study group
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(new ObjectId());
        studyGroup.setMaxMembers(5);
        for (int i = 0; i < 5; i++) {
            Student member = new Student();
            member.setGroupList(List.of(studyGroup.getId()));
            studyGroupController.studentRepository.add(member);
        }

        Student student = new Student();
        student.setId(new ObjectId());
        student.setGroupList(new ArrayList<>());

        studyGroupController.addStudyGroup(student, studyGroup, studentController);

        // verify that the study group list is not added to student's group list
        assertThat(student.getGroupList()).doesNotContain(studyGroup.getId());
    }

    /**
     * Tests that a student is added to a study group when the group's maximum member limit is not
     * set. Verifies that the absence of a max member limit allows the addition of students to the
     * group.
     */
    @Test
    void testAddStudyGroupWhenMaxMembersNotSet() {
        StudyGroupController studyGroupController = getStudyGroupController();
        StudentController studentController = getStudentController();

        // create a study group with member
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(new ObjectId());
        Student member = new Student();
        member.setGroupList(List.of(studyGroup.getId()));
        studyGroupController.studentRepository.add(member);

        Student student = new Student();
        student.setId(new ObjectId());
        student.setGroupList(new ArrayList<>());

        // act
        studyGroupController.addStudyGroup(student, studyGroup, studentController);

        // verify that the study group list is added to student's group list
        assertThat(student.getGroupList()).contains(studyGroup.getId());
    }

    /****************** tests for getStudyGroupById() **************/
    /**
     * Tests that a study group can be retrieved by its valid ID. Verifies that the method correctly
     * fetches the study group from the repository when provided with a valid identifier.
     */
    @Test
    void testGetStudyGroupByIdWithValidId() {
        // set up
        StudyGroupController studyGroupController = getStudyGroupController();

        // create a study group
        ObjectId groupId = new ObjectId();
        StudyGroup studyGroup =
                StudyGroup.builder()
                        .id(groupId)
                        .name("Study Group")
                        .interestSet(new HashSet<>())
                        .description("Study Group for unit test")
                        .autoApprove(true)
                        .maxMembers(10)
                        .groupLeaderId(new ObjectId())
                        .customCriteria("Sample Criteria")
                        .build();

        // add to repo
        studyGroupController.studyGroupRepository.add(studyGroup);

        // retrieve group by id
        StudyGroup retrieveGroup = studyGroupController.getStudyGroupById(groupId);

        // assert the results
        assertThat(retrieveGroup).isNotNull();
        assertThat(retrieveGroup.getId()).isEqualTo(groupId);
    }

    /**
     * Tests that an exception is thrown when trying to retrieve a study group using an invalid ID.
     * Ensures the method handles invalid input gracefully and returns an appropriate error.
     */
    @Test
    void testGetStudyGroupByIdWithInvalidId() {
        // set up
        StudyGroupController studyGroupController = getStudyGroupController();

        // invalid id
        ObjectId invalidId = new ObjectId();

        // Verify that an IllegalArgumentException is thrown
        try {
            studyGroupController.getStudyGroupById(invalidId);
        } catch (RuntimeException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getMessage())
                    .contains("Unable to find study group with ID: " + invalidId);
            return;
        }

        // Fail the test if no exception is thrown
        assertThat("Expected exception was not thrown").isEmpty();
    }

    /**
     * Simulates a database failure when retrieving a study group by ID. Ensures the method can
     * handle database interaction errors and provides meaningful feedback.
     */
    @Test
    void testGetStudyGroupByIdWithDatabaseFailure() {
        // Use a repository that simulates a database failure
        InMemoryRepository<StudyGroup> failingRepository =
                new InMemoryRepository<StudyGroup>() {
                    @Override
                    public StudyGroup get(@Nonnull ObjectId id) {
                        throw new MongoException("Simulated database failure");
                    }
                };

        StudyGroupController failingController =
                new StudyGroupController(
                        failingRepository, new InMemoryRepository<>(), new InMemoryRepository<>());

        ObjectId groupId = new ObjectId();

        // Verify that an IllegalArgumentException is thrown
        try {
            failingController.getStudyGroupById(groupId);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Failed to interact with the database.");
            assertThat(e.getCause()).isInstanceOf(MongoException.class);
            return; // Exception caught
        }

        // Fail the test if no exception is thrown
        assertThat("Expected exception was not thrown").isEmpty();
    }
    /****************** tests for createStudyGroup() *******************/
    @Test
    void testCreateStudyGroupSuccessfully() {
        Student leader = new Student();
        leader.setId(new ObjectId());

        // Create a new study group
        StudyGroup newGroup = new StudyGroup();
        newGroup.setId(new ObjectId());
        newGroup.setName("Math Study Group");
        newGroup.setMaxMembers(5);

        StudyGroupController studyGroupController = getStudyGroupController();
        StudentController studentController = getStudentController();

        StudyGroup createdGroup =
                studyGroupController.createStudyGroup(leader, newGroup, studentController);

        StudyGroup storedGroup =
                studyGroupController.studyGroupRepository.getAll().stream()
                        .filter(group -> group.getId().equals(newGroup.getId()))
                        .findFirst()
                        .orElse(null);

        // Verify the group exists in the repository
        assertThat(storedGroup).isEqualTo(createdGroup);
        assertThat(storedGroup.getName()).isEqualTo("Math Study Group");
        assertThat(storedGroup.getMaxMembers()).isEqualTo(5);
    }

    // Test that study group with invalid leader would throw an exception
    @Test
    void testCreateStudyGroupWithInvalidLeader() {
        // Create a study group with no leader
        Student invalidLeader = null;
        StudyGroup newGroup = new StudyGroup();
        newGroup.setId(new ObjectId());
        newGroup.setName("Physics Study Group");

        StudyGroupController studyGroupController = getStudyGroupController();
        StudentController studentController = getStudentController();

        try {
            // Call the method with invalid leader
            studyGroupController.createStudyGroup(invalidLeader, newGroup, studentController);
        } catch (NullPointerException e) {
            // Assert that an exception was thrown
            assertThat(e).isNotNull();
            return;
        }

        // Fail the test if no exception is thrown
        assertThat("Expected NullPointerException was not thrown").isEmpty();
    }

    /****************** tests for getStudyGroupForLeader() ****************/
    // Test that getStudyGroupForLeader() returns correct group with valid leader
    @Test
    void testGetStudyGroupForLeaderWithValidLeader() {
        // Create a student (group leader)
        ObjectId leaderId = new ObjectId();
        Student leader = new Student();
        leader.setId(leaderId);

        // Create a study group with the leader
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(new ObjectId());
        studyGroup.setGroupLeaderId(leaderId);

        // Add study group to repository
        StudyGroupController studyGroupController = getStudyGroupController();
        studyGroupController.studyGroupRepository.add(studyGroup);

        // Call the method
        StudyGroup result = studyGroupController.getStudyGroupForLeader(leader);

        // Assert results
        assertThat(result).isNotNull();
        assertThat(result.getGroupLeaderId()).isEqualTo(leaderId);
    }

    // Test that getStudyGroupForLeader does not return groups for non-leader
    @Test
    void testGetStudyGroupForLeaderWithNonLeaderStudent() {
        // Create a student who is not a leader
        ObjectId studentId = new ObjectId();
        Student student = new Student();
        student.setId(studentId);

        // Create a study group with a different leader
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(new ObjectId());
        studyGroup.setGroupLeaderId(new ObjectId());

        StudyGroupController studyGroupController = getStudyGroupController();
        studyGroupController.studyGroupRepository.add(studyGroup);

        StudyGroup result = studyGroupController.getStudyGroupForLeader(student);

        assertThat(result).isNull();
    }

    @Test
    void testExceptionHandlingInGetStudyGroupForLeader() {
        // Create a student
        Student leader = new Student();
        leader.setId(new ObjectId());

        // Simulate an exception by overriding getAll() to throw an exception
        StudyGroupController studyGroupController = getStudyGroupController();
        studyGroupController.studyGroupRepository =
                new InMemoryRepository<StudyGroup>() {
                    @Override
                    public Collection<StudyGroup> getAll() {
                        throw new RuntimeException("Simulated repository exception");
                    }
                };

        try {
            studyGroupController.getStudyGroupForLeader(leader);
        } catch (RuntimeException e) {
            // Assert that an exception was thrown and contains the expected message
            assertThat(e).isNotNull();
            assertThat(e.getMessage()).isEqualTo("Simulated repository exception");
            return;
        }

        // Fail the test if no exception is thrown
        assertThat("Expected exception was not thrown").isEmpty();
    }

    // Test that submit application works correctly by saving application in repository
    @Test
    void testSubmitApplicationSuccessfully() {
        StudyGroupController controller = getStudyGroupController();
        StudentController studentController = getStudentController();
        MeetingController meetingController = getMeetingController();
        ReminderController reminderController = getReminderController();

        ObjectId studentId = new ObjectId();
        ObjectId groupId = new ObjectId();
        Student student = createDefaultStudent(studentId, "123456789", "Test Student");
        StudyGroup studyGroup = createDefaultStudyGroup(groupId, new ObjectId(), "Test Group");

        // Add to repositories
        studentController.studentRepository.add(student);
        controller.studyGroupRepository.add(studyGroup);

        // Submit application
        controller.submitApplication(
                studentId,
                "123456789",
                studyGroup,
                "I want to join",
                studentController,
                meetingController,
                reminderController);

        // Validate application was created
        List<GroupApplication> applications =
                List.copyOf(controller.groupApplicationRepository.getAll());
        assertThat(applications).hasSize(1);
        GroupApplication application = applications.get(0);
        assertThat(application.getSender()).isEqualTo("123456789");
        assertThat(application.getReceiver()).isEqualTo(groupId);
        assertThat(application.getMessage()).isEqualTo("I want to join");
    }

    // Test that groups with matching interest are recommended
    @Test
    void testRecommendStudyGroupsSuccessfully() {
        StudyGroupController controller = getStudyGroupController();
        StudentController studentController = getStudentController();

        ObjectId studentId = new ObjectId();
        ObjectId groupId = new ObjectId();
        Student student = createDefaultStudent(studentId, "123456789", "Test Student");
        StudyGroup studyGroup = createDefaultStudyGroup(groupId, new ObjectId(), "Test Group");

        // Add matching interest
        Interest interest =
                Interest.builder()
                        .studentInterest("Java")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();
        student.getInterestSet().add(interest);
        studyGroup.getInterestSet().add(interest);

        studentController.studentRepository.add(student);
        controller.studyGroupRepository.add(studyGroup);

        // Recommend study groups
        List<StudyGroup> recommendedGroups =
                controller.recommendStudyGroups("123456789", studentController);

        // Validate recommendation
        assertThat(recommendedGroups).hasSize(1);
        assertThat(recommendedGroups.get(0).getId()).isEqualTo(groupId);
    }

    // Test that after applicaiton is approved, the application is removed and the group is added to
    // applicant's groupList
    @Test
    void testApproveApplicationSuccessfully() {
        StudyGroupController studyGroupController = getStudyGroupController();
        StudentController studentController = getStudentController();
        MeetingController meetingController = getMeetingController();
        ReminderController reminderController = getReminderController();

        ObjectId studentId = new ObjectId();
        ObjectId groupId = new ObjectId();
        Student student = createDefaultStudent(studentId, "123456789", "Test Student");
        StudyGroup studyGroup = createDefaultStudyGroup(groupId, new ObjectId(), "Test Group");

        // Add matching interest
        Interest interest =
                Interest.builder()
                        .studentInterest("Java")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();
        student.getInterestSet().add(interest);
        studyGroup.getInterestSet().add(interest);

        // Add to repositories
        studentController.studentRepository.add(student);
        studyGroupController.studyGroupRepository.add(studyGroup);

        // Create application
        GroupApplication application =
                GroupApplication.builder()
                        .id(new ObjectId())
                        .sender(student.getDiscordUserId())
                        .message("")
                        .receiver(groupId)
                        .timestamp(LocalDateTime.now())
                        .interestSet(new HashSet<>())
                        .build();

        // Add to repository
        studyGroupController.groupApplicationRepository.add(application);

        // Approve application
        studyGroupController.approveApplication(
                application, studentController, meetingController, reminderController);

        // Validate application is removed
        assertThat(studyGroupController.groupApplicationRepository.getAll()).isEmpty();

        // Validate student is added to group
        assertThat(student.getGroupList()).contains(groupId);
    }

    // Test that study group is updated succefully
    @Test
    void testUpdateStudyGroupSuccessfully() {
        StudyGroupController controller = getStudyGroupController();

        ObjectId groupId = new ObjectId();
        StudyGroup studyGroup = createDefaultStudyGroup(groupId, new ObjectId(), "Test Group");

        // Add to repository
        controller.studyGroupRepository.add(studyGroup);

        // Update group
        studyGroup.setDescription("Updated description");
        controller.updateStudyGroup(studyGroup);

        // Validate update
        StudyGroup updatedGroup = controller.studyGroupRepository.get(groupId);
        assertThat(updatedGroup.getDescription()).isEqualTo("Updated description");
    }

    // Test that find group by application returns the correct group associated
    @Test
    void testFindGroupByApplicationSuccessfully() {
        StudyGroupController controller = getStudyGroupController();

        ObjectId groupId = new ObjectId();
        StudyGroup studyGroup = createDefaultStudyGroup(groupId, new ObjectId(), "Test Group");

        // Create application
        GroupApplication application =
                GroupApplication.builder()
                        .id(new ObjectId())
                        .sender("123456")
                        .message("")
                        .receiver(groupId)
                        .timestamp(LocalDateTime.now())
                        .interestSet(new HashSet<>())
                        .build();

        // Add to repositories
        controller.studyGroupRepository.add(studyGroup);
        controller.groupApplicationRepository.add(application);

        // Find group by application
        StudyGroup foundGroup = controller.findGroupByApplication(application);

        // Validate group is found
        assertThat(foundGroup).isNotNull();
        assertThat(foundGroup.getId()).isEqualTo(groupId);
    }

    // Helper function to create a Student object
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

    // Helper function to create a StudyGroup object
    private StudyGroup createDefaultStudyGroup(ObjectId id, ObjectId leaderId, String name) {
        return StudyGroup.builder()
                .id(id)
                .groupLeaderId(leaderId)
                .name(name)
                .description("Default description")
                .interestSet(new HashSet<>())
                .maxMembers(10)
                .autoApprove(false)
                .customCriteria("Default criteria")
                .build();
    }
    /**
     * Test for the disbandGroup method removes study group id from each member's group list and
     * delete the study group from database
     *
     * <p>Ensures the study group is correctly removed from all members' group lists and deleted
     * from the repository.
     */
    @Test
    void testDisbandGroup() {
        StudyGroupController studyGroupController = getStudyGroupController();
        StudentController studentController = getStudentController();
        ReminderController reminderController = getReminderController();
        BookingController bookingController = getBookingController();
        MeetingController meetingController = getMeetingController();

        // the group to be disbanded
        StudyGroup studyGroup = new StudyGroup();
        ObjectId studyGroupId = new ObjectId();
        studyGroup.setId(studyGroupId);
        studyGroupController.studyGroupRepository.add(studyGroup);

        // add members
        List<Student> members = new ArrayList<>();
        List<ObjectId> memberGroupList = new ArrayList<>();
        memberGroupList.add(studyGroupId);
        for (int i = 1; i <= 5; i++) {
            Student member = new Student();
            member.setGroupList(memberGroupList);
            members.add(member);
            studyGroupController.studentRepository.add(member);
            studentController.studentRepository.add(member);
        }

        // Act
        studyGroupController.disbandGroup(
                studyGroup,
                studentController,
                meetingController,
                bookingController,
                reminderController);

        // Assert
        for (int i = 0; i < 5; i++) {
            assertThat(members.get(i).getGroupList()).doesNotContain(studyGroupId);
        }
        assertThat(studyGroupController.studentRepository.get(studyGroupId)).isNull();
    }

    @Test
    void testLeaveGroup() {
        StudyGroupController studyGroupController = getStudyGroupController();
        ReminderController reminderController = getReminderController();
        BookingController bookingController = getBookingController();
        MeetingController meetingController = getMeetingController();

        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(new ObjectId());

        List<ObjectId> leaderGroupList = new ArrayList<>();
        leaderGroupList.add(studyGroup.getId());
        Student leader = new Student();
        leader.setId(new ObjectId());
        leader.setGroupList(leaderGroupList);
        List<ObjectId> groupList = new ArrayList<>();
        groupList.add(studyGroup.getId());
        Student student = new Student();
        student.setId(new ObjectId());
        student.setDiscordUserId("123456");
        student.setGroupList(groupList);

        studyGroupController.studentRepository.add(student);
        studyGroupController.studentRepository.add(leader);
        studyGroupController.studyGroupRepository.add(studyGroup);

        studyGroupController.leaveGroup(
                studyGroup, student, meetingController, bookingController, reminderController);

        assertThat(student.getGroupList()).doesNotContain(studyGroup.getId());
        assertThat(leader.getGroupList()).contains(studyGroup.getId());
        assertThat(studyGroupController.getMemberListOfStudyGroup(studyGroup)).hasSize(1);
    }
}
