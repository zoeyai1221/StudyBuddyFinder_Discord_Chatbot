package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.ReminderController;
import edu.northeastern.cs5500.starterbot.controller.StudentController;
import edu.northeastern.cs5500.starterbot.model.Student;
import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

public class ReminderCommand implements SlashCommandHandler, StringSelectHandler {
    static final String NAME = "reminder";
    static final Integer ONE = 1;
    static final Integer TEN = 10;
    static final Integer ONE_HUNDRED_TWENTY = 120;
    static final Integer HOUR_MINUTE_CONVERSION = 60;
    @Inject StudentController studentController;
    @Inject ReminderController reminderController;

    @Inject
    public ReminderCommand() {
        // Empty for Dagger
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
     * Builds and returns the command data for the "reminder" slash command.
     *
     * @return The command data for the slash command.
     */
    @Override
    @Nonnull
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Set your reminder preference.");
    }

    /**
     * Handles the "/reminder" slash command interaction
     *
     * @param event the event user triggered
     */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();
        handleReminderPreference(event.getHook());
    }

    /**
     * Handles the reminder preference
     *
     * @param hool the hook
     */
    public void handleReminderPreference(InteractionHook hook) {
        StringSelectMenu menu = showReminderMenu(hook);
        hook.sendMessage("Set a reminder for your meetings:")
                .setEphemeral(true)
                .addActionRow(menu)
                .setEphemeral(true)
                .queue();
    }

    private StringSelectMenu showReminderMenu(InteractionHook hook) {
        // Create the dropdown menu for reminder times
        StringSelectMenu.Builder menuBuilder =
                StringSelectMenu.create(NAME)
                        .setPlaceholder("Set reminder preference")
                        .setMinValues(1)
                        .setMaxValues(1);

        // Add "No Reminder" as the first option
        menuBuilder.addOption("No Reminder", "0");

        // Add options from 10 minutes to 120 minutes in 10-minute increments
        for (int minutes = TEN; minutes <= ONE_HUNDRED_TWENTY; minutes += TEN) {
            int hours = minutes / HOUR_MINUTE_CONVERSION;
            int remainingMinutes = minutes % HOUR_MINUTE_CONVERSION;

            String label =
                    (hours > 0)
                            ? String.format(
                                    "%d hour%s %d minute%s",
                                    hours,
                                    (hours > ONE ? "s" : ""),
                                    remainingMinutes,
                                    (remainingMinutes > ONE ? "s" : ""))
                            : String.format("%d minute%s", minutes, (minutes > ONE ? "s" : ""));
            menuBuilder.addOption(label, String.valueOf(minutes));
        }
        return menuBuilder.build();
    }

    /**
     * The selection board for user to set reminder
     *
     * @param event the event user triggered
     */
    @Override
    public void onStringSelectInteraction(@Nonnull StringSelectInteractionEvent event) {
        if (!NAME.equals(event.getComponentId())) {
            return;
        }

        String userId = event.getUser().getId();
        List<String> selectedValues = event.getValues();
        if (selectedValues.isEmpty()) {
            event.reply("You didn't select a reminder.").setEphemeral(true).queue();
            return;
        }

        int reminderTimeInMin = Integer.parseInt(selectedValues.get(0));
        Student student = studentController.getStudentByDiscordUserId(userId);

        if (reminderTimeInMin == 0) {
            student.setReminderTimeInMin(
                    reminderTimeInMin); // directly set 0 to avoid invoking reminder creation
            event.reply("Okay! If you change mind later, use /reminders command!")
                    .setEphemeral(true)
                    .queue();
        } else {
            // Update the student's reminder time
            reminderController.setReminder(userId, reminderTimeInMin);
            event.reply("Reminder preference has been set successfully!")
                    .setEphemeral(true)
                    .queue();
        }
    }
}
