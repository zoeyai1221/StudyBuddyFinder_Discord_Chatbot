package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.BookingController;
import edu.northeastern.cs5500.starterbot.controller.MeetingController;
import edu.northeastern.cs5500.starterbot.controller.StudentController;
import edu.northeastern.cs5500.starterbot.controller.StudyGroupController;
import edu.northeastern.cs5500.starterbot.model.AbstractMeeting;
import edu.northeastern.cs5500.starterbot.model.Booking;
import edu.northeastern.cs5500.starterbot.model.InPersonMeeting;
import edu.northeastern.cs5500.starterbot.model.Room;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import edu.northeastern.cs5500.starterbot.model.TimeSlot;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.bson.types.ObjectId;

@Slf4j
public class CreateBooking implements StringSelectHandler, ButtonHandler {
    static final String NAME = "createbooking";
    @Inject BookingController bookingController;
    @Inject StudentController studentController;
    @Inject MeetingController meetingController;
    @Inject StudyGroupController studyGroupController;
    @Inject JDA jda;

    // id
    static final String SELECT_ROOM = "select-room";
    static final String CANCEL_BUTTON = NAME + ":cancel";
    static final String NEVERMIND_BUTTON = NAME + ":nevermind";

    // Emoji
    private static final String CLOCK_EMOJI = "\uD83D\uDD52"; // 🕒
    private static final String PROHIBITED_EMOJI = "\uD83D\uDEAB"; // 🚫
    private static final String BUILDING_EMOJI = "\uD83C\uDFE2"; // 🏢
    private static final String PARTY_POPPER_EMOJI = "\uD83C\uDF89"; // 🎉
    private static final String CALENDAR_EMOJI = "\uD83D\uDCC5"; // 📅
    private static final String MEGAPHONE_EMOJI = "\uD83D\uDCE2"; // 📢
    private static final String ALARM_EMOJI = "\u23F0"; // ⏰
    private static final String SCHOOL_EMOJI = "\uD83C\uDFEB"; // 🏫

    // formatter
    private static final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

    @Inject
    public CreateBooking() {
        // Empty and public for Dagger
    }

    /**
     * Gets the name of the command.
     *
     * @return the name of the command, used to register it with the bot.
     */
    @Override
    @Nonnull
    public String getName() {
        return NAME;
    }

    /**
     * Starts the booking creation process and displays a dropdown list of available rooms for the
     * first time slot of the provided AbstractMeeting.
     *
     * @param meeting the AbstractMeeting object containing the meeting details
     * @return a MessageCreateBuilder object containing the dropdown menu
     */
    MessageCreateBuilder startCreateBooking(AbstractMeeting meeting) {
        log.info("event: createbooking");

        Booking booking = bookingController.getBookingForMeeting(meeting);

        if (booking != null) {
            String bookingDetails = getBookingDetails(meeting, booking);
            return new MessageCreateBuilder()
                    .setContent(
                            CALENDAR_EMOJI
                                    + " You have already made a booking for this meeting.\n"
                                    + bookingDetails
                                    + "\n\nDo you want to cancel it?")
                    .addActionRow(
                            Button.danger(
                                    CANCEL_BUTTON + ":" + booking.getId().toString(), " Cancel it"),
                            Button.secondary(NEVERMIND_BUTTON, " Never Mind"));
        }

        // Get the first time slot from the meeting
        if (meeting.getTimeSlots().isEmpty()) {
            throw new IllegalArgumentException(
                    CLOCK_EMOJI + " No time slots available in the meeting.");
        }
        TimeSlot firstTimeSlot = meeting.getTimeSlots().get(0);

        // Get available rooms from the BookingController
        List<Room> availableRooms = bookingController.getAvailableRooms(firstTimeSlot);

        // Create a dropdown menu with available rooms
        StringSelectMenu.Builder menuBuilder =
                StringSelectMenu.create(SELECT_ROOM)
                        .setPlaceholder("Select a room for your booking");

        for (Room room : availableRooms) {
            menuBuilder.addOption(
                    room.getLocation() + " Capacity: " + room.getCapacity(),
                    room.getId().toString() + ":" + meeting.getId().toString());
        }

        // If no rooms are available, show a message
        if (availableRooms.isEmpty()) {
            return new MessageCreateBuilder()
                    .setContent(PROHIBITED_EMOJI + " No rooms are available for this meeting.");
        }

        // Build and return the message with the dropdown
        return new MessageCreateBuilder()
                .setContent(BUILDING_EMOJI + " Select a room from the list below:")
                .addActionRow(menuBuilder.build());
    }

    /**
     * User create booking and select room
     *
     * @param event the event user triggered
     */
    @Override
    public void onStringSelectInteraction(@Nonnull StringSelectInteractionEvent event) {
        log.info("Room selected: {}", event.getValues());

        String[] selectedId = event.getInteraction().getValues().get(0).split(":");
        InPersonMeeting inPersonMeeting =
                meetingController.getInPersonMeetingById(new ObjectId(selectedId[1]));

        ObjectId roomId = new ObjectId(selectedId[0]);

        String discordUserId = event.getUser().getId();
        Student student = studentController.getStudentByDiscordUserId(discordUserId);

        Booking booking =
                bookingController.createBooking(
                        roomId, inPersonMeeting, student, meetingController);
        notifyMemberAboutBookedRoom(inPersonMeeting, booking);

        // Respond to the user
        event.reply(PARTY_POPPER_EMOJI + " Booking created successfully!")
                .setEphemeral(true)
                .queue();
    }

    /**
     * Notify all members in the meeting about the booking room
     *
     * @param meeting the abstract meeting
     * @param bookng booking info
     */
    private void notifyMemberAboutBookedRoom(AbstractMeeting meeting, Booking booking) {
        if (meeting == null || booking == null) {
            throw new IllegalArgumentException("Meeting, booking, and study group cannot be null.");
        }

        StudyGroup studyGroup = studyGroupController.getStudyGroupById(meeting.getStudyGroup());
        String message = getBookingDetails(meeting, booking);

        // Notify each member in the study group
        for (Student member : studyGroupController.getMemberListOfStudyGroup(studyGroup)) {
            jda.retrieveUserById(member.getDiscordUserId())
                    .queue(
                            user -> {
                                user.openPrivateChannel()
                                        .flatMap(channel -> channel.sendMessage(message))
                                        .queue();
                            },
                            error -> {
                                // Handle cases where the user cannot be notified (e.g., invalid
                                // Discord ID)
                                log.error(
                                        "Failed to send notification to user: "
                                                + member.getDiscordUserId(),
                                        error);
                            });
        }
    }

    /**
     * Get the booking details
     *
     * @param meeting the abstract meeting
     * @param booking the booking
     * @return the booking info
     */
    private String getBookingDetails(AbstractMeeting meeting, Booking booking) {
        StudyGroup studyGroup = studyGroupController.getStudyGroupById(meeting.getStudyGroup());
        Room room = bookingController.getRoombyId(booking.getRoomId());

        // Format the meeting date and time
        TimeSlot timeSlot = meeting.getTimeSlots().get(0);
        String meetingDate = timeSlot.getStart().format(dateFormatter);
        String meetingStartTime = timeSlot.getStart().format(timeFormatter);
        String meetingEndTime = timeSlot.getEnd().format(timeFormatter);
        String message =
                String.format(
                        "%s The meeting for your study group '%s' is booked!\n\n"
                                + "%s Date: %s\n"
                                + "%s Time: %s - %s\n"
                                + "%s Location: %s\n\n"
                                + "Please be on time!",
                        MEGAPHONE_EMOJI,
                        studyGroup.getName(),
                        CALENDAR_EMOJI,
                        meetingDate,
                        ALARM_EMOJI,
                        meetingStartTime,
                        meetingEndTime,
                        SCHOOL_EMOJI,
                        room.getLocation());

        return message;
    }

    /**
     * The button for user select
     *
     * @param event the event user triggered
     */
    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        final String buttonId = event.getComponentId();

        log.info(buttonId);

        String[] actionId = buttonId.split(":");
        String action = actionId[0] + ":" + actionId[1];

        if (action.equals(CANCEL_BUTTON)) {
            String bookingId = actionId[2];
            Booking booking = bookingController.getBookingById(new ObjectId(bookingId));
            bookingController.cancelBooking(booking, meetingController);
            event.reply(
                            "Booking successfully canceled. Please use /meetings command to view and create booking for you meeting.")
                    .setEphemeral(true)
                    .queue();
        } else if (action.equals(NEVERMIND_BUTTON)) {
            event.reply("No problem.").setEphemeral(true).queue();
        } else {
            event.reply("Error: Unknown action").setEphemeral(true).queue();
        }
    }
}
