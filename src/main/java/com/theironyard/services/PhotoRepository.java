package com.theironyard.services;

import com.theironyard.entities.Photo;
import com.theironyard.entities.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by alhanger on 11/17/15.
 */
public interface PhotoRepository extends CrudRepository<Photo, Integer> {
    List<Photo> findByReceiver(User receiver);
    List<Photo> findBySender(User sender);
    Photo findByFilename(String filename);
}
