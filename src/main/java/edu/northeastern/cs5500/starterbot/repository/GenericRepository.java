package edu.northeastern.cs5500.starterbot.repository;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.bson.types.ObjectId;

public interface GenericRepository<T> {
    public T get(@Nonnull ObjectId id);

    public List<T> get(@Nonnull List<ObjectId> ids);

    public T add(@Nonnull T item);

    public T update(@Nonnull T item);

    public void delete(@Nonnull ObjectId id);

    public Collection<T> getAll();

    public long count();
}
