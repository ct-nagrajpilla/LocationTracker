package com.aniapps.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;


@Database(entities = {LocationDBPojo.class}, version = 1, exportSchema= false)
public abstract class AppDataBase extends RoomDatabase {
    public abstract R2RDao r2RDao();
}
