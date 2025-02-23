package edu.northeastern.cs5500.starterbot.controller;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.TimeSlot;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import edu.northeastern.cs5500.starterbot.service.FakeOpenTelemetryService;
import edu.northeastern.cs5500.starterbot.service.OpenTelemetry;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentAvailabilityControllerTest {
    private StudentAvailabilityController studentAvailabilityController;
    private StudentController studentController;

    private StudentController getStudentController() {
        return new StudentController(new InMemoryRepository<>(), getInterestController());
    }

    private InterestController getInterestController() {
        return new InterestController(new InMemoryRepository<>());
    }

    @BeforeEach
    void setUp() {
        studentController = getStudentController();
        OpenTelemetry openTelemetry = new FakeOpenTelemetryService();
        studentAvailabilityController = new StudentAvailabilityController(studentController);
        studentAvailabilityController.openTelemetry = openTelemetry;
    }

    /****************** tests for setTimeSlot() **************/
    @Test
    void testSetTimeSlotValidInput() {
        // set up
        String discordUserId = "23h5ikoqaehokljhaoe";
        String day = "Monday";
        String startTime = "09:00AM";
        String endTime = "10:00AM";

        // Set the time slot with
        boolean res =
                studentAvailabilityController.setTimeSlot(discordUserId, day, startTime, endTime);
        assertThat(res).isNotNull();
        assertThat(res).isTrue();

        // Verify the retrieved student matches the original
        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        assertThat(student.getAvailability()).hasSize(1);

        TimeSlot timeSlot = student.getAvailability().get(0);
        assertThat(timeSlot.getDay()).isEqualTo(day);
        assertThat(timeSlot.getStart().toLocalTime()).isEqualTo(LocalTime.parse("09:00"));
        assertThat(timeSlot.getEnd().toLocalTime()).isEqualTo(LocalTime.parse("10:00"));
    }

    @Test
    void testSetTimeSlotWithInvalidTime() {
        // set up
        String discordUserId = "23h5ikoqaehokljhaoe";
        String day = "Monday";
        String startTime = "10:00AM";
        String endTime = "09:00AM";

        // a random student ID not in the repository
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                studentAvailabilityController.setTimeSlot(
                                        discordUserId, day, startTime, endTime));

        // Fail the test if no exception is thrown
        assertThat(exception.getMessage())
                .isEqualTo("Start time have to be ealier than the end time!");
    }

    @Test
    void testSetMultipleTimeSlot() {
        // set up
        String discordUserId = "23h5ikoqaehokljhaoe";
        String day1 = "Friday";
        String startTime1 = "09:00AM";
        String endTime1 = "10:00AM";
        String day2 = "Monday";
        String startTime2 = "10:00AM";
        String endTime2 = "11:00AM";

        // Use a repository that simulates a database failure
        studentAvailabilityController.setTimeSlot(discordUserId, day1, startTime1, endTime1);
        studentAvailabilityController.setTimeSlot(discordUserId, day2, startTime2, endTime2);
        studentAvailabilityController.setTimeSlot(discordUserId, day2, startTime1, endTime1);

        // Verify the retrieved student matches the original
        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        assertThat(student.getAvailability()).hasSize(3);

        TimeSlot timeSlot1 = student.getAvailability().get(0);
        assertThat(timeSlot1.getDay()).isEqualTo(day1);
        assertThat(timeSlot1.getStart().toLocalTime()).isEqualTo(LocalTime.parse("09:00"));
        assertThat(timeSlot1.getEnd().toLocalTime()).isEqualTo(LocalTime.parse("10:00"));

        TimeSlot timeSlot2 = student.getAvailability().get(1);
        assertThat(timeSlot2.getDay()).isEqualTo(day2);
        assertThat(timeSlot2.getStart().toLocalTime()).isEqualTo(LocalTime.parse("10:00"));
        assertThat(timeSlot2.getEnd().toLocalTime()).isEqualTo(LocalTime.parse("11:00"));
    }

    @Test
    void testSetTimeSlotDuplicateTime() {
        // set up
        String discordUserId = "23h5ikoqaehokljhaoe";
        String day = "Monday";
        String startTime = "09:00AM";
        String endTime = "10:00AM";

        // Set the time slot
        studentAvailabilityController.setTimeSlot(
                discordUserId, day, startTime, endTime); // valid add
        studentAvailabilityController.setTimeSlot(
                discordUserId, day, startTime, endTime); // duplicate

        // Verify the retrieved student availability is only one
        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        assertThat(student.getAvailability()).hasSize(1);

        TimeSlot timeSlot = student.getAvailability().get(0);
        assertThat(timeSlot.getDay()).isEqualTo(day);
        assertThat(timeSlot.getStart().toLocalTime()).isEqualTo(LocalTime.parse("09:00"));
        assertThat(timeSlot.getEnd().toLocalTime()).isEqualTo(LocalTime.parse("10:00"));
    }

    @Test
    void testRemoveTimeSlotSuccess() {
        String discordUserId = "123456789123456789";
        String day = "Monday";
        String startTime = "09:00";

        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        student.setDiscordUserId(discordUserId);

        List<TimeSlot> availability = new ArrayList<>();
        availability.add(
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.of(2024, 12, 4, 9, 0))
                        .end(LocalDateTime.of(2024, 12, 4, 10, 0))
                        .build());
        availability.add(
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.of(2024, 12, 4, 11, 0))
                        .end(LocalDateTime.of(2024, 12, 4, 12, 0))
                        .build());
        availability.add(
                TimeSlot.builder()
                        .day("Friday")
                        .start(LocalDateTime.of(2024, 12, 4, 11, 0))
                        .end(LocalDateTime.of(2024, 12, 4, 12, 0))
                        .build());

        student.setAvailability(availability);

        boolean removed =
                studentAvailabilityController.removeTimeSlot(discordUserId, day, startTime);

        assertThat(removed).isTrue();
        assertThat(student.getAvailability()).hasSize(2);
    }

    @Test
    void testRemoveTimeSlotEmptyAvailability() {
        String discordUserId = "123456789123456789";
        String day = "Monday";
        String startTime = "09:00";

        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        student.setDiscordUserId(discordUserId);

        List<TimeSlot> availability = new ArrayList<>();
        student.setAvailability(availability); // set empty availability

        boolean removed =
                studentAvailabilityController.removeTimeSlot(discordUserId, day, startTime);

        assertThat(removed).isFalse();
        assertThat(student.getAvailability()).isEmpty();
    }

    @Test
    void testRemoveTimeSlotNotExistAvailability() {
        String discordUserId = "123456789123456789";
        String day = "Sunday";
        String startTime = "09:00";

        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        student.setDiscordUserId(discordUserId);

        List<TimeSlot> availability = new ArrayList<>();
        availability.add(
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.of(2024, 12, 4, 9, 0))
                        .end(LocalDateTime.of(2024, 12, 4, 10, 0))
                        .build());
        availability.add(
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.of(2024, 12, 4, 11, 0))
                        .end(LocalDateTime.of(2024, 12, 4, 12, 0))
                        .build());
        availability.add(
                TimeSlot.builder()
                        .day("Friday")
                        .start(LocalDateTime.of(2024, 12, 4, 11, 0))
                        .end(LocalDateTime.of(2024, 12, 4, 12, 0))
                        .build());

        student.setAvailability(availability);

        boolean removed =
                studentAvailabilityController.removeTimeSlot(discordUserId, day, startTime);

        assertThat(removed).isFalse();
        assertThat(student.getAvailability()).hasSize(3);
    }
}
