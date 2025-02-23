package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.BookingController;
import edu.northeastern.cs5500.starterbot.controller.MeetingController;
import edu.northeastern.cs5500.starterbot.controller.ReminderController;
import edu.northeastern.cs5500.starterbot.controller.StudentController;
import edu.northeastern.cs5500.starterbot.controller.StudyGroupController;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.bson.types.ObjectId;

/**
 * The MyStudyGroupsCommand class handles the "mystudygroups" slash command. It allows user to
 * select a study group to perform actions
 *
 * @author Team Wolf
 */
@Slf4j
public class MyStudyGroupsCommand
        implements SlashCommandHandler, StringSelectHandler, ButtonHandler {

    static final String NAME = "mystudygroups";
    @Inject StudentController studentController;
    @Inject StudyGroupController studyGroupController;
    @Inject CreateMeetingCommand createMeetingCommand;
    @Inject MeetingController meetingController;
    @Inject BookingController bookingController;
    @Inject ReminderController reminderController;
    @Inject JDA jda;

    // emoji
    static final String ENVELOPE = "\u2709";
    static final String STAR = "\u2B50";
    static final String BUST_IN_SILHOUETTE = "\uD83D\uDC64";
    static final String CURIOUS_FACE = "\uD83E\uDDD0";
    static final String EMPTY_CHAIR = "\uD83E\uDE91";
    static final String THINKING_FACE = "\uD83E\uDD14";
    static final String FAREWELL = "\uD83D\uDC4B";
    static final String CRYING_FACE = "\uD83D\uDE22";

    // dropdown id
    static final String SELECT_GROUP = "select-group";
    static final String SELECT_ACTION = "select-action";

    // button ids
    static final String CONFIRM_LEAVE = "confirm-leave";
    static final String CONFIRM_DISBAND = "confirm-disband";
    static final String CANCEL = "cancel";

    // actions
    static final String VIEW_MEMBER = "view-member";
    static final String LEAVE_GROUP = "leave-group";
    static final String DISBAND_GROUP = "disband-group";
    static final String CREATE_MEETING = "create-meeting";

    /** Constructs a new instance of MyStudyGroupsCommand. */
    @Inject
    public MyStudyGroupsCommand() {
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
     * Builds and returns the command data for the "mystudygroups" slash command.
     *
     * @return The command data for the slash command.
     */
    @Override
    @Nonnull
    public CommandData getCommandData() {
        return Commands.slash(
                getName(),
                "View and manage your study groups: members list, create meetings, leave, or disband groups.");
    }

    /**
     * Handles the interaction when the "mystudygroups" slash command is executed.
     *
     * @param event the {@link SlashCommandInteractionEvent} triggered by the command.
     */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        log.info("event: /mystudygroups");

        // get user discord id and get student with the discord id
        String discordUserId = event.getUser().getId();
        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        List<ObjectId> groupList = student.getGroupList();
        if (groupList.isEmpty()) {
            event.reply("You are not part of any study group.").setEphemeral(true).queue();
            return;
        }

        StringSelectMenu studyGroupSelectMenu = formatGroupSelection(groupList);

        event.reply(CURIOUS_FACE + " Please select a study group:")
                .setEphemeral(true)
                .addActionRow(studyGroupSelectMenu)
                .queue();
    }

    /** Formats a selection menu for choosing study groups. */
    private StringSelectMenu formatGroupSelection(List<ObjectId> groupList) {
        StringSelectMenu.Builder menuBuilder =
                StringSelectMenu.create(SELECT_GROUP).setPlaceholder("Choose a study group.");

        for (ObjectId groupId : groupList) {
            StudyGroup studyGroup = studyGroupController.getStudyGroupById(groupId);
            menuBuilder.addOption(studyGroup.getName(), groupId.toString());
        }
        return menuBuilder.build();
    }

    /** Returns a selection menu for choosing action to perform to the selected study group */
    private StringSelectMenu getActionSelection(StudyGroup studyGroup, Student student) {
        StringSelectMenu.Builder menuBuilder =
                StringSelectMenu.create(SELECT_ACTION).setPlaceholder("Please select an action:");

        menuBuilder.addOption("View Memebers", VIEW_MEMBER + ":" + studyGroup.getId().toString());

        if (studyGroup.getGroupLeaderId().equals(student.getId())) { // is leader
            menuBuilder.addOption(
                    "Disband Group", DISBAND_GROUP + ":" + studyGroup.getId().toString());
        } else { // not leader
            menuBuilder.addOption("Leave Group", LEAVE_GROUP + ":" + studyGroup.getId().toString());
        }

        menuBuilder.addOption(
                "Create Meeting", CREATE_MEETING + ":" + studyGroup.getId().toString());

        return menuBuilder.build();
    }

    /**
     * Handles the interaction when a study group or action is selected from the dropdown menu.
     *
     * @param event the {@link StringSelectInteractionEvent} triggered by the selection.
     */
    @Override
    public void onStringSelectInteraction(@Nonnull StringSelectInteractionEvent event) {
        final String[] dropdownIdParts = event.getComponentId().split(":");
        final String dropdownId = dropdownIdParts[0];

        String discordUserId = event.getUser().getId();
        Student student = studentController.getStudentByDiscordUserId(discordUserId);
        switch (dropdownId) {
            case SELECT_GROUP:
                handleGroupSelection(event, student);
                break;
            case SELECT_ACTION:
                handleActionSelection(event, student);
                break;
            default:
                event.reply("Error: Unknown selection.").setEphemeral(true).queue();
        }
    }

    private void handleGroupSelection(StringSelectInteractionEvent event, Student student) {
        final String selectedGroupId = event.getInteraction().getValues().get(0);
        StudyGroup selectedGroup =
                studyGroupController.getStudyGroupById(new ObjectId(selectedGroupId));

        StringSelectMenu actionSelectMenu = getActionSelection(selectedGroup, student);

        event.reply(THINKING_FACE + " What would you like to do?")
                .setEphemeral(true)
                .addActionRow(actionSelectMenu)
                .queue();
    }

    private void handleActionSelection(StringSelectInteractionEvent event, Student student) {
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        final String[] selectedAction = event.getInteraction().getValues().get(0).split(":");

        String action = selectedAction[0];
        String studyGroupId = selectedAction[1];
        StudyGroup studyGroup = studyGroupController.getStudyGroupById(new ObjectId(studyGroupId));

        switch (action) {
            case VIEW_MEMBER:
                messageCreateBuilder =
                        getViewMembersMessage(
                                studyGroup,
                                studyGroupController.getMemberListOfStudyGroup(studyGroup));
                break;
            case LEAVE_GROUP:
                messageCreateBuilder = getLeaveGroupMessage(studyGroup);
                break;
            case DISBAND_GROUP:
                messageCreateBuilder = getDisbandGroupMessage(studyGroup);
                break;
            case CREATE_MEETING:
                messageCreateBuilder = createMeetingCommand.startCreateMeeting(studyGroup);
                break;
            default:
                event.reply("Error: Unknown action selected.").setEphemeral(true).queue();
        }

        event.reply(messageCreateBuilder.build()).setEphemeral(true).queue();
    }

    /** Creates a message containing the formatted list of members in the specified study group. */
    MessageCreateBuilder getViewMembersMessage(StudyGroup selectedGroup, List<Student> members) {
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();

        messageCreateBuilder.setContent(formatMemberList(members, selectedGroup));
        return messageCreateBuilder;
    }

    /**
     * Creates a message prompting the user to confirm or cancel leaving the specified study group.
     */
    MessageCreateBuilder getLeaveGroupMessage(StudyGroup leaveGroup) {
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();

        // Prompt the user with confirmation buttons
        messageCreateBuilder
                .setContent(
                        THINKING_FACE
                                + " Are you sure you want to leave **"
                                + leaveGroup.getName()
                                + "**?")
                .addActionRow(
                        Button.danger(
                                getName() + ":" + CONFIRM_LEAVE + ":" + leaveGroup.getId(),
                                "Yes, Leave"),
                        Button.secondary(
                                getName() + ":" + CANCEL + ":" + leaveGroup.getId(), "No, Cancel"));
        return messageCreateBuilder;
    }

    /** Creates a message prompting the user to confirm or cancel disbanding the study group. */
    MessageCreateBuilder getDisbandGroupMessage(StudyGroup disbandGroup) {
        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();

        // Prompt the user with confirmation buttons
        messageCreateBuilder
                .setContent(
                        THINKING_FACE
                                + " Are you sure you want to disband **"
                                + disbandGroup.getName()
                                + "**?")
                .addActionRow(
                        Button.danger(
                                getName() + ":" + CONFIRM_DISBAND + ":" + disbandGroup.getId(),
                                "Yes, Disband"),
                        Button.secondary(
                                getName() + ":" + CANCEL + ":" + disbandGroup.getId(),
                                "No, Cancel"));
        return messageCreateBuilder;
    }

    private void notifyMemberLeaveToLeader(StudyGroup leaveGroup, Student student) {
        String message =
                CRYING_FACE
                        + " A member has left your study group **"
                        + leaveGroup.getName()
                        + "**: "
                        + student.getDisplayName();

        Student leader = studentController.getStudentByStudentId(leaveGroup.getGroupLeaderId());
        if (leader != null) {
            jda.retrieveUserById(leader.getDiscordUserId())
                    .queue(
                            user -> {
                                user.openPrivateChannel()
                                        .flatMap(channel -> channel.sendMessage(message))
                                        .queue();
                            });
        }
    }

    /** Formats the list of members in the selected study group. */
    String formatMemberList(List<Student> members, StudyGroup selectedGroup) {
        if (members.size() == 1) {
            return EMPTY_CHAIR + " This study group has no other members";
        }

        StringBuilder response =
                new StringBuilder("Members of **" + selectedGroup.getName() + "**:\n");
        for (Student member : members) {
            response.append(
                    BUST_IN_SILHOUETTE
                            + " "
                            + member.getDisplayName()
                            + " "
                            + ENVELOPE
                            + " "
                            + member.getEmail());
            if (selectedGroup.getGroupLeaderId().equals(member.getId())) {
                response.append(" " + STAR + " Leader");
            }
            response.append("\n");
        }
        return response.toString();
    }

    /**
     * Handles the interaction when a button is clicked.
     *
     * @param event the event
     */
    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        final String buttonId = event.getComponentId();

        log.info(buttonId);

        String[] actionAndGroupId = buttonId.split(":");
        String action = actionAndGroupId[1];
        String studyGroupId = actionAndGroupId[2];
        StudyGroup studyGroup = studyGroupController.getStudyGroupById(new ObjectId(studyGroupId));

        Student student = studentController.getStudentByDiscordUserId(event.getUser().getId());
        String response = "";

        switch (action) {
            case CONFIRM_LEAVE:
                response = handleLeaveGroup(studyGroup, student);
                break;

            case CONFIRM_DISBAND:
                response = handleDisbandGroup(studyGroup);
                break;

            case CANCEL:
                // Acknowledge the user who canceled
                response = "You have canceled.";
                break;
            default:
                break;
        }

        event.reply(response).setEphemeral(true).queue();
    }

    private String handleDisbandGroup(StudyGroup disbandGroup) {
        // Remove all channel member and delete channel
        removeAllUsersFromChannelAndRemoveChannel(disbandGroup.getId());

        List<Student> members = studyGroupController.getMemberListOfStudyGroup(disbandGroup);
        studyGroupController.disbandGroup(
                disbandGroup,
                studentController,
                meetingController,
                bookingController,
                reminderController);

        // Notify the leader
        notifyMemberAboutDisband(disbandGroup, members);

        // Acknowledge the user who confirmed leaving
        return FAREWELL + " You have disbanded the study group **" + disbandGroup.getName() + "**.";
    }

    private String handleLeaveGroup(StudyGroup leaveGroup, Student student) {
        studyGroupController.leaveGroup(
                leaveGroup, student, meetingController, bookingController, reminderController);
        removeUserFromChannel(student.getDiscordUserId(), leaveGroup.getId());

        // Notify the leader
        notifyMemberLeaveToLeader(leaveGroup, student);

        // Acknowledge the user who confirmed leaving
        return FAREWELL + " You have left the study group **" + leaveGroup.getName() + "**.";
    }

    private void notifyMemberAboutDisband(StudyGroup disbandGroup, List<Student> members) {
        Student leader = studentController.getStudentByStudentId(disbandGroup.getGroupLeaderId());

        if (leader != null) {
            String message =
                    CRYING_FACE
                            + " Leader **"
                            + leader.getDisplayName()
                            + "** decided to disband the study group **"
                            + disbandGroup.getName()
                            + "**";
            for (Student member : members) {
                jda.retrieveUserById(member.getDiscordUserId())
                        .queue(
                                user -> {
                                    user.openPrivateChannel()
                                            .flatMap(channel -> channel.sendMessage(message))
                                            .queue();
                                });
            }
        }
    }

    /**
     * Removes a specific user from the group's private channel.
     *
     * @param discordUserId The Discord user ID of the user to remove.
     * @param groupId The group ID associated with the private channel.
     */
    public void removeUserFromChannel(@Nonnull String discordUserId, @Nonnull ObjectId groupId) {
        StudyGroup studyGroup = studyGroupController.getStudyGroupById(groupId);
        String channelId = studyGroup.getChannelId();

        if (channelId == null || channelId.isBlank()) {
            log.info("Channel ID is null or blank for group ID: {}", groupId);
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            log.info("Channel not found for ID: {}", channelId);
            return;
        }

        channel.getGuild()
                .retrieveMemberById(discordUserId)
                .queue(
                        member -> {
                            log.info(
                                    "Removing user {} from channel {}",
                                    member.getEffectiveName(),
                                    channel.getName());
                            channel.upsertPermissionOverride(member)
                                    .deny(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                                    .queue(
                                            success ->
                                                    log.info(
                                                            "User {} removed from channel {}",
                                                            member.getEffectiveName(),
                                                            channel.getName()),
                                            error ->
                                                    log.error(
                                                            "Failed to remove user permissions: ",
                                                            error));
                        },
                        error ->
                                log.error(
                                        "Failed to retrieve member with ID: {}",
                                        discordUserId,
                                        error));
    }

    /**
     * Removes all users from the private channel and deletes the channel.
     *
     * @param groupId The group ID associated with the private channel.
     */
    public void removeAllUsersFromChannelAndRemoveChannel(@Nonnull ObjectId groupId) {
        StudyGroup studyGroup = studyGroupController.getStudyGroupById(groupId);
        String channelId = studyGroup.getChannelId();

        if (channelId == null || channelId.isBlank()) {
            log.info("Channel ID is null or blank for group ID: {}", groupId);
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            log.info("Channel not found for ID: {}", channelId);
            return;
        }

        log.info("Removing all users and deleting channel {}", channel.getName());
        channel.delete()
                .queue(
                        success -> log.info("Channel {} deleted successfully", channel.getName()),
                        error ->
                                log.error(
                                        "Failed to delete channel {}: ", channel.getName(), error));
    }
}
