package edu.northeastern.cs5500.starterbot.controller;

import com.mongodb.MongoException;
import edu.northeastern.cs5500.starterbot.model.*;
import edu.northeastern.cs5500.starterbot.repository.GenericRepository;
import edu.northeastern.cs5500.starterbot.service.FakeOpenTelemetryService;
import edu.northeastern.cs5500.starterbot.service.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import java.time.LocalDateTime;
import java.util.*;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

/**
 * Represents the study group controller class
 *
 * @author Team Wolf
 */
@Slf4j
public class StudyGroupController {
    GenericRepository<StudyGroup> studyGroupRepository;
    GenericRepository<GroupApplication> groupApplicationRepository;
    GenericRepository<Student> studentRepository;
    @Inject OpenTelemetry openTelemetry;
    static final String EMPTY_STRING = "";

    /**
     * The StudyGroupController constructor
     *
     * @param studyGroupRepository repo for managing the study group entities
     * @param groupApplicationRepository repo for managing the group application entities
     * @param studentRepository repo for managing the student entities
     */
    @Inject
    public StudyGroupController(
            GenericRepository<StudyGroup> studyGroupRepository,
            GenericRepository<GroupApplication> groupApplicationRepository,
            GenericRepository<Student> studentRepository) {
        this.studyGroupRepository = studyGroupRepository;
        this.groupApplicationRepository = groupApplicationRepository;
        this.studentRepository = studentRepository;

        if (studyGroupRepository.count() == 0) {
            StudyGroup studyGroup =
                    StudyGroup.builder()
                            .name("Study Group")
                            .interestSet(new HashSet<>())
                            .description("CS 5500 Study Group")
                            .autoApprove(true)
                            .maxMembers(10)
                            .groupLeaderId(new ObjectId())
                            .customCriteria("Sample Criteria")
                            .channelId(EMPTY_STRING)
                            .build();
            studyGroupRepository.add(studyGroup);
        }

        openTelemetry = new FakeOpenTelemetryService();
    }

    /**
     * Create a study group
     *
     * @param leader the student leader
     * @param newGroup the study group
     * @param studentController the student controller
     * @return the new study group
     */
    public StudyGroup createStudyGroup(
            Student leader, StudyGroup newGroup, StudentController studentController) {

        var span = openTelemetry.span("createStudyGroup");
        span.setAttribute("groupId", newGroup.getId().toHexString());

        try (Scope scope = span.makeCurrent()) {

            studyGroupRepository.add(newGroup);
            addStudyGroup(leader, newGroup, studentController);
            return newGroup;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Update the study group
     *
     * @param newGroup the study group
     * @return the study group
     */
    public StudyGroup updateStudyGroup(StudyGroup newGroup) {
        var span = openTelemetry.span("updateStudyGroup");
        span.setAttribute("groupId", newGroup.getId().toHexString());

        try (Scope scope = span.makeCurrent()) {
            studyGroupRepository.update(newGroup);
            return newGroup;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Recommend study group based on the user's preferences
     *
     * @param discordUserId discord user id
     * @param studentController student contoller class
     * @return recommend study group
     */
    public List<StudyGroup> recommendStudyGroups(
            String discordUserId, StudentController studentController) {
        var span = openTelemetry.span("recommendStudyGroups");
        span.setAttribute("discordUserId", discordUserId);

        try (Scope scope = span.makeCurrent()) {
            Student student = studentController.getStudentByDiscordUserId(discordUserId);
            List<StudyGroup> recommendList = new ArrayList<>();
            for (StudyGroup group : studyGroupRepository.getAll()) {
                if (matchedGroup(student, group)) {
                    recommendList.add(group);
                }
            }
            return recommendList;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Submit application to join the study group
     *
     * @param studentId student id
     * @param discordUserId discord user id
     * @param studyGroup the study group
     * @param message message
     * @param studentController student contoller class
     */
    public void submitApplication(
            ObjectId studentId,
            String discordUserId,
            StudyGroup studyGroup,
            String message,
            StudentController studentController,
            MeetingController meetingController,
            ReminderController reminderController) {
        var span = openTelemetry.span("submitApplication");
        span.setAttribute("studentId", studentId.toHexString());
        span.setAttribute("discordUserId", discordUserId);
        span.setAttribute("groupId", studyGroup.getId().toHexString());

        try (Scope scope = span.makeCurrent()) {
            Student student = studentController.getStudentByDiscordUserId(discordUserId);
            if (!studyGroup.isAutoApprove()) {
                GroupApplication application =
                        GroupApplication.builder()
                                .id(new ObjectId())
                                .sender(discordUserId)
                                .receiver(studyGroup.getId())
                                .timestamp(LocalDateTime.now())
                                .interestSet(
                                        studentController.getInterestsForStudent(discordUserId))
                                .message(message)
                                .build();
                groupApplicationRepository.add(application);
            } else {
                addStudyGroup(student, studyGroup, studentController);
                meetingController.updateParticipantsAfterJoinGroup(
                        student, studyGroup, reminderController);
            }
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Approve a study group application request
     *
     * @param application group application
     * @param studentController student contoller class
     */
    public void approveApplication(
            GroupApplication application,
            StudentController studentController,
            MeetingController meetingController,
            ReminderController reminderController) {
        var span = openTelemetry.span("approveApplication");
        span.setAttribute("applicationId", application.getId().toHexString());

        try (Scope scope = span.makeCurrent()) {
            Student student = studentController.getStudentByDiscordUserId(application.getSender());
            StudyGroup studyGroup = studyGroupRepository.get(application.getReceiver());
            if (student != null && studyGroup != null) {
                addStudyGroup(student, studyGroup, studentController);
                meetingController.updateParticipantsAfterJoinGroup(
                        student, studyGroup, reminderController);
                groupApplicationRepository.delete(application.getId());
            }
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Add the student to the study group that has available spots
     *
     * @param student group application student submitted
     * @param studyGroup the study group
     * @param studentController student contoller class
     * @return the study group
     */
    public void addStudyGroup(
            Student student, StudyGroup studyGroup, StudentController studentController) {
        if (studyGroup.getMaxMembers() == null
                || getMemberListOfStudyGroup(studyGroup).size() < studyGroup.getMaxMembers()) {
            student.getGroupList().add(studyGroup.getId());
            studentController.updateStudent(student);
        }
    }

    /**
     * Find the group by application
     *
     * @param groupApplication group application student submitted
     * @return the study group
     */
    public StudyGroup findGroupByApplication(GroupApplication groupApplication) {
        return studyGroupRepository.get(groupApplication.getReceiver());
    }

    /**
     * Check if the study group that is no full and the student has not joined matched with the
     * student's interest
     *
     * @param student student with the same intrest
     * @param studyGroup the study group
     * @return true if the group matched with the student
     */
    public boolean matchedGroup(Student student, StudyGroup studyGroup) {
        boolean hasMatchingInterest =
                !Collections.disjoint(student.getInterestSet(), studyGroup.getInterestSet());
        boolean isNotInGroupList = !student.getGroupList().contains(studyGroup.getId());
        boolean groupNotFull =
                getMemberListOfStudyGroup(studyGroup).size() < studyGroup.getMaxMembers();
        return hasMatchingInterest && isNotInGroupList && groupNotFull;
    }

    /**
     * Get the member list of the study group
     *
     * @param studyGroup the study group whose members are to be retrieved
     * @return a list of students who are members of the specified study group
     * @throws Exception
     */
    public List<Student> getMemberListOfStudyGroup(StudyGroup studyGroup) {
        var span = openTelemetry.span("getMemberListOfStudyGroup");
        span.setAttribute("studyGroupId", studyGroup.getId().toString());

        try (Scope scope = span.makeCurrent()) {
            // Filter students to list
            List<Student> memberList =
                    studentRepository.getAll().stream()
                            .filter(
                                    student -> {
                                        return new HashSet<>(student.getGroupList())
                                                .contains(studyGroup.getId());
                                    })
                            .toList();
            span.setAttribute("memberCount", memberList.size());
            return memberList;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Retrieves a StudyGroup object by its unique identifier.
     *
     * @param groupId the unique identifier of the study group to retrieve.
     * @return the {@link StudyGroup} object if found.
     * @throws IllegalArgumentException if no study group is found with the specified ID.
     * @throws RuntimeException if an error occurs while interacting with the database or any
     *     unexpected error occurs.
     */
    public StudyGroup getStudyGroupById(ObjectId groupId) {
        try {
            StudyGroup studyGroup = studyGroupRepository.get(groupId);
            if (studyGroup == null) {
                throw new IllegalArgumentException(
                        "Unable to find study group with ID: " + groupId);
            }
            return studyGroup;
        } catch (MongoException e) {
            throw new RuntimeException("Failed to interact with the database.", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred:", e);
        }
    }
    /**
     * Get the study group for the group leader
     *
     * @param groupLeader the student who creates the study group
     * @return the study group
     */
    public StudyGroup getStudyGroupForLeader(Student groupLeader) {
        return studyGroupRepository.getAll().stream()
                .filter(group -> group.getGroupLeaderId().equals(groupLeader.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Disbands a study group by removing all relationships between the group and its members and
     * deleting the group from the repository.
     */
    /**
     * Disbands a study group by removing all relationships between the group and its members,
     * canceling all related meetings and reminders, and deleting the group from the repository.
     *
     * @param studyGroup
     * @param studentController
     * @param meetingController
     * @param bookingController
     */
    public void disbandGroup(
            StudyGroup studyGroup,
            StudentController studentController,
            MeetingController meetingController,
            BookingController bookingController,
            ReminderController reminderController) {
        List<Student> members = getMemberListOfStudyGroup(studyGroup);
        for (Student member : members) {
            List<ObjectId> memberGroupList = member.getGroupList();

            // remove relationship between student and study group
            memberGroupList.remove(studyGroup.getId());
            member.setGroupList(memberGroupList);
            studentController.updateStudent(member);
        }

        // Cancel all related meetings
        List<AbstractMeeting> studyGroupMeetings =
                meetingController.getMeetingsForStudyGroup(studyGroup);
        for (AbstractMeeting meeting : studyGroupMeetings) {
            meetingController.cancelMeeting(meeting, bookingController, reminderController);
        }

        studyGroupRepository.delete(studyGroup.getId());
    }

    /**
     * Remove the relationship between student and study group, remove student from the meeting
     * participants, and remove all related reminders
     *
     * @param leaveGroup
     * @param student
     * @param meetingController
     */
    public void leaveGroup(
            StudyGroup leaveGroup,
            Student student,
            MeetingController meetingController,
            BookingController bookingController,
            ReminderController reminderController) {

        meetingController.removeParticipantWhenLeaveGroup(
                leaveGroup, student, reminderController, bookingController);

        // remove student relationship with the study group
        List<ObjectId> studentGroupList = student.getGroupList();
        studentGroupList.remove(leaveGroup.getId());
        student.setGroupList(studentGroupList);
        studentRepository.update(student);
    }
}
