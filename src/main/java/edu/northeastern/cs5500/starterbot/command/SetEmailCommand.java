package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.StudentController;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class SetEmailCommand implements SlashCommandHandler {
    static final String NAME = "email";
    @Inject StudentController studentController;

    @Inject
    public SetEmailCommand() {
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
     * Builds and returns the command data for the "email" slash command.
     *
     * @return The command data for the slash command.
     */
    @Override
    @Nonnull
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Set your school email")
                .addOptions(
                        new OptionData(OptionType.STRING, NAME, "Your school email address")
                                .setRequired(true));
    }

    /*
     * Handles direct call for command /email
     */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        handleEmailInteraction(event, false);
    }

    /**
     * Handles email command based on whether the command is called by student directly through
     * /email or the command is being used as a part of /createprofile command
     *
     * @param event
     * @param isProfileCreation true if it is a part of /createprofile, false if student calls it
     *     directly
     */
    private void handleEmailInteraction(
            SlashCommandInteractionEvent event, boolean isProfileCreation) {
        String discordUserId = event.getUser().getId();

        String email = Objects.requireNonNull(event.getOption(NAME)).getAsString();
        if (!studentController.setEmailForStudent(discordUserId, email)) {
            event.reply("Please enter a valid Northeastern email (e.g., name@northeastern.edu)")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Respond based on whether this is during profile creation
        if (isProfileCreation) {
            event.reply("Thank you! Now, please select your interests.").queue();
        } else {
            event.reply("Got it! Your email has been updated to: " + email).queue();
        }
    }
}
