package edu.northeastern.cs5500.starterbot.model;

import java.util.Iterator;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

/**
 * IteratorHandler is a helper class to store an iterator and the current item of a generic type
 * that extends the Model interface.
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class IteratorHandler<T extends Model> implements Model {
    @Builder.Default private ObjectId id = new ObjectId();

    @Nonnull private String discordUserId; // the discord user that the iterator is associated with

    @Nonnull
    private Iterator<T> iterator; // iterator that is used to iterate through the list of models

    @Nonnull private T currentItem; // current displaying item
}
