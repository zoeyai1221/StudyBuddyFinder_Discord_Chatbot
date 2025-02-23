package edu.northeastern.cs5500.starterbot.command;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;

@Module
public class CommandModule {
    @Provides
    @IntoMap
    @StringKey(SetProfileCommand.NAME)
    public SlashCommandHandler provideSetProfileCommand(SetProfileCommand setProfileCommand) {
        return setProfileCommand;
    }

    @Provides
    @IntoMap
    @StringKey(SetProfileCommand.MODAL_ID)
    public ModalHandler provideSetProfileModalHandler(SetProfileCommand setProfileCommand) {
        return setProfileCommand;
    }

    @Provides
    @IntoMap
    @StringKey(SetInterestCommand.NAME)
    public SlashCommandHandler provideSetInterestCommand(SetInterestCommand setInterestCommand) {
        return setInterestCommand;
    }

    @Provides
    @IntoMap
    @StringKey(AvailabilityCommand.NAME)
    public SlashCommandHandler provideAvailabilityCommand(AvailabilityCommand availabilityCommand) {
        return availabilityCommand;
    }

    @Provides
    @IntoMap
    @StringKey(AvailabilityCommand.NAME)
    public ButtonHandler provideAvailabilityCommandHandler(
            AvailabilityCommand availabilityCommand) {
        return availabilityCommand;
    }

    @Provides
    @IntoMap
    @StringKey(AvailabilityCommand.DAY_SELECT)
    public StringSelectHandler provideAvailabilityCommandDayMenuHandler(
            AvailabilityCommand availabilityCommand) {
        return availabilityCommand;
    }

    @Provides
    @IntoMap
    @StringKey(ViewApplicationsCommand.NAME)
    public SlashCommandHandler provideViewApplicationsCommand(
            ViewApplicationsCommand viewApplicationsCommand) {
        return viewApplicationsCommand;
    }

    @Provides
    @IntoMap
    @StringKey(ViewApplicationsCommand.NAME)
    public ButtonHandler provideViewApplicationsCommandClickHandler(
            ViewApplicationsCommand viewApplicationsCommand) {
        return viewApplicationsCommand;
    }

    @Provides
    @IntoMap
    @StringKey(AvailabilityCommand.TIME_SELECT)
    public StringSelectHandler provideAvailabilityCommandTimeMenuHandler(
            AvailabilityCommand availabilityCommand) {
        return availabilityCommand;
    }

    @Provides
    @IntoMap
    @StringKey(AvailabilityCommand.TIMESLOT_SELECT)
    public StringSelectHandler provideAvailabilityRemoveTimeSlotMenuHandler(
            AvailabilityCommand availabilityCommand) {
        return availabilityCommand;
    }

    @Provides
    @IntoMap
    @StringKey(FindGroupCommand.NAME)
    public SlashCommandHandler provideFindGroupCommand(FindGroupCommand findGroupCommand) {
        return findGroupCommand;
    }

    @Provides
    @IntoMap
    @StringKey(FindGroupCommand.NAME)
    public ButtonHandler provideFindGroupCommandClickHandler(FindGroupCommand findGroupCommand) {
        return findGroupCommand;
    }

    // Provide the StringSelect handlers for SetInterestCommand menus
    @Provides
    @IntoMap
    @StringKey(SetInterestCommand.LANGUAGES_SKILLS_MENU_ID)
    public StringSelectHandler provideLanguagesSkillsMenuHandler(
            SetInterestCommand setInterestCommand) {
        return setInterestCommand;
    }

    @Provides
    @IntoMap
    @StringKey(SetInterestCommand.COURSES_BREADTH_AREA_MENU_ID)
    public StringSelectHandler provideCoursesBreadthAreaMenuHandler(
            SetInterestCommand setInterestCommand) {
        return setInterestCommand;
    }

    @Provides
    @IntoMap
    @StringKey(SetInterestCommand.OTHERS_MENU_ID)
    public StringSelectHandler provideOthersMenuHandler(SetInterestCommand setInterestCommand) {
        return setInterestCommand;
    }

    @Provides
    @IntoMap
    @StringKey(SetInterestCommand.CONTINUE_BUTTON_ID)
    public ButtonHandler provideSetInterestContinueButtonHandler(
            SetInterestCommand setInterestCommand) {
        return setInterestCommand;
    }

    @Provides
    @IntoMap
    @StringKey(SetInterestCommand.ACCEPT_BUTTON_ID)
    public ButtonHandler provideSetInterestAcceptButtonHandler(
            SetInterestCommand setInterestCommand) {
        return setInterestCommand;
    }

    @Provides
    @IntoMap
    @StringKey(SetInterestCommand.DECLINE_BUTTON_ID)
    public ButtonHandler provideSetInterestDeclineButtonHandler(
            SetInterestCommand setInterestCommand) {
        return setInterestCommand;
    }

    @Provides
    @IntoMap
    @StringKey(SetInterestCommand.COURSES_PREREQ_CORE_MENU_ID)
    public StringSelectHandler provideCoursesPrereqCoreMenuHandler(
            SetInterestCommand setInterestCommand) {
        return setInterestCommand;
    }

    @Provides
    @IntoMap
    @StringKey(CreateGroupCommand.NAME)
    public SlashCommandHandler provideCreateGroupCommand(CreateGroupCommand createGroupCommand) {
        return createGroupCommand;
    }

    @Provides
    @IntoMap
    @StringKey(CreateGroupCommand.NAME)
    public ButtonHandler provideCreateGroupCommandClickHandler(
            CreateGroupCommand createGroupCommand) {
        return createGroupCommand;
    }

    @Provides
    @IntoMap
    @StringKey(CreateGroupCommand.SELECT_INTEREST_ACTION)
    public StringSelectHandler provideCreateGroupStringSelectHandler(
            CreateGroupCommand createGroupCommand) {
        return createGroupCommand;
    }

    @Provides
    @IntoMap
    @StringKey(CreateGroupCommand.NAME)
    public ModalHandler provideCreateGroupModalHandler(CreateGroupCommand createGroupCommand) {
        return createGroupCommand;
    }

    @Provides
    @IntoMap
    @StringKey(MyStudyGroupsCommand.NAME)
    public SlashCommandHandler provideMyStudyGroupsCommand(
            MyStudyGroupsCommand myStudyGroupsCommand) {
        return myStudyGroupsCommand;
    }

    @Provides
    @IntoMap
    @StringKey(MyStudyGroupsCommand.SELECT_ACTION)
    public StringSelectHandler provideActionMenuHandler(MyStudyGroupsCommand myStudyGroupsCommand) {
        return myStudyGroupsCommand;
    }

    @Provides
    @IntoMap
    @StringKey(MyStudyGroupsCommand.SELECT_GROUP)
    public StringSelectHandler provideStudyGroupMenuHandler(
            MyStudyGroupsCommand myStudyGroupsCommand) {
        return myStudyGroupsCommand;
    }

    @Provides
    @IntoMap
    @StringKey(MyStudyGroupsCommand.NAME)
    public ButtonHandler provideMyStudyGroupsCommandClickHandler(
            MyStudyGroupsCommand myStudyGroupsCommand) {
        return myStudyGroupsCommand;
    }

    @Provides
    @IntoMap
    @StringKey(CreateMeetingCommand.NAME)
    public ButtonHandler provideCreateMeetingCommandClickHandler(
            CreateMeetingCommand createMeetingCommand) {
        return createMeetingCommand;
    }

    @Provides
    @IntoMap
    @StringKey(CreateMeetingCommand.FREQUENCY_SELECT)
    public StringSelectHandler provideCreateMeetingCommandFrequentMenuHandler(
            CreateMeetingCommand createMeetingCommand) {
        return createMeetingCommand;
    }

    @Provides
    @IntoMap
    @StringKey(CreateMeetingCommand.MODAL_NAME)
    public ModalHandler provideCreateMeetingModalHandler(
            CreateMeetingCommand createMeetingCommand) {
        return createMeetingCommand;
    }

    @Provides
    @IntoMap
    @StringKey(CreateMeetingCommand.DAY_SELECT)
    public StringSelectHandler provideCreateMeetingCommandDayMenuHandler(
            CreateMeetingCommand createMeetingCommand) {
        return createMeetingCommand;
    }

    @Provides
    @IntoMap
    @StringKey(CreateMeetingCommand.TIME_SELECT)
    public StringSelectHandler provideCreateMeetingCommandTimeMenuHandler(
            CreateMeetingCommand createMeetingCommand) {
        return createMeetingCommand;
    }

    @Provides
    @IntoMap
    @StringKey(MeetingsCommand.NAME)
    public SlashCommandHandler provideMeetingsCommand(MeetingsCommand meetingsCommand) {
        return meetingsCommand;
    }

    @Provides
    @IntoMap
    @StringKey(MeetingsCommand.MEETING_ACTION)
    public StringSelectHandler provideMeetingActionMenuHandler(MeetingsCommand meetingsCommand) {
        return meetingsCommand;
    }

    @Provides
    @IntoMap
    @StringKey(MeetingsCommand.SELECT_MEETING)
    public StringSelectHandler provideMeetingMenuHandler(MeetingsCommand meetingsCommand) {
        return meetingsCommand;
    }

    @Provides
    @IntoMap
    @StringKey(CreateBooking.SELECT_ROOM)
    public StringSelectHandler provideCreateBookingMenuHandler(CreateBooking createBooking) {
        return createBooking;
    }

    @Provides
    @IntoMap
    @StringKey(CreateBooking.NAME)
    public ButtonHandler provideCreateBookingClickHandler(CreateBooking createBooking) {
        return createBooking;
    }

    @Provides
    @IntoMap
    @StringKey(ReminderCommand.NAME)
    public SlashCommandHandler provideReminderCommand(ReminderCommand reminderCommand) {
        return reminderCommand;
    }

    @Provides
    @IntoMap
    @StringKey(ReminderCommand.NAME)
    public StringSelectHandler provideReminderMenuHandler(ReminderCommand reminderCommand) {
        return reminderCommand;
    }
}
