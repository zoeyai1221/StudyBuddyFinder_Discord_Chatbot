package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.MeetingController;
import edu.northeastern.cs5500.starterbot.controller.ReminderController;
import edu.northeastern.cs5500.starterbot.controller.StudentController;
import edu.northeastern.cs5500.starterbot.controller.StudyGroupController;
import edu.northeastern.cs5500.starterbot.model.AbstractMeeting;
import edu.northeastern.cs5500.starterbot.model.Booking;
import edu.northeastern.cs5500.starterbot.model.Frequency;
import edu.northeastern.cs5500.starterbot.model.InPersonMeeting;
import edu.northeastern.cs5500.starterbot.model.OnlineMeeting;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import edu.northeastern.cs5500.starterbot.model.TimeSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.bson.types.ObjectId;

@Slf4j
public class CreateMeetingCommand implements ButtonHandler, StringSelectHandler, ModalHandler {
    static final String NAME = "createmeeting";
    static final String RECURRING = NAME + ":recurring";
    static final String ONETIME = NAME + ":onetime";
    static final String ONLINE = NAME + ":online";
    static final String IN_PERSON = NAME + ":inperson";
    static final String MODAL_NAME = NAME + ":modal";
    static final String FREQUENCY_SELECT = NAME + ":frequency_select";
    static final String DAY_SELECT = NAME + ":day_select";
    static final String TIME_SELECT = NAME + ":time_select";
    static final String CONFIRM_BUTTON = NAME + ":confirm";
    static final String CANCEL_BUTTON = NAME + ":cancel";
    static final String YES_BUTTON = NAME + ":yes";
    static final String LATER_BUTTON = NAME + ":later";
    static final String SET_REMINDER_BUTTON = NAME + ":setreminder";
    static final String NO_REMINDER_BUTTON = NAME + ":noreminder";

    static List<String> selectedTime = new ArrayList<>();
    // Avoid using magic numbers
    static final Integer FREQ_INDEX = 0;
    static final Integer DAY_INDEX = 1;

    // Emojies
    private static final String INPERSON_EMOJI = "\uD83D\uDD0E";
    private static final String ONLINE_EMOJI = "\uD83D\uDC81";
    private static final String CLOCK_EMOJI = "\uD83D\uDD56";
    private static final String ALARM_EMOJI = "\u23F0";
    private static final String CHECK_EMOJI = "\u2705"; // ✅
    private static final String TADA_EMOJI = "\uD83C\uDF89";
    private static final String SCHOOL_EMOJI = "\uD83C\uDFEB"; // 🏫
    private static final String SMILE_EMOJI = "\uD83D\uDE04"; // 😄
    private static final String LATER_EMOJI = "\u23F3"; // ⏳

    // formatter
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mma");
    private static final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

    @Inject MeetingController meetingController;
    @Inject StudyGroupController studyGroupController;
    @Inject StudentController studentController;
    @Inject CreateBooking createBooking;
    @Inject ReminderCommand reminderCommand;
    @Inject ReminderController reminderController;

    @Inject
    public CreateMeetingCommand() {
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
     * handle the start of creat meeting process and returns a create meeting message
     *
     * @param studyGroup
     * @return
     */
    MessageCreateBuilder startCreateMeeting(StudyGroup studyGroup) {
        log.info("event: createmeeting");

        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        messageCreateBuilder =
                messageCreateBuilder.addActionRow(
                        Button.primary(
                                IN_PERSON + ":" + studyGroup.getId(), INPERSON_EMOJI + "In Person"),
                        Button.primary(ONLINE + ":" + studyGroup.getId(), ONLINE_EMOJI + "Online"));
        messageCreateBuilder =
                messageCreateBuilder.setContent(
                        "Would you like this meeting to be in person or online?");
        return messageCreateBuilder;
    }

    /**
     * Handles button interaction
     *
     * @param event the event user triggered
     */
    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        String discordUserId = event.getUser().getId();
        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        final String buttonId = event.getComponentId();

        log.info(buttonId);

        String[] actionId = buttonId.split(":");
        String action = actionId[0] + ":" + actionId[1];

        log.info("Handling button interaction: {}", action);

        switch (action) {
            case ONLINE:
                createMeetingInMemory(ONLINE, actionId[2], student);
                event.replyModal(meetingTopicLinkModal(true)).queue();
                break;
            case IN_PERSON:
                createMeetingInMemory(IN_PERSON, actionId[2], student);
                event.replyModal(meetingTopicLinkModal(false)).queue();
                break;
            case CONFIRM_BUTTON:
                handleCreateMeeting(discordUserId, event);

                break;
            case CANCEL_BUTTON:
                handleCancelCreateMeeting(discordUserId);
                event.reply(
                                "Please use /createmeeting command again if you want to create a meeting")
                        .setEphemeral(true)
                        .queue();
                break;
            case YES_BUTTON:
                AbstractMeeting meeting =
                        meetingController.getMeetingById(new ObjectId(actionId[2]));
                event.reply(createBooking.startCreateBooking(meeting).build())
                        .setEphemeral(true)
                        .queue();
                break;
            case LATER_BUTTON:
                event.reply(
                                "No problem! "
                                        + SMILE_EMOJI
                                        + " Please use /meetings command to view and create booking for you meeting.")
                        .setEphemeral(true)
                        .queue();
                break;
            case SET_REMINDER_BUTTON:
                event.deferReply().setEphemeral(true).queue();
                reminderCommand.handleReminderPreference(event.getHook());
                break;
            case NO_REMINDER_BUTTON:
                reminderController.setReminder(discordUserId, 0);
                event.reply("Got it, no reminder as of now.").setEphemeral(true).queue();
                break;
            default:
                break;
        }
    }

    /**
     * Get the creating meeting from in memory repository and pass to MeetingController to create
     * and store the meeting to database
     *
     * @param discordUserId
     */
    private void handleCreateMeeting(String discordUserId, @Nonnull ButtonInteractionEvent event) {

        AbstractMeeting meeting =
                meetingController.getMeetingFromMemorybyDiscordId(discordUserId, studentController);

        if (meeting.getType().equals("InPersonMeeting")) {
            InPersonMeeting inPersonMeeting = (InPersonMeeting) meeting;
            meetingController.createInPersonMeeting(
                    inPersonMeeting, studyGroupController, reminderController);
            // give options to choose whether to create booking now
            MessageCreateBuilder messageCreateBuilder = getCreateBookingMessage(inPersonMeeting);
            event.reply(messageCreateBuilder.build()).setEphemeral(true).queue();

        } else if (meeting.getType().equals("OnlineMeeting")) {
            OnlineMeeting onlineMeeting = (OnlineMeeting) meeting;
            meetingController.createOnlineMeeting(
                    onlineMeeting, studyGroupController, reminderController);
            event.reply(TADA_EMOJI + "Onine meeting successfully set!").setEphemeral(true).queue();
        } else {
            throw new IllegalArgumentException("Unpexted meeting type");
        }
        // Prompt user to set reminder
    }

    private MessageCreateBuilder getCreateBookingMessage(InPersonMeeting inPersonMeeting) {
        // Get the first time slot for the next meeting
        TimeSlot nextTimeSlot = inPersonMeeting.getTimeSlots().get(0);

        String nextMeetingDate = nextTimeSlot.getStart().format(dateFormatter);
        String nextMeetingStartTime = nextTimeSlot.getStart().format(timeFormatter);
        String nextMeetingEndTime = nextTimeSlot.getEnd().format(timeFormatter);

        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        messageCreateBuilder.setContent(
                TADA_EMOJI
                        + "In-person meeting successfully set!\n\n"
                        + CLOCK_EMOJI
                        + "Next meeting: "
                        + nextMeetingDate
                        + "\n"
                        + "Time: "
                        + nextMeetingStartTime
                        + " - "
                        + nextMeetingEndTime
                        + "\n\n"
                        + SCHOOL_EMOJI
                        + "Do you want to make a booking of an on-campus room for the meeting?");

        // Add options for booking
        messageCreateBuilder =
                messageCreateBuilder.addActionRow(
                        Button.primary(
                                YES_BUTTON + ":" + inPersonMeeting.getId().toString(),
                                CHECK_EMOJI + " Yes"),
                        Button.danger(LATER_BUTTON, LATER_EMOJI + " Later"));
        return messageCreateBuilder;
    }

    /** Remove the creating meeting in memory repository for the user */
    private void handleCancelCreateMeeting(String discordUserId) {
        meetingController.deleteMeetingFromMemory(
                meetingController
                        .getMeetingFromMemorybyDiscordId(discordUserId, studentController)
                        .getId());
    }

    /** create a new meeting based on meeting type to in memory repository */
    private void createMeetingInMemory(String meetingType, String studyGroupId, Student student) {
        log.info("Creating " + meetingType);
        StudyGroup studyGroup = studyGroupController.getStudyGroupById(new ObjectId(studyGroupId));
        if (meetingType.equals(ONLINE)) {

            OnlineMeeting newOnlineMeeting =
                    OnlineMeeting.builder()
                            .id(new ObjectId())
                            .topic("")
                            .frequency(Frequency.ONETIME) // default
                            .timeSlots(new ArrayList<>())
                            .studyGroup(studyGroup.getId())
                            .organizer(student.getId())
                            .meetingLink("")
                            .participants(new HashMap<>())
                            .build();
            meetingController.addMeetingToMemory(newOnlineMeeting);
            log.info("Creating " + newOnlineMeeting.getType());
        } else {
            // in person
            InPersonMeeting newInPersonMeeting =
                    InPersonMeeting.builder()
                            .id(new ObjectId())
                            .topic("")
                            .frequency(Frequency.ONETIME) // default
                            .timeSlots(new ArrayList<>())
                            .studyGroup(studyGroup.getId())
                            .organizer(student.getId())
                            .booking(new Booking())
                            .participants(new HashMap<>())
                            .build();
            meetingController.addMeetingToMemory(newInPersonMeeting);
            log.info("Creating " + newInPersonMeeting.getType());
        }
    }

    /** Modal for user to enter topic and link */
    private Modal meetingTopicLinkModal(boolean needLink) {
        Modal.Builder modal = Modal.create(MODAL_NAME, "Enter Meeting Details!");

        // Add the meeting topic input field
        TextInput meetingTopicInput =
                TextInput.create("meetingTopic", "Meeting Topic", TextInputStyle.SHORT)
                        .setPlaceholder("Enter the meeting topic")
                        .setRequired(true)
                        .build();
        modal.addActionRow(meetingTopicInput);

        // Conditionally add the meeting link input field if required
        if (needLink) {
            TextInput meetingLinkInput =
                    TextInput.create("meetingLink", "Meeting Link", TextInputStyle.SHORT)
                            .setPlaceholder("Enter the meeting link")
                            .setRequired(true)
                            .build();
            modal.addActionRow(meetingLinkInput);
        }

        // Build and return the modal
        return modal.build();
    }

    /**
     * Handles the modal interaction allowing a user to input
     *
     * @param event the modal interacte event
     */
    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        String modalId = event.getModalId();
        String discordUserId = event.getUser().getId();
        Student student = studentController.getStudentByDiscordUserId(discordUserId);

        log.info("the modal id {}: " + modalId);
        if (MODAL_NAME.equals(modalId)) {
            String topic = event.getValue("meetingTopic").getAsString();

            // update meeting topic in memory

            AbstractMeeting meeting =
                    meetingController.getMeetingFromMemorybyDiscordId(
                            discordUserId, studentController);
            meeting.setTopic(topic);
            meetingController.updateMeetingToMemory(meeting);

            StringBuilder message =
                    new StringBuilder(CHECK_EMOJI + "Got it! The topic will be " + topic);

            log.info("in modal " + meeting.getType());

            if (meeting.getType().equals("OnlineMeeting")) {
                String meetingLink = event.getValue("meetingLink").getAsString();
                OnlineMeeting onlineMeeting = (OnlineMeeting) meeting;
                onlineMeeting.setMeetingLink(meetingLink);
                meetingController.updateMeetingToMemory(onlineMeeting);
                message.append("\n The meeting link is set to" + meetingLink);
            }

            StudyGroup studyGroup = studyGroupController.getStudyGroupById(meeting.getStudyGroup());

            event.reply("How often would you like the meeting to repeat?")
                    .addActionRow(frequencySelection(student, studyGroup))
                    .setEphemeral(true)
                    .queue();
        }
    }

    /**
     * All the selection menu for frequency
     *
     * @param event the event user triggered
     */
    private static StringSelectMenu frequencySelection(Student student, StudyGroup studyGroup) {
        log.info("Select frequency");
        StringSelectMenu.Builder menuBuilder =
                StringSelectMenu.create(FREQUENCY_SELECT)
                        .setPlaceholder("Choose meeting frequency")
                        .addOption("One-Time", Frequency.ONETIME.toString());

        if (student.getId().equals(studyGroup.getGroupLeaderId())) {
            // Add recurring options only if the user is the group leader
            menuBuilder
                    .addOption("Weekly", Frequency.WEEKLY.toString())
                    .addOption("Biweekly", Frequency.BIWEEKLY.toString())
                    .addOption("Monthly", Frequency.MONTHLY.toString());
        }

        return menuBuilder.build();
    }

    /**
     * Handles the selection menu for date + time
     *
     * @param event the event user triggered
     */
    MessageCreateBuilder getEnterTimeSlotsMessage() {
        log.info("Building message for selecting meeting time");

        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();

        messageCreateBuilder.setContent(
                CLOCK_EMOJI
                        + " Perfect! Please confirm the meeting length, choose the date, start time, and end time only");

        // Generate options for the next 7 days starting tomorrow
        StringSelectMenu.Builder daySelectMenuBuilder =
                StringSelectMenu.create(DAY_SELECT).setPlaceholder("Choose a date");

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        for (int i = 0; i < 7; i++) {
            LocalDate date = tomorrow.plusDays(i);
            String label = date.format(dateFormatter); // e.g., "Tuesday, March 14"
            daySelectMenuBuilder.addOption(label, date.toString()); // Use ISO-8601 format as value
        }

        messageCreateBuilder.addComponents(
                ActionRow.of(daySelectMenuBuilder.build()),
                ActionRow.of(
                        StringSelectMenu.create(TIME_SELECT)
                                .setPlaceholder("Choose Time Slot")
                                .addOption("07:00AM - 08:00AM", "07:00AM-08:00AM")
                                .addOption("08:00AM - 09:00AM", "08:00AM-09:00AM")
                                .addOption("09:00AM - 10:00AM", "09:00AM-10:00AM")
                                .addOption("10:00AM - 11:00AM", "10:00AM-11:00AM")
                                .addOption("11:00AM - 12:00PM", "11:00AM-12:00PM")
                                .addOption("12:00PM - 01:00PM", "12:00PM-01:00PM")
                                .addOption("01:00PM - 02:00PM", "01:00PM-02:00PM")
                                .addOption("02:00PM - 03:00PM", "02:00PM-03:00PM")
                                .addOption("03:00PM - 04:00PM", "03:00PM-04:00PM")
                                .addOption("04:00PM - 05:00PM", "04:00PM-05:00PM")
                                .addOption("05:00PM - 06:00PM", "05:00PM-06:00PM")
                                .addOption("06:00PM - 07:00PM", "06:00PM-07:00PM")
                                .addOption("07:00PM - 08:00PM", "07:00PM-08:00PM")
                                .addOption("08:00PM - 09:00PM", "08:00PM-09:00PM")
                                .addOption("09:00PM - 10:00PM", "09:00PM-10:00PM")
                                .addOption("10:00PM - 11:00PM", "10:00PM-11:00PM")
                                .setMaxValues(2)
                                .setMinValues(1)
                                .build()));

        return messageCreateBuilder;
    }

    /**
     * Handles selection from dropdown menu
     *
     * @param event the event user triggered
     */
    @Override
    public void onStringSelectInteraction(@Nonnull StringSelectInteractionEvent event) {
        String discordUserId = event.getUser().getId();
        List<String> currentSelections = event.getInteraction().getValues();
        // Uset list to store all user's response
        selectedTime.addAll(currentSelections);
        log.info("Current Selections: {}", selectedTime);

        AbstractMeeting meeting =
                meetingController.getMeetingFromMemorybyDiscordId(discordUserId, studentController);

        if (FREQUENCY_SELECT.equals(event.getComponentId())) {
            String frequency = event.getInteraction().getValues().get(0);
            Objects.requireNonNull(frequency);
            boolean setFrequency = true;
            meeting.setFrequency(Frequency.valueOf(frequency.toUpperCase()));
            meetingController.updateMeetingToMemory(meeting);
            if (setFrequency) {
                MessageCreateBuilder messageBuilder = getEnterTimeSlotsMessage();
                event.reply(messageBuilder.getContent())
                        .addComponents(messageBuilder.getComponents())
                        .setEphemeral(true)
                        .queue();
            } else {
                event.reply("Error recording the frequency please try again!!!!");
            }
            log.info("Meeting command frequency: " + frequency);
        } else if (TIME_SELECT.equals(event.getComponentId())) {
            String selectedDay = selectedTime.get(DAY_INDEX);
            selectedTime.remove(FREQ_INDEX.intValue());
            selectedTime.remove(DAY_INDEX.intValue() - 1);

            log.info("Removed Selections: {}", selectedTime);

            if (selectedDay != null) {
                handleMeetingInfoRecord(selectedDay, selectedTime, event, meeting);
            } else {
                event.reply("Please select a day").setEphemeral(true).queue();
            }
        } else {
            // deferEdit() to avoid sending multiple messages to user when selecting interests
            event.deferEdit().queue();
        }
    }

    /**
     * Handles the recording of meeting information, including recurring time slots for weekly,
     * biweekly, or monthly meetings, and updates the meeting record.
     *
     * @param discordUserId
     * @param selectedDay
     * @param timeSlots
     * @param event
     * @param meeting
     */
    private void handleMeetingInfoRecord(
            String selectedDate,
            List<String> timeSlots,
            StringSelectInteractionEvent event,
            AbstractMeeting meeting) {

        StringBuilder successMessage =
                new StringBuilder(ALARM_EMOJI + " Confirming the meeting time \n");
        successMessage.append("Selected Date: ").append(selectedDate).append("\n");
        successMessage
                .append("Selected Time Slots: \n")
                .append("- ")
                .append(String.join(", ", timeSlots))
                .append("\n");

        List<TimeSlot> timeSlotList = new ArrayList<>();

        // Parse the selected date
        LocalDate date;
        try {
            date = LocalDate.parse(selectedDate); // Parse ISO-8601 formatted date
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid date format. Expected ISO-8601 format (e.g., 2024-03-14).");
        }

        // Process each selected time slot
        for (String timeSlot : timeSlots) {
            String[] times = timeSlot.split("-");
            String startTime = times[0].trim();
            String endTime = times[1].trim();
            LocalTime start = LocalTime.parse(startTime, timeFormatter);
            LocalTime end = LocalTime.parse(endTime, timeFormatter);

            // Validate the time range
            if (!start.isBefore(end)) {
                throw new IllegalArgumentException("Start time must be earlier than the end time!");
            }

            // Convert to ZonedDateTime
            ZoneId zoneId = ZoneId.of("UTC");
            ZonedDateTime startZone = start.atDate(date).atZone(zoneId);
            ZonedDateTime endZone = end.atDate(date).atZone(zoneId);

            // Add recurring time slots if necessary
            switch (meeting.getFrequency()) {
                case WEEKLY:
                    addRecurringTimeSlots(timeSlotList, startZone, endZone, 7);
                    break;
                case BIWEEKLY:
                    addRecurringTimeSlots(timeSlotList, startZone, endZone, 14);
                    break;
                case MONTHLY:
                    addRecurringTimeSlots(timeSlotList, startZone, endZone, 28);
                    break;
                default:
                    // Add a single instance for one-time meetings
                    timeSlotList.add(
                            createTimeSlot(
                                    startZone.getDayOfWeek().toString(), startZone, endZone));
            }
        }

        meeting.setTimeSlots(timeSlotList);
        meetingController.updateMeetingToMemory(meeting);

        // Prepare response message
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        messageCreateBuilder.addActionRow(
                Button.primary(CONFIRM_BUTTON, CHECK_EMOJI + " Confirm"),
                Button.danger(CANCEL_BUTTON, "Cancel"));
        messageCreateBuilder.setContent(successMessage.toString());
        event.reply(messageCreateBuilder.build()).setEphemeral(true).queue();

        selectedTime.clear();
    }

    /** Adds recurring time slots to the list based on the specified interval and end date. */
    private void addRecurringTimeSlots(
            List<TimeSlot> timeSlotList,
            ZonedDateTime startZone,
            ZonedDateTime endZone,
            int intervalDays) {

        ZonedDateTime currentStart = startZone;
        ZonedDateTime currentEnd = endZone;

        // Add time slots for a year
        while (currentStart.isBefore(ZonedDateTime.now().plusYears(1))) {
            timeSlotList.add(
                    createTimeSlot(
                            currentStart.getDayOfWeek().toString(), currentStart, currentEnd));
            currentStart = currentStart.plusDays(intervalDays);
            currentEnd = currentEnd.plusDays(intervalDays);
        }
    }

    /** Creates a new time slot with the specified day and time range. */
    private TimeSlot createTimeSlot(String day, ZonedDateTime startZone, ZonedDateTime endZone) {
        return TimeSlot.builder()
                .day(day)
                .start(startZone.toLocalDateTime())
                .end(endZone.toLocalDateTime())
                .build();
    }
}
