package edu.northeastern.cs5500.starterbot.command;

import javax.annotation.Nonnull;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public interface ModalHandler {
    @Nonnull
    public String getName();

    public void onModalInteraction(@Nonnull ModalInteractionEvent event);
}
