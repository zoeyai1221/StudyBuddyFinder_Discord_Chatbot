package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.StudentController;
import edu.northeastern.cs5500.starterbot.model.Student;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class SetProfileCommand implements SlashCommandHandler, ModalHandler {
    static final String NAME = "profile";
    static final String MODAL_ID = "profilemodal";
    static final String DISPLAY_NAME_ID = "displayname";
    static final String EMAIL_ID = "email";
    static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$");
    @Inject StudentController studentController;
    @Inject SetDisplayNameCommand setDisplayNameCommand;
    @Inject SetEmailCommand setEmailCommand;
    @Inject SetInterestCommand setInterestCommand;
    @Inject FindGroupCommand findGroupCommand;

    @Inject
    public SetProfileCommand() {
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
     * Builds and returns the command data for the "profile" slash command.
     *
     * @return The command data for the slash command.
     */
    @Override
    @Nonnull
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Set up your profile.");
    }

    /**
     * Handles the "/profile" slash command interaction
     *
     * @param event the event user triggered
     */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        Modal modal =
                Modal.create(MODAL_ID, "Set Up Your Profile")
                        .addActionRow(
                                TextInput.create(
                                                DISPLAY_NAME_ID,
                                                "Display Name",
                                                TextInputStyle.SHORT)
                                        .setRequired(true)
                                        .setMinLength(1)
                                        .setMaxLength(100)
                                        .build())
                        .addActionRow(
                                TextInput.create(EMAIL_ID, "Email", TextInputStyle.SHORT)
                                        .setRequired(true)
                                        .setPlaceholder("example@northeastern.edu")
                                        .build())
                        .build();

        event.replyModal(modal).queue();
    }

    /** Display modal and let user to select interest */
    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        if (!event.getModalId().equals(MODAL_ID)) {
            return;
        }

        String displayName = event.getValue(DISPLAY_NAME_ID).getAsString();
        String email = event.getValue(EMAIL_ID).getAsString();

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            event.reply("Invalid email format. Please try again.").setEphemeral(true).queue();
            return;
        }

        // Save user profile
        String discordUserId = event.getUser().getId();
        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        student.setDisplayName(displayName);
        student.setEmail(email);

        studentController.updateStudent(student);

        event.reply("Profile saved successfully! Please select your interests.")
                .setEphemeral(true)
                .queue(
                        hook -> {
                            // Display the interest menus using SetInterestCommand
                            setInterestCommand.displayInterestMenus(hook);
                        });
    }
}
