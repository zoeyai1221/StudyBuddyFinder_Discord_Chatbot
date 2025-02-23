package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.StudentAvailabilityController;
import edu.northeastern.cs5500.starterbot.controller.StudentController;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.TimeSlot;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

@Slf4j
public class AvailabilityCommand
        implements SlashCommandHandler, ButtonHandler, StringSelectHandler {
    static final String NAME = "availability";
    static final String ENTER_TIME = NAME + ":time";
    static final String ADD_BUTTON = NAME + ":add_time";
    static final String REMOVE_BUTTON = NAME + ":remove_time";
    static final String NOT_NOW = NAME + ":not";
    static List<String> selectedTime = new ArrayList<>();
    static final String FINISH_BUTTON = NAME + ":finish";
    private static final String TIME_EMOJI = "\u23f0";
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
    private final List<String> DAY_ORDER =
            List.of("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");

    // Avoid using magic numbers, the date index will always be 0
    static final Integer DAY_INDEX = 0;

    // dropdown id
    static final String DAY_SELECT = NAME + ":day_select";
    static final String TIME_SELECT = NAME + ":time_select";
    static final String TIMESLOT_SELECT = NAME + ":timeslot_select";

    @Inject StudentController studentController;
    @Inject StudentAvailabilityController studentAvailabilityController;

    @Inject
    public AvailabilityCommand() {
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
     * Builds and returns the command data for the "availability" slash command.
     *
     * @return The command data for the slash command.
     */
    @Override
    @Nonnull
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Set your availability");
    }

    /**
     * Handles the "/availability" slash command interaction
     *
     * @param event the event user triggered
     */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        log.info("event: /availiability");

        String discordUserId = event.getUser().getId();
        Student student = studentController.getStudentByDiscordUserId(discordUserId);

        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        messageCreateBuilder =
                messageCreateBuilder.addActionRow(Button.primary(ADD_BUTTON, "Add Availability"));

        // only show remove button when there are availibility to be removed
        List<TimeSlot> currentAvailability = student.getAvailability();
        if (currentAvailability != null && !currentAvailability.isEmpty()) {
            messageCreateBuilder.addActionRow(Button.danger(REMOVE_BUTTON, "Remove Availability"));
        }

        messageCreateBuilder =
                messageCreateBuilder.addActionRow(Button.secondary(NOT_NOW, "Not Now"));
        messageCreateBuilder =
                messageCreateBuilder.setContent(
                        getCurrentAvailability(student)
                                + "\n\nDo you want to update your availability?");
        event.reply(messageCreateBuilder.build()).setEphemeral(true).setEphemeral(true).queue();
    }

    /**
     * Sort availability
     *
     * @param availability the list of availability
     */
    private List<TimeSlot> sortAvailability(List<TimeSlot> availability) {
        // Sort availability by day (Sunday to Saturday) and then by start time
        availability.sort(
                (slot1, slot2) -> {
                    int dayComparison =
                            Integer.compare(
                                    DAY_ORDER.indexOf(slot1.getDay()),
                                    DAY_ORDER.indexOf(slot2.getDay()));
                    if (dayComparison != 0) {
                        return dayComparison;
                    }
                    return slot1.getStart().compareTo(slot2.getStart());
                });

        return availability;
    }

    /**
     * get availability for student
     *
     * @param student student
     */
    private String getCurrentAvailability(Student student) {
        List<TimeSlot> currentAvailability = sortAvailability(student.getAvailability());

        if (currentAvailability == null || currentAvailability.isEmpty()) {
            return "You have not set any availability yet.";
        }

        // Merge overlapping or contiguous time slots
        Map<String, List<String>> mergedAvailability = new LinkedHashMap<>();
        for (TimeSlot timeSlot : currentAvailability) {
            String day = timeSlot.getDay();
            mergedAvailability.putIfAbsent(day, new ArrayList<>());

            List<String> slotsForDay = mergedAvailability.get(day);

            if (!slotsForDay.isEmpty()) {
                // Parse the last added slot to compare times
                String lastSlot = slotsForDay.get(slotsForDay.size() - 1);
                String[] lastSlotTimes = lastSlot.split(" - ");
                LocalTime lastEnd = LocalTime.parse(lastSlotTimes[1], timeFormatter);
                LocalTime currentStart = timeSlot.getStart().toLocalTime();

                if (!lastEnd.isBefore(currentStart)) {
                    // Merge overlapping or contiguous slots
                    LocalTime currentEnd = timeSlot.getEnd().toLocalTime();
                    String mergedSlot = lastSlotTimes[0] + " - " + currentEnd.format(timeFormatter);
                    slotsForDay.set(slotsForDay.size() - 1, mergedSlot);
                } else {
                    // Add a new time slot
                    slotsForDay.add(
                            String.format(
                                    "%s - %s",
                                    timeSlot.getStart().toLocalTime().format(timeFormatter),
                                    timeSlot.getEnd().toLocalTime().format(timeFormatter)));
                }
            } else {
                // Add the first time slot for the day
                slotsForDay.add(
                        String.format(
                                "%s - %s",
                                timeSlot.getStart().toLocalTime().format(timeFormatter),
                                timeSlot.getEnd().toLocalTime().format(timeFormatter)));
            }
        }

        // Build the availability string
        StringBuilder availabilityListBuilder = new StringBuilder("📅 **Current Availability:**\n");
        for (Map.Entry<String, List<String>> entry : mergedAvailability.entrySet()) {
            String day = entry.getKey();
            String timeSlots = String.join(", ", entry.getValue());
            availabilityListBuilder.append(String.format("- **%s**: %s%n", day, timeSlots));
        }

        return availabilityListBuilder.toString();
    }

    /**
     * All button interaction with helper method
     *
     * @param event the event user triggered
     */
    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        String action = event.getComponentId();
        log.info("Handling button interaction: {}", action);
        if (ADD_BUTTON.equals(action)) {
            handleEnterTimeSlots(event);
        } else if (REMOVE_BUTTON.equals(action)) {
            handleRemoveTimeSlots(event);
        } else if (NOT_NOW.equals(action)) {
            handleSkipSetup(event);
        } else if (FINISH_BUTTON.equals(action)) {
            handleFinishButton(event);
        }
    }

    /**
     * All the selection menu for date + time
     *
     * @param event the event user triggered
     */
    private void handleEnterTimeSlots(ButtonInteractionEvent event) {
        log.info("Selected time");
        event.reply(TIME_EMOJI + " Set your availability")
                .addComponents(
                        ActionRow.of(
                                StringSelectMenu.create(DAY_SELECT)
                                        .setPlaceholder("Day")
                                        .addOption("Monday", "Monday")
                                        .addOption("Tuesday", "Tuesday")
                                        .addOption("Wednesday", "Wednesday")
                                        .addOption("Thursday", "Thursday")
                                        .addOption("Friday", "Friday")
                                        .addOption("Saturday", "Saturday")
                                        .addOption("Sunday", "Sunday")
                                        .build()),
                        ActionRow.of(
                                StringSelectMenu.create(TIME_SELECT)
                                        .setPlaceholder("Choose Time Slot")
                                        .addOption("07:00 AM - 08:00 AM", "07:00AM-08:00AM")
                                        .addOption("08:00 AM - 09:00 AM", "08:00AM-09:00AM")
                                        .addOption("09:00 AM - 10:00 AM", "09:00AM-10:00AM")
                                        .addOption("10:00 AM - 11:00 AM", "10:00AM-11:00AM")
                                        .addOption("11:00 AM - 12:00 PM", "11:00AM-12:00PM")
                                        .addOption(
                                                "12:00 PM - 01:00 PM",
                                                "12:00PM-01:00PM") // Added missing time slot
                                        .addOption("01:00 PM - 02:00 PM", "01:00PM-02:00PM")
                                        .addOption("02:00 PM - 03:00 PM", "02:00PM-03:00PM")
                                        .addOption("03:00 PM - 04:00 PM", "03:00PM-04:00PM")
                                        .addOption("04:00 PM - 05:00 PM", "04:00PM-05:00PM")
                                        .addOption("05:00 PM - 06:00 PM", "05:00PM-06:00PM")
                                        .addOption("06:00 PM - 07:00 PM", "06:00PM-07:00PM")
                                        .addOption("07:00 PM - 08:00 PM", "07:00PM-08:00PM")
                                        .addOption("08:00 PM - 09:00 PM", "08:00PM-09:00PM")
                                        .addOption("09:00 PM - 10:00 PM", "09:00PM-10:00PM")
                                        .addOption("10:00 PM - 11:00 PM", "10:00PM-11:00PM")
                                        .setMaxValues(14)
                                        .setMinValues(1)
                                        .build()))
                .setEphemeral(true)
                .queue();
    }

    /**
     * Handle the removed time slots
     *
     * @param event button event
     */
    private void handleRemoveTimeSlots(ButtonInteractionEvent event) {
        String discordUserId = event.getUser().getId();
        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        List<TimeSlot> availability = sortAvailability(student.getAvailability());

        StringSelectMenu.Builder menuBuilder =
                StringSelectMenu.create(TIMESLOT_SELECT)
                        .setPlaceholder("Select availability to remove")
                        .setMaxValues(availability.size())
                        .setMinValues(1);

        for (TimeSlot slot : availability) {
            String display =
                    String.format(
                            "%s: %s - %s",
                            slot.getDay(),
                            slot.getStart().toLocalTime().format(timeFormatter),
                            slot.getEnd().toLocalTime().format(timeFormatter));
            String value =
                    String.format(
                            "%s|%s|%s",
                            slot.getDay(),
                            slot.getStart().toLocalTime(),
                            slot.getEnd().toLocalTime());

            menuBuilder.addOption(display, value);
        }

        event.reply("Select a time slot to remove:")
                .addActionRow(menuBuilder.build())
                .setEphemeral(true)
                .queue();
    }

    /**
     * Handles the skip button, will be used for /editprofile
     *
     * @param event the event user triggered
     */
    private void handleSkipSetup(ButtonInteractionEvent event) {
        event.reply("Got it! Please use /availibility command to enter your availability later")
                .setEphemeral(true)
                .queue();
    }

    /**
     * Handles the finish button, will be used for /editprofile
     *
     * @param event the event user triggered
     */
    private void handleFinishButton(ButtonInteractionEvent event) {
        event.reply("👍 Perfect! All your time slot has been recorded!").setEphemeral(true).queue();
    }

    /**
     * Get all the user's selction and pass to handleTimeSlotSelectionRecord method
     *
     * @param event the event user triggered
     */
    @Override
    public void onStringSelectInteraction(@Nonnull StringSelectInteractionEvent event) {
        String discordUserId = event.getUser().getId();
        String action = event.getComponentId();
        List<String> currentSelections = event.getInteraction().getValues();
        // Uset list to store all user's response
        selectedTime.addAll(currentSelections);
        if (TIME_SELECT.equals(action)) { // add time case

            log.info("Current Selections: {}", selectedTime);
            String selectedDay = selectedTime.get(DAY_INDEX);
            selectedTime.remove(DAY_INDEX.intValue());
            if (selectedDay != null) {

                handleTimeSlotSelectionRecord(discordUserId, selectedDay, selectedTime, event);
            } else {
                event.reply("Please select a day").setEphemeral(true).queue();
            }
        } else if (TIMESLOT_SELECT.equals(action)) {
            List<String> selectedTimeSlots = event.getInteraction().getValues();
            log.info("Current Selections: {}", selectedTimeSlots);
            handleTimeSlotSelectionRemove(discordUserId, selectedTimeSlots, event);
        } else {
            // deferEdit() to avoid sending multiple messages to user when selecting interests
            event.deferEdit().queue();
        }
    }

    /**
     * Handle time slot selection removed
     *
     * @param discordUserId discord user id
     * @param selectedTimeSlots all selected time slots
     * @param event
     */
    private void handleTimeSlotSelectionRemove(
            String discordUserId,
            List<String> selectedTimeSlots,
            StringSelectInteractionEvent event) {
        // Parse the selected TimeSlot
        boolean allRemoved = true;
        StringBuilder message = new StringBuilder("You have removed this availability.\n");
        for (String selectedTimeSlot : selectedTimeSlots) {
            String[] details = selectedTimeSlot.split("\\|");
            String day = details[0].trim();
            String startTime = details[1];
            String endTime = details[2];

            boolean success =
                    studentAvailabilityController.removeTimeSlot(discordUserId, day, startTime);
            message.append("- ")
                    .append(
                            "**"
                                    + day
                                    + "**: "
                                    + LocalTime.parse(startTime).format(timeFormatter)
                                    + " - "
                                    + LocalTime.parse(endTime).format(timeFormatter))
                    .append("\n");

            if (!success) {
                allRemoved = false;
            }
        }

        if (allRemoved) {
            event.reply(getNextActionMessage(discordUserId, message.toString()).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        event.reply("Failed to remove the time slot. Please use /availability to try again.")
                .setEphemeral(true)
                .queue();
    }

    /**
     * Pass all time slot selected by user to contoller class and store it to student
     *
     * @param userId the user id
     * @param selectedDay the selected day
     * @param timeSlots the time slots
     * @param event the event user triggered
     */
    private void handleTimeSlotSelectionRecord(
            String discordUserId,
            String selectedDay,
            List<String> timeSlots,
            StringSelectInteractionEvent event) {

        StringBuilder successMessage =
                new StringBuilder(
                        "Your availability for has been recorded! \n**" + selectedDay + "**\n");
        boolean allSuccess = true;

        for (String timeSlot : timeSlots) {
            String[] times = timeSlot.split("-");
            String startTime = times[0].trim();
            String endTime = times[1].trim();
            if (studentAvailabilityController.setTimeSlot(
                    discordUserId, selectedDay, startTime, endTime)) {
                successMessage.append("- ").append(timeSlot).append("\n");
            } else {
                allSuccess = false;
            }
        }
        if (allSuccess) {
            event.reply(getNextActionMessage(discordUserId, successMessage.toString()).build())
                    .setEphemeral(true)
                    .queue();
        } else {
            event.reply(
                            "The time slot has not been recorded correctly. Please use /availability to try again.")
                    .setEphemeral(true)
                    .queue();
        }
        // Clear out selectedInterests to store another user's selection
        selectedTime.clear();
    }

    private MessageCreateBuilder getNextActionMessage(String discordUserId, String message) {
        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        messageCreateBuilder =
                messageCreateBuilder.addActionRow(Button.primary(ADD_BUTTON, "Add Availability"));
        List<TimeSlot> currentAvailability = student.getAvailability();
        if (currentAvailability != null && !currentAvailability.isEmpty()) {
            messageCreateBuilder.addActionRow(Button.danger(REMOVE_BUTTON, "Remove Availability"));
        }

        messageCreateBuilder =
                messageCreateBuilder.addActionRow(Button.success(FINISH_BUTTON, "Finish"));
        messageCreateBuilder = messageCreateBuilder.setContent(message);
        return messageCreateBuilder;
    }
}
