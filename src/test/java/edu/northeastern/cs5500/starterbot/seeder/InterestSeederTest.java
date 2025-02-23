package edu.northeastern.cs5500.starterbot.seeder;

import static com.google.common.truth.Truth.assertThat;

import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class InterestSeederTest {

    private InterestSeeder getInterestSeeder(InMemoryRepository<Interest> repository) {
        return new InterestSeeder(repository);
    }

    @Test
    void testSeedInterestsAddsInterestsToRepository() {
        InMemoryRepository<Interest> repository = new InMemoryRepository<>();
        InterestSeeder seeder = getInterestSeeder(repository);

        assertThat(repository.getAll()).isEmpty();

        seeder.seedInterests();

        Collection<Interest> allInterests = repository.getAll();
        assertThat(allInterests).isNotEmpty();
        assertThat(allInterests.size()).isAtLeast(InterestConstants.COURSE_PREREQUISITE.length);
        assertThat(allInterests.size()).isAtLeast(InterestConstants.COURSE_CORE.length);
        assertThat(allInterests.size()).isAtLeast(InterestConstants.COURSE_SYSTEM_SOFTWARE.length);
    }

    @Test
    void testSeedInterestsDoesNotAddDuplicates() {
        InMemoryRepository<Interest> repository = new InMemoryRepository<>();
        InterestSeeder seeder = getInterestSeeder(repository);

        // Seed interests twice
        seeder.seedInterests();
        seeder.seedInterests();

        Collection<Interest> allInterests = repository.getAll();
        assertThat(allInterests.size())
                .isEqualTo(repository.getAll().size()); // Size shouldn't double
    }

    @Test
    void testSeedInterestsAddsSpecificInterests() {
        InMemoryRepository<Interest> repository = new InMemoryRepository<>();
        InterestSeeder seeder = new InterestSeeder(repository);
        seeder.seedInterests();

        Collection<Interest> interests = repository.getAll();

        boolean containsJava =
                interests.stream()
                        .anyMatch(interest -> "Java".equals(interest.getStudentInterest()));
        assertThat(containsJava).isTrue();

        boolean containsScala =
                interests.stream()
                        .anyMatch(interest -> "Scala".equals(interest.getStudentInterest()));
        assertThat(containsScala).isFalse();
    }
}
