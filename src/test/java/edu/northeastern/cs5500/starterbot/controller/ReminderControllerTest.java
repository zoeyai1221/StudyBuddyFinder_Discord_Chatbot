package edu.northeastern.cs5500.starterbot.controller;

import static com.google.common.truth.Truth.assertThat;

import edu.northeastern.cs5500.starterbot.model.Frequency;
import edu.northeastern.cs5500.starterbot.model.InPersonMeeting;
import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.model.OnlineMeeting;
import edu.northeastern.cs5500.starterbot.model.Reminder;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import edu.northeastern.cs5500.starterbot.model.TimeSlot;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

public class ReminderControllerTest {
    private final InMemoryRepository<Reminder> reminderRepository = new InMemoryRepository<>();
    private final InMemoryRepository<Student> studentRepository = new InMemoryRepository<>();
    private final InMemoryRepository<StudyGroup> studyGroupRepository = new InMemoryRepository<>();
    private final InMemoryRepository<OnlineMeeting> onlineMeetingRepository =
            new InMemoryRepository<>();
    private final InMemoryRepository<InPersonMeeting> inPersonMeetingRepository =
            new InMemoryRepository<>();

    private ReminderController getReminderController() {
        return new ReminderController(
                reminderRepository,
                getStudentController(),
                getMeetingController(),
                getStudyGroupController(),
                null);
    }

    private StudentController getStudentController() {
        return new StudentController(
                studentRepository, new InterestController(new InMemoryRepository<>()));
    }

    private MeetingController getMeetingController() {
        return new MeetingController(onlineMeetingRepository, inPersonMeetingRepository);
    }

    private StudyGroupController getStudyGroupController() {
        return new StudyGroupController(
                studyGroupRepository, new InMemoryRepository<>(), studentRepository);
    }

    /* Test that reminder gets successfully created and saved to repository */
    @Test
    void testCreateReminderSuccess() {
        ReminderController reminderController = getReminderController();
        MeetingController meetingController = getMeetingController();
        StudentController studentController = getStudentController();
        StudyGroupController studyGroupController = getStudyGroupController();
        ObjectId meetingId = new ObjectId();
        ObjectId studentId = new ObjectId();
        ObjectId studyGroupId = new ObjectId();
        OnlineMeeting meeting = createDefaultOnlineMeeting(meetingId, studyGroupId, studentId);
        meetingController.onlineMeetingRepository.add(meeting);

        Student student = createDefaultStudent(studentId, studyGroupId);
        studentController.studentRepository.add(student);

        StudyGroup studyGroup = createDefaultStudyGroup(studyGroupId, studentId);
        studyGroupController.studyGroupRepository.add(studyGroup);

        Reminder reminder = reminderController.createReminder(meetingId, 10, student);

        Reminder storedReminder = reminderController.reminderRepository.get(reminder.getId());
        assertThat(storedReminder).isNotNull();
        assertThat(storedReminder.getReminderDateTime())
                .isEqualTo(meeting.getTimeSlots().get(0).getStart().minusMinutes(10));
        assertThat(storedReminder.getStudent().getId()).isEqualTo(studentId);
    }

    /* Test that when user first set reminder preference, reminders get created for all meetings*/
    @Test
    void testSetReminderCreatesRemindersForExistingMeetings() {
        ReminderController reminderController = getReminderController();
        MeetingController meetingController = getMeetingController();
        StudentController studentController = getStudentController();
        StudyGroupController studyGroupController = getStudyGroupController();
        ObjectId studyGroupId = new ObjectId();
        ObjectId meetingId = new ObjectId();
        ObjectId studentId = new ObjectId();

        OnlineMeeting meeting = createDefaultOnlineMeeting(meetingId, studyGroupId, studentId);
        meetingController.onlineMeetingRepository.add(meeting);

        Student student = createDefaultStudent(studentId, studyGroupId);
        studentController.studentRepository.add(student);

        StudyGroup studyGroup = createDefaultStudyGroup(studyGroupId, studentId);
        studyGroupController.studyGroupRepository.add(studyGroup);

        reminderController.setReminder(student.getDiscordUserId(), 15);

        List<Reminder> reminders = new ArrayList<>(reminderController.reminderRepository.getAll());
        assertThat(reminders).hasSize(1);
        Reminder storedReminder = reminders.get(0);
        assertThat(storedReminder.getReminderDateTime())
                .isEqualTo(meeting.getTimeSlots().get(0).getStart().minusMinutes(15));
        assertThat(storedReminder.getStudent().getId()).isEqualTo(studentId);
    }

    /* Without reminder preference, no reminder gets created */
    @Test
    void testCreateRemindersForExistingMeetings_InvalidReminderTime() {
        ReminderController reminderController = getReminderController();
        MeetingController meetingController = getMeetingController();
        StudentController studentController = getStudentController();
        StudyGroupController studyGroupController = getStudyGroupController();
        ObjectId studyGroupId = new ObjectId();
        ObjectId meetingId = new ObjectId();
        ObjectId studentId = new ObjectId();

        OnlineMeeting meeting = createDefaultOnlineMeeting(meetingId, studyGroupId, studentId);
        meetingController.onlineMeetingRepository.add(meeting);

        StudyGroup studyGroup = createDefaultStudyGroup(studyGroupId, studentId);
        studyGroupController.studyGroupRepository.add(studyGroup);
        String studentDiscordId = "123456789";
        Student student =
                Student.builder()
                        .id(studentId)
                        .displayName("Test name")
                        .email("")
                        .interestSet(new HashSet<>())
                        .availability(new ArrayList<>())
                        .discordUserId(studentDiscordId)
                        .reminderTimeInMin(null) // Invalid null reminder time
                        .groupList(new ArrayList<>())
                        .build();
        studentController.studentRepository.add(student);

        reminderController.createRemindersForExistingMeetings(studentDiscordId);

        Collection<Reminder> reminders = reminderController.reminderRepository.getAll();
        assertThat(reminders).isEmpty();
    }

    /* Reminder should not get created when user choose no reminder */
    @Test
    void testCreateRemindersForExistingMeetings_ZeroReminderTime() {
        ReminderController reminderController = getReminderController();
        MeetingController meetingController = getMeetingController();
        StudentController studentController = getStudentController();
        StudyGroupController studyGroupController = getStudyGroupController();
        ObjectId studyGroupId = new ObjectId();
        ObjectId meetingId = new ObjectId();
        ObjectId studentId = new ObjectId();

        OnlineMeeting meeting = createDefaultOnlineMeeting(meetingId, studyGroupId, studentId);
        meetingController.onlineMeetingRepository.add(meeting);

        StudyGroup studyGroup = createDefaultStudyGroup(studyGroupId, studentId);
        studyGroupController.studyGroupRepository.add(studyGroup);
        String studentDiscordId = "123456789";
        Student student =
                Student.builder()
                        .id(studentId)
                        .displayName("Test name")
                        .email("")
                        .interestSet(new HashSet<>())
                        .availability(new ArrayList<>())
                        .discordUserId(studentDiscordId)
                        .reminderTimeInMin(0) // Invalid null reminder time
                        .build();
        studentController.studentRepository.add(student);

        reminderController.createRemindersForExistingMeetings(studentDiscordId);

        // No reminders should be created
        Collection<Reminder> reminders = reminderController.reminderRepository.getAll();
        assertThat(reminders).isEmpty();
    }

    /* Reminder gets deleted for all participants for a specific meeting */
    @Test
    void testDeleteReminderForSpecificMeeting() {
        ReminderController reminderController = getReminderController();
        MeetingController meetingController = getMeetingController();
        StudentController studentController = getStudentController();
        StudyGroupController studyGroupController = getStudyGroupController();
        ObjectId meetingId = new ObjectId();
        ObjectId studentId = new ObjectId();
        ObjectId studyGroupId = new ObjectId();

        OnlineMeeting meeting = createDefaultOnlineMeeting(meetingId, studyGroupId, studentId);
        meetingController.onlineMeetingRepository.add(meeting);

        Student student = createDefaultStudent(studentId, studyGroupId);
        studentController.studentRepository.add(student);

        StudyGroup studyGroup = createDefaultStudyGroup(studyGroupId, studentId);
        studyGroupController.studyGroupRepository.add(studyGroup);

        Reminder reminder =
                Reminder.builder()
                        .meetingId(meetingId)
                        .student(student)
                        .reminderTimeInMin(10)
                        .reminderDateTime(meeting.getTimeSlots().get(0).getStart().minusMinutes(10))
                        .message("Test Reminder")
                        .build();
        reminderController.reminderRepository.add(reminder);

        reminderController.deleteAllRemindersForSpecificMeeting(
                meeting, meeting.getTimeSlots().get(0));

        Reminder deletedReminder = reminderController.reminderRepository.get(reminder.getId());
        assertThat(deletedReminder).isNull();
    }

    @Test
    void testUpdateReminder() {
        ReminderController reminderController = getReminderController();
        MeetingController meetingController = getMeetingController();
        StudentController studentController = getStudentController();
        StudyGroupController studyGroupController = getStudyGroupController();
        ObjectId meetingId = new ObjectId();
        ObjectId studyGroupId = new ObjectId();
        ObjectId studentId = new ObjectId();

        OnlineMeeting meeting = createDefaultOnlineMeeting(meetingId, studyGroupId, studentId);
        meetingController.onlineMeetingRepository.add(meeting);

        Student student = createDefaultStudent(studentId, studyGroupId);
        studentController.studentRepository.add(student);

        StudyGroup studyGroup = createDefaultStudyGroup(studyGroupId, studentId);
        studyGroupController.studyGroupRepository.add(studyGroup);

        // Add a reminder
        Reminder reminder =
                Reminder.builder()
                        .meetingId(meetingId)
                        .student(student)
                        .reminderTimeInMin(10)
                        .reminderDateTime(meeting.getTimeSlots().get(0).getStart().minusMinutes(10))
                        .message("Test Reminder")
                        .build();
        reminderController.reminderRepository.add(reminder);

        reminderController.updateReminder(student.getDiscordUserId(), 20, false);

        // Reminder is updated with new time
        Reminder updatedReminder = reminderController.reminderRepository.get(reminder.getId());
        assertThat(updatedReminder).isNotNull();
        assertThat(updatedReminder.getReminderTimeInMin()).isEqualTo(20);
        assertThat(updatedReminder.getReminderDateTime())
                .isEqualTo(meeting.getTimeSlots().get(0).getStart().minusMinutes(20));
    }

    /* Delete all reminders for one student for the meeting series when the meeting series is canceled */
    @Test
    void testDeleteAllRemindersForMeetingSeries() {
        ReminderController reminderController = getReminderController();
        MeetingController meetingController = getMeetingController();
        StudentController studentController = getStudentController();
        ObjectId meetingId = new ObjectId();
        ObjectId studyGroupId = new ObjectId();
        ObjectId studentId = new ObjectId();

        // Create a meeting
        OnlineMeeting meeting = createRecurringOnlineMeeting(meetingId, studyGroupId, studentId);
        meetingController.onlineMeetingRepository.add(meeting);

        // Create a student
        Student student = createDefaultStudent(studentId, meeting.getStudyGroup());
        studentController.studentRepository.add(student);

        // Add reminders
        Reminder reminder1 =
                Reminder.builder()
                        .meetingId(meetingId)
                        .student(student)
                        .reminderTimeInMin(10)
                        .reminderDateTime(meeting.getTimeSlots().get(0).getStart().minusMinutes(10))
                        .message("Test Reminder 1")
                        .build();
        Reminder reminder2 =
                Reminder.builder()
                        .meetingId(meetingId)
                        .student(student)
                        .reminderTimeInMin(15)
                        .reminderDateTime(meeting.getTimeSlots().get(0).getStart().minusMinutes(15))
                        .message("Test Reminder 2")
                        .build();
        reminderController.reminderRepository.add(reminder1);
        reminderController.reminderRepository.add(reminder2);

        reminderController.deleteAllRemindersForMeetingSeries(meeting);

        assertThat(reminderController.reminderRepository.get(reminder1.getId())).isNull();
        assertThat(reminderController.reminderRepository.get(reminder2.getId())).isNull();
    }

    /* When student declines one meeting, reminder for that meeting gets deleted. */
    @Test
    void testDeleteReminderForStudentForOneMeeting() {
        ReminderController reminderController = getReminderController();
        MeetingController meetingController = getMeetingController();
        StudentController studentController = getStudentController();
        StudyGroupController studyGroupController = getStudyGroupController();
        ObjectId meetingId = new ObjectId();
        ObjectId studyGroupId = new ObjectId();
        ObjectId studentId = new ObjectId();

        // Create a meeting with two time slots
        OnlineMeeting meeting = createRecurringOnlineMeeting(meetingId, studyGroupId, studentId);
        meetingController.onlineMeetingRepository.add(meeting);

        // Create a student
        Student student = createDefaultStudent(studentId, meeting.getStudyGroup());
        studentController.studentRepository.add(student);

        // Add a reminder for the first time slot
        Reminder reminder =
                Reminder.builder()
                        .meetingId(meetingId)
                        .student(student)
                        .reminderTimeInMin(10)
                        .reminderDateTime(meeting.getTimeSlots().get(0).getStart().minusMinutes(10))
                        .message("Test Reminder")
                        .build();
        reminderController.reminderRepository.add(reminder);

        reminderController.deleteReminderForStudentForOneMeeting(
                student.getDiscordUserId(), meeting.getTimeSlots().get(0), meetingId);

        // current reminder is deleted, but next reminder is generated
        assertThat(reminderController.reminderRepository.get(reminder.getId())).isNull();
        Reminder nextReminder =
                reminderController.reminderRepository.getAll().stream().findFirst().orElse(null);
        assertThat(nextReminder).isNotNull();
        assertThat(nextReminder.getReminderTimeInMin()).isEqualTo(10);
        assertThat(nextReminder.getStudent().getId()).isEqualTo(studentId);
    }

    // Helper function to create a default online meeting object
    private OnlineMeeting createDefaultOnlineMeeting(
            ObjectId meetingId, ObjectId studyGroupId, ObjectId organizerId) {
        List<TimeSlot> meetingTime = new ArrayList<>();
        TimeSlot timeSlot1 =
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.now().plusMinutes(60))
                        .end(LocalDateTime.now().plusMinutes(120))
                        .build();
        TimeSlot timeSlot2 =
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.now().plusMinutes(60))
                        .end(LocalDateTime.now().plusMinutes(120))
                        .build();
        meetingTime.add(timeSlot1);
        meetingTime.add(timeSlot2);
        // Create a meeting using builder
        return OnlineMeeting.builder()
                .id(meetingId)
                .topic("Test Meeting")
                .frequency(Frequency.ONETIME)
                .timeSlots(meetingTime)
                .studyGroup(studyGroupId)
                .organizer(organizerId)
                .meetingLink("example.com")
                .participants(new HashMap<>())
                .build();
    }

    // Helper function to create a recurring online meeting
    private OnlineMeeting createRecurringOnlineMeeting(
            ObjectId meetingId, ObjectId studyGroupId, ObjectId organizerId) {
        List<TimeSlot> meetingTime = new ArrayList<>();
        TimeSlot timeSlot1 =
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.now().plusMinutes(60))
                        .end(LocalDateTime.now().plusMinutes(120))
                        .build();
        TimeSlot timeSlot2 =
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.now().plusMinutes(180))
                        .end(LocalDateTime.now().plusMinutes(240))
                        .build();
        meetingTime.add(timeSlot1);
        meetingTime.add(timeSlot2);
        // Create a meeting using builder
        return OnlineMeeting.builder()
                .id(meetingId)
                .topic("Test Meeting")
                .frequency(Frequency.WEEKLY)
                .timeSlots(meetingTime)
                .studyGroup(studyGroupId)
                .organizer(organizerId)
                .meetingLink("example.com")
                .participants(new HashMap<>())
                .build();
    }

    // Helper function create a default student object
    private Student createDefaultStudent(ObjectId studentId, ObjectId studyGroupId) {
        Set<Interest> interestSet = new HashSet<>();
        interestSet.add(
                Interest.builder()
                        .studentInterest("Python")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build());
        List<ObjectId> groupList = new ArrayList<>();
        groupList.add(studyGroupId);
        return Student.builder()
                .id(studentId)
                .discordUserId("123456789")
                .displayName("Test Student")
                .email("test@northeastern.edu")
                .interestSet(interestSet)
                .availability(new ArrayList<>())
                .groupList(groupList)
                .build();
    }

    // Helper function to create a StudyGroup object
    private StudyGroup createDefaultStudyGroup(ObjectId studyGroupId, ObjectId leaderId) {
        Set<Interest> interestSet = new HashSet<>();
        interestSet.add(
                Interest.builder()
                        .studentInterest("Python")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build());

        return StudyGroup.builder()
                .id(studyGroupId)
                .groupLeaderId(leaderId)
                .name("Group name")
                .description("Default description")
                .interestSet(interestSet)
                .maxMembers(10)
                .autoApprove(false)
                .customCriteria("Default criteria")
                .build();
    }

    /* Test reminder associated with the group successfully gets removed when student leaves group */
    @Test
    void testDeleteRemindersWhenStudentLeavesGroup() {
        ReminderController reminderController = getReminderController();
        MeetingController meetingController = getMeetingController();
        StudentController studentController = getStudentController();

        ObjectId meetingId1 = new ObjectId();
        ObjectId meetingId2 = new ObjectId();
        ObjectId studyGroupId1 = new ObjectId();
        ObjectId studyGroupId2 = new ObjectId();
        ObjectId studentId = new ObjectId();

        // Create meetings
        OnlineMeeting meeting1 = createDefaultOnlineMeeting(meetingId1, studyGroupId1, studentId);
        OnlineMeeting meeting2 = createDefaultOnlineMeeting(meetingId2, studyGroupId2, studentId);
        meetingController.onlineMeetingRepository.add(meeting1);
        meetingController.onlineMeetingRepository.add(meeting2);

        // Create a student
        Student student = createDefaultStudent(studentId, studyGroupId1);
        student.getGroupList().add(studyGroupId2);
        studentController.studentRepository.add(student);

        // Add reminders for two groups
        Reminder reminder1 =
                Reminder.builder()
                        .meetingId(meetingId1)
                        .student(student)
                        .reminderTimeInMin(10)
                        .reminderDateTime(
                                meeting1.getTimeSlots().get(0).getStart().minusMinutes(10))
                        .message("Test Reminder 1")
                        .build();
        Reminder reminder2 =
                Reminder.builder()
                        .meetingId(meetingId2)
                        .student(student)
                        .reminderTimeInMin(15)
                        .reminderDateTime(
                                meeting2.getTimeSlots().get(0).getStart().minusMinutes(15))
                        .message("Test Reminder 2")
                        .build();
        reminderController.reminderRepository.add(reminder1);
        reminderController.reminderRepository.add(reminder2);

        reminderController.deleteRemindersWhenStudentLeavesGroup(
                student.getDiscordUserId(), studyGroupId1);

        // Only reminder1 should be deleted since student leaves group1
        assertThat(reminderController.reminderRepository.get(reminder1.getId())).isNull();
        assertThat(reminderController.reminderRepository.get(reminder2.getId())).isNotNull();
    }
}
