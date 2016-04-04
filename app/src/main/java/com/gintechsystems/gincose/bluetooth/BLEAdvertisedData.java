package com.gintechsystems.gincose.bluetooth;

import java.util.List;
import java.util.UUID;

/**
 * Created by joeginley on 4/2/16.
 */
public class BLEAdvertisedData {
    private List<UUID> mUuids;
    private String mName;

    public BLEAdvertisedData(List<UUID> uuids, String name){
        mUuids = uuids;
        mName = name;
    }

    public List<UUID> getUUIDs(){
        return mUuids;
    }

    public String getName(){
        return mName;
    }
}