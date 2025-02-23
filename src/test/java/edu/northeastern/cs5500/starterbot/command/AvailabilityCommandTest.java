package edu.northeastern.cs5500.starterbot.command;

import static com.google.common.truth.Truth.assertThat;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.junit.jupiter.api.Test;

class AvailabilityCommandTest {
    @Test
    void testNameMatchesData() {
        AvailabilityCommand availabilityCommand = new AvailabilityCommand();
        String name = availabilityCommand.getName();
        CommandData commandData = availabilityCommand.getCommandData();

        assertThat(name).isEqualTo(commandData.getName());
    }
}
