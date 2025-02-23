package edu.northeastern.cs5500.starterbot.seeder;

import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.repository.GenericRepository;
import java.util.Collection;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/** A seeder class responsible for populating the interest repository with predefined interests. */
@Slf4j
public class InterestSeeder {
    GenericRepository<Interest> interestRepository;

    @Inject
    public InterestSeeder(GenericRepository<Interest> interestRepository) {
        this.interestRepository = interestRepository;
    }

    /** Seeds the repository with a predefined set of interests. */
    public void seedInterests() {
        Map<String[], Interest.Category> interestMappings =
                Map.of(
                        InterestConstants.COURSE_PREREQUISITE,
                                Interest.Category.COURSE_PREREQUISITE,
                        InterestConstants.COURSE_CORE, Interest.Category.COURSE_CORE,
                        InterestConstants.COURSE_SYSTEM_SOFTWARE,
                                Interest.Category.COURSE_SYSTEM_SOFTWARE,
                        InterestConstants.COURSE_THEORY_SECURITY,
                                Interest.Category.COURSE_THEORY_SECURITY,
                        InterestConstants.COURSE_AI_DATA_SCIENCE,
                                Interest.Category.COURSE_AI_DATA_SCIENCE,
                        InterestConstants.PROGRAMMING_LANGUAGES,
                                Interest.Category.PROGRAMMING_LANGUAGES,
                        InterestConstants.SOFTWARE_PROGRAMMING_SKILLS,
                                Interest.Category.SOFTWARE_PROGRAMMING_SKILLS,
                        InterestConstants.OTHER_TOPICS, Interest.Category.OTHER_TOPICS);
        for (Map.Entry<String[], Interest.Category> entry : interestMappings.entrySet()) {
            seedInterestsHelper(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Helper function to seed the interest repository with the provided names and category.
     *
     * @param interestNames List of interest names to seed
     * @param category The category for the interests
     */
    private void seedInterestsHelper(String[] interestNames, Interest.Category category) {
        for (String name : interestNames) {
            if (!interestExists(name)) {
                Interest interest =
                        Interest.builder().studentInterest(name).category(category).build();
                interestRepository.add(interest);
            }
        }
        log.info("Interests seeding complete.");
    }

    /**
     * Helper function to determine if the given interest name already exists in repository.
     *
     * @param interestName
     * @return true if exists, false if not
     */
    private boolean interestExists(String interestName) {
        Collection<Interest> allInterests = interestRepository.getAll();
        for (Interest interest : allInterests) {
            if (interestName.equals(interest.getStudentInterest())) {
                return true;
            }
        }
        return false;
    }
}
