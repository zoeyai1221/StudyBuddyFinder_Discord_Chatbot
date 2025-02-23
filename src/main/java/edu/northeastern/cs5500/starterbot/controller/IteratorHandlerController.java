package edu.northeastern.cs5500.starterbot.controller;

import edu.northeastern.cs5500.starterbot.model.IteratorHandler;
import edu.northeastern.cs5500.starterbot.model.Model;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import edu.northeastern.cs5500.starterbot.service.OpenTelemetry;
import java.util.Collection;
import javax.inject.Inject;
import org.bson.types.ObjectId;

/**
 * The IteratorHandlerController class is responsible for managing operations related to {@link
 * IteratorHandler}. It provides functionality to add, retrieve, update, and delete iterator handler
 * stored in the repository.
 */
public class IteratorHandlerController<T extends Model> {
    InMemoryRepository<IteratorHandler<T>> iteratorHandlerRepository;
    @Inject OpenTelemetry openTelemetry;

    /**
     * Constructs a IteratorHandlerController with the repository.
     *
     * @param iteratorHandlerRepository the repository to store and manage {@link IteratorHandler}.
     */
    @Inject
    IteratorHandlerController(InMemoryRepository<IteratorHandler<T>> iteratorHandlerRepository) {
        this.iteratorHandlerRepository = iteratorHandlerRepository;
    }

    /**
     * Adds a new matched group to the repository.
     *
     * @param iteratorHandler the {@link IteratorHandler} object to add.
     */
    public void addIteratorHandler(IteratorHandler<T> iteratorHandler) {
        iteratorHandlerRepository.add(iteratorHandler);
    }

    /**
     * Retrieves a matched group by the Discord user ID.
     *
     * @param discordUserId the Discord user ID of the matched group to retrieve.
     * @return the {@link IteratorHandler} associated with the specified Discord user ID.
     * @throws IllegalArgumentException if no matched group is found for the given Discord user ID.
     */
    public IteratorHandler<T> getIteratorHandlerByDiscordUserId(String discordUserId) {
        Collection<IteratorHandler<T>> iteratorHandlers = iteratorHandlerRepository.getAll();
        for (IteratorHandler<T> iteratorHandler : iteratorHandlers) {
            if (iteratorHandler.getDiscordUserId().equals(discordUserId)) {
                return iteratorHandler;
            }
        }
        throw new IllegalArgumentException(
                "Unable to find iterator handler with user ID: " + discordUserId);
    }

    /**
     * Retrieves an iterator handler by its unique ID.
     *
     * @param id the {@link ObjectId} of the iterator handler to retrieve.
     * @return the {@link IteratorHandler} associated with the specified ID
     */
    public IteratorHandler<T> getIteratorHandlerById(ObjectId id) {
        return iteratorHandlerRepository.get(id);
    }

    /**
     * Removes an iterator handler from the repository.
     *
     * @param iteratorHandler the {@link IteratorHandler} object to remove.
     */
    public void removeIteratorHandler(IteratorHandler<T> iteratorHandler) {
        iteratorHandlerRepository.delete(iteratorHandler.getId());
    }

    /**
     * Updates an existing iterator handler in the repository.
     *
     * @param updatedIteratorHandler the updated {@link IteratorHandler} object.
     * @throws IllegalArgumentException if the provided iterator handler is null or missing an ID.
     */
    public void updateIteratorHandler(IteratorHandler<T> updatedIteratorHandler) {
        iteratorHandlerRepository.update(updatedIteratorHandler);
    }
}
