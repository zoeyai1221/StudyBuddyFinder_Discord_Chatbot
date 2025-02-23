package edu.northeastern.cs5500.starterbot.command;

import edu.northeastern.cs5500.starterbot.controller.InterestController;
import edu.northeastern.cs5500.starterbot.controller.StudentController;
import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.seeder.InterestConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

@Slf4j
public class SetInterestCommand implements SlashCommandHandler, StringSelectHandler, ButtonHandler {

    static final String NAME = "interests";
    static final String LANGUAGES_SKILLS_MENU_ID = "languages_skills";
    static final String COURSES_PREREQ_CORE_MENU_ID = "courses_prereq_core";
    static final String COURSES_BREADTH_AREA_MENU_ID = "courses_breadth";
    static final String OTHERS_MENU_ID = "others";
    static final String CONTINUE_BUTTON_ID = "continue";
    private static final String SPARKLES_EMOJI = "\u2728";
    private static final String BANGBANG_EMOJI = "\u203C";
    private static final String ROCKET_EMOJI = "\uD83D\uDE80";
    static final String ACCEPT_BUTTON_ID = "accept";
    static final String DECLINE_BUTTON_ID = "decline";
    @Inject FindGroupCommand findGroupCommand;

    // Map to store the menuId to category relationship and their associated labels and options
    private static final Map<String, MenuConfig> menuCategoryMap = new HashMap<>();

    static {
        menuCategoryMap.put(
                COURSES_PREREQ_CORE_MENU_ID,
                new MenuConfig(
                        Set.of(
                                Interest.Category.COURSE_PREREQUISITE,
                                Interest.Category.COURSE_CORE),
                        "Select Align or Core Courses",
                        combineArrays(
                                InterestConstants.COURSE_PREREQUISITE,
                                InterestConstants.COURSE_CORE)));

        menuCategoryMap.put(
                COURSES_BREADTH_AREA_MENU_ID,
                new MenuConfig(
                        Set.of(
                                Interest.Category.COURSE_SYSTEM_SOFTWARE,
                                Interest.Category.COURSE_THEORY_SECURITY),
                        "Select Elective Courses",
                        combineArrays(
                                InterestConstants.COURSE_SYSTEM_SOFTWARE,
                                InterestConstants.COURSE_THEORY_SECURITY,
                                InterestConstants.COURSE_AI_DATA_SCIENCE)));

        menuCategoryMap.put(
                LANGUAGES_SKILLS_MENU_ID,
                new MenuConfig(
                        Set.of(
                                Interest.Category.PROGRAMMING_LANGUAGES,
                                Interest.Category.SOFTWARE_PROGRAMMING_SKILLS),
                        "Select Programming Languages and Skills",
                        combineArrays(
                                InterestConstants.PROGRAMMING_LANGUAGES,
                                InterestConstants.SOFTWARE_PROGRAMMING_SKILLS)));

        menuCategoryMap.put(
                OTHERS_MENU_ID,
                new MenuConfig(
                        Set.of(Interest.Category.OTHER_TOPICS),
                        "Select Other Topics",
                        InterestConstants.OTHER_TOPICS));
    }

    @Inject StudentController studentController;
    @Inject InterestController interestController;

    @Inject
    public SetInterestCommand() {
        // public and empty for dagger
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
     * Builds and returns the command data for the "interests" slash command.
     *
     * @return The command data for the slash command.
     */
    @Override
    @Nonnull
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Select your interests.");
    }

    /**
     * Handles the "/interests" slash command interaction
     *
     * @param event the event user triggered
     */
    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        String discordUserId = event.getUser().getId();
        Set<Interest> currentInterests = studentController.getInterestsForStudent(discordUserId);
        String formattedInterest =
                currentInterests.stream()
                        .map(Interest::getStudentInterest)
                        .collect(Collectors.joining(", "));
        if (currentInterests != null && currentInterests.isEmpty()) {
            event.reply("You don't have any interests right now.").setEphemeral(true).queue();
        } else {
            event.reply("Your current interests:\n " + formattedInterest)
                    .setEphemeral(true)
                    .queue();
        }
        displayInterestMenus(event.getHook());
    }

    /**
     * Get all the user's selction
     *
     * @param event the event user triggered
     */
    @Override
    public void onStringSelectInteraction(@Nonnull StringSelectInteractionEvent event) {
        List<String> newSelections = event.getInteraction().getValues();
        String discordUserId = event.getUser().getId();
        String menuId = event.getComponentId();

        // Retrieve categories to remove based on menuId
        Set<Interest.Category> categoriesToRemove = menuCategoryMap.get(menuId).categories;

        // Remove the interests for the specified categories
        studentController.clearInterestsForStudent(discordUserId, categoriesToRemove);

        // Retrieve the student's current interest set again, and update with new selections
        Set<Interest> currentInterests = studentController.getInterestsForStudent(discordUserId);
        Set<Interest> updatedInterests = new HashSet<>(currentInterests);
        for (String selection : newSelections) {
            Interest interest = interestController.getInterestByInterestName(selection);
            if (interest != null) {
                updatedInterests.add(interest);
            } else {
                log.warn("Interest '{}' not found in the database.", selection);
            }
        }

        studentController.setInterestsForStudent(discordUserId, updatedInterests);

        log.info("Interests updated for user {}: {}", discordUserId, updatedInterests);

        // Defer edit to avoid sending multiple messages to user when selecting interests
        event.deferEdit().queue();
    }

    /**
     * Get all the user's button interaction
     *
     * @param event the event user triggered
     */
    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        String discordUserId = event.getUser().getId();
        String action = event.getComponentId();

        if (ACCEPT_BUTTON_ID.equals(action)) {
            event.deferReply().setEphemeral(true).queue();
            // Trigger FindGroupCommand
            findGroupCommand.handleGroupIteratorInstantiation(event.getHook(), discordUserId);
        } else if (DECLINE_BUTTON_ID.equals(action)) {
            event.reply("No problem! You can use /findgroups anytime to explore groups.")
                    .setEphemeral(true)
                    .queue();
        } else if (CONTINUE_BUTTON_ID.equals(action)) {
            event.reply(handleInterestInteraction(discordUserId))
                    .setEphemeral(true)
                    .queue(
                            hook -> {
                                // Ask user if they want to view groups
                                hook.sendMessage(
                                                "Would you like to view some matching study groups now?")
                                        .addActionRow(
                                                Button.primary(ACCEPT_BUTTON_ID, "Yes!"),
                                                Button.danger(DECLINE_BUTTON_ID, "No, Later"))
                                        .setEphemeral(true)
                                        .queue();
                            });
        } else {
            event.reply("Unknown action. Please try again later.").setEphemeral(true).queue();
        }
    }

    /*
     * Construct interest menus.
     */
    public void displayInterestMenus(@Nonnull InteractionHook hook) {
        List<ActionRow> actionRows = new ArrayList<>();
        for (Map.Entry<String, MenuConfig> entry : menuCategoryMap.entrySet()) {
            String menuId = entry.getKey();
            MenuConfig config = entry.getValue();

            // Create the menu using the data in the map
            StringSelectMenu menu = createMenu(menuId, config.label, config.options);
            actionRows.add(ActionRow.of(menu));
        }

        Button continueButton = Button.primary(CONTINUE_BUTTON_ID, ROCKET_EMOJI + " Continue");

        hook.sendMessage(
                        SPARKLES_EMOJI
                                + "Please let us know your interests! Select up to 3 from each menu:")
                .addComponents(actionRows)
                .addComponents(ActionRow.of(continueButton))
                .setEphemeral(true)
                .queue();
    }

    /**
     * Handles select interest interaction, and returns a reply string
     *
     * @param discordUserId
     * @return reply from bot after user selects interests
     */
    String handleInterestInteraction(@Nonnull String discordUserId) {
        Set<Interest> selectedInterests = studentController.getInterestsForStudent(discordUserId);

        // Prompt user to set interests later if none selected
        if (selectedInterests.isEmpty()) {
            return BANGBANG_EMOJI
                    + " You didn't select any interests, that's fine! You can set them later using the /interests command.";
        }

        String formattedInterests =
                selectedInterests.stream()
                        .map(Interest::getStudentInterest)
                        .collect(Collectors.joining(", "));

        return SPARKLES_EMOJI
                + " Interests updated! Here are your selected interests:\n"
                + formattedInterests
                + ".";
    }

    private StringSelectMenu createMenu(String id, String placeholder, String[] options) {
        StringSelectMenu.Builder builder =
                StringSelectMenu.create(id)
                        .setPlaceholder(placeholder)
                        .setMinValues(0)
                        .setMaxValues(3);

        for (String option : options) {
            builder.addOption(option, option, Emoji.fromUnicode("\uD83D\uDCBB"));
        }

        return builder.build();
    }

    private static String[] combineArrays(String[]... arrays) {
        return Arrays.stream(arrays).flatMap(Arrays::stream).toArray(String[]::new);
    }

    // Helper class to store menu information along with categories
    private static class MenuConfig {
        Set<Interest.Category> categories;
        String label;
        String[] options;

        public MenuConfig(Set<Interest.Category> categories, String label, String[] options) {
            this.categories = categories;
            this.label = label;
            this.options = options;
        }
    }
}
