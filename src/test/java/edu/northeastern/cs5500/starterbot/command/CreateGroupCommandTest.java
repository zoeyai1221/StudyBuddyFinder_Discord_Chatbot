package edu.northeastern.cs5500.starterbot.command;

import static com.google.common.truth.Truth.assertThat;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.junit.jupiter.api.Test;

class CreateGroupCommandTest {
    @Test
    void testNameMatchesData() {
        CreateGroupCommand createGroupCommand = new CreateGroupCommand();
        String name = createGroupCommand.getName();
        CommandData commandData = createGroupCommand.getCommandData();

        assertThat(name).isEqualTo(commandData.getName());
    }
}
