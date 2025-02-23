package edu.northeastern.cs5500.starterbot.seeder;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a predefined set of NEU Seattle room data for the application.
 *
 * @author Team wolf
 */
public final class RoomConstants {
    private RoomConstants() {
        // restrict instantiation
    }

    /** A map of predefined rooms with their capacities. */
    public static final Map<String, Integer> SEATTLE_CAMPUS_ROOMS;

    static {
        SEATTLE_CAMPUS_ROOMS = new HashMap<>();
        SEATTLE_CAMPUS_ROOMS.put("Building 225: 107 Huddle", 3);
        SEATTLE_CAMPUS_ROOMS.put("Building 225: 108 Huddle", 3);
        SEATTLE_CAMPUS_ROOMS.put("Building 225: 109 Huddle", 3);
        SEATTLE_CAMPUS_ROOMS.put(
                "Building 225: 214 Huddle",
                4); // no capcity data on website, estimate by observing the room layout and
        // available seating
        SEATTLE_CAMPUS_ROOMS.put(
                "Building 225: 222 Collaboration",
                3); // no capcity data on website, estimate by observing the room layout and
        // available seating
        SEATTLE_CAMPUS_ROOMS.put("Building 225: 224 Huddle", 3);
        SEATTLE_CAMPUS_ROOMS.put("Building 225: 414 Interview", 2);
        SEATTLE_CAMPUS_ROOMS.put("Building 225: 111 Fir", 6);
        SEATTLE_CAMPUS_ROOMS.put("Building 225: 408 Crane", 10);
        SEATTLE_CAMPUS_ROOMS.put("Building 310: 101 Student Conference", 6);
        SEATTLE_CAMPUS_ROOMS.put("Building 310: 114 Focus", 2);
        SEATTLE_CAMPUS_ROOMS.put("Building 310: 203 Focus", 2);
        SEATTLE_CAMPUS_ROOMS.put("Building 401: 109G Mercer", 12);
    }
}
