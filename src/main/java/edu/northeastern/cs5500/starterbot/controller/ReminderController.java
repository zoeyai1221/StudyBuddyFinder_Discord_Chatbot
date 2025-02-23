package edu.northeastern.cs5500.starterbot.controller;

import edu.northeastern.cs5500.starterbot.model.*;
import edu.northeastern.cs5500.starterbot.repository.GenericRepository;
import edu.northeastern.cs5500.starterbot.service.OpenTelemetry;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.bson.types.ObjectId;

@Slf4j
public class ReminderController {
    GenericRepository<Reminder> reminderRepository;
    @Inject StudentController studentController;
    @Inject StudyGroupController studyGroupController;
    @Inject MeetingController meetingController;
    @Inject OpenTelemetry openTelemetry;
    @Inject JDA jda;
    private static Integer ONE_INDEX = 1;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * The ReminderController constructor
     *
     * @param reminderRepository repo for managing the reminder entities
     * @param studentController the student controller
     * @param meetingController the meeting controller
     * @param studyGroupController the study group controller
     * @param jda
     */
    @Inject
    public ReminderController(
            GenericRepository<Reminder> reminderRepository,
            StudentController studentController,
            MeetingController meetingController,
            StudyGroupController studyGroupController,
            JDA jda) {
        this.reminderRepository = reminderRepository;
        this.studentController = studentController;
        this.meetingController = meetingController;
        this.studyGroupController = studyGroupController;
        this.jda = jda;
    }
    /** Create a single reminder associated with a meeting and a student and save to repository */
    public Reminder createReminder(
            @NonNull ObjectId meetingId,
            @NonNull Integer reminderTimeInMin,
            @NonNull Student student) {
        // Fetch the meeting details using MeetingController
        AbstractMeeting meeting = meetingController.getMeetingById(meetingId);

        if (meeting.getTimeSlots() == null || meeting.getTimeSlots().isEmpty()) {
            log.warn(
                    "Meeting '{}' has no time slots, skipping reminder creation.",
                    meeting.getTopic());
            throw new IllegalArgumentException("No time slots available for the meeting");
        }

        LocalDateTime meetingStartTime = meeting.getTimeSlots().get(0).getStart();
        LocalDateTime calculatedReminderTime = meetingStartTime.minusMinutes(reminderTimeInMin);

        String formattedMessage = formatReminderMessage(meeting, reminderTimeInMin);

        // Create and return the Reminder object
        Reminder reminder =
                Reminder.builder()
                        .meetingId(meetingId) // Use meetingId
                        .reminderTimeInMin(reminderTimeInMin)
                        .reminderDateTime(calculatedReminderTime)
                        .message(formattedMessage)
                        .student(student)
                        .build();
        reminderRepository.add(reminder);
        return reminder;
    }

    /**
     * Format reminder message
     *
     * @param meeting the abstract meeting
     * @param reminderTimeInMin the reminder time in mins
     */
    public String formatReminderMessage(AbstractMeeting meeting, int reminderTimeInMin) {
        String studyGroupName =
                studyGroupController.getStudyGroupById(meeting.getStudyGroup()).getName();
        String topic = meeting.getTopic();

        // Calculate hours and minutes
        int hours = reminderTimeInMin / 60;
        int minutes = reminderTimeInMin % 60;

        String timeString =
                hours > 0
                        ? (minutes > 0
                                ? String.format(
                                        "%d hour%s %d minute%s",
                                        hours,
                                        (hours > 1 ? "s" : ""),
                                        minutes,
                                        (minutes > 1 ? "s" : ""))
                                : String.format("%d hour%s", hours, (hours > 1 ? "s" : "")))
                        : String.format("%d minute%s", minutes, (minutes > 1 ? "s" : ""));

        return String.format(
                "Hey! You have a meeting coming up in %s for the study group \"%s\" on the topic \"%s\".",
                timeString, studyGroupName, topic);
    }
    /** Scheduling reminder process every minute */
    public void start() {
        scheduler.scheduleAtFixedRate(
                () -> {
                    log.info("Scheduler heartbeat: " + LocalDateTime.now());
                    processReminders();
                },
                0,
                1,
                TimeUnit.MINUTES);
    }

    /**
     * Set reminder preference and automatically create reminders for all accepted/created meeting
     * associated with the student
     *
     * @param studentDiscordId the student discor id
     * @param reminderTimeInMin the reminder time in mins
     */
    public void setReminder(String studentDiscordId, Integer reminderTimeInMin) {
        Student student = studentController.getStudentByDiscordUserId(studentDiscordId);
        student.setReminderTimeInMin(reminderTimeInMin);
        studentController.studentRepository.update(student);
        createRemindersForExistingMeetings(studentDiscordId);
    }

    /** Fetch reminder from repository every minute to send out reminders punctually */
    public void processReminders() {
        log.info("Processing reminders");
        try {
            LocalDateTime now = LocalDateTime.now();
            Collection<Reminder> reminders = reminderRepository.getAll();
            log.info("Retrieved reminders: " + (reminders != null ? reminders.size() : 0));
            reminders.forEach(
                    reminder -> {
                        log.info("Reminder datetime: " + reminder.getReminderDateTime());
                        if (now.isAfter(reminder.getReminderDateTime())) {
                            log.info("Now: " + now);
                            log.info("Reminder time: " + reminder.getReminderDateTime());

                            AbstractMeeting meeting =
                                    meetingController.getMeetingById(reminder.getMeetingId());
                            sendReminder(reminder);

                            if (!meeting.getFrequency().equals(Frequency.ONETIME)) {
                                createNextReminder(reminder);
                            }
                            reminderRepository.delete(reminder.getId());
                        }
                    });
        } catch (Exception e) {
            log.error("Error occurred while processing reminders", e);
        }
    }

    /** Send reminder in dm to user */
    private void sendReminder(Reminder reminder) {
        Student student = reminder.getStudent();
        jda.retrieveUserById(student.getDiscordUserId())
                .queue(
                        user ->
                                user.openPrivateChannel()
                                        .flatMap(
                                                channel ->
                                                        channel.sendMessage(reminder.getMessage()))
                                        .queue());
    }

    /** Create next reminder when previous reminder is removed for recurring meetings */
    private void createNextReminder(Reminder currentReminder) {
        Student student = currentReminder.getStudent();
        ObjectId meetingId = currentReminder.getMeetingId();
        AbstractMeeting meeting = meetingController.getMeetingById(currentReminder.getMeetingId());
        TimeSlot nextTimeSlot = meeting.getTimeSlots().get(ONE_INDEX);
        LocalDateTime nextReminderTime =
                nextTimeSlot.getStart().minusMinutes(currentReminder.getReminderTimeInMin());
        // Create the next reminder
        Reminder nextReminder =
                Reminder.builder()
                        .meetingId(meetingId)
                        .reminderTimeInMin(currentReminder.getReminderTimeInMin())
                        .reminderDateTime(nextReminderTime)
                        .message(
                                currentReminder
                                        .getMessage()) // reminder message is the same for all
                        // recurring meetings
                        .student(student)
                        .build();
        reminderRepository.add(nextReminder);
    }

    /*
     * After user sets reminder preference, create reminders for all existing meetings
     * @param studentDiscordId the student discord id
     */
    public void createRemindersForExistingMeetings(String studentDiscordId) {
        // Fetch the student object using the discord ID
        Student student = studentController.getStudentByDiscordUserId(studentDiscordId);
        Integer reminderTimeInMin = student.getReminderTimeInMin();

        if (reminderTimeInMin == null || reminderTimeInMin <= 0) {
            log.info("No valid reminder time set for student: {}", studentDiscordId);
            return; // Skip creating reminders if no valid reminder preference is set
        }

        // Fetch all meetings associated with the student
        List<AbstractMeeting> meetings = meetingController.getMeetingsForStudent(student);
        log.info("Found {} meetings for student {}", meetings.size(), studentDiscordId);
        for (AbstractMeeting meeting : meetings) {
            if (meeting.getTimeSlots() == null || meeting.getTimeSlots().isEmpty()) {
                log.warn(
                        "Meeting '{}' has no time slots, skipping reminder creation.",
                        meeting.getTopic());
                continue;
            }

            // Check if a reminder already exists for this student and meeting
            boolean reminderExists =
                    reminderRepository.getAll().stream()
                            .anyMatch(
                                    reminder ->
                                            reminder.getMeetingId().equals(meeting.getId())
                                                    && reminder.getStudent()
                                                            .getDiscordUserId()
                                                            .equals(studentDiscordId));

            if (reminderExists) {
                log.info(
                        "Skipping reminder for meeting '{}', reminder already exists for '{}'.",
                        meeting.getTopic(),
                        studentDiscordId);
                continue; // Skip creating a reminder if one already exists
            }

            // Use createReminder to generate the reminder and save to repo
            Reminder reminder = createReminder(meeting.getId(), reminderTimeInMin, student);

            if (reminder.getReminderDateTime().isBefore(LocalDateTime.now())) {
                log.info(
                        "Skipping reminder for meeting '{}' because the reminder time has already passed.",
                        meeting.getTopic());
                continue; // Skip if the reminder time is already in the past
            }

            log.info(
                    "Created reminder for meeting '{}' at time: {}",
                    meeting.getTopic(),
                    reminder.getReminderDateTime());
        }
    }

    /**
     * When user update reminder preference, update all existing reminders
     *
     * @param studentDiscordId the student discord id
     * @param reminderTimeInMin the reminder time in mins
     * @param isForRecurringMeeting check if its for the recurring meeting or not
     */
    public void updateReminder(
            String studentDiscordId, Integer reminderTimeInMin, boolean isForRecurringMeeting) {
        // first change student's preference of reminder setting
        setReminder(studentDiscordId, reminderTimeInMin);
        // update all student's reminders
        List<Reminder> remindersForStudent =
                reminderRepository.getAll().stream()
                        .filter(
                                reminder ->
                                        reminder.getStudent()
                                                .getDiscordUserId()
                                                .equals(studentDiscordId))
                        .collect(Collectors.toList());

        for (Reminder reminder : remindersForStudent) {
            ObjectId meetingId = reminder.getMeetingId();
            AbstractMeeting meeting = meetingController.getMeetingById(meetingId);
            LocalDateTime updatedReminderDateTime =
                    meeting.getTimeSlots().get(0).getStart().minusMinutes(reminderTimeInMin);
            reminder.setReminderDateTime(updatedReminderDateTime);
            reminder.setReminderTimeInMin(reminderTimeInMin);

            // Update the reminder in the repository
            reminderRepository.update(reminder);
        }
    }
    /**
     * Delete reminders for one meeting instance in the recurring series
     *
     * @param meeting
     * @param timeslotToCancel
     */
    public void deleteAllRemindersForSpecificMeeting(
            AbstractMeeting meeting, TimeSlot timeslotToCancel) {
        List<TimeSlot> timeSlots = meeting.getTimeSlots();

        if (!timeSlots.contains(timeslotToCancel)) {
            log.warn(
                    "Timeslot '{}' not found in meeting '{}'.",
                    timeslotToCancel,
                    meeting.getTopic());
            return;
        }

        // Remove the reminder associated with the specific timeslot
        Reminder reminderToRemove =
                reminderRepository.getAll().stream()
                        .filter(
                                reminder ->
                                        reminder.getMeetingId().equals(meeting.getId())
                                                && reminder.getReminderDateTime()
                                                        .equals(
                                                                timeslotToCancel
                                                                        .getStart()
                                                                        .minusMinutes(
                                                                                reminder
                                                                                        .getReminderTimeInMin())))
                        .findFirst()
                        .orElse(null);

        if (reminderToRemove != null) {
            reminderRepository.delete(reminderToRemove.getId());
        } else {
            log.warn("No reminder found！");
            return;
        }

        // If the canceled timeslot is the first in the list, create a new reminder for the next
        // timeslot
        if (!timeSlots.isEmpty() && timeSlots.get(0).equals(timeslotToCancel)) {
            createNextReminder(reminderToRemove);
            log.info("New reminder created for the next timeslot");
        }
    }
    /**
     * Cancel all reminders when the whole meeting series gets canceled
     *
     * @param meeting
     */
    public void deleteAllRemindersForMeetingSeries(AbstractMeeting meeting) {
        List<Reminder> relatedReminders =
                reminderRepository.getAll().stream()
                        .filter(reminder -> reminder.getMeetingId().equals(meeting.getId()))
                        .collect(Collectors.toList());

        if (relatedReminders.isEmpty()) {
            log.warn("No reminders found for meeting series '{}'.", meeting.getTopic());
            return;
        }

        // Remove all reminders associated with the meeting series
        for (Reminder reminder : relatedReminders) {
            reminderRepository.delete(reminder.getId());
        }

        log.info(
                "All reminders for meeting series '{}' have been successfully canceled.",
                meeting.getTopic());
    }

    /**
     * Deletes all reminders associated with the meetings of a specific study group when a user
     * leaves the group.
     *
     * @param studentDiscordId
     * @param studyGroupId
     */
    public void deleteRemindersWhenStudentLeavesGroup(
            String studentDiscordId, ObjectId studyGroupId) {
        // Fetch all reminders for the student
        List<Reminder> studentReminders =
                reminderRepository.getAll().stream()
                        .filter(
                                reminder ->
                                        reminder.getStudent()
                                                .getDiscordUserId()
                                                .equals(studentDiscordId))
                        .collect(Collectors.toList());

        // Filter reminders associated with the study group
        List<Reminder> remindersToDelete =
                studentReminders.stream()
                        .filter(
                                reminder -> {
                                    try {
                                        AbstractMeeting meeting =
                                                meetingController.getMeetingById(
                                                        reminder.getMeetingId());
                                        return meeting.getStudyGroup().equals(studyGroupId);
                                    } catch (Exception e) {
                                        log.warn(
                                                "Failed to retrieve meeting for reminder ID {}: {}",
                                                reminder.getId(),
                                                e.getMessage());
                                        return false;
                                    }
                                })
                        .collect(Collectors.toList());

        if (remindersToDelete.isEmpty()) {
            log.info(
                    "No reminders found for student {} in study group {}",
                    studentDiscordId,
                    studyGroupId);
            return;
        }

        // Delete each reminder
        for (Reminder reminder : remindersToDelete) {
            reminderRepository.delete(reminder.getId());
        }

        log.info(
                "Successfully deleted all reminders for student {} in study group {}",
                studentDiscordId,
                studyGroupId);
    }

    /**
     * Delete reminder when a student declines one instance of meeting
     *
     * @param studentDiscordId the student discord id
     * @param timeSlotToDelete time slots to be deleted
     * @param meetingId the meeting id
     */
    public void deleteReminderForStudentForOneMeeting(
            String studentDiscordId, TimeSlot timeSlotToDelete, ObjectId meetingId) {
        AbstractMeeting meeting = meetingController.getMeetingById(meetingId);
        // Fetch all reminders for the student
        List<Reminder> studentReminders =
                reminderRepository.getAll().stream()
                        .filter(
                                reminder ->
                                        reminder.getStudent()
                                                .getDiscordUserId()
                                                .equals(studentDiscordId))
                        .collect(Collectors.toList());

        // Filter for reminders matching the timeslot and meeting
        Reminder reminderToDelete =
                studentReminders.stream()
                        .filter(
                                reminder ->
                                        reminder.getMeetingId().equals(meeting.getId())
                                                && reminder.getReminderDateTime()
                                                        .equals(
                                                                timeSlotToDelete
                                                                        .getStart()
                                                                        .minusMinutes(
                                                                                reminder
                                                                                        .getReminderTimeInMin())))
                        .findFirst()
                        .orElse(null);

        // Delete the reminder if found
        if (reminderToDelete != null) {
            reminderRepository.delete(reminderToDelete.getId());

            // Check if the deleted timeslot is the first one in the meeting's time slots
            if (!meeting.getTimeSlots().isEmpty()
                    && meeting.getTimeSlots().get(0).equals(timeSlotToDelete)) {
                createNextReminder(reminderToDelete);
            }
        } else {
            log.info(
                    "No matching reminder found for student {} and timeslot {} in meeting {}",
                    studentDiscordId,
                    timeSlotToDelete,
                    meeting.getTopic());
        }
    }
}
