package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.BookingController;
import edu.northeastern.cs5500.starterbot.controller.MeetingController;
import edu.northeastern.cs5500.starterbot.controller.ReminderController;
import edu.northeastern.cs5500.starterbot.controller.StudentController;
import edu.northeastern.cs5500.starterbot.controller.StudyGroupController;
import edu.northeastern.cs5500.starterbot.model.AbstractMeeting;
import edu.northeastern.cs5500.starterbot.model.Booking;
import edu.northeastern.cs5500.starterbot.model.InPersonMeeting;
import edu.northeastern.cs5500.starterbot.model.OnlineMeeting;
import edu.northeastern.cs5500.starterbot.model.Room;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import edu.northeastern.cs5500.starterbot.model.TimeSlot;
import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.bson.types.ObjectId;

@Slf4j
public class MeetingsCommand implements SlashCommandHandler, StringSelectHandler {

    static final String NAME = "meetings";

    // Dropdown IDs
    static final String SELECT_MEETING = "meetings-select-meeting";
    static final String MEETING_ACTION = "meetings-select-action";
    static final String CONFIRM_CANCEL_MEETING = "confirm-cancel-meeting";
    static final String REJECT_CANCEL_MEETING = "reject-cancel-meeting";

    // Action types
    static final String ACCEPT_MEETING = "accept-meeting";
    static final String DECLINE_MEETING = "decline-meeting";
    static final String CANCEL_MEETING = "cancel-meeting";
    static final String CREATE_BOOKING = "create-booking";

    // emoji
    static final String THINKING_FACE = "\uD83E\uDD14";
    // Emoji for meeting cancellation
    static final String CANCELLED_MEETING_EMOJI = "\uD83D\uDEAB"; // 🚫

    @Inject MeetingController meetingController;
    @Inject StudentController studentController;
    @Inject StudyGroupController studyGroupController;
    @Inject CreateBooking createBooking;
    @Inject BookingController bookingController;
    @Inject ReminderController reminderController;
    @Inject JDA jda;

    @Inject
    public MeetingsCommand() {
        // Empty constructor for Dagger injection
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
     * Builds and returns the command data for the "meetings" slash command.
     *
     * @return The command data for the slash command.
     */
    @Override
    @Nonnull
    public CommandData getCommandData() {
        return Commands.slash(
                getName(),
                "View and manage study group meetings: view, accept, decline, cancel, or manage bookings.");
    }

    /**
     * Handles the "/meeting" slash command interaction
     *
     * @param event the event user triggered
     */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        log.info("event: /meetings");

        // Get user's Discord ID and find the student
        String discordUserId = event.getUser().getId();
        Student student = studentController.getStudentByDiscordUserId(discordUserId);

        // Get meetings for the student
        List<AbstractMeeting> meetings = meetingController.getMeetingsForStudent(student);

        if (meetings.isEmpty()) {
            event.reply("You have no meetings scheduled.").setEphemeral(true).queue();
            return;
        }

        // Create meeting selection menu
        StringSelectMenu meetingSelectMenu = formatMeetingSelection(meetings);

        event.reply(THINKING_FACE + " Please select a meeting:")
                .setEphemeral(true)
                .addActionRow(meetingSelectMenu)
                .queue();
    }

    private StringSelectMenu formatMeetingSelection(List<AbstractMeeting> meetings) {
        StringSelectMenu.Builder menuBuilder =
                StringSelectMenu.create(SELECT_MEETING).setPlaceholder("Choose a meeting");

        for (AbstractMeeting meeting : meetings) {
            // Get study group name
            StudyGroup studyGroup = studyGroupController.getStudyGroupById(meeting.getStudyGroup());

            // Format meeting details for dropdown
            String meetingLabel =
                    String.format(
                            "%s - %s (%s)",
                            meeting.getTopic(),
                            studyGroup != null ? studyGroup.getName() : "No Group",
                            meeting.getFrequency());

            menuBuilder.addOption(meetingLabel, meeting.getId().toString());
        }

        return menuBuilder.build();
    }

    /**
     * Get all the user's selction for meeting status and update
     *
     * @param event the event user triggered
     */
    @Override
    public void onStringSelectInteraction(@Nonnull StringSelectInteractionEvent event) {
        log.info("MeetingsCommand handling string select interaction: {}", event.getComponentId());

        String discordUserId = event.getUser().getId();
        Student student = studentController.getStudentByDiscordUserId(discordUserId);

        String componentId = event.getComponentId();

        if (componentId.startsWith(SELECT_MEETING)) {
            handleMeetingSelection(event, student);
        } else if (componentId.startsWith(MEETING_ACTION)) {
            handleMeetingAction(event, student);
        } else {
            event.reply("Error: Unknown selection").setEphemeral(true).queue();
        }
    }

    private void handleMeetingAction(StringSelectInteractionEvent event, Student student) {
        String[] parts = event.getInteraction().getValues().get(0).split(":");
        if (parts.length < 2) {
            event.reply("Error: Invalid action selection").setEphemeral(true).queue();
            return;
        }

        String actionType = parts[0];
        String meetingId = parts[1];

        try {
            AbstractMeeting meeting = meetingController.getMeetingById(new ObjectId(meetingId));

            switch (actionType) {
                case ACCEPT_MEETING:
                    meetingController.updateMeetingStatus(
                            meeting.getId(), student.getId(), AbstractMeeting.Status.ACCEPT);
                    event.reply("Meeting accept").setEphemeral(true).queue();
                    break;
                case DECLINE_MEETING:
                    meetingController.updateMeetingStatus(
                            meeting.getId(), student.getId(), AbstractMeeting.Status.DECLINE);
                    TimeSlot timeSlot = meeting.getTimeSlots().get(0);
                    reminderController.deleteReminderForStudentForOneMeeting(
                            student.getDiscordUserId(), timeSlot, meeting.getId());
                    event.reply("Meeting declined").setEphemeral(true).queue();
                    break;
                case CANCEL_MEETING:
                    handleMeetingCancellation(event, meeting, student);
                    break;
                case CONFIRM_CANCEL_MEETING:
                    confirmMeetingCancellation(event, meeting);
                    break;
                case REJECT_CANCEL_MEETING:
                    rejectMeetingCancellation(event);
                    break;
                case CREATE_BOOKING:
                    event.reply(createBooking.startCreateBooking(meeting).build())
                            .setEphemeral(true)
                            .queue();
                    break;
                default:
                    event.reply("Error: Unknown action").setEphemeral(true).queue();
            }
        } catch (Exception e) {
            log.error("Error processing meeting action", e);
            event.reply("An error occurred while processing your action.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    // New method to handle meeting cancellation
    private void handleMeetingCancellation(
            StringSelectInteractionEvent event, AbstractMeeting meeting, Student student) {
        // Check if the user is the meeting organizer
        if (!meeting.getOrganizer().equals(student.getId())) {
            event.reply("Only the meeting organizer can cancel the meeting.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Create confirmation buttons
        StringSelectMenu confirmationMenu =
                StringSelectMenu.create(MEETING_ACTION)
                        .setPlaceholder("Confirm Meeting Cancellation")
                        .addOption(
                                "Confirm Cancellation",
                                CONFIRM_CANCEL_MEETING + ":" + meeting.getId())
                        .addOption("Keep Meeting", REJECT_CANCEL_MEETING + ":" + meeting.getId())
                        .build();

        event.reply("Are you sure you want to cancel this meeting?")
                .setEphemeral(true)
                .addActionRow(confirmationMenu)
                .queue();
    }

    private void confirmMeetingCancellation(
            StringSelectInteractionEvent event, AbstractMeeting meeting) {
        // Perform actual meeting cancellation
        meetingController.cancelMeeting(meeting, bookingController, reminderController);

        // Get study group to include in the notification
        StudyGroup studyGroup = studyGroupController.getStudyGroupById(meeting.getStudyGroup());

        // Notify participants about the cancellation
        notifyParticipantsAboutMeetingCancellation(meeting, studyGroup);

        event.reply(
                        String.format(
                                "Meeting \"%s\" for study group \"%s\" has been canceled.",
                                meeting.getTopic(),
                                studyGroup != null ? studyGroup.getName() : "Unknown Group"))
                .setEphemeral(false) // Make this visible to all group members
                .queue();
    }

    private void rejectMeetingCancellation(StringSelectInteractionEvent event) {
        event.reply("Meeting cancellation was canceled. The meeting remains scheduled.")
                .setEphemeral(true)
                .queue();
    }

    private void notifyParticipantsAboutMeetingCancellation(
            AbstractMeeting canceledMeeting, StudyGroup studyGroup) {

        // Retrieve the meeting organizer
        Student organizer = studentController.getStudentByStudentId(canceledMeeting.getOrganizer());

        // Prepare the cancellation message
        String cancellationMessage =
                CANCELLED_MEETING_EMOJI
                        + " Meeting **"
                        + canceledMeeting.getTopic()
                        + "** for study group **"
                        + studyGroup.getName()
                        + "** has been canceled by **"
                        + (organizer != null ? organizer.getDisplayName() : "Group Leader")
                        + "**";

        // Get all members of the study group
        List<Student> members = studyGroupController.getMemberListOfStudyGroup(studyGroup);

        // Send private message to each member
        for (Student member : members) {
            try {
                jda.retrieveUserById(member.getDiscordUserId())
                        .queue(
                                user -> {
                                    user.openPrivateChannel()
                                            .flatMap(
                                                    channel ->
                                                            channel.sendMessage(
                                                                    cancellationMessage))
                                            .queue(
                                                    success ->
                                                            log.info(
                                                                    "Cancellation notification sent to user: "
                                                                            + member
                                                                                    .getDisplayName()),
                                                    failure ->
                                                            log.error(
                                                                    "Failed to send cancellation notification to user: "
                                                                            + member
                                                                                    .getDisplayName()));
                                },
                                failure ->
                                        log.error(
                                                "Failed to retrieve user for cancellation notification: "
                                                        + member.getDiscordUserId()));
            } catch (Exception e) {
                log.error(
                        "Error sending cancellation notification to member: "
                                + member.getDisplayName(),
                        e);
            }
        }
    }

    private void handleMeetingSelection(StringSelectInteractionEvent event, Student student) {
        try {
            final String selectedMeetingId = event.getInteraction().getValues().get(0);
            AbstractMeeting selectedMeeting =
                    meetingController.getMeetingById(new ObjectId(selectedMeetingId));

            // Display meeting details
            displayMeetingDetails(event, selectedMeeting, student);
        } catch (Exception e) {
            log.error("Error in meeting selection", e);
            event.reply("An error occurred while processing your selection.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void displayMeetingDetails(
            @Nonnull StringSelectInteractionEvent event, AbstractMeeting meeting, Student student) {
        // Get study group details
        StudyGroup studyGroup = studyGroupController.getStudyGroupById(meeting.getStudyGroup());

        // Format time slots as a string
        String timeSlot;
        if (meeting.getTimeSlots().isEmpty()) {
            timeSlot = "No time slots!";
        } else {
            TimeSlot slot = meeting.getTimeSlots().get(0);
            String date = slot.getStart().toLocalDate().toString();
            String startTime = slot.getStart().toLocalTime().toString();
            String endTime = slot.getEnd().toLocalTime().toString();
            timeSlot = String.format("Date: %s, Time: %s - %s", date, startTime, endTime);
        }

        AbstractMeeting.Status userStatus =
                meeting.getParticipants()
                        .getOrDefault(student.getId().toString(), AbstractMeeting.Status.TENTATIVE);

        // Create meeting details string
        String meetingDetails =
                String.format(
                        "**Meeting Details**\n\n"
                                + "**Topic:** %s\n"
                                + "**Type:** %s\n"
                                + "**Frequency:** %s\n"
                                + "**Study Group:** %s\n"
                                + "**Time Slots:** %s\n"
                                + "**Status:** %s\n",
                        meeting.getTopic(),
                        meeting.getType(),
                        meeting.getFrequency(),
                        studyGroup != null ? studyGroup.getName() : "No Associated Group",
                        timeSlot,
                        userStatus.name());

        // Add specific meeting type details
        if (meeting instanceof OnlineMeeting onlineMeeting) {
            meetingDetails +=
                    String.format("**Meeting Link:** %s\n", onlineMeeting.getMeetingLink());
        } else if (meeting instanceof InPersonMeeting inPersonMeeting) {
            String location = "";
            Booking booking = inPersonMeeting.getBooking();
            if (!booking.isConfirmed()) {
                location = "(No Booking is Made)";
            } else {
                Room room = bookingController.getRoombyId(booking.getRoomId());
                location = room.getLocation();
            }

            meetingDetails += String.format("**Location:** %s\n", location);
        }

        // Create action selection menu
        StringSelectMenu actionSelectMenu = getActionSelection(meeting, student);

        // Send the meeting details and action menu
        event.reply(meetingDetails).setEphemeral(true).addActionRow(actionSelectMenu).queue();
    }

    private StringSelectMenu getActionSelection(AbstractMeeting meeting, Student student) {
        StringSelectMenu.Builder menuBuilder =
                StringSelectMenu.create(MEETING_ACTION).setPlaceholder("Select an action");

        String meetingId = meeting.getId().toString();

        // If student is the organizer, add cancel meeting option
        if (meeting.getOrganizer().equals(student.getId())) {
            menuBuilder.addOption("Cancel Meeting", CANCEL_MEETING + ":" + meetingId);
            if (meeting.getType().equals("InPersonMeeting")) {
                // if student if the organizer and the meeting is inperson, add create booking
                // option
                menuBuilder.addOption("Manage Booking", CREATE_BOOKING + ":" + meetingId);
            }
        } else {
            AbstractMeeting.Status currStatus =
                    meeting.getParticipants()
                            .getOrDefault(
                                    student.getId().toString(), AbstractMeeting.Status.TENTATIVE);
            // For participants, add accept/decline options
            if (currStatus == AbstractMeeting.Status.TENTATIVE
                    || currStatus == AbstractMeeting.Status.DECLINE) {
                menuBuilder.addOption("Accept Meeting", ACCEPT_MEETING + ":" + meetingId);
            }
            if (currStatus == AbstractMeeting.Status.TENTATIVE
                    || currStatus == AbstractMeeting.Status.ACCEPT) {
                menuBuilder.addOption("Decline Meeting", DECLINE_MEETING + ":" + meetingId);
            }
        }

        return menuBuilder.build();
    }
}
