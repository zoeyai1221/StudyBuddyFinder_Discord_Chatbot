package edu.northeastern.cs5500.starterbot.controller;

import edu.northeastern.cs5500.starterbot.model.*;
import edu.northeastern.cs5500.starterbot.service.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.inject.Inject;

/**
 * Represents the student availability controller class
 *
 * @author Team Wolf
 */
public class StudentAvailabilityController {
    private final StudentController studentController;
    @Inject OpenTelemetry openTelemetry;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mma");

    /**
     * The StudentAvailabilityController constructor
     *
     * @param studentController student contoller class
     */
    @Inject
    public StudentAvailabilityController(StudentController studentController) {
        this.studentController = studentController;
    }

    /**
     * Recommend study group based on the user's preferences
     *
     * @param discordUserId discord user id
     * @param day the user selecte day
     * @param startTime start time
     * @param endTime end time
     * @return true if all the data have successfuly recorded
     */
    public boolean setTimeSlot(String discordUserId, String day, String startTime, String endTime) {
        var span = openTelemetry.span("setTimeSlot");
        span.setAttribute("discordUserId", discordUserId);
        span.setAttribute("day", day);
        span.setAttribute("startTime", startTime);
        span.setAttribute("endTime", endTime);

        try (Scope scope = span.makeCurrent()) {
            LocalTime start = LocalTime.parse(startTime, formatter);
            LocalTime end = LocalTime.parse(endTime, formatter);
            // Check if its a valid time range
            if (!start.isBefore(end))
                throw new IllegalArgumentException(
                        "Start time have to be ealier than the end time!");
            // To avoid time zone issue
            ZoneId zoneId = ZoneId.of("UTC");
            ZonedDateTime startZone = start.atDate(LocalDate.now()).atZone(zoneId);
            ZonedDateTime endZone = end.atDate(LocalDate.now()).atZone(zoneId);
            TimeSlot timeSlot =
                    TimeSlot.builder()
                            .day(day)
                            .start(startZone.toLocalDateTime())
                            .end(endZone.toLocalDateTime())
                            .build();
            Student student = studentController.getStudentByDiscordUserId(discordUserId);
            List<TimeSlot> availabilityList = student.getAvailability();

            if (!isDuplicate(availabilityList, timeSlot)) {
                availabilityList.add(timeSlot);
                studentController.setAvailabilityForStudent(discordUserId, availabilityList);
            }

            return true;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private boolean isDuplicate(List<TimeSlot> availabilityList, TimeSlot timeSlot) {
        return availabilityList.stream()
                .anyMatch(
                        existingSlot ->
                                existingSlot.getDay().equals(timeSlot.getDay())
                                        && existingSlot
                                                .getStart()
                                                .toLocalTime()
                                                .equals(timeSlot.getStart().toLocalTime()));
    }

    /**
     * Remove the time slots
     *
     * @param discordUserId discord user id
     * @param day the user selecte day
     * @param startTime start time
     * @return true if all the data have successfuly removed
     */
    public boolean removeTimeSlot(String discordUserId, String day, String startTime) {
        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        List<TimeSlot> availability = student.getAvailability();

        if (availability.isEmpty()) {
            return false; // No availability to remove
        }

        LocalTime start = LocalTime.parse(startTime);
        // To avoid time zone issue

        LocalDateTime startDateTime = start.atDate(LocalDate.now());

        // Remove the matching time slot
        boolean removed =
                availability.removeIf(
                        slot ->
                                slot.getDay().equals(day)
                                        && slot.getStart()
                                                .toLocalTime()
                                                .equals(startDateTime.toLocalTime()));

        if (removed) {
            studentController.setAvailabilityForStudent(discordUserId, availability);
        }

        return removed;
    }
}
