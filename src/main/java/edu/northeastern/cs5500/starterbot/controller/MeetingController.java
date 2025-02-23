package edu.northeastern.cs5500.starterbot.controller;

import com.mongodb.MongoException;
import edu.northeastern.cs5500.starterbot.model.*;
import edu.northeastern.cs5500.starterbot.repository.GenericRepository;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import edu.northeastern.cs5500.starterbot.service.FakeOpenTelemetryService;
import edu.northeastern.cs5500.starterbot.service.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import java.util.*;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

/**
 * Represents the meeting controller class
 *
 * @author Team Wolf
 */
@Slf4j
public class MeetingController {
    GenericRepository<OnlineMeeting> onlineMeetingRepository;
    GenericRepository<InPersonMeeting> inPersonMeetingRepository;
    @Inject InMemoryRepository<AbstractMeeting> meetingMemory;
    @Inject OpenTelemetry openTelemetry;
    @Inject StudyGroupController studyGroupController;
    static final String EMPTY_STRING = "";

    /**
     * The MeetingController constructor
     *
     * @param onlineMeetingRepository repo for managing the online meeting entities
     * @param inPersonMeetingRepository repo for managing the in person meeting entities
     */
    @Inject
    public MeetingController(
            GenericRepository<OnlineMeeting> onlineMeetingRepository,
            GenericRepository<InPersonMeeting> inPersonMeetingRepository) {
        this.onlineMeetingRepository = onlineMeetingRepository;
        this.inPersonMeetingRepository = inPersonMeetingRepository;
        this.meetingMemory = new InMemoryRepository<>();
        openTelemetry = new FakeOpenTelemetryService();
    }

    /**
     * Creates a new online meeting and assigns all members of the associated study group as
     * tentative participants.
     *
     * @param newMeeting
     * @param studyGroupController
     * @param reminderController
     */
    public void createOnlineMeeting(
            OnlineMeeting newOnlineMeeting,
            StudyGroupController studyGroupController,
            ReminderController reminderController) {
        var span = openTelemetry.span("createStudyGroup");
        span.setAttribute("meetingId", newOnlineMeeting.getId().toHexString());

        try (Scope scope = span.makeCurrent()) {
            HashMap<String, AbstractMeeting.Status> participants = new HashMap<>();
            StudyGroup studyGroup =
                    studyGroupController.getStudyGroupById(newOnlineMeeting.getStudyGroup());
            List<Student> members = studyGroupController.getMemberListOfStudyGroup(studyGroup);
            for (Student member : members) {

                if (member.getId().equals(newOnlineMeeting.getOrganizer())) {
                    participants.put(member.getId().toString(), AbstractMeeting.Status.ACCEPT);
                    continue;
                }
                participants.put(member.getId().toString(), AbstractMeeting.Status.TENTATIVE);
            }
            newOnlineMeeting.setParticipants(participants);
            onlineMeetingRepository.add(newOnlineMeeting);
            createReminderHelper(
                    studyGroup, reminderController, newOnlineMeeting.getId(), studyGroupController);
            meetingMemory.delete(newOnlineMeeting.getId()); // delete from creating memory
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Creates a new online meeting and assigns all members of the associated study group as
     * tentative participants.
     *
     * @param newMeeting
     * @param studyGroupController
     * @param reminderController
     */
    public void createInPersonMeeting(
            InPersonMeeting newInPersonMeeting,
            StudyGroupController studyGroupController,
            ReminderController reminderController) {
        var span = openTelemetry.span("createStudyGroup");
        span.setAttribute("meetingId", newInPersonMeeting.getId().toHexString());

        try (Scope scope = span.makeCurrent()) {
            HashMap<String, AbstractMeeting.Status> participants = new HashMap<>();
            StudyGroup studyGroup =
                    studyGroupController.getStudyGroupById(newInPersonMeeting.getStudyGroup());
            List<Student> members = studyGroupController.getMemberListOfStudyGroup(studyGroup);
            for (Student member : members) {

                if (member.getId().equals(newInPersonMeeting.getOrganizer())) {
                    participants.put(member.getId().toString(), AbstractMeeting.Status.ACCEPT);
                    continue;
                }
                participants.put(member.getId().toString(), AbstractMeeting.Status.TENTATIVE);
            }
            newInPersonMeeting.setParticipants(participants);
            inPersonMeetingRepository.add(newInPersonMeeting);
            createReminderHelper(
                    studyGroup,
                    reminderController,
                    newInPersonMeeting.getId(),
                    studyGroupController);
            meetingMemory.delete(newInPersonMeeting.getId()); // delete from creating memory
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /** Add the new member to the meeting participant list */
    public void updateParticipantsAfterJoinGroup(
            Student member, StudyGroup studyGroup, ReminderController reminderController) {
        List<AbstractMeeting> meetings = getMeetingsForStudyGroup(studyGroup);
        for (AbstractMeeting meeting : meetings) {
            if (member.getReminderTimeInMin() != null) {
                reminderController.createReminder(
                        meeting.getId(), member.getReminderTimeInMin(), member);
            }

            HashMap<String, AbstractMeeting.Status> participants = meeting.getParticipants();
            participants.put(member.getId().toString(), AbstractMeeting.Status.TENTATIVE);
            meeting.setParticipants(participants);
            if (meeting instanceof OnlineMeeting onlineMeeting) {
                onlineMeetingRepository.update(onlineMeeting);
            } else if (meeting instanceof InPersonMeeting inPersonMeeting) {
                inPersonMeetingRepository.update(inPersonMeeting);
            }
        }
    }

    /**
     * Create the reminder while create meetings
     *
     * @param member student member
     * @param reminderController
     * @param meetingId
     */
    private void createReminderHelper(
            StudyGroup studyGroup,
            ReminderController reminderController,
            ObjectId meetingId,
            StudyGroupController studyGroupController) {
        List<Student> members = studyGroupController.getMemberListOfStudyGroup(studyGroup);
        for (Student member : members) {
            Integer reminderTimeInMin = member.getReminderTimeInMin();
            if (reminderTimeInMin != null && reminderTimeInMin > 0) {
                reminderController.createReminder(meetingId, reminderTimeInMin, member);
            }
        }
    }

    /**
     * Find online meetying by associated discord user id
     *
     * @param discordUserId discord user Id
     * @return OnlineMeeting object
     */
    @Nonnull
    public AbstractMeeting getMeetingFromMemorybyDiscordId(
            String discordUserId, StudentController studentController) {
        try {
            Student student = studentController.getStudentByDiscordUserId(discordUserId);
            Collection<AbstractMeeting> meetings = meetingMemory.getAll();
            for (AbstractMeeting meeting : meetings) {
                if (meeting.getOrganizer() != null
                        && meeting.getOrganizer().equals(student.getId())) {
                    return meeting;
                }
            }
            throw new IllegalArgumentException("The discord user does not have meeting creating.");
        } catch (MongoException e) {
            throw new RuntimeException("Failed to interact with the database.", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred:", e);
        }
    }

    /**
     * Retrieves all meetings associated with a student
     *
     * @param student The student to retrieve meetings for
     * @return List of meetings the student is involved in
     */
    public List<AbstractMeeting> getMeetingsForStudent(Student student) {
        var span = openTelemetry.span("getMeetingsForStudent");
        span.setAttribute("studentId", student.getId().toHexString());

        try (Scope scope = span.makeCurrent()) {
            List<AbstractMeeting> studentMeetings = new ArrayList<>();

            // Get all online meetings
            Collection<OnlineMeeting> onlineMeetings = onlineMeetingRepository.getAll();
            for (OnlineMeeting meeting : onlineMeetings) {
                if (isMeetingRelevantToStudent(meeting, student)) {
                    studentMeetings.add(meeting);
                }
            }

            // Get all in-person meetings
            Collection<InPersonMeeting> inPersonMeetings = inPersonMeetingRepository.getAll();
            for (InPersonMeeting meeting : inPersonMeetings) {
                if (isMeetingRelevantToStudent(meeting, student)) {
                    studentMeetings.add(meeting);
                }
            }

            return studentMeetings;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw new RuntimeException("Failed to retrieve meetings for student", e);
        } finally {
            span.end();
        }
    }

    /** Helper method to check if a meeting is relevant to a student */
    private boolean isMeetingRelevantToStudent(AbstractMeeting meeting, Student student) {
        return meeting.getOrganizer().equals(student.getId())
                || meeting.getParticipants().containsKey(student.getId().toString());
    }

    /**
     * Retrieves a meeting by its ObjectId, supporting both online and in-person meetings
     *
     * @param meetingId The ObjectId of the meeting
     * @return The meeting with the given ID
     * @throws IllegalArgumentException if no meeting is found
     */
    @Nonnull
    public AbstractMeeting getMeetingById(ObjectId meetingId) {
        var span = openTelemetry.span("getMeetingById");
        span.setAttribute("meetingId", meetingId.toHexString());

        try (Scope scope = span.makeCurrent()) {
            // First check OnlineMeetings
            Collection<OnlineMeeting> onlineMeetings = onlineMeetingRepository.getAll();
            for (OnlineMeeting meeting : onlineMeetings) {
                if (meeting.getId().equals(meetingId)) {
                    return meeting;
                }
            }

            // Then check InPersonMeetings
            return getInPersonMeetingById(meetingId);

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    @Nonnull
    public InPersonMeeting getInPersonMeetingById(ObjectId meetingId) {
        Collection<InPersonMeeting> inPersonMeetings = inPersonMeetingRepository.getAll();
        for (InPersonMeeting meeting : inPersonMeetings) {
            if (meeting.getId().equals(meetingId)) {
                return meeting;
            }
        }
        // If no meeting found
        throw new IllegalArgumentException("No meeting found with ID: " + meetingId);
    }

    /**
     * Add the meeting to memory
     *
     * @param newMeeting new abstrct meeting
     */
    public void addMeetingToMemory(AbstractMeeting newMeeting) {
        if (newMeeting != null) {
            meetingMemory.add(newMeeting);
            return;
        }
        throw new IllegalArgumentException("New meeting cannot be null");
    }

    /**
     * Delete the meeting to memory
     *
     * @param meetingId the meeting id
     */
    public void deleteMeetingFromMemory(ObjectId meetingId) {
        if (meetingId != null) {
            meetingMemory.delete(meetingId);
            return;
        }
        throw new IllegalArgumentException("This meeting does not exist.");
    }

    /**
     * Update the meeting to memory
     *
     * @param meetingId new abstrct meeting
     */
    public void updateMeetingToMemory(AbstractMeeting meeting) {
        if (meeting != null) {
            meetingMemory.update(meeting);
            return;
        }
        throw new IllegalArgumentException("This meeting does not exist.");
    }

    /**
     * Cancels a meeting by its ID.
     *
     * @param meetingId the ID of the meeting to cancel
     */
    public void cancelMeeting(
            AbstractMeeting meeting,
            BookingController bookingController,
            ReminderController reminderController) {
        var span = openTelemetry.span("cancelMeeting");
        span.setAttribute("meetingId", meeting.getId().toHexString());

        try (Scope scope = span.makeCurrent()) {

            reminderController.deleteAllRemindersForMeetingSeries(meeting);

            // Remove the meeting from the appropriate repository
            if (meeting instanceof OnlineMeeting onlineMeeting) {
                onlineMeetingRepository.delete(onlineMeeting.getId());
            } else if (meeting instanceof InPersonMeeting inPersonMeeting) {
                // cancel the booking related to the meeingt
                Booking booking = bookingController.getBookingForMeeting(meeting);
                if (booking != null) {
                    bookingController.cancelBooking(booking, this);
                }

                inPersonMeetingRepository.delete(inPersonMeeting.getId());
            }

            log.info("Meeting {} has been canceled", meeting.getId());

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw new RuntimeException("Failed to cancel meeting", e);
        } finally {
            span.end();
        }
    }

    /**
     * Retrieves all meetings associated with a StudyGroup
     *
     * @param StudyGroup The StudyGroup to retrieve meetings for
     * @return List of meetings the StudyGroup is involved in
     */
    public List<AbstractMeeting> getMeetingsForStudyGroup(StudyGroup studyGroup) {
        var span = openTelemetry.span("getMeetingsForStudent");
        span.setAttribute("studyGroupId", studyGroup.getId().toHexString());

        List<AbstractMeeting> studyGroupMeetings = new ArrayList<>();

        studyGroupMeetings.addAll(getOnlineMeetingsForStudyGroup(studyGroup));
        studyGroupMeetings.addAll(getInPersonMeetingsForStudyGroup(studyGroup));

        return studyGroupMeetings;
    }

    /**
     * Retrieves all online meetings associated with a StudyGroup
     *
     * @param StudyGroup The StudyGroup to retrieve meetings for
     * @return List of meetings the StudyGroup is involved in
     */
    public List<OnlineMeeting> getOnlineMeetingsForStudyGroup(StudyGroup studyGroup) {
        var span = openTelemetry.span("getMeetingsForStudent");
        span.setAttribute("studyGroupId", studyGroup.getId().toHexString());

        List<OnlineMeeting> studyGroupMeetings = new ArrayList<>();

        // Get all online meetings
        Collection<OnlineMeeting> onlineMeetings = onlineMeetingRepository.getAll();
        for (OnlineMeeting meeting : onlineMeetings) {
            if (isMeetingRelevantToStudyGroup(meeting, studyGroup)) {
                studyGroupMeetings.add(meeting);
            }
        }

        return studyGroupMeetings;
    }

    /**
     * Retrieves all in-personmeetings associated with a StudyGroup
     *
     * @param StudyGroup The StudyGroup to retrieve meetings for
     * @return List of meetings the StudyGroup is involved in
     */
    public List<InPersonMeeting> getInPersonMeetingsForStudyGroup(StudyGroup studyGroup) {
        var span = openTelemetry.span("getMeetingsForStudent");
        span.setAttribute("studyGroupId", studyGroup.getId().toHexString());

        List<InPersonMeeting> studyGroupMeetings = new ArrayList<>();

        // Get all in-person meetings
        Collection<InPersonMeeting> inPersonMeetings = inPersonMeetingRepository.getAll();
        for (InPersonMeeting meeting : inPersonMeetings) {
            if (isMeetingRelevantToStudyGroup(meeting, studyGroup)) {
                studyGroupMeetings.add(meeting);
            }
        }

        return studyGroupMeetings;
    }

    /** Helper method to check if a meeting is relevant to a student */
    private boolean isMeetingRelevantToStudyGroup(AbstractMeeting meeting, StudyGroup studyGroup) {
        return meeting.getStudyGroup().equals(studyGroup.getId());
    }

    /**
     * Removes a student from all meetings and reminders in a study group when they leave the group.
     *
     * <p>If the student is the organizer of a meeting, the meeting is canceled. If the student is a
     * participant in a meeting, they are removed from the list of participants. The method also
     * deletes any reminders associated with the student for the study group.
     *
     * @param studyGroup the study group from which the student is leaving
     * @param student the student who is leaving the study group
     * @param reminderController the controller for managing reminders, used to delete reminders for
     *     the student
     * @param bookingController the controller for managing bookings, used to cancel meetings when
     *     necessary
     */
    public void removeParticipantWhenLeaveGroup(
            StudyGroup studyGroup,
            Student student,
            ReminderController reminderController,
            BookingController bookingController) {
        List<OnlineMeeting> onlineMeetings = getOnlineMeetingsForStudyGroup(studyGroup);
        for (OnlineMeeting onlineMeeting : onlineMeetings) {
            if (onlineMeeting.getOrganizer().equals(student.getId())) {
                cancelMeeting(onlineMeeting, bookingController, reminderController);
            } else {
                HashMap<String, AbstractMeeting.Status> participants =
                        onlineMeeting.getParticipants();
                participants.remove(student.getId().toString());
                onlineMeeting.setParticipants(participants);
                onlineMeetingRepository.update(onlineMeeting);
            }
        }

        List<InPersonMeeting> inPersonMeetings = getInPersonMeetingsForStudyGroup(studyGroup);
        for (InPersonMeeting inPersonMeeting : inPersonMeetings) {
            if (inPersonMeeting.getOrganizer().equals(student.getId())) {
                cancelMeeting(inPersonMeeting, bookingController, reminderController);
            } else {
                HashMap<String, AbstractMeeting.Status> participants =
                        inPersonMeeting.getParticipants();
                participants.remove(student.getId().toString());
                inPersonMeeting.setParticipants(participants);
                inPersonMeetingRepository.update(inPersonMeeting);
            }
        }

        reminderController.deleteRemindersWhenStudentLeavesGroup(
                student.getDiscordUserId(), studyGroup.getId());
    }

    /**
     * Udate the meeting status for student only
     *
     * @param meetingId The ObjectId of the meeting
     * @param studentId The ObjectId of the student
     * @param status meeting status
     * @throws IllegalArgumentException if no meeting is found
     */
    public void updateMeetingStatus(
            ObjectId meetingId, ObjectId studentId, AbstractMeeting.Status status) {
        var span = openTelemetry.span("updateMeetingStatus");
        AbstractMeeting abstractMeeting = getMeetingById(meetingId);
        try (Scope scope = span.makeCurrent()) {
            if (abstractMeeting.getParticipants().containsKey(studentId.toString())) {
                abstractMeeting.getParticipants().put(studentId.toString(), status);
                if (abstractMeeting instanceof OnlineMeeting) {
                    onlineMeetingRepository.update((OnlineMeeting) abstractMeeting);
                } else {
                    inPersonMeetingRepository.update((InPersonMeeting) abstractMeeting);
                }
            } else {
                throw new IllegalArgumentException("Student is not in this meeting!");
            }
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    public void updateInPersonMeetingBooking(InPersonMeeting inPersonMeeting, Booking booking) {
        inPersonMeeting.setBooking(booking);
        inPersonMeetingRepository.update(inPersonMeeting);
    }
}
