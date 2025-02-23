package edu.northeastern.cs5500.starterbot.command;

import static com.google.common.truth.Truth.assertThat;

import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.model.Interest.Category;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

class FindGroupCommandTest {
    /**
     * Verifies that the command name matches the name in the CommandData object. Ensures that the
     * name returned by the `getName` method aligns with the name specified in the command's
     * metadata.
     */
    @Test
    void testNameMatchesData() {
        FindGroupCommand findGroupCommand = new FindGroupCommand();
        String name = findGroupCommand.getName();
        CommandData commandData = findGroupCommand.getCommandData();

        assertThat(name).isEqualTo(commandData.getName());
    }

    /**
     * Tests formatting of study group details with valid data and auto approved is enable. Ensures
     * that the formatStudyGroupDetails method formats the group details correctly.
     */
    @Test
    void testFormatStudyGroupDetailsAutoApprove() {
        FindGroupCommand findGroupCommand = new FindGroupCommand();
        // Prepare test data
        Student groupLeader = new Student();
        groupLeader.setDisplayName("test leader");
        groupLeader.setId(new ObjectId());

        StudyGroup studyGroup =
                StudyGroup.builder()
                        .name("Math Study Group")
                        .description("A group for discussing advanced math topics.")
                        .autoApprove(true)
                        .lastActiveTime(LocalDateTime.of(2023, 10, 10, 12, 30))
                        .maxMembers(10)
                        .interestSet(
                                Set.of(
                                        Interest.builder()
                                                .studentInterest("Mathematics")
                                                .category(Category.OTHER_TOPICS)
                                                .build(),
                                        Interest.builder()
                                                .studentInterest("Algebra")
                                                .category(Category.OTHER_TOPICS)
                                                .build()))
                        .groupLeaderId(groupLeader.getId())
                        .customCriteria("")
                        .build();

        String expectedOutput =
                "I found a study group that matched your interests\n\n"
                        + "**Group Name:** Math Study Group\n"
                        + "**Interests:** Algebra, Mathematics\n"
                        + "**Description:** A group for discussing advanced math topics.\n"
                        + "**Auto-Approve:** Yes\n"
                        + "**Last Active:** 2023/10/10 12:30\n"
                        + "**Max Members:** 10\n"
                        + "**Group Leader:** test leader";

        // Test the method
        String result = findGroupCommand.formatStudyGroupDetails(studyGroup, groupLeader);

        // Assertions
        assertThat(result).isEqualTo(expectedOutput);
    }

    /**
     * Tests formatting of study group details with valid data and auto approved is disabled.
     * Ensures that the formatStudyGroupDetails method formats the group details correctly.
     */
    @Test
    void testFormatStudyGroupDetailsNotAutoApprove() {
        FindGroupCommand findGroupCommand = new FindGroupCommand();
        // Prepare test data
        Student groupLeader = new Student();
        groupLeader.setDisplayName("test leader");
        groupLeader.setId(new ObjectId());

        StudyGroup studyGroup =
                StudyGroup.builder()
                        .name("Math Study Group")
                        .description("A group for discussing advanced math topics.")
                        .autoApprove(false)
                        .lastActiveTime(LocalDateTime.of(2023, 10, 10, 12, 30))
                        .maxMembers(10)
                        .interestSet(
                                Set.of(
                                        Interest.builder()
                                                .studentInterest("Algebra")
                                                .category(Category.OTHER_TOPICS)
                                                .build()))
                        .groupLeaderId(groupLeader.getId())
                        .customCriteria("")
                        .build();

        String expectedOutput =
                "I found a study group that matched your interests\n\n"
                        + "**Group Name:** Math Study Group\n"
                        + "**Interests:** Algebra\n"
                        + "**Description:** A group for discussing advanced math topics.\n"
                        + "**Auto-Approve:** No\n"
                        + "**Last Active:** 2023/10/10 12:30\n"
                        + "**Max Members:** 10\n"
                        + "**Group Leader:** test leader";

        // Test the method
        String result = findGroupCommand.formatStudyGroupDetails(studyGroup, groupLeader);

        // Assertions
        assertThat(result).isEqualTo(expectedOutput);
    }

    /**
     * Tests the response when no study groups are available. Ensures that the method correctly
     * builds a response suggesting the user create a group.
     */
    @Test
    void testGetSlashCommandResponseNoGroups() {
        Iterator<StudyGroup> emptyIterator = Collections.emptyIterator();
        String discordUserId = "user1";
        FindGroupCommand findGroupCommand = new FindGroupCommand();
        MessageCreateBuilder response =
                findGroupCommand.getSlashCommandResponse(emptyIterator, discordUserId);

        assertThat(response.getContent())
                .isEqualTo(
                        "Unfortunately, there is no study group that matched your interests at the moment.\nWould you like to create your own?");
        assertThat(response.getComponents().size()).isEqualTo(1);
        assertThat(response.getComponents().get(0).getButtons().size()).isEqualTo(2);

        Button createGroupButton = response.getComponents().get(0).getButtons().get(0);
        assertThat(createGroupButton.getLabel()).isEqualTo("\uD83C\uDD95 Create Group");

        Button cancelButton = response.getComponents().get(0).getButtons().get(1);
        assertThat(cancelButton.getLabel()).isEqualTo("\u274C Not Now");
    }
}
