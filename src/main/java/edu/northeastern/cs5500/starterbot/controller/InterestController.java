package edu.northeastern.cs5500.starterbot.controller;

import edu.northeastern.cs5500.starterbot.model.Interest;
import edu.northeastern.cs5500.starterbot.repository.GenericRepository;
import edu.northeastern.cs5500.starterbot.service.FakeOpenTelemetryService;
import edu.northeastern.cs5500.starterbot.service.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import java.util.Collection;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

/**
 * Controller for managing interests. This class provides methods for retrieving interests by their
 * ID or name.
 */
@Slf4j
public class InterestController {
    GenericRepository<Interest> interestRepository;
    @Inject OpenTelemetry openTelemetry;

    @Inject
    public InterestController(GenericRepository<Interest> interestRepository) {
        this.interestRepository = interestRepository;
        openTelemetry = new FakeOpenTelemetryService();
    }

    /**
     * Retrieves an interest from the repository by its unique ID.
     *
     * @param id Interest's unique object id.
     * @return Interest with the given ID.
     * @throws IllegalArgumentException if the interest with the given ID is not found.
     */
    public Interest getInterestByInterestId(ObjectId id) {
        var span = openTelemetry.span("getInterestByInterestId");
        span.setAttribute("id", id.toHexString());

        try (Scope scope = span.makeCurrent()) {
            Interest interest = interestRepository.get(id);
            if (interest == null) {
                throw new IllegalArgumentException("Interest with ID " + id + " not found.");
            }
            return interest;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Retrieves an interest from the repository by its name. If the interest is not found, a
     * default "Interest Not Found" object will be returned.
     *
     * @param interestName The name of the interest.
     * @return Interest with the given name, or a default interest if not found.
     */
    public Interest getInterestByInterestName(String interestName) {
        var span = openTelemetry.span("getInterestByInterestName");
        span.setAttribute("interestName", interestName);

        try (Scope scope = span.makeCurrent()) {
            Collection<Interest> allInterests = interestRepository.getAll();
            for (Interest interest : allInterests) {
                if (interest.getStudentInterest().equalsIgnoreCase(interestName)) {
                    return interest;
                }
            }
            return Interest.builder()
                    .studentInterest("Interest Not Found")
                    .category(Interest.Category.OTHER_TOPICS)
                    .build();
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
