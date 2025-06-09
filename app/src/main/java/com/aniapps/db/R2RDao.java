package com.aniapps.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Maybe;

@Dao
public interface R2RDao {

    @Query("SELECT * FROM tb_location")
    Maybe<List<LocationDBPojo>> getAllLocations();

    @Insert
    void insertLocations(LocationDBPojo myLocations);

    @Query("DELETE FROM tb_location WHERE name =:name")
    void deleteLocations(String name);

    @Query("DELETE FROM tb_location WHERE timestamp =:time")
    void deleteLoc(String time);

    @Query("SELECT COUNT(timestamp) FROM tb_location")
    int getOfflineCount();

}

