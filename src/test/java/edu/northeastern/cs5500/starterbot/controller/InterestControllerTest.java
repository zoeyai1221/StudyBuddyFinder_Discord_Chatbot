package edu.northeastern.cs5500.starterbot.controller;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

class InterestControllerTest {
    private InterestController getInterestController() {
        return new InterestController(new InMemoryRepository<>());
    }

    /** Test that the method getInterestByInterestId correctly retrieves an interest by its ID */
    @Test
    void testGetInterestByInterestIdReturnsCorrectInterest() {
        InterestController interestController = getInterestController();
        Interest interest =
                Interest.builder()
                        .studentInterest("Java")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();
        interestController.interestRepository.add(interest);

        Interest fetchedInterest = interestController.getInterestByInterestId(interest.getId());

        assertThat(fetchedInterest).isNotNull();
        assertThat(fetchedInterest.getId()).isEqualTo(interest.getId());
        assertThat(fetchedInterest.getStudentInterest()).isEqualTo("Java");
    }

    /** Test that the method getInterestByInterestId throws an exception for an invalid ID. */
    @Test
    void testGetInterestByInterestIdThrowsExceptionForInvalidId() {
        InterestController interestController = getInterestController();
        ObjectId invalidId = new ObjectId();
        assertThrows(
                RuntimeException.class,
                () -> interestController.getInterestByInterestId(invalidId));
    }

    /**
     * Test that the method getInterestByInterestName correctly retrieves an interest by its name.
     */
    @Test
    void testGetInterestByInterestNameReturnsCorrectInterest() {
        InterestController interestController = getInterestController();
        Interest pythonInterest =
                Interest.builder()
                        .studentInterest("Python")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();
        Interest javaInterest =
                Interest.builder()
                        .studentInterest("Java")
                        .category(Interest.Category.PROGRAMMING_LANGUAGES)
                        .build();
        interestController.interestRepository.add(pythonInterest);
        interestController.interestRepository.add(javaInterest);
        Interest fetchedInterest = interestController.getInterestByInterestName("Python");

        assertThat(fetchedInterest).isNotNull();
        assertThat(fetchedInterest.getStudentInterest()).isEqualTo("Python");
    }

    /**
     * Test that the method getInterestByInterestName returns an object with interest name "Interest
     * Not Found"
     */
    @Test
    void testGetInterestByInterestNameReturnsNotFoundForUnknownName() {
        InterestController interestController = getInterestController();
        Interest fetchedInterest = interestController.getInterestByInterestName("Unknown");

        assertThat(fetchedInterest).isNotNull();
        assertThat(fetchedInterest.getStudentInterest()).isEqualTo("Interest Not Found");
    }
}
