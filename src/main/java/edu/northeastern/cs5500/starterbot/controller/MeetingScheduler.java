package edu.northeastern.cs5500.starterbot.controller;

import edu.northeastern.cs5500.starterbot.model.*;
import edu.northeastern.cs5500.starterbot.repository.GenericRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents the meeting scheduler class, starts when the bot starts
 *
 * @author Team Wolf
 */
@Slf4j
public class MeetingScheduler {
    GenericRepository<OnlineMeeting> onlineMeetingRepository;
    GenericRepository<InPersonMeeting> inPersonMeetingRepository;
    @Inject MeetingController meetingController;
    @Inject BookingController bookingController;
    @Inject ReminderController reminderController;
    static final String EMPTY_STRING = "";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Inject
    public MeetingScheduler(
            GenericRepository<OnlineMeeting> onlineMeetingRepository,
            GenericRepository<InPersonMeeting> inPersonMeetingRepository,
            MeetingController meetingController,
            BookingController bookingController,
            ReminderController reminderController) {
        this.onlineMeetingRepository = onlineMeetingRepository;
        this.inPersonMeetingRepository = inPersonMeetingRepository;
        this.meetingController = meetingController;
        this.bookingController = bookingController;
        this.reminderController = reminderController;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::processMeetings, 0, 1, TimeUnit.MINUTES);
        log.info("Meeting scheduler started");
    }

    /** Filter out all meetings that are over and invoke deletion of the object. */
    private void processMeetings() {
        log.info("Processing meetings");
        try {
            // Combine all meetings into a single stream
            Collection<OnlineMeeting> onlineMeetings = onlineMeetingRepository.getAll();
            Collection<InPersonMeeting> inPersonMeetings = inPersonMeetingRepository.getAll();

            // Process all meetings after converting them to AbstractMeeting
            Stream.concat(
                            onlineMeetings.stream().map(meeting -> (AbstractMeeting) meeting),
                            inPersonMeetings.stream().map(meeting -> (AbstractMeeting) meeting))
                    .filter(this::isMeetingOver)
                    .forEach(this::cancelMeeting);
        } catch (Exception e) {
            log.error("Error occurred while processing meetings", e);
        }
    }

    // To be called by processMeetings() only
    private void cancelMeeting(AbstractMeeting meeting) {
        log.info("Canceling meeting {}", meeting.getId());
        meetingController.cancelMeeting(meeting, bookingController, reminderController);
    }

    // Check if the meeting has ended based on the latest time slot
    private boolean isMeetingOver(AbstractMeeting meeting) {
        if (meeting.getTimeSlots() == null || meeting.getTimeSlots().isEmpty()) {
            return false; // No time slots, can't determine if it's over
        }

        LocalDateTime lastEndTime =
                meeting.getTimeSlots().stream()
                        .map(slot -> slot.getEnd())
                        .max(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.MIN);

        return LocalDateTime.now().isAfter(lastEndTime);
    }
}
