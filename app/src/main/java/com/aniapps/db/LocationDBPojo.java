package com.aniapps.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tb_location")
public class LocationDBPojo {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "device_id")
    public String device_id = "";

    @ColumnInfo(name = "device_details")
    public String device_details = "";

    @ColumnInfo(name = "latitude")
    public String latitude = "";

    @ColumnInfo(name = "longitude")
    public String longitude = "";

    @ColumnInfo(name = "timestamp")
    public String timestamp = "";

    @ColumnInfo(name = "offline")
    public String offline = "";

    @ColumnInfo(name = "sequence")
    public String sequence = "";



    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getOffline() {
        return offline;
    }

    public void setOffline(String offline) {
        this.offline = offline;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getDevice_details() {
        return device_details;
    }

    public void setDevice_details(String device_details) {
        this.device_details = device_details;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public LocationDBPojo(String name, String device_id, String latitude, String longitude, String timestamp,
                          String offline, String sequence, String device_details) {
        this.name = name;
        this.device_id = device_id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.offline = offline;
        this.sequence = sequence;
        this.device_details=device_details;
    }
}
