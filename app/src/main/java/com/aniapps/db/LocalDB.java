package com.aniapps.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.room.Room;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class LocalDB {
    private static final String DB_NAME = "SATISH_LOCS";
    private AppDataBase db;
    Context context;
    private static LocalDB ourInstance;

    public static LocalDB getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new LocalDB(context);
        }
        return ourInstance;
    }

    public LocalDB(Context context) {
        this.context = context;
        db = Room.databaseBuilder(context, AppDataBase.class, DB_NAME).allowMainThreadQueries().build();
    }


    public AppDataBase getDb() {
        if (db != null)
            return db;
        else
            return null;
    }

    public int getOfflineCount() {
        return db.r2RDao().getOfflineCount();
    }



    public void insertLocation(final LocationDBPojo myLocations, final DBStatus dbStatus) {
        Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                db.r2RDao().insertLocations(myLocations);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io()).subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onComplete() {
                dbStatus.onSuccess();
            }

            @Override
            public void onError(Throwable e) {
                dbStatus.onFailure();

            }
        });
    }




    @SuppressLint("CheckResult")
    public void getAllLocations(final DBLocations dbLocations) {
        db.r2RDao().getAllLocations().subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<List<LocationDBPojo>>() {
            @Override
            public void accept(List<LocationDBPojo> myLocations1) throws Exception {
                dbLocations.getAllLocations(myLocations1);
            }
        });
    }


    // delete data
    public void deleteLocation(final String lan) {
        Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                db.r2RDao().deleteLocations(lan);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onComplete() {
                //og.e("#DB Success#", "Deleted Locations ");
            }

            @Override
            public void onError(Throwable e) {
                // Log.e("#DB Error#", "Failed Delete Location" + " " + e.getMessage());
            }
        });
    }

    public void deleteAllLocation() {
        Completable.fromAction(() -> db.r2RDao().deleteLocations(Pref.getIn().getName())).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onComplete() {
                //og.e("#DB Success#", "Deleted Locations ");
            }

            @Override
            public void onError(Throwable e) {
                // Log.e("#DB Error#", "Failed Delete Location" + " " + e.getMessage());
            }
        });
    }


    public void deleteLoc(final String time, DBStatus dbInsert) {
        Completable.fromAction(() -> db.r2RDao().deleteLoc(time)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onComplete() {
                dbInsert.onSuccess();
                Log.e("#DB Success#", "## Deleted Location row");
            }

            @Override
            public void onError(Throwable e) {
                dbInsert.onFailure();
                //  Log.e("#DB Error#", "## Failed Delete Location row" + " " + e.getMessage());
            }
        });
    }

    // delete single row
    boolean flag;

    public boolean deleteLoc(final String time) {

        Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                db.r2RDao().deleteLoc(time);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onComplete() {
               // Log.e("#DB Success#", "## Deleted Location row");
                flag = true;
            }

            @Override
            public void onError(Throwable e) {
                flag = false;
              //  Log.e("#DB Error#", "## Failed Delete Location row" + " " + e.getMessage());
            }
        });
        return flag;
    }


}
