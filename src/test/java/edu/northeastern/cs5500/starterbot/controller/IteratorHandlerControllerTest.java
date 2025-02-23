package edu.northeastern.cs5500.starterbot.controller;

import static com.google.common.truth.Truth.assertThat;

import edu.northeastern.cs5500.starterbot.model.IteratorHandler;
import edu.northeastern.cs5500.starterbot.model.StudyGroup;
import edu.northeastern.cs5500.starterbot.repository.InMemoryRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

class IteratorHandlerControllerTest {
    private IteratorHandlerController<StudyGroup> getIteratorHandlerController() {
        return new IteratorHandlerController<>(new InMemoryRepository<>());
    }

    /****************** tests for addIteratorHandler() **************/
    /**
     * Tests adding an iterator handler to the repository. Ensures that the addIteratorHandler
     * method works as expected and the handler is added to the repository.
     */
    void testAddIteratorHandler() {
        IteratorHandlerController<StudyGroup> controller = getIteratorHandlerController();
        IteratorHandler<StudyGroup> iteratorHandler = new IteratorHandler<>();
        controller.addIteratorHandler(iteratorHandler);

        assertThat(controller.iteratorHandlerRepository.getAll()).hasSize(1);
    }

    /****************** tests for getIteratorHandlerByUserId() **************/
    /**
     * Tests retrieving an iterator handler by user ID when the handler exists. Ensures that the
     * getIteratorHandlerByUserId method works as expected when the ID exists.
     */
    @Test
    void testGetIteratorHandlerByUserIdWithValidId() {
        IteratorHandlerController<StudyGroup> controller = getIteratorHandlerController();
        String validUserId = "123456789123456789";
        ObjectId handlerId = new ObjectId();
        IteratorHandler<StudyGroup> iteratorHandler = new IteratorHandler<>();
        iteratorHandler.setDiscordUserId(validUserId);
        iteratorHandler.setId(handlerId);
        controller.iteratorHandlerRepository.add(iteratorHandler);

        IteratorHandler<StudyGroup> retrievedHandler =
                controller.getIteratorHandlerByDiscordUserId(validUserId);
        assertThat(retrievedHandler).isEqualTo(iteratorHandler);
    }

    /**
     * Tests retrieving an iterator handler by user ID when the handler does not exist. Ensures that
     * the getIteratorHandlerByUserId method throws an exception when the ID does not exist.
     */
    @Test
    void testGetIteratorHandlerByUserIdWithInvalidId() {
        IteratorHandlerController<StudyGroup> controller = getIteratorHandlerController();
        String invalidUserId = "123456789123456789";
        ObjectId handlerId = new ObjectId();
        IteratorHandler<StudyGroup> iteratorHandler = new IteratorHandler<>();
        iteratorHandler.setId(handlerId);
        iteratorHandler.setDiscordUserId("987654321987654321");
        controller.iteratorHandlerRepository.add(iteratorHandler);

        try {
            controller.getIteratorHandlerByDiscordUserId(invalidUserId);
        } catch (IllegalArgumentException e) {
            assertThat(e)
                    .hasMessageThat()
                    .contains("Unable to find iterator handler with user ID: " + invalidUserId);
            return;
        }

        // Fail the test if no exception is thrown
        assertThat("Expected exception was not thrown").isEmpty();
    }

    /****************** tests for getIteratorHandlerById() **************/
    /**
     * Tests retrieving an iterator handler by ID when the handler exists. Ensures that the
     * getIteratorHandlerById method works as expected when the handler exists.
     */
    @Test
    void testGetIteratorHandlerByIdWithValidId() {
        IteratorHandlerController<StudyGroup> controller = getIteratorHandlerController();

        ObjectId handlerId = new ObjectId();
        IteratorHandler<StudyGroup> iteratorHandler = new IteratorHandler<>();
        iteratorHandler.setId(handlerId);

        controller.iteratorHandlerRepository.add(iteratorHandler);

        IteratorHandler<StudyGroup> result = controller.getIteratorHandlerById(handlerId);

        assertThat(result).isEqualTo(iteratorHandler);
    }

    /****************** tests for removeIteratorHandler() **************/
    /**
     * Tests removing an iterator handler from the repository. Ensures that the
     * removeIteratorHandler method works as expected and the handler is removed.
     */
    @Test
    void testRemoveIteratorHandler() {
        IteratorHandlerController<StudyGroup> controller = getIteratorHandlerController();
        ObjectId handlerId = new ObjectId();
        IteratorHandler<StudyGroup> iteratorHandler = new IteratorHandler<>();
        iteratorHandler.setId(handlerId);

        controller.iteratorHandlerRepository.add(iteratorHandler);

        controller.removeIteratorHandler(iteratorHandler);

        assertThat(controller.getIteratorHandlerById(handlerId)).isNull();
    }

    /****************** tests for updateIteratorHandler() **************/
    /**
     * Tests updating an existing iterator handler in the repository. Ensures that the
     * updateIteratorHandler method works as expected and the handler is updated.
     */
    @Test
    void testUpdateIteratorHandler() {
        IteratorHandlerController<StudyGroup> controller = getIteratorHandlerController();
        ObjectId handlerId = new ObjectId();
        IteratorHandler<StudyGroup> iteratorHandler = new IteratorHandler<>();
        StudyGroup currentItem = new StudyGroup();
        iteratorHandler.setId(handlerId);
        iteratorHandler.setCurrentItem(currentItem);

        controller.iteratorHandlerRepository.add(iteratorHandler);

        IteratorHandler<StudyGroup> updatedHandler = new IteratorHandler<>();
        StudyGroup updatedItem = new StudyGroup();
        updatedHandler.setId(handlerId);
        updatedHandler.setCurrentItem(updatedItem);

        controller.updateIteratorHandler(updatedHandler);

        IteratorHandler<StudyGroup> result = controller.getIteratorHandlerById(handlerId);

        assertThat(result).isEqualTo(updatedHandler);
    }
}
