package edu.northeastern.cs5500.starterbot.controller;

import com.mongodb.lang.Nullable;
import edu.northeastern.cs5500.starterbot.model.AbstractMeeting;
import edu.northeastern.cs5500.starterbot.model.Booking;
import edu.northeastern.cs5500.starterbot.model.InPersonMeeting;
import edu.northeastern.cs5500.starterbot.model.Room;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.TimeSlot;
import edu.northeastern.cs5500.starterbot.repository.GenericRepository;
import edu.northeastern.cs5500.starterbot.service.FakeOpenTelemetryService;
import edu.northeastern.cs5500.starterbot.service.OpenTelemetry;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

@Slf4j
public class BookingController {
    GenericRepository<Room> roomRepository;
    GenericRepository<Booking> bookingRepository;
    @Inject OpenTelemetry openTelemetry;

    @Inject
    public BookingController(
            GenericRepository<Room> roomRepository, GenericRepository<Booking> bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        openTelemetry = new FakeOpenTelemetryService();
    }

    /**
     * Retrieves a list of available rooms for a given time slot.
     *
     * @param timeSlot the TimeSlot to check for availability
     * @return a list of rooms that are available during the given time slot
     */
    public List<Room> getAvailableRooms(TimeSlot timeSlot) {
        return roomRepository.getAll().stream()
                .filter(room -> isRoomAvailable(room, timeSlot))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a room is available during the given time slot.
     *
     * @param room the Room object to check
     * @param timeSlot the TimeSlot to check availability against
     * @return true if the room is available, false otherwise
     */
    private boolean isRoomAvailable(Room room, TimeSlot timeSlot) {
        Set<TimeSlot> bookedSlots = room.getBookedSlots();

        for (TimeSlot bookedSlot : bookedSlots) {
            // Check for overlap
            if (bookedSlot.getDay().equalsIgnoreCase(timeSlot.getDay())
                    && !(timeSlot.getEnd().isBefore(bookedSlot.getStart())
                            || timeSlot.getStart().isAfter(bookedSlot.getEnd()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return the room with the given Id
     *
     * @param roomId
     * @return
     */
    public Room getRoombyId(ObjectId roomId) {
        return roomRepository.get(roomId);
    }

    /**
     * Creates a booking for the user with the selected room
     *
     * @param roomId
     * @param inPersonMeeting
     * @param student
     * @return
     */
    public Booking createBooking(
            ObjectId roomId,
            InPersonMeeting inPersonMeeting,
            Student student,
            MeetingController meetingController) {
        Room room = getRoombyId(roomId);

        if (room == null) {
            throw new IllegalArgumentException("Room with the provided ID does not exist.");
        }

        // Create and save the booking
        Booking booking =
                Booking.builder()
                        .isConfirmed(true) // Set initial confirmation status as false
                        .roomId(roomId)
                        .inPersonMeetingId(inPersonMeeting.getId())
                        .studentId(student.getId())
                        .build();

        bookingRepository.add(booking);
        meetingController.updateInPersonMeetingBooking(inPersonMeeting, booking);
        Set<TimeSlot> roomBookedSlots = room.getBookedSlots();
        TimeSlot newbookedSlot = inPersonMeeting.getTimeSlots().get(0);
        roomBookedSlots.add(newbookedSlot);
        room.setBookedSlots(roomBookedSlots);
        roomRepository.update(room);
        log.info("Booking created: {}", booking);

        return booking;
    }

    /**
     * Returns the booking related to the given meeting
     *
     * @param meeting
     * @return
     */
    @Nullable
    public Booking getBookingForMeeting(@NonNull AbstractMeeting meeting) {
        return bookingRepository.getAll().stream()
                .filter(b -> b.getInPersonMeetingId().equals(meeting.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove the booked slot in room and remove the booking in the database
     *
     * @param booking
     * @param meetingController
     */
    public void cancelBooking(Booking booking, MeetingController meetingController) {
        Room room = getRoombyId(booking.getRoomId());
        InPersonMeeting meeting =
                meetingController.getInPersonMeetingById(booking.getInPersonMeetingId());

        if (room == null || meeting == null) {
            throw new IllegalArgumentException("Room or meeting not found for the given booking.");
        }

        TimeSlot timeSlot = meeting.getTimeSlots().get(0);
        // Remove the time slot from the room's bookedSlots set
        if (room.getBookedSlots().contains(timeSlot)) {
            room.getBookedSlots().remove(timeSlot);
            roomRepository.update(room);
        } else {
            log.warn("The time slot was not found in the room's booked slots.");
        }

        meetingController.updateInPersonMeetingBooking(meeting, new Booking());

        // Remove the booking from the repository
        bookingRepository.delete(booking.getId());
        log.info("Booking successfully canceled for meeting ID: " + booking.getInPersonMeetingId());
    }

    /**
     * Returns the booking of the given id
     *
     * @param bookingId
     * @return
     */
    @Nullable
    public Booking getBookingById(ObjectId bookingId) {
        return bookingRepository.get(bookingId);
    }
}
