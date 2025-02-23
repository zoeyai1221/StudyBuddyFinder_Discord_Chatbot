package edu.northeastern.cs5500.starterbot.controller;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import edu.northeastern.cs5500.starterbot.model.AbstractMeeting;
import edu.northeastern.cs5500.starterbot.model.Booking;
import edu.northeastern.cs5500.starterbot.model.Frequency;
import edu.northeastern.cs5500.starterbot.model.GroupApplication;
import edu.northeastern.cs5500.starterbot.model.InPersonMeeting;
import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.model.OnlineMeeting;
import edu.northeastern.cs5500.starterbot.model.Room;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import edu.northeastern.cs5500.starterbot.model.TimeSlot;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

class MeetingControllerTest {
    // Shared repositories
    private final InMemoryRepository<Student> studentRepository = new InMemoryRepository<>();
    private final InMemoryRepository<StudyGroup> studyGroupRepository = new InMemoryRepository<>();
    private final InMemoryRepository<GroupApplication> groupApplicationRepository =
            new InMemoryRepository<>();
    private final InMemoryRepository<Interest> interestRepository = new InMemoryRepository<>();

    private MeetingController getMeetingController() {
        return new MeetingController(new InMemoryRepository<>(), new InMemoryRepository<>());
    }

    private StudyGroupController getStudyGroupController() {
        return new StudyGroupController(
                studyGroupRepository, groupApplicationRepository, studentRepository);
    }

    private StudentController getStudentController() {
        return new StudentController(studentRepository, new InterestController(interestRepository));
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

    /**
     * Test the `createOnlineMeeting` method to ensure it adds a new online meeting to the
     * repository and sets all members of the associated study group as tentative participants.
     * Verify that the meeting's participants are initialized correctly and the meeting is stored in
     * the repository.
     */
    @Test
    void testCreateOnlineMeetingSuccess() {
        // setup
        StudyGroupController studyGroupController = getStudyGroupController();
        MeetingController meetingController = getMeetingController();
        ReminderController reminderController = getReminderController();

        ObjectId studyGroupId = new ObjectId();
        ObjectId meetingId = new ObjectId();
        OnlineMeeting newMeeting =
                new OnlineMeeting(
                        meetingId,
                        "Test Meeting",
                        Frequency.ONETIME,
                        new ArrayList<>(),
                        studyGroupId,
                        new ObjectId(),
                        "https://example.com",
                        new HashMap<>());

        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(studyGroupId);
        studyGroupController.studyGroupRepository.add(studyGroup);

        List<ObjectId> groupList = new ArrayList<>();
        groupList.add(studyGroupId);
        Student student1 = new Student();
        student1.setId(new ObjectId());
        student1.setGroupList(groupList);
        studyGroupController.studentRepository.add(student1);
        Student student2 = new Student();
        student2.setId(new ObjectId());
        student2.setGroupList(groupList);
        studyGroupController.studentRepository.add(student2);

        // Act createMeeting method
        meetingController.createOnlineMeeting(newMeeting, studyGroupController, reminderController);

        // Assert the result

        // retrieve
        AbstractMeeting retrieveMeeting = meetingController.onlineMeetingRepository.get(meetingId);
        HashMap<String, AbstractMeeting.Status> participants = retrieveMeeting.getParticipants();
        assertThat(participants).hasSize(2);
        assertThat(participants.get(student1.getId().toString()))
                .isEqualTo(AbstractMeeting.Status.TENTATIVE);
        assertThat(participants.get(student2.getId().toString()))
                .isEqualTo(AbstractMeeting.Status.TENTATIVE);
    }

    /**
     * Test the `createInPersonMeeting` method to ensure it adds a new in-person meeting to the
     * repository and sets all members of the associated study group as tentative participants.
     * Verify that the meeting's participants are initialized correctly and the meeting is stored in
     * the repository.
     */
    @Test
    void testCreateInPersonMeetingSuccess() {
        // setup
        StudyGroupController studyGroupController = getStudyGroupController();
        MeetingController meetingController = getMeetingController();
        ReminderController reminderController = getReminderController();

        ObjectId studyGroupId = new ObjectId();
        ObjectId meetingId = new ObjectId();
        InPersonMeeting newMeeting =
                new InPersonMeeting(
                        meetingId,
                        "Test Meeting",
                        Frequency.ONETIME,
                        new ArrayList<>(),
                        studyGroupId,
                        new ObjectId(),
                        new Booking(),
                        new HashMap<>());

        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(studyGroupId);
        studyGroupController.studyGroupRepository.add(studyGroup);

        List<ObjectId> groupList = new ArrayList<>();
        groupList.add(studyGroupId);
        Student student1 = new Student();
        student1.setId(new ObjectId());
        student1.setGroupList(groupList);
        studyGroupController.studentRepository.add(student1);
        Student student2 = new Student();
        student2.setId(new ObjectId());
        student2.setGroupList(groupList);
        studyGroupController.studentRepository.add(student2);

        // Act createMeeting method
        meetingController.createInPersonMeeting(
                newMeeting, studyGroupController, reminderController);

        // Assert the result

        // retrieve
        AbstractMeeting retrieveMeeting =
                meetingController.inPersonMeetingRepository.get(meetingId);
        HashMap<String, AbstractMeeting.Status> participants = retrieveMeeting.getParticipants();
        assertThat(participants).hasSize(2);
        assertThat(participants.get(student1.getId().toString()))
                .isEqualTo(AbstractMeeting.Status.TENTATIVE);
        assertThat(participants.get(student2.getId().toString()))
                .isEqualTo(AbstractMeeting.Status.TENTATIVE);
    }

    /**
     * Test the `getMeetingbyDiscordId` method to ensure it returns the correct meeting associated
     * with the organizer's Discord user ID.
     *
     * <p>Purpose: Verify that the method retrieves a meeting where the organizer matches the
     * provided Discord user ID.
     */
    @Test
    void testGetMeetingFromMemorybyDiscordIdValid() {
        StudentController studentController = getStudentController();
        MeetingController meetingController = getMeetingController();

        String discordUserId = "test-user";
        ObjectId studentId = new ObjectId();
        Student student = new Student();
        student.setId(studentId);
        student.setDiscordUserId(discordUserId);
        studentController.studentRepository.add(student);

        AbstractMeeting meeting =
                new OnlineMeeting(
                        new ObjectId(),
                        "Test Meeting",
                        Frequency.ONETIME,
                        new ArrayList<>(),
                        new ObjectId(),
                        studentId,
                        "https://example.com",
                        new HashMap<>());
        meetingController.meetingMemory.add(meeting);

        // Act
        AbstractMeeting result =
                meetingController.getMeetingFromMemorybyDiscordId(discordUserId, studentController);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrganizer()).isEqualTo(studentId);
    }

    @Test
    void testAddMeetingToMemorySuccess() {
        MeetingController meetingController = getMeetingController();
        ObjectId meetingId = new ObjectId();
        AbstractMeeting meeting =
                new OnlineMeeting(
                        meetingId,
                        "Test Meeting",
                        Frequency.ONETIME,
                        new ArrayList<>(),
                        new ObjectId(),
                        new ObjectId(),
                        "https://example.com",
                        new HashMap<>());
        meetingController.addMeetingToMemory(meeting);

        AbstractMeeting retrieveMeeting = meetingController.meetingMemory.get(meetingId);
        assertThat(retrieveMeeting).isNotNull();
    }

    @Test
    void testDeleteMeetingToMemorySuccess() {
        MeetingController meetingController = getMeetingController();
        ObjectId meetingId = new ObjectId();
        AbstractMeeting meeting =
                new OnlineMeeting(
                        meetingId,
                        "Test Meeting",
                        Frequency.ONETIME,
                        new ArrayList<>(),
                        new ObjectId(),
                        new ObjectId(),
                        "https://example.com",
                        new HashMap<>());
        meetingController.meetingMemory.add(meeting);
        meetingController.deleteMeetingFromMemory(meetingId);

        assertThat(meetingController.meetingMemory.getAll()).isEmpty();
    }

    @Test
    void testUpdateMeetingToMemorySuccess() {
        MeetingController meetingController = getMeetingController();
        ObjectId meetingId = new ObjectId();
        AbstractMeeting meeting =
                new OnlineMeeting(
                        meetingId,
                        "Test Meeting",
                        Frequency.ONETIME,
                        new ArrayList<>(),
                        new ObjectId(),
                        new ObjectId(),
                        "https://example.com",
                        new HashMap<>());
        meetingController.meetingMemory.add(meeting);

        meeting.setTopic("Test Test");
        meetingController.updateMeetingToMemory(meeting);

        AbstractMeeting retrieveMeeting = meetingController.meetingMemory.get(meetingId);
        assertThat(retrieveMeeting.getTopic()).isEqualTo("Test Test");
    }

    /** Tests the retrieval of all meetings associated with a specific student */
    @Test
    void testGetMeetingsForStudent() {
        MeetingController meetingController = getMeetingController();
        // Arrange
        Student student = new Student();
        student.setId(new ObjectId());
        studentRepository.add(student);

        OnlineMeeting onlineMeeting = new OnlineMeeting();
        onlineMeeting.setId(new ObjectId());
        onlineMeeting.setOrganizer(student.getId());

        InPersonMeeting inPersonMeeting = new InPersonMeeting();
        inPersonMeeting.setId(new ObjectId());
        inPersonMeeting.setOrganizer(student.getId());

        meetingController.onlineMeetingRepository.add(onlineMeeting);
        meetingController.inPersonMeetingRepository.add(inPersonMeeting);

        // Act
        List<AbstractMeeting> meetings = meetingController.getMeetingsForStudent(student);

        // Assert
        assertNotNull(meetings);
        assertEquals(2, meetings.size());
        assertTrue(meetings.contains(onlineMeeting));
        assertTrue(meetings.contains(inPersonMeeting));
    }

    /** Tests retrieval of a meeting by its ObjectId and handles non-existent IDs */
    @Test
    void testGetMeetingById() {
        MeetingController meetingController = getMeetingController();
        // Arrange
        OnlineMeeting onlineMeeting = new OnlineMeeting();
        onlineMeeting.setId(new ObjectId());
        meetingController.onlineMeetingRepository.add(onlineMeeting);

        InPersonMeeting inPersonMeeting = new InPersonMeeting();
        inPersonMeeting.setId(new ObjectId());
        meetingController.inPersonMeetingRepository.add(inPersonMeeting);

        // Act
        AbstractMeeting retrievedOnlineMeeting =
                meetingController.getMeetingById(onlineMeeting.getId());
        AbstractMeeting retrievedInPersonMeeting =
                meetingController.getMeetingById(inPersonMeeting.getId());

        // Assert
        assertNotNull(retrievedOnlineMeeting);
        assertEquals(onlineMeeting.getId(), retrievedOnlineMeeting.getId());

        assertNotNull(retrievedInPersonMeeting);
        assertEquals(inPersonMeeting.getId(), retrievedInPersonMeeting.getId());

        // Test for non-existent ID
        ObjectId nonExistentId = new ObjectId();
        Exception exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> meetingController.getMeetingById(nonExistentId));
        assertEquals("No meeting found with ID: " + nonExistentId, exception.getMessage());
    }

    @Test
    void testRemoveParticipantWhenLeaveGroup() {
        // Setup
        MeetingController meetingController = getMeetingController();
        ReminderController reminderController = getReminderController();
        BookingController bookingController = getBookingController();

        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(new ObjectId());

        Student student = new Student();
        student.setId(new ObjectId());
        student.setDiscordUserId("123456");

        OnlineMeeting onlineMeeting1 = new OnlineMeeting();
        onlineMeeting1.setId(new ObjectId());
        onlineMeeting1.setOrganizer(student.getId());
        onlineMeeting1.setParticipants(new HashMap<>());
        onlineMeeting1.setStudyGroup(studyGroup.getId());

        OnlineMeeting onlineMeeting2 = new OnlineMeeting();
        onlineMeeting2.setId(new ObjectId());
        onlineMeeting2.setOrganizer(new ObjectId());
        onlineMeeting2.setParticipants(
                new HashMap<>() {
                    {
                        put(student.getId().toString(), AbstractMeeting.Status.ACCEPT);
                    }
                });
        onlineMeeting2.setStudyGroup(studyGroup.getId());

        List<TimeSlot> timeSlots1 = new ArrayList<>();
        timeSlots1.add(
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.of(2023, 12, 11, 10, 0))
                        .end(LocalDateTime.of(2023, 12, 11, 11, 0))
                        .build());
        List<TimeSlot> timeSlots2 = new ArrayList<>();
        timeSlots2.add(
                TimeSlot.builder()
                        .day("Tuesday")
                        .start(LocalDateTime.of(2023, 12, 12, 12, 0))
                        .end(LocalDateTime.of(2023, 12, 12, 13, 0))
                        .build());
        HashSet<TimeSlot> bookedSlots = new HashSet<>();
        bookedSlots.add(timeSlots1.get(0));
        bookedSlots.add(timeSlots2.get(0));
        Room room =
                Room.builder()
                        .id(new ObjectId())
                        .location("Room 2")
                        .capacity(10)
                        .bookedSlots(bookedSlots)
                        .build();
        bookingController.roomRepository.add(room);

        InPersonMeeting inPersonMeeting1 = new InPersonMeeting();
        inPersonMeeting1.setId(new ObjectId());
        inPersonMeeting1.setOrganizer(student.getId());
        inPersonMeeting1.setParticipants(new HashMap<>());
        inPersonMeeting1.setStudyGroup(studyGroup.getId());
        inPersonMeeting1.setTimeSlots(timeSlots1);

        Booking booking1 =
                Booking.builder()
                        .inPersonMeetingId(inPersonMeeting1.getId())
                        .roomId(room.getId())
                        .studentId(inPersonMeeting1.getOrganizer())
                        .build();
        inPersonMeeting1.setBooking(booking1);

        InPersonMeeting inPersonMeeting2 = new InPersonMeeting();
        inPersonMeeting2.setId(new ObjectId());
        inPersonMeeting2.setOrganizer(new ObjectId());
        Booking booking2 =
                Booking.builder()
                        .inPersonMeetingId(inPersonMeeting2.getId())
                        .roomId(room.getId())
                        .studentId(inPersonMeeting2.getOrganizer())
                        .build();
        inPersonMeeting2.setBooking(booking2);
        inPersonMeeting2.setParticipants(
                new HashMap<>() {
                    {
                        put(student.getId().toString(), AbstractMeeting.Status.ACCEPT);
                    }
                });
        inPersonMeeting2.setStudyGroup(studyGroup.getId());

        inPersonMeeting2.setTimeSlots(timeSlots2);

        meetingController.inPersonMeetingRepository.add(inPersonMeeting1);
        meetingController.inPersonMeetingRepository.add(inPersonMeeting2);
        meetingController.onlineMeetingRepository.add(onlineMeeting1);
        meetingController.onlineMeetingRepository.add(onlineMeeting2);
        bookingController.bookingRepository.add(booking1);
        bookingController.bookingRepository.add(booking2);

        // Act
        meetingController.removeParticipantWhenLeaveGroup(
                studyGroup, student, reminderController, bookingController);

        // Verify student is removed from onlineMeeting2
        assertThat(onlineMeeting2.getParticipants().containsKey(student.getId().toString()))
                .isFalse();

        // Verify student is removed from inPersonMeeting2
        assertThat(inPersonMeeting2.getParticipants().containsKey(student.getId().toString()))
                .isFalse();
    }

    @Test
    void testUpdateMeetingStatus() {
        MeetingController meetingController = getMeetingController();
        ObjectId meetingId = new ObjectId();
        ObjectId studentId = new ObjectId();
        // Arrange
        OnlineMeeting onlineMeeting = new OnlineMeeting();
        onlineMeeting.setId(meetingId);
        onlineMeeting.setParticipants(
                new HashMap<>() {
                    {
                        put(studentId.toString(), AbstractMeeting.Status.TENTATIVE);
                    }
                });
        meetingController.onlineMeetingRepository.add(onlineMeeting);

        // Act
        meetingController.updateMeetingStatus(meetingId, studentId, AbstractMeeting.Status.ACCEPT);

        // Assert
        OnlineMeeting newMeeting = meetingController.onlineMeetingRepository.get(meetingId);
        assertNotNull(newMeeting);
        assertEquals(
                AbstractMeeting.Status.ACCEPT,
                newMeeting.getParticipants().get(studentId.toString()));

        // Test for no student
        ObjectId meetingId2 = new ObjectId();
        ObjectId studentId2 = new ObjectId();
        onlineMeeting.setId(meetingId2);
        onlineMeeting.setParticipants(new HashMap<>());
        meetingController.onlineMeetingRepository.add(onlineMeeting);
        Exception exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                meetingController.updateMeetingStatus(
                                        meetingId2, studentId2, AbstractMeeting.Status.ACCEPT));
        assertEquals("Student is not in this meeting!", exception.getMessage());
    }
}
