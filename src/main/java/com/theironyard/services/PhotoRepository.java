package com.theironyard.services;

import com.theironyard.entities.Photo;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by DrScott on 11/17/15.
 */
public interface PhotoRepository extends CrudRepository<Photo, Integer>{

}
