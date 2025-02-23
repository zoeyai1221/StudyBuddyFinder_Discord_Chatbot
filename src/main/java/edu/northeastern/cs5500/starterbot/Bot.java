package edu.northeastern.cs5500.starterbot;

import dagger.Component;
import edu.northeastern.cs5500.starterbot.command.CommandModule;
import edu.northeastern.cs5500.starterbot.controller.ReminderController;
import edu.northeastern.cs5500.starterbot.listener.MessageListener;
import edu.northeastern.cs5500.starterbot.repository.RepositoryModule;
import edu.northeastern.cs5500.starterbot.seeder.InterestSeeder;
import edu.northeastern.cs5500.starterbot.seeder.RoomSeeder;
import edu.northeastern.cs5500.starterbot.service.OpenTelemetryService;
import edu.northeastern.cs5500.starterbot.service.ServiceModule;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

@Component(
        modules = {
            CommandModule.class,
            RepositoryModule.class,
            ServiceModule.class,
        })
@Singleton
interface BotComponent {
    public Bot bot();
}

@Slf4j
public class Bot {

    @Inject
    Bot() {}

    @Inject MessageListener messageListener;
    @Inject OpenTelemetryService openTelemetryService;
    @Inject JDA jda;
    @Inject InterestSeeder interestSeeder;
    @Inject RoomSeeder roomSeeder;
    @Inject ReminderController reminderController;

    private static final String WELCOME_CHANNEL = "1317335221544161291";
    private static final String TADA_EMOJI = "\uD83C\uDF89";
    private static final String ROCKET_EMOJI = "\uD83D\uDE80";
    private static final String STAR_EMOJI = "\u2B50";

    void start() {
        var span = openTelemetryService.span("updateCommands", SpanKind.PRODUCER);
        try (Scope scope = span.makeCurrent()) {
            jda.addEventListener(messageListener);
            CommandListUpdateAction commands = jda.updateCommands();
            commands.addCommands(messageListener.allCommandData());
            commands.queue();
            interestSeeder.seedInterests();

            String channelId = WELCOME_CHANNEL;
            String welcomeMessage =
                    TADA_EMOJI
                            + " Welcome to StudyBuddyFinder!\n"
                            + " Start with /profile, then we will find some matching groups for you!\n\n"
                            + ROCKET_EMOJI
                            + "Some helpful commands:\n"
                            + "/mystudygroups : Manage your study groups and create meetings\n"
                            + "/meetings : Manage your meetings\n"
                            + "/interests: When you want to change your interests\n"
                            + "/findgroups: When you want to find some more groups that match your interests\n"
                            + "/viewapplications: View applications to your groups\n"
                            + "/reminder: Set up reminders for your meetings\n"
                            + STAR_EMOJI
                            + " There are more, feel free to explore!";

            var channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(welcomeMessage).queue();
            } else {
                log.error("Channel not found. Check the channel ID.");
            }
            roomSeeder.seedRooms();
            reminderController.start();
        } catch (Exception e) {
            log.error("Unable to add message listeners", e);
            span.recordException(e);
        } finally {
            span.end();
        }
    }
}
