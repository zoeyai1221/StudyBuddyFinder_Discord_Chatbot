package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.IteratorHandlerController;
import edu.northeastern.cs5500.starterbot.controller.MeetingController;
import edu.northeastern.cs5500.starterbot.controller.ReminderController;
import edu.northeastern.cs5500.starterbot.controller.StudentController;
import edu.northeastern.cs5500.starterbot.controller.StudyGroupController;
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
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

/**
 * The FindGroupCommand class handles the "findgroups" slash command. It recommends study groups to
 * users based on their interests and allows them to interact with groups by joining or declining.
 *
 * @author Team Wolf
 */
@Slf4j
public class FindGroupCommand implements SlashCommandHandler, ButtonHandler {
    static final String NAME = "findgroups";
    @Inject StudentController studentController;
    @Inject StudyGroupController studyGroupController;
    @Inject CreateGroupCommand createGroupCommand;
    @Inject IteratorHandlerController<StudyGroup> iteratorHandlerController;
    @Inject MeetingController meetingController;
    @Inject ReminderController reminderController;
    @Inject JDA jda;

    static final String JOIN_BUTTON_ACTION = "join";
    static final String DECLINE_BUTTON_ACTION = "decline";
    static final String CANCEL_BUTTON_ACTION = "cancel";
    static final String CREATEGROUP_BUTTON_ACTION = "creategroup";
    static final String JOIN_BUTTON_LABEL = "\uD83D\uDC4D Join!";
    static final String DECLINE_BUTTON_LABEL = "\uD83D\uDE45 Nope!";
    static final String TADA_EMOJI = "\uD83C\uDF89";

    @Inject
    public FindGroupCommand() {
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
     * Builds and returns the command data for the "findgroups" slash command.
     *
     * @return The command data for the slash command.
     */
    @Override
    @Nonnull
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Display study groups that matches your interests.");
    }

    /**
     * Handles the "/findgroups" slash command interaction. Recommends study groups to the user and
     * allows them to interact with the recommendations.
     *
     * @param event The SlashCommandInteractionEvent triggered by the command.
     */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        log.info("event: /findgroups");

        String discordUserId = event.getUser().getId();
        event.deferReply().setEphemeral(true).queue();
        handleGroupIteratorInstantiation(event.getHook(), discordUserId);
    }

    /**
     * Handles the group iterator instantiation
     *
     * @param hook the interaction hook
     * @param discordUserId the discord user id
     */
    public void handleGroupIteratorInstantiation(InteractionHook hook, String discordUserId) {
        // get matched group to iterator
        Iterator<StudyGroup> groupIterator =
                studyGroupController
                        .recommendStudyGroups(discordUserId, studentController)
                        .iterator();
        hook.sendMessage(getSlashCommandResponse(groupIterator, discordUserId).build())
                .setEphemeral(true)
                .queue();
    }

    MessageCreateBuilder getSlashCommandResponse(
            Iterator<StudyGroup> groupIterator, String discordUserId) {
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        if (groupIterator == null || !groupIterator.hasNext()) {
            // suggest user to creat a group if no matched group
            messageCreateBuilder =
                    messageCreateBuilder.addActionRow(
                            Button.primary(
                                    getName() + ":" + CREATEGROUP_BUTTON_ACTION,
                                    "\uD83C\uDD95 Create Group"),
                            Button.secondary(
                                    getName() + ":" + CANCEL_BUTTON_ACTION, "\u274C Not Now"));
            messageCreateBuilder =
                    messageCreateBuilder.setContent(
                            "Unfortunately, there is no study group that matched your interests at the moment.\nWould you like to create your own?");

        } else {
            // there are groups to display
            // store the iterator and the current group to memory
            StudyGroup currentGroup = groupIterator.next();
            IteratorHandler<StudyGroup> matchedGroups =
                    IteratorHandler.<StudyGroup>builder()
                            .currentItem(currentGroup)
                            .discordUserId(discordUserId)
                            .iterator(groupIterator)
                            .build();
            iteratorHandlerController.addIteratorHandler(matchedGroups);

            // disply the first group
            Student groupLeader =
                    studentController.getStudentByStudentId(currentGroup.getGroupLeaderId());
            String groupDetails = formatStudyGroupDetails(currentGroup, groupLeader);
            messageCreateBuilder =
                    new MessageCreateBuilder()
                            .setContent(groupDetails)
                            .addActionRow(
                                    Button.primary(
                                            getName() + ":" + JOIN_BUTTON_ACTION,
                                            JOIN_BUTTON_LABEL),
                                    Button.danger(
                                            getName() + ":" + DECLINE_BUTTON_ACTION,
                                            DECLINE_BUTTON_LABEL));
        }
        return messageCreateBuilder;
    }

    /**
     * Formats the details of a study group into a readable string.
     *
     * @param group
     * @return
     */
    String formatStudyGroupDetails(StudyGroup group, Student groupLeader) {
        // Format the lastActiveTime to YYYY:MM:DD hh:mm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        String lastActiveFormatted = group.getLastActiveTime().format(formatter);

        // formate the interests as comma separated string
        String interests =
                group.getInterestSet().stream()
                        .map(Interest::getStudentInterest)
                        .sorted()
                        .reduce((interest1, interest2) -> interest1 + ", " + interest2)
                        .orElse("None");

        return String.format(
                "I found a study group that matched your interests\n\n"
                        + "**Group Name:** %s\n"
                        + "**Interests:** %s\n"
                        + "**Description:** %s\n"
                        + "**Auto-Approve:** %s\n"
                        + "**Last Active:** %s\n"
                        + "**Max Members:** %d\n"
                        + "**Group Leader:** %s",
                group.getName(),
                interests,
                group.getDescription(),
                group.isAutoApprove() ? "Yes" : "No",
                lastActiveFormatted,
                group.getMaxMembers(),
                groupLeader.getDisplayName());
    }

    /**
     * Handles button interactions
     *
     * @param event The ButtonInteractionEvent triggered by the button click.
     */
    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        String[] buttonIdParts = event.getComponentId().split(":");
        String action = buttonIdParts[1]; // Extract the action (join or decline)
        String discordUserId = event.getUser().getId();

        // case 1: no group
        if (CANCEL_BUTTON_ACTION.equals(action)) {
            event.reply("No problem! Let me know if you change your mind \uD83D\uDE0A")
                    .setEphemeral(true)
                    .queue();
            return;
        } else if (CREATEGROUP_BUTTON_ACTION.equals(action)) {
            createGroupCommand.startCreateGroup(event);
            return;
        }

        // case 2: displaying groups
        // get matched group from memory
        IteratorHandler<StudyGroup> matchedGroups =
                iteratorHandlerController.getIteratorHandlerByDiscordUserId(discordUserId);
        event.reply(handleJoinGroup(action, matchedGroups)).setEphemeral(true).queue();

        // Display the next group
        displayNextGroup(event, matchedGroups);
    }

    /**
     * Handles user actions for joining or declining a study group.
     *
     * @param action
     * @param matchedGroups
     * @return
     */
    String handleJoinGroup(String action, IteratorHandler<StudyGroup> matchedGroups) {
        if (JOIN_BUTTON_ACTION.equals(action)) {
            StudyGroup currentGroup = matchedGroups.getCurrentItem();
            String discordUserId = matchedGroups.getDiscordUserId();
            Student student = studentController.getStudentByDiscordUserId(discordUserId);
            if (currentGroup.isAutoApprove()) {
                // Add student to the study group
                studyGroupController.addStudyGroup(student, currentGroup, studentController);
                meetingController.updateParticipantsAfterJoinGroup(
                        student, currentGroup, reminderController);
                addUserToChannel(student, currentGroup);
                // Notify the user
                return "This group is auto-approved. You are now a member of **"
                        + currentGroup.getName()
                        + "**!";
            } else {
                // Submit a group application
                String message =
                        student.getDisplayName()
                                + " wants to join your group "
                                + currentGroup.getName();
                studyGroupController.submitApplication(
                        student.getId(),
                        discordUserId,
                        currentGroup,
                        message,
                        studentController,
                        meetingController,
                        reminderController);

                // Notify the group leader
                notifyGroupLeader(currentGroup);
                // Notify the user
                return "An application has been submitted to join **"
                        + currentGroup.getName()
                        + "**.";
            }
        }

        // decline button
        return "No problem.";
    }

    /**
     * Displays the next study group in the matched groups iterator.
     *
     * @param event
     * @param matchedGroups
     */
    void displayNextGroup(ButtonInteractionEvent event, IteratorHandler<StudyGroup> matchedGroups) {
        Iterator<StudyGroup> groupIterator = matchedGroups.getIterator();
        if (groupIterator == null || !groupIterator.hasNext()) {
            event.getHook()
                    .sendMessage("\uD83D\uDE22 No more study groups that match your interests.")
                    .setEphemeral(true)
                    .queue();
            iteratorHandlerController.removeIteratorHandler(matchedGroups);
            return;
        }

        StudyGroup currentGroup = groupIterator.next();
        matchedGroups.setCurrentItem(currentGroup);
        iteratorHandlerController.updateIteratorHandler(matchedGroups);

        Student groupLeader =
                studentController.getStudentByStudentId(currentGroup.getGroupLeaderId());
        String groupDetails = formatStudyGroupDetails(currentGroup, groupLeader);

        event.getHook()
                .sendMessage(groupDetails)
                .setActionRow(
                        Button.primary(getName() + ":" + JOIN_BUTTON_ACTION, JOIN_BUTTON_LABEL),
                        Button.danger(
                                getName() + ":" + DECLINE_BUTTON_ACTION, DECLINE_BUTTON_LABEL))
                .setEphemeral(true)
                .queue();
    }

    /**
     * Notify the group leader about new group application through dm
     *
     * @param group
     */
    private void notifyGroupLeader(StudyGroup group) {
        Student leader = studentController.getStudentByStudentId(group.getGroupLeaderId());
        String leaderDiscordId = leader.getDiscordUserId();

        if (leaderDiscordId == null || leaderDiscordId.isBlank()) {
            log.warn("Group {} has no leader Discord ID. Unable to send DM.", group.getName());
            return;
        }

        String message =
                TADA_EMOJI
                        + "Your group **"
                        + group.getName()
                        + "** has received a new application. Please use /viewapplications to check it out!";
        // Retrieve the leader's User object using JDA
        jda.retrieveUserById(leaderDiscordId)
                .queue(
                        user -> {
                            user.openPrivateChannel()
                                    .flatMap(channel -> channel.sendMessage(message))
                                    .queue();
                        });
    }

    /**
     * When student joins a group with auto-approve, add the student to the private channel the
     * group associates with
     *
     * @param student
     * @param studyGroup
     */
    void addUserToChannel(@Nonnull Student student, @Nonnull StudyGroup studyGroup) {
        String channelId = studyGroup.getChannelId();
        String applicantDiscordId = student.getDiscordUserId();
        log.info("!!!Attempting to retrieve channel with ID: {}", channelId);
        if (channelId == null || channelId.isBlank() || jda.getTextChannelById(channelId) == null) {
            log.info("Group channel not found.");
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
}
