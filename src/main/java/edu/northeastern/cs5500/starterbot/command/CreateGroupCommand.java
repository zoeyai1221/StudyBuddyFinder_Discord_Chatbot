package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.InterestController;
import edu.northeastern.cs5500.starterbot.controller.StudentController;
import edu.northeastern.cs5500.starterbot.controller.StudyGroupController;
import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.bson.types.ObjectId;

/**
 * Handles the creation of new study groups through Discord slash commands. This command allows
 * users to create study groups with specific parameters such as name, description, auto-approve
 * settings, and member limits.
 *
 * @author Team Wolf
 */
@Slf4j
public class CreateGroupCommand
        implements SlashCommandHandler, ButtonHandler, StringSelectHandler, ModalHandler {
    static final String NAME = "creategroup";
    @Inject StudentController studentController;
    @Inject StudyGroupController studyGroupController;
    @Inject InterestController interestController;
    @Inject InMemoryRepository<StudyGroup> studyGroupMemory;
    // Action constants
    static final String SELECT_INTEREST_ACTION = "select-interest";
    static final String ENABLE_AUTO_APPROVE_ACTION = "enable-auto-approve";
    static final String DISABLE_AUTO_APPROVE_ACTION = "disable-create-group";
    static final String START_BUTTON = "start";
    private static final int MIN_GROUP_NAME_LENGTH = 3;
    private static final int MAX_GROUP_NAME_LENGTH = 50;
    private static final int MIN_GROUP_DESCRIPTION_LENGTH = 10;
    private static final int MAX_GROUP_DESCRIPTION_LENGTH = 500;
    private static final int MIN_MEMBER = 2;
    private static final int MAX_MEMBER = 500;
    private static final String CROSS_MARK_EMOJI = "\u274C";
    private static final String TADA_EMOJI = "\uD83C\uDF89";
    private static final String ROCKET_EMOJI = "\uD83D\uDE80";
    private static final String WINK_EMOJI = "\uD83D\uDE09";
    private static final String PARTY_EMOJI = "\uD83C\uDF89";

    @Inject
    public CreateGroupCommand() {
        // Empty constructor for Dagger injection
    }

    /**
     * @return The name of the slash command
     */
    @Override
    @Nonnull
    public String getName() {
        return NAME;
    }

    /**
     * Defines the structure and parameters of the create group slash command.
     *
     * @return CommandData object containing command definition and options
     */
    @Override
    @Nonnull
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Create a new study group");
    }

    /**
     * Creates a TextInput component for a modal with the specified parameters.
     *
     * @param customId the unique identifier for the input field
     * @param label the label displayed above the input field
     * @param placeholder the placeholder text shown inside the input field
     * @return a configured TextInput object
     */
    private static TextInput createTextInput(String customId, String label, String placeholder) {
        return TextInput.create(customId, label, TextInputStyle.SHORT)
                .setPlaceholder(placeholder)
                .setRequired(true)
                .build();
    }

    /**
     * Validates if a string meets the specified length constraints and is not null or empty.
     *
     * @param input the string to validate
     * @param minLength the minimum allowable length for the string
     * @param maxLength the maximum allowable length for the string
     * @return true if the string is valid; false otherwise
     */
    private boolean isValidString(String input, int minLength, int maxLength) {
        return input != null
                && !input.trim().isEmpty()
                && input.length() >= minLength
                && input.length() <= maxLength;
    }

    /**
     * Validates a string input and sends an ephemeral reply to the user if the input is invalid.
     *
     * @param input the string to validate
     * @param minLength the minimum allowable length for the string
     * @param maxLength the maximum allowable length for the string
     * @param fieldName the name of the field being validated (used in the reply message)
     * @param event the modal interaction event used to send the reply
     */
    private void validateAndReply(
            String input,
            int minLength,
            int maxLength,
            String fieldName,
            ModalInteractionEvent event) {
        if (!isValidString(input, minLength, maxLength)) {
            event.reply(
                            String.format(
                                    "%s must be between %d and %d characters long. Please try again.",
                                    fieldName, minLength, maxLength))
                    .setEphemeral(true)
                    .queue();
        }
    }

    /**
     * Handles the initial interaction when the user invokes the /creategroup command. Displays a
     * message with a "Start Creating" button that initiates the study group creation workflow.
     *
     * @param event The SlashCommandInteractionEvent triggered by the /creategroup command.
     */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        log.info("event: /creategroup");

        // Create a "Start" button
        Button startButton =
                Button.primary(getName() + ":" + START_BUTTON, PARTY_EMOJI + " Start Creating");

        // Send an ephemeral message with the "Start" button
        event.reply("Ready to create your study group? Click the button below to get started!")
                .setEphemeral(true)
                .addActionRow(startButton)
                .queue();
    }

    /**
     * Handles the interaction triggered by the "Start Creating" button. Prompts the user to select
     * the primary interests for the study group by displaying a dropdown menu.
     *
     * @param event The ButtonInteractionEvent triggered by clicking the "Start Creating" button.
     */
    void startCreateGroup(ButtonInteractionEvent event) {
        String discordUserId = event.getUser().getId();
        Student groupLeader = studentController.getStudentByDiscordUserId(discordUserId);

        // Build a dropdown menu for selecting group interests
        StringSelectMenu.Builder menuBuilder =
                StringSelectMenu.create(SELECT_INTEREST_ACTION)
                        .setPlaceholder("Select your group's primary interests")
                        .setMaxValues(3); // Allow selecting up to 3 interests

        // Add options to the menu
        Set<Interest> interests = groupLeader.getInterestSet();
        log.info(interests.toString());
        log.info(interests.isEmpty() + "");
        if (interests != null && !interests.isEmpty()) {
            interests.forEach(
                    interest ->
                            menuBuilder.addOption(
                                    interest.getStudentInterest(), interest.getId().toString()));
        } else {
            event.reply("Please add you interests using /profile first.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        StringSelectMenu menu = menuBuilder.build();

        // Send an ephemeral message with the menu
        event.reply(
                        "Let's create a study group! First, select the primary interests for your group.")
                .setEphemeral(true)
                .addActionRow(menu)
                .queue();
    }

    /**
     * Handles the interaction when a user selects interests for the group. Stores the selected
     * interests and prompts the user to provide a group name.
     */
    @Override
    public void onStringSelectInteraction(@Nonnull StringSelectInteractionEvent event) {
        String action = event.getComponentId();
        String discordUserId = event.getUser().getId();

        List<String> selectedInterestsId = event.getInteraction().getValues();

        if (SELECT_INTEREST_ACTION.equals(action)) {
            // Convert selected interests values to Interest objects
            Set<Interest> groupInterests = new HashSet<>();
            for (String id : selectedInterestsId) {
                Interest interest = interestController.getInterestByInterestId(new ObjectId(id));
                groupInterests.add(interest);
            }

            Student groupLeader = studentController.getStudentByDiscordUserId(discordUserId);
            // Save the selected interests temporarily (could be stored with the user ID)

            StudyGroup newStudyGroup =
                    StudyGroup.builder()
                            .interestSet(groupInterests)
                            .name("") // Placeholder
                            .description("") // Placeholder
                            .groupLeaderId(groupLeader.getId())
                            .maxMembers(MAX_MEMBER) // Default value; adjust as needed
                            .autoApprove(true) // Default to true; adjust as needed
                            .customCriteria("")
                            .build();
            studyGroupMemory.add(newStudyGroup);

            // Create the modal
            Modal modal =
                    Modal.create(getName(), "Create Study Group")
                            .addActionRow(
                                    createTextInput(
                                            "groupName",
                                            "Group Name",
                                            String.format(
                                                    "Enter the group name (%d-%d characters)",
                                                    MIN_GROUP_NAME_LENGTH,
                                                    MAX_GROUP_NAME_LENGTH))) // Updated placeholder
                            .addActionRow(
                                    createTextInput(
                                            "groupDescription",
                                            "Group Description",
                                            String.format(
                                                    "Enter the group description (%d-%d characters)",
                                                    MIN_GROUP_DESCRIPTION_LENGTH,
                                                    MAX_GROUP_DESCRIPTION_LENGTH))) // Updated
                            // placeholder
                            .addActionRow(
                                    createTextInput(
                                            "maxMembers",
                                            "Maximum number of Members",
                                            String.format(
                                                    "Enter a number between %d and %d",
                                                    MIN_MEMBER,
                                                    MAX_MEMBER))) // Add the range to guide the user
                            // during input
                            .build();

            event.replyModal(modal).queue();
        }
    }

    /**
     * Handles the modal interaction allowing a user to input group name, group description, and
     * maximum number of members in the group. Stores the group information details, and prompts the
     * user to confirm auto-approve.
     *
     * @param event the event
     */
    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        String discordUserId = event.getUser().getId();
        if (event.getModalId().equals(NAME)) {
            // Retrieve inputs from the modal
            String groupName = event.getValue("groupName").getAsString();
            String groupDescription = event.getValue("groupDescription").getAsString();
            int maxMembers;

            // Validate groupName
            if (!isValidString(groupName, MIN_GROUP_NAME_LENGTH, MAX_GROUP_NAME_LENGTH)) {
                validateAndReply(
                        groupName,
                        MIN_GROUP_NAME_LENGTH,
                        MAX_GROUP_NAME_LENGTH,
                        "Group Name",
                        event);
                return;
            }

            // Validate groupDescription
            if (!isValidString(
                    groupDescription, MIN_GROUP_DESCRIPTION_LENGTH, MAX_GROUP_DESCRIPTION_LENGTH)) {
                validateAndReply(
                        groupDescription,
                        MIN_GROUP_DESCRIPTION_LENGTH,
                        MAX_GROUP_DESCRIPTION_LENGTH,
                        "Group Description",
                        event);
                return;
            }

            try {
                maxMembers = Integer.parseInt(event.getValue("maxMembers").getAsString());

                // Validate user input of group members range
                if (maxMembers < MIN_MEMBER || maxMembers > MAX_MEMBER) {
                    event.reply(
                                    String.format(
                                            "The number of members must be between %d and %d. Please try again.",
                                            MAX_MEMBER))
                            .setEphemeral(true)
                            .queue();
                    return;
                }
            } catch (NumberFormatException e) {
                event.reply("Invalid number for maximum members. Please try again.")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            Student groupLeader = studentController.getStudentByDiscordUserId(discordUserId);
            StudyGroupController temporaryStudyGroupController =
                    new StudyGroupController(studyGroupMemory, null, null);
            StudyGroup currentGroup =
                    temporaryStudyGroupController.getStudyGroupForLeader(groupLeader);
            log.info(currentGroup.getInterestSet().toString());
            currentGroup.setName(groupName);
            currentGroup.setDescription(groupDescription);
            currentGroup.setMaxMembers(maxMembers);

            // Save the study group
            studyGroupMemory.update(currentGroup);

            MessageCreateBuilder messageBuilder =
                    new MessageCreateBuilder()
                            .setContent("Do you want to auto-approve new member requests?")
                            .addActionRow(
                                    Button.success(
                                            getName() + ":" + ENABLE_AUTO_APPROVE_ACTION,
                                            "Yes, Auto-Approve"),
                                    Button.danger(
                                            getName() + ":" + DISABLE_AUTO_APPROVE_ACTION,
                                            "No, Manual Approval"));
            // Confirm group creation to the user
            event.reply(messageBuilder.build()).setEphemeral(true).queue();
        }
    }

    /**
     * Handles button interactions for confirming auto-approve or cancelling group creation. And
     * displays a success message when group creation is done.
     *
     * @param event the event user triggered
     */
    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        String[] buttonParts = event.getComponentId().split(":");
        String action = buttonParts[1];

        if (START_BUTTON.equals(action)) {
            // Begin the group creation workflow
            startCreateGroup(event);
            return;
        }

        String discordUserId = event.getUser().getId();
        Student groupLeader = studentController.getStudentByDiscordUserId(discordUserId);

        StudyGroupController temporaryStudyGroupController =
                new StudyGroupController(studyGroupMemory, null, null);
        log.info("studyGroupMemory size: " + studyGroupMemory.getAll().size());

        StudyGroup currentGroup = temporaryStudyGroupController.getStudyGroupForLeader(groupLeader);
        // Handling situation if no group found
        if (currentGroup == null) {
            event.reply(
                            CROSS_MARK_EMOJI
                                    + " Error: No creating study group found for you. Please try again.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (DISABLE_AUTO_APPROVE_ACTION.equals(action)) {
            currentGroup.setAutoApprove(false);
        }

        // Try to save the group to the real database
        StudyGroup createdGroup =
                studyGroupController.createStudyGroup(groupLeader, currentGroup, studentController);

        // Handle situation if create study groups failed
        if (createdGroup == null) {
            event.reply(
                            CROSS_MARK_EMOJI
                                    + " Error: Failed to create the study group. Please try again later.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Remove the temporary group
        studyGroupMemory.delete(currentGroup.getId());
        createPrivateChannelForGroup(event, currentGroup, groupLeader);
        // Confirm successful group creation to the user
        event.reply(
                        ROCKET_EMOJI
                                + "Study Group created! Checkout the new channel!\n"
                                + "Your study buddies are on the way."
                                + WINK_EMOJI)
                .setEphemeral(true)
                .queue();
    }

    // Create a private channel for new study group
    private void createPrivateChannelForGroup(
            ButtonInteractionEvent event, StudyGroup group, Student leader) {
        String leaderDiscordUserId = leader.getDiscordUserId();

        // Validate the guild
        if (event.getGuild() == null) {
            event.reply("Unable to retrieve current server.").setEphemeral(true).queue();
            return;
        }
        Guild currentGuild = event.getGuild();
        // Retrieve the leader as a member
        currentGuild
                .retrieveMemberById(leaderDiscordUserId)
                .queue(
                        member -> createTextChannelForGroupHelper(currentGuild, group, member),
                        error ->
                                log.error(
                                        "Unable to retrieve member with ID: {}",
                                        leaderDiscordUserId,
                                        error));
    }

    // Helper function to create the text channel for the group and set permissions
    private void createTextChannelForGroupHelper(
            Guild currentGuild, StudyGroup group, Member member) {
        currentGuild
                .createTextChannel(group.getName().toLowerCase())
                .setTopic("Private channel for the study group: " + group.getName())
                .addPermissionOverride(
                        currentGuild.getPublicRole(),
                        null,
                        EnumSet.of(Permission.VIEW_CHANNEL)) // Deny public access
                .addPermissionOverride(
                        member,
                        EnumSet.of(
                                Permission.VIEW_CHANNEL,
                                Permission.MANAGE_CHANNEL,
                                Permission.MANAGE_PERMISSIONS),
                        null) // Allow access and manage permissions to the leader
                .queue(
                        channel -> handleChannelCreationSuccess(channel, group),
                        error ->
                                log.error(
                                        "Failed to create private channel for group: {}",
                                        group.getName(),
                                        error));
    }

    // Handle successfully channel creation:
    // 1. Update study group with the new channel id
    // 2. Send welcome message
    private void handleChannelCreationSuccess(TextChannel channel, StudyGroup group) {
        // Save the channel id to the study group
        group.setChannelId(channel.getId());
        studyGroupController.updateStudyGroup(group);
        channel.sendMessage(
                        String.format(
                                TADA_EMOJI
                                        + " Welcome to your new study group! \n"
                                        + "**Group Name:** %s\n"
                                        + "**Interests:** %s\n"
                                        + "**Description:** %s\n"
                                        + "**Max Members:** %d\n"
                                        + "**Auto-Approve:** %s",
                                group.getName(),
                                group.getInterestSet().stream()
                                        .map(Interest::getStudentInterest)
                                        .collect(Collectors.joining(", ")),
                                group.getDescription(),
                                group.getMaxMembers(),
                                group.isAutoApprove() ? "Yes" : "No"))
                .queue();
        log.info("Private channel created and saved for group: {}", group.getName());
    }
}
