package edu.northeastern.cs5500.starterbot.seeder;

import edu.northeastern.cs5500.starterbot.model.Room;
import edu.northeastern.cs5500.starterbot.model.TimeSlot;
import edu.northeastern.cs5500.starterbot.repository.GenericRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeder class for initializing room data in the application.
 *
 * @author Team wolf
 */
@Slf4j
public class RoomSeeder {
    GenericRepository<Room> roomRepository;

    /**
     * Constructs a RoomSeeder with the given room repository.
     *
     * @param roomRepository the repository where room data is stored
     */
    @Inject
    public RoomSeeder(GenericRepository<Room> roomRepository) {
        this.roomRepository = roomRepository;
    }

    /** Seeds predefined room data into the repository. */
    public void seedRooms() {
        List<Room> rooms = getRooms();
        addFakeBookedSlots(rooms); // Add fake bookings for some rooms

        for (Room room : rooms) {
            if (!roomExists(room.getLocation())) {
                roomRepository.add(room);
            }
        }

        log.info("Rooms seeding complete.");
    }

    /**
     * Retrieves a list of predefined rooms.
     *
     * @return a list of rooms with their locations and capacities initialized
     */
    private List<Room> getRooms() {
        List<Room> rooms = new ArrayList<>();

        RoomConstants.SEATTLE_CAMPUS_ROOMS.forEach(
                (location, capacity) -> {
                    rooms.add(
                            Room.builder()
                                    .location(location)
                                    .capacity(capacity)
                                    .bookedSlots(new HashSet<>())
                                    .build());
                });

        return rooms;
    }

    /**
     * Checks if a room with the given location already exists in the repository.
     *
     * @param roomLocation the location of the room to check
     * @return true if the room exists, false otherwise
     */
    private boolean roomExists(String roomLocation) {
        Collection<Room> rooms = roomRepository.getAll();
        for (Room room : rooms) {
            if (room.getLocation().equals(roomLocation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds fake booked slots to some rooms to simulate room usage.
     *
     * @param rooms the list of rooms to potentially add booked slots to
     */
    private void addFakeBookedSlots(List<Room> rooms) {
        Random random = new Random();
        String[] daysOfWeek = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        };

        for (Room room : rooms) {
            // Simulate adding booked slots to about half of the rooms
            if (random.nextBoolean()) {
                int numberOfSlots = random.nextInt(3) + 1; // 1 to 3 slots
                Set<TimeSlot> bookedSlots = new HashSet<>();

                for (int i = 0; i < numberOfSlots; i++) {
                    LocalDateTime start = LocalDateTime.now().plusDays(random.nextInt(365));
                    LocalDateTime end = start.plusHours(1 + random.nextInt(3)); // 1 to 3 hours
                    String day = start.getDayOfWeek().name();
                    bookedSlots.add(
                            TimeSlot.builder()
                                    .day(day) // Set the day based on start's day of the week
                                    .start(start)
                                    .end(end)
                                    .build());
                }

                room.setBookedSlots(bookedSlots);
            }
        }
    }
}
