package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.GroupApplicationController;
import edu.northeastern.cs5500.starterbot.controller.IteratorHandlerController;
import edu.northeastern.cs5500.starterbot.controller.StudentController;
import edu.northeastern.cs5500.starterbot.controller.StudyGroupController;
import edu.northeastern.cs5500.starterbot.model.GroupApplication;
import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.model.IteratorHandler;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

@Slf4j
public class ViewApplicationsCommand implements SlashCommandHandler, ButtonHandler {
    static final String NAME = "viewapplications";
    static final String ACCEPT_BUTTON_ACTION = "accept";
    static final String DECLINE_BUTTON_ACTION = "decline";
    static final String NEXT_BUTTON_ACTION = "next";
    static final String TADA_EMOJI = "\uD83C\uDF89";
    static final String CRYING_FACE = "\uD83D\uDE22";

    @Inject JDA jda;
    @Inject GroupApplicationController groupApplicationController;
    @Inject StudentController studentController;
    @Inject StudyGroupController studyGroupController;
    @Inject IteratorHandlerController<GroupApplication> iteratorHandlerController;

    @Inject
    public ViewApplicationsCommand() {
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
     * Builds and returns the command data for the "viewapplications" slash command.
     *
     * @return The command data for the slash command.
     */
    @Override
    @Nonnull
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Pending Applications");
    }

    /**
     * Handles the "/viewapplications" slash command interaction
     *
     * @param event the event user triggered
     */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        String discordUserId = event.getUser().getId();
        Iterator<GroupApplication> applicationIterator =
                groupApplicationController.getApplicationsByLeader(discordUserId).iterator();

        event.reply(getSlashCommandResponse(applicationIterator, discordUserId).build())
                .setEphemeral(true)
                .queue();
    }
    /**
     * Creates text reply and buttons when /viewapplications is called
     *
     * @param applicationIterator
     * @param discordUserId
     * @return text reply and buttons
     */
    MessageCreateBuilder getSlashCommandResponse(
            Iterator<GroupApplication> applicationIterator, String discordUserId) {
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();

        if (applicationIterator == null || !applicationIterator.hasNext()) {
            messageCreateBuilder.setContent("You have no pending group applications.");
        } else {
            GroupApplication currentApplication = applicationIterator.next();

            IteratorHandler<GroupApplication> iteratorHandler =
                    IteratorHandler.<GroupApplication>builder()
                            .currentItem(currentApplication)
                            .discordUserId(discordUserId)
                            .iterator(applicationIterator)
                            .build();

            iteratorHandlerController.addIteratorHandler(iteratorHandler);

            messageCreateBuilder
                    .setContent(formatGroupApplicationDetails(currentApplication))
                    .addActionRow(
                            Button.success(NAME + ":" + ACCEPT_BUTTON_ACTION, "Accept"),
                            Button.danger(NAME + ":" + DECLINE_BUTTON_ACTION, "Decline"),
                            Button.secondary(NAME + ":" + NEXT_BUTTON_ACTION, "Next"));
        }
        return messageCreateBuilder;
    }

    /**
     * Format Group Application String into a block of text
     *
     * @param application
     * @return
     */
    String formatGroupApplicationDetails(GroupApplication application) {
        Student sender = studentController.getStudentByDiscordUserId(application.getSender());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        return String.format(
                "**Group Application Details**\n\n"
                        + "**Applicant:** %s\n"
                        + "**Message:** %s\n"
                        + "**Time:** %s\n"
                        + "**Interests:** %s",
                sender.getDisplayName(),
                application.getMessage(),
                application.getTimestamp().format(formatter),
                application.getInterestSet().stream()
                        .map(Interest::getStudentInterest)
                        .reduce((i1, i2) -> i1 + ", " + i2)
                        .orElse("None"));
    }

    /**
     * All button interaction with helper method
     *
     * @param event the event user triggered
     */
    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        String[] buttonIdParts = event.getComponentId().split(":");
        String action = buttonIdParts[1];
        String discordUserId = event.getUser().getId();

        IteratorHandler<GroupApplication> iteratorHandler =
                iteratorHandlerController.getIteratorHandlerByDiscordUserId(discordUserId);

        if (iteratorHandler == null) {
            event.reply("No active group applications.").setEphemeral(true).queue();
            return;
        }
        GroupApplication currentApplication = iteratorHandler.getCurrentItem();
        if (ACCEPT_BUTTON_ACTION.equals(action)) {
            handleAcceptAction(event, currentApplication);
            notifyApplicant(currentApplication, true);
            event.reply(TADA_EMOJI + "Hooray! New member!").setEphemeral(true).queue();
        } else if (DECLINE_BUTTON_ACTION.equals(action)) {
            handleDeclineAction(currentApplication);
            notifyApplicant(currentApplication, false);
            event.reply("Got it, looking forward to the next one!").setEphemeral(true).queue();
        } else if (NEXT_BUTTON_ACTION.equals(action)) {
            event.deferReply().setEphemeral(true).queue();
        }

        displayNextApplication(event, iteratorHandler);
    }

    void handleAcceptAction(
            @Nonnull ButtonInteractionEvent event, @Nonnull GroupApplication application) {
        addUserToChannel(event, application);
        groupApplicationController.acceptApplication(application);
    }

    void handleDeclineAction(@Nonnull GroupApplication application) {
        groupApplicationController.declineApplication(application);
    }

    /**
     * Add the approved user to the private channel of the group. the group is not auto-approve.
     *
     * @param application
     */
    void addUserToChannel(
            @Nonnull ButtonInteractionEvent event, @Nonnull GroupApplication application) {
        StudyGroup studyGroup = studyGroupController.getStudyGroupById(application.getReceiver());
        String channelId = studyGroup.getChannelId();
        String applicantDiscordId = application.getSender();
        log.info("!!!Attempting to retrieve channel with ID: {}", channelId);
        if (channelId == null || channelId.isBlank() || jda.getTextChannelById(channelId) == null) {
            event.reply("Group channel not found.").setEphemeral(true).queue();
            return;
        }
        TextChannel currentChannel = jda.getTextChannelById(channelId);
        currentChannel
                .getGuild()
                .retrieveMemberById(applicantDiscordId)
                .queue(
                        member -> {
                            currentChannel
                                    .upsertPermissionOverride(member)
                                    .grant(
                                            Permission.VIEW_CHANNEL,
                                            Permission.MESSAGE_SEND,
                                            Permission.MESSAGE_HISTORY)
                                    .queue(
                                            success ->
                                                    log.info("Permissions granted successfully."),
                                            error ->
                                                    log.error(
                                                            "Failed to grant permissions: ",
                                                            error));
                        });
    }

    /** Display next application after user clicks Accept or Decline or Next button. */
    void displayNextApplication(
            ButtonInteractionEvent event, IteratorHandler<GroupApplication> iteratorHandler) {
        Iterator<GroupApplication> applicationIterator = iteratorHandler.getIterator();

        if (applicationIterator == null || !applicationIterator.hasNext()) {
            event.getHook().sendMessage("No more pending applications.").setEphemeral(true).queue();
            iteratorHandlerController.removeIteratorHandler(iteratorHandler);
            return;
        }
        // Update the iterator
        GroupApplication nextApplication = applicationIterator.next();
        iteratorHandler.setCurrentItem(nextApplication);
        iteratorHandlerController.updateIteratorHandler(iteratorHandler);

        event.getHook()
                .sendMessage(formatGroupApplicationDetails(nextApplication))
                .setActionRow(
                        Button.success(NAME + ":" + ACCEPT_BUTTON_ACTION, "Accept"),
                        Button.danger(NAME + ":" + DECLINE_BUTTON_ACTION, "Decline"),
                        Button.secondary(NAME + ":" + NEXT_BUTTON_ACTION, "Next"))
                .queue();
    }

    /**
     * Notify the applicant about the status of their application through DM.
     *
     * @param application The application object containing the applicant and group details.
     * @param isAccepted Whether the application was accepted (true) or declined (false).
     */
    private void notifyApplicant(GroupApplication application, boolean isAccepted) {
        String applicantDiscordId = application.getSender();

        if (applicantDiscordId == null || applicantDiscordId.isBlank()) {
            return;
        }
        String groupName =
                studyGroupController.getStudyGroupById(application.getReceiver()).getName();

        String message =
                isAccepted
                        ? TADA_EMOJI
                                + "Congratulations! Your application to join the group **"
                                + groupName
                                + "** has been accepted. Welcome!"
                        : CRYING_FACE
                                + "Sorry that your application to join the group **"
                                + groupName
                                + "** has been declined. But don't worry, use /findgroups to find other groups!";

        jda.retrieveUserById(applicantDiscordId)
                .queue(
                        user -> {
                            user.openPrivateChannel()
                                    .flatMap(channel -> channel.sendMessage(message))
                                    .queue();
                        });
    }
}
