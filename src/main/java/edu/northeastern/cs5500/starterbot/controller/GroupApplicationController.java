package edu.northeastern.cs5500.starterbot.controller;

import edu.northeastern.cs5500.starterbot.model.GroupApplication;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import edu.northeastern.cs5500.starterbot.repository.GenericRepository;
import edu.northeastern.cs5500.starterbot.service.FakeOpenTelemetryService;
import edu.northeastern.cs5500.starterbot.service.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

/**
 * Controller for managing group applications. This class provides methods for retrieving,
 * accepting, and declining group applications.
 */
@Slf4j
public class GroupApplicationController {

    GenericRepository<GroupApplication> groupApplicationRepository;
    @Inject OpenTelemetry openTelemetry;
    @Inject StudentController studentController;
    @Inject StudyGroupController studyGroupController;

    @Inject
    public GroupApplicationController(
            GenericRepository<GroupApplication> groupApplicationRepository,
            StudentController studentController,
            StudyGroupController studyGroupController) {
        this.groupApplicationRepository = groupApplicationRepository;
        this.studentController = studentController;
        this.studyGroupController = studyGroupController;
        this.openTelemetry = new FakeOpenTelemetryService();
    }
    /**
     * Retrieves all group applications for the groups owned by a specific leader.
     *
     * @param leaderDiscordUserId The Discord user ID of the group leader.
     * @return A collection of group applications ordered by timestamp.
     */
    public Collection<GroupApplication> getApplicationsByLeader(String leaderDiscordUserId) {
        var span = openTelemetry.span("getApplicationsByLeader");
        span.setAttribute("leaderDiscordUserId", leaderDiscordUserId);
        Student leader = studentController.getStudentByDiscordUserId(leaderDiscordUserId);
        ObjectId leaderId = leader.getId();
        try (Scope scope = span.makeCurrent()) {
            Collection<GroupApplication> allApplications = groupApplicationRepository.getAll();
            return allApplications.stream()
                    .filter(
                            application -> {
                                ObjectId groupId = application.getReceiver();
                                StudyGroup studyGroup =
                                        studyGroupController.getStudyGroupById(groupId);
                                // Check if the group's leader matches the provided user ID
                                return studyGroup != null
                                        && leaderId.equals(studyGroup.getGroupLeaderId());
                            })
                    .sorted(Comparator.comparing(GroupApplication::getTimestamp))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Accepts a group application and performs necessary actions.
     *
     * @param application The group application to accept.
     */
    public void acceptApplication(@Nonnull GroupApplication application) {
        var span = openTelemetry.span("acceptApplication");
        span.setAttribute("applicationId", application.getId().toHexString());
        ObjectId applicationId = Objects.requireNonNull(application.getId());
        try (Scope scope = span.makeCurrent()) {
            // Add the sender Student to the group
            Student applicant =
                    studentController.getStudentByDiscordUserId(application.getSender());
            StudyGroup studyGroup =
                    studyGroupController.getStudyGroupById(application.getReceiver());
            studyGroupController.addStudyGroup(applicant, studyGroup, studentController);
            // Remove the application from the repository
            groupApplicationRepository.delete(applicationId);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Declines a group application and performs necessary actions.
     *
     * @param application The group application to decline.
     */
    public void declineApplication(@Nonnull GroupApplication application) {
        var span = openTelemetry.span("declineApplication");
        span.setAttribute("applicationId", application.getId().toHexString());
        ObjectId applicationId = Objects.requireNonNull(application.getId());
        try (Scope scope = span.makeCurrent()) {
            // Remove the application from the repository
            groupApplicationRepository.delete(applicationId);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Retrieves a group application by its unique ID.
     *
     * @param applicationId The ID of the application.
     * @return The group application with the specified ID.
     * @throws IllegalArgumentException if no application with the given ID is found.
     */
    public GroupApplication getApplicationById(ObjectId applicationId) {
        var span = openTelemetry.span("getApplicationById");
        span.setAttribute("applicationId", applicationId.toHexString());

        try (Scope scope = span.makeCurrent()) {
            GroupApplication application = groupApplicationRepository.get(applicationId);
            if (application == null) {
                throw new IllegalArgumentException(
                        "Group application with ID " + applicationId + " not found.");
            }
            return application;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
