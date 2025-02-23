package edu.northeastern.cs5500.starterbot.seeder;

import static com.google.common.truth.Truth.assertThat;

import edu.northeastern.cs5500.starterbot.model.Room;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import java.util.Collection;
import org.junit.jupiter.api.Test;

/** Unit tests for the RoomSeeder */
class RoomSeederTest {
    private RoomSeeder getRoomSeeder(InMemoryRepository<Room> repository) {
        return new RoomSeeder(repository);
    }

    /**
     * Test that seeding rooms adds predefined rooms to the repository.
     *
     * <p>Verify that the RoomSeedercorrectly adds all predefined rooms from RoomConstants to the
     * repository and ensures the repository is not empty after seeding.
     */
    @Test
    void testGetRooms() {
        RoomSeeder roomSeeder = getRoomSeeder(new InMemoryRepository<>());
        roomSeeder.seedRooms();

        Collection<Room> rooms = roomSeeder.roomRepository.getAll();
        assertThat(rooms).isNotEmpty();
        assertThat(rooms.size()).isEqualTo(RoomConstants.SEATTLE_CAMPUS_ROOMS.size());
    }

    /**
     * Test that seeding rooms prevents duplicate entries in the repository.
     *
     * <p>Ensure that calling seedRooms multiple times does not result in duplicate entries being
     * added to the repository.
     */
    @Test
    public void testSeedRoomsPreventsDuplicateEntries() {
        RoomSeeder roomSeeder = getRoomSeeder(new InMemoryRepository<>());
        // Seed rooms twice
        roomSeeder.seedRooms();
        roomSeeder.seedRooms();

        // Retrieve all rooms from the repository
        Collection<Room> rooms = roomSeeder.roomRepository.getAll();

        // Assert that no duplicate rooms exist
        assertThat(rooms.size()).isEqualTo(RoomConstants.SEATTLE_CAMPUS_ROOMS.size());
    }
}
