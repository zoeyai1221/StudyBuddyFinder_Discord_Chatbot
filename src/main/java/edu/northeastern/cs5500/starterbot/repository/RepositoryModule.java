package edu.northeastern.cs5500.starterbot.repository;

import dagger.Module;
import dagger.Provides;
import edu.northeastern.cs5500.starterbot.model.*;
import edu.northeastern.cs5500.starterbot.service.MongoDBService;
import javax.inject.Singleton;

@Module
public class RepositoryModule {
    @Provides
    @Singleton
    public GenericRepository<Student> provideStudentRepository(MongoDBService mongoDBService) {
        return new MongoDBRepository<>(Student.class, mongoDBService);
    }

    @Provides
    @Singleton
    public GenericRepository<StudyGroup> provideStudyGroupRepository(
            MongoDBService mongoDBService) {
        return new MongoDBRepository<>(StudyGroup.class, mongoDBService);
    }

    @Provides
    @Singleton
    public GenericRepository<Interest> provideInterestRepository(MongoDBService mongoDBService) {
        return new MongoDBRepository<>(Interest.class, mongoDBService);
    }

    @Provides
    @Singleton
    public GenericRepository<GroupApplication> provideGroupApplicationRepository(
            MongoDBService mongoDBService) {
        return new MongoDBRepository<>(GroupApplication.class, mongoDBService);
    }

    @Provides
    @Singleton
    public GenericRepository<Room> provideRoomRepository(MongoDBService mongoDBService) {
        return new MongoDBRepository<>(Room.class, mongoDBService);
    }

    @Provides
    public GenericRepository<AbstractMeeting> provideAbstractMeetingRepository(
            InMemoryRepository<AbstractMeeting> repository) {
        return repository;
    }

    @Provides
    @Singleton
    public GenericRepository<OnlineMeeting> provideOnlineMeetingRepository(
            MongoDBService mongoDBService) {
        return new MongoDBRepository<>(OnlineMeeting.class, mongoDBService);
    }

    @Provides
    @Singleton
    public GenericRepository<InPersonMeeting> provideInPersonMeetingRepository(
            MongoDBService mongoDBService) {
        return new MongoDBRepository<>(InPersonMeeting.class, mongoDBService);
    }

    @Provides
    @Singleton
    public GenericRepository<Booking> provideBookingRepository(MongoDBService mongoDBService) {
        return new MongoDBRepository<>(Booking.class, mongoDBService);
    }

    @Provides
    @Singleton
    public GenericRepository<Reminder> provideReminderRepository(MongoDBService mongoDBService) {
        return new MongoDBRepository<>(Reminder.class, mongoDBService);
    }
}
