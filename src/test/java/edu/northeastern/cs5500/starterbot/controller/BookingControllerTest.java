package edu.northeastern.cs5500.starterbot.controller;

import static com.google.common.truth.Truth.assertThat;

import edu.northeastern.cs5500.starterbot.model.AbstractMeeting;
import edu.northeastern.cs5500.starterbot.model.Booking;
import edu.northeastern.cs5500.starterbot.model.InPersonMeeting;
import edu.northeastern.cs5500.starterbot.model.Room;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.TimeSlot;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

public class BookingControllerTest {
    private BookingController getBookingController() {
        return new BookingController(new InMemoryRepository<>(), new InMemoryRepository<>());
    }

    private MeetingController getMeetingController() {
        return new MeetingController(new InMemoryRepository<>(), new InMemoryRepository<>());
    }

    @Test
    void testGetAvailableRooms() {
        // setup
        BookingController bookingController = getBookingController();
        Room room1 =
                Room.builder()
                        .id(new ObjectId())
                        .location("Room 1")
                        .capacity(5)
                        .bookedSlots(
                                Set.of(
                                        TimeSlot.builder()
                                                .day("Monday")
                                                .start(LocalDateTime.of(2023, 12, 11, 10, 0))
                                                .end(LocalDateTime.of(2023, 12, 11, 11, 0))
                                                .build()))
                        .build();

        Room room2 =
                Room.builder()
                        .id(new ObjectId())
                        .location("Room 2")
                        .capacity(10)
                        .bookedSlots(new HashSet<>())
                        .build();

        bookingController.roomRepository.add(room1);
        bookingController.roomRepository.add(room2);

        TimeSlot timeSlot =
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.of(2023, 12, 11, 11, 0))
                        .end(LocalDateTime.of(2023, 12, 11, 12, 0))
                        .build();

        // Act
        List<Room> availableRooms = bookingController.getAvailableRooms(timeSlot);

        // Assert
        assertThat(availableRooms).hasSize(1);
        assertThat(availableRooms.get(0).getLocation()).isEqualTo("Room 2");
    }

    @Test
    void testCreateBooking() {
        // setup
        BookingController bookingController = getBookingController();
        MeetingController meetingController = getMeetingController();
        Room room =
                Room.builder()
                        .id(new ObjectId())
                        .location("Room 1")
                        .capacity(5)
                        .bookedSlots(new HashSet<>())
                        .build();
        bookingController.roomRepository.add(room);

        ObjectId inPersonMeetingId = new ObjectId();
        InPersonMeeting meeting = new InPersonMeeting();
        meeting.setId(inPersonMeetingId);
        List<TimeSlot> timeSlots = new ArrayList<>();
        TimeSlot timeSlot =
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.of(2023, 12, 11, 10, 0))
                        .end(LocalDateTime.of(2023, 12, 11, 11, 0))
                        .build();
        timeSlots.add(timeSlot);
        meeting.setTimeSlots(timeSlots);

        Student student = new Student();
        student.setId(new ObjectId());

        // Act
        Booking booking =
                bookingController.createBooking(room.getId(), meeting, student, meetingController);

        // Assert
        assertThat(booking).isNotNull();
        assertThat(booking.getRoomId()).isEqualTo(room.getId());
        assertThat(booking.getInPersonMeetingId()).isEqualTo(meeting.getId());
        assertThat(booking.getStudentId()).isEqualTo(student.getId());

        Room updatedRoom = bookingController.getRoombyId(room.getId());
        assertThat(updatedRoom.getBookedSlots()).contains(timeSlot);
    }

    @Test
    void testGetBookingForMeetingFound() {
        // setup
        BookingController bookingController = getBookingController();

        ObjectId inPersonMeetingId = new ObjectId();
        AbstractMeeting meeting = new InPersonMeeting();
        meeting.setId(inPersonMeetingId);

        Booking booking = new Booking();
        booking.setInPersonMeetingId(inPersonMeetingId);
        bookingController.bookingRepository.add(booking);

        Booking retrieveBooking = bookingController.getBookingForMeeting(meeting);

        // result
        assertThat(retrieveBooking).isNotNull();
        assertThat(retrieveBooking.getInPersonMeetingId()).isEqualTo(inPersonMeetingId);
    }

    @Test
    void testGetBookingForMeetingNotFound() {
        // setup
        BookingController bookingController = getBookingController();

        AbstractMeeting meeting = new InPersonMeeting();
        meeting.setId(new ObjectId());

        Booking booking = new Booking();
        booking.setInPersonMeetingId(new ObjectId());
        bookingController.bookingRepository.add(booking);

        // retrieve the booking
        Booking retrieveBooking = bookingController.getBookingForMeeting(meeting);

        // result
        assertThat(retrieveBooking).isNull();
    }

    @Test
    void testCancelBooking() {
        // Setup
        BookingController bookingController = getBookingController();
        MeetingController meetingController = getMeetingController();

        Room room =
                Room.builder()
                        .id(new ObjectId())
                        .location("Room 1")
                        .capacity(5)
                        .bookedSlots(new HashSet<>())
                        .build();
        bookingController.roomRepository.add(room);

        ObjectId inPersonMeetingId = new ObjectId();
        AbstractMeeting meeting = new InPersonMeeting();
        meeting.setId(inPersonMeetingId);
        List<TimeSlot> timeSlots = new ArrayList<>();
        TimeSlot timeSlot =
                TimeSlot.builder()
                        .day("Monday")
                        .start(LocalDateTime.of(2023, 12, 11, 10, 0))
                        .end(LocalDateTime.of(2023, 12, 11, 11, 0))
                        .build();
        timeSlots.add(timeSlot);
        meeting.setTimeSlots(timeSlots);

        Student student = new Student();
        student.setId(new ObjectId());

        Booking booking =
                Booking.builder()
                        .studentId(student.getId())
                        .inPersonMeetingId(inPersonMeetingId)
                        .roomId(room.getId())
                        .build();

        // Add booked slot to the room
        room.getBookedSlots().add(timeSlot);

        meetingController.inPersonMeetingRepository.add((InPersonMeeting) meeting);
        bookingController.roomRepository.add(room);
        bookingController.bookingRepository.add(booking);

        // Act
        bookingController.cancelBooking(booking, meetingController);

        // Assert results
        // Assert that the booking is removed from the booking repository
        assertThat(bookingController.bookingRepository.getAll()).hasSize(0);

        // Assert that the time slot is removed from the room's bookedSlots set
        Room updatedRoom = bookingController.roomRepository.get(room.getId());
        assertThat(updatedRoom.getBookedSlots()).doesNotContain(timeSlot);

        // Assert that the meeting still exists in the repository
        InPersonMeeting updatedMeeting =
                meetingController.inPersonMeetingRepository.get(inPersonMeetingId);
        assertThat(updatedMeeting).isNotNull();
        assertThat(updatedMeeting.getTimeSlots())
                .contains(timeSlot); // Meeting time slots remain unchanged
    }

    @Test
    void testGetBookingById() {
        // setup
        BookingController bookingController = getBookingController();

        ObjectId bookingId = new ObjectId();
        Booking booking =
                Booking.builder()
                        .id(bookingId)
                        .roomId(new ObjectId())
                        .inPersonMeetingId(new ObjectId())
                        .studentId(new ObjectId())
                        .isConfirmed(true)
                        .build();

        // add the booking to the repository
        bookingController.bookingRepository.add(booking);

        Booking retrievedBooking = bookingController.getBookingById(bookingId);

        assertThat(retrievedBooking).isNotNull();
        assertThat(retrievedBooking.getId()).isEqualTo(bookingId);
    }
}
