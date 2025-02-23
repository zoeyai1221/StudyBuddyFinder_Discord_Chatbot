package edu.northeastern.cs5500.starterbot.command;

import static com.google.common.truth.Truth.assertThat;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.junit.jupiter.api.Test;

class MeetingsCommandTest {
    @Test
    void testNameMatchesData() {
        MeetingsCommand meetingsCommand = new MeetingsCommand();
        String name = meetingsCommand.getName();
        CommandData commandData = meetingsCommand.getCommandData();

        assertThat(name).isEqualTo(commandData.getName());
    }
}
