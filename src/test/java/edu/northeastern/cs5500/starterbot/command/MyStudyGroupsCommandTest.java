package edu.northeastern.cs5500.starterbot.command;

import static com.google.common.truth.Truth.assertThat;

import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.model.Student;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

class MyStudyGroupsCommandTest {

    /**
     * Verifies that the command name matches the name in the CommandData object. Ensures that the
     * name returned by the `getName` method aligns with the name specified in the command's
     * metadata.
     */
    @Test
    void testNameMatchesData() {
        MyStudyGroupsCommand myStudyGroupsCommand = new MyStudyGroupsCommand();
        String name = myStudyGroupsCommand.getName();
        CommandData commandData = myStudyGroupsCommand.getCommandData();

        assertThat(name).isEqualTo(commandData.getName());
    }

    /**
     * Tests the formatting of a member list when a study group has multiple members. Ensures that
     * the list is properly formatted with emojis and highlights the group leader.
     */
    @Test
    void testFormatMemberListForManyMembers() {
        MyStudyGroupsCommand myStudyGroupsCommand = new MyStudyGroupsCommand();

        // create list of members
        List<Student> members = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Student student = new Student();
            student.setId(new ObjectId());
            student.setDisplayName("test student" + i);
            student.setEmail("test-student" + i + "@northeastern.edu");
            members.add(student);
        }

        // create study group
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setGroupLeaderId(members.get(0).getId());
        studyGroup.setName("test study group");

        String memberListString = myStudyGroupsCommand.formatMemberList(members, studyGroup);

        // build expected string
        // emoji
        final String ENVELOPE = "\u2709";
        final String STAR = "\u2B50";
        final String BUST_IN_SILHOUETTE = "\uD83D\uDC64";

        StringBuilder expectedMemberListString =
                new StringBuilder("Members of **" + "test study group" + "**:\n");
        for (Student member : members) {
            expectedMemberListString.append(
                    BUST_IN_SILHOUETTE
                            + " "
                            + member.getDisplayName()
                            + " "
                            + ENVELOPE
                            + " "
                            + member.getEmail());
            if (studyGroup.getGroupLeaderId().equals(member.getId())) {
                expectedMemberListString.append(" " + STAR + " Leader");
            }
            expectedMemberListString.append("\n");
        }

        assertThat(memberListString).isEqualTo(expectedMemberListString.toString());
    }

    /**
     * Tests the formatting of a member list when the study group has only one member. Ensures that
     * a message indicating no other members is returned.
     */
    @Test
    void testFormatMemberListForOneMember() {
        MyStudyGroupsCommand myStudyGroupsCommand = new MyStudyGroupsCommand();

        // create list of members
        List<Student> members = new ArrayList<>();
        Student student = new Student();
        student.setId(new ObjectId());
        student.setDisplayName("test student");
        student.setEmail("test-student@northeastern.edu");
        members.add(student);

        // create study group
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setGroupLeaderId(student.getId());
        studyGroup.setName("test study group");

        String memberListString = myStudyGroupsCommand.formatMemberList(members, studyGroup);
        final String EMPTY_CHAIR = "\uD83E\uDE91";
        assertThat(memberListString)
                .isEqualTo(EMPTY_CHAIR + " This study group has no other members");
    }

    /**
     * Test for the getViewMembersMessage returns a message content matches the formatted member
     * list.
     *
     * <p>Verifies that the method correctly formats a message displaying the members of the
     * specified study group.
     */
    @Test
    void testGetViewMembersMessage() {
        // setup
        MyStudyGroupsCommand myStudyGroupsCommand = new MyStudyGroupsCommand();

        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(new ObjectId());
        studyGroup.setName("Test Group");

        List<Student> members = new ArrayList<>();
        Interest interest =
                Interest.builder()
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .studentInterest("Java")
                        .build();
        HashSet<Interest> interests = new HashSet<>();
        interests.add(interest);
        List<ObjectId> groupList = new ArrayList<>();
        groupList.add(studyGroup.getId());
        for (int i = 1; i <= 5; i++) {
            Student student =
                    Student.builder()
                            .displayName("Student " + i)
                            .email("fake.student" + i + "@example.com")
                            .discordUserId("12345678912345678" + i)
                            .interestSet(interests)
                            .availability(new ArrayList<>())
                            .groupList(groupList)
                            .build();
            members.add(student);
        }
        studyGroup.setGroupLeaderId(members.get(0).getId());

        // Act
        MessageCreateBuilder result =
                myStudyGroupsCommand.getViewMembersMessage(studyGroup, members);

        // Assert
        assertThat(result.getContent())
                .isEqualTo(myStudyGroupsCommand.formatMemberList(members, studyGroup).trim());
    }

    /**
     * Test for the getLeaveGroupMessage method to returns a message content includes the group name
     * and prompts the user to confirm leaving.
     *
     * <p>Ensures the method correctly generates a confirmation message with appropriate buttons for
     * leaving a study group.
     */
    @Test
    void testGetLeaveGroupMessage() {
        // setup
        MyStudyGroupsCommand myStudyGroupsCommand = new MyStudyGroupsCommand();
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(new ObjectId());
        studyGroup.setName("Test Group");
        MessageCreateBuilder result = myStudyGroupsCommand.getLeaveGroupMessage(studyGroup);

        // Assert
        assertThat(result.getContent())
                .isEqualTo(
                        myStudyGroupsCommand.THINKING_FACE
                                + " Are you sure you want to leave **"
                                + studyGroup.getName()
                                + "**?");

        List<Button> buttons = result.getComponents().get(0).getButtons();

        assertThat(buttons).hasSize(2);
        assertThat(buttons.get(0).getLabel()).isEqualTo("Yes, Leave");
        assertThat(buttons.get(1).getLabel()).isEqualTo("No, Cancel");
    }

    /**
     * Test for the getDisbandGroupMessage method to returns a message content includes the group
     * name and prompts the user to confirm disbanding.
     *
     * <p>Ensures the method correctly generates a confirmation message with appropriate buttons for
     * disbanding a study group.
     */
    @Test
    void testGetDisbandGroupMessage() {
        // setup
        MyStudyGroupsCommand myStudyGroupsCommand = new MyStudyGroupsCommand();
        StudyGroup studyGroup = new StudyGroup();
        studyGroup.setId(new ObjectId());
        studyGroup.setName("Test Group");
        MessageCreateBuilder result = myStudyGroupsCommand.getDisbandGroupMessage(studyGroup);

        // Assert
        assertThat(result.getContent())
                .isEqualTo(
                        myStudyGroupsCommand.THINKING_FACE
                                + " Are you sure you want to disband **"
                                + studyGroup.getName()
                                + "**?");

        List<Button> buttons = result.getComponents().get(0).getButtons();

        assertThat(buttons).hasSize(2);
        assertThat(buttons.get(0).getLabel()).isEqualTo("Yes, Disband");
        assertThat(buttons.get(1).getLabel()).isEqualTo("No, Cancel");
    }
}
