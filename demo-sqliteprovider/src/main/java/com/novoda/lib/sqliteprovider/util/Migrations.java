package com.novoda.lib.sqliteprovider.util;

import android.content.res.AssetManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class Migrations {

    private static final String TAG = Migrations.class.getSimpleName();
    private final SortedSet<String> migrations;

    private final int startDate;

    public Migrations() {
        this(-1);
    }

    public Migrations(int startDate) {
        this.startDate = startDate;
        migrations = new TreeSet<String>(comparator);
    }

    public boolean add(String migration) {
        if (shouldInsert(migration)) {
            return migrations.add(migration);
        }
        return false;
    }

    private boolean shouldInsert(String migration) {
        return extractDate(migration) > startDate;
    }

    private int extractDate(String migration) {
        try {
            return Integer.parseInt(migration.split("_", 0)[0]);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid int, returning -1.", e);
            return -1;
        }
    }

    public SortedSet<String> getMigrationsFiles() {
        return migrations;
    }

    /**
     * Comparator against filename: <date>_create.sql vs <date2>_create.sql will compare date with date2
     */
    private final Comparator<String> comparator = new Comparator<String>() {
        @Override
        public int compare(String file, String another) {
            return Integer.valueOf(extractDate(file)).compareTo(Integer.valueOf(extractDate(another)));
        }
    };

    public static void migrate(SQLiteDatabase db, AssetManager manager, String assetLocation) throws IOException {

        if (infoLoggingEnabled()) {
            Log.i(TAG, "current DB version is: " + db.getVersion());
        }

        String[] sqls = manager.list(assetLocation);

        if (sqls.length == 0) {
            Log.w(TAG, "No SQL file found in asset folder");
            return;
        }

        Migrations migrations = new Migrations(db.getVersion());
        Reader reader;

        for (String sqlfile : sqls) {
            migrations.add(sqlfile);
        }

        for (String sql : migrations.getMigrationsFiles()) {
            reader = new InputStreamReader(manager.open(assetLocation + File.separator + sql, AssetManager.ACCESS_RANDOM));
            if (infoLoggingEnabled()) {
                Log.i(TAG, "executing SQL file: " + assetLocation + File.separator + sql);
            }
            try {
                db.beginTransaction();
                for (String insert : SQLFile.statementsFrom(reader)) {
                    if (TextUtils.isEmpty(insert.trim())) {
                        continue;
                    }
                    if (infoLoggingEnabled()) {
                        Log.i(TAG, "executing insert: " + insert);
                    }
                    db.execSQL(insert);
                }
                db.setTransactionSuccessful();

            } catch (SQLException exception) {
                Log.e(TAG, "error in migrate against file: " + sql, exception);
            } finally {
                db.endTransaction();
            }
        }

        if (migrations.getMigrationsFiles().size() > 0) {
            int v = migrations.extractDate(migrations.getMigrationsFiles().last());
            db.setVersion(v);
            if (infoLoggingEnabled()) {
                Log.i(TAG, "setting version of DB to: " + v);
            }
        }
    }

    private static boolean infoLoggingEnabled() {
        return Log.isLoggable(TAG, Log.INFO);
    }

    public static int getVersion(AssetManager assets, String migrationsPath) throws IOException {
        int version = 1;
        String[] sqls = assets.list(migrationsPath);
        if (sqls.length == 0) {
            Log.w(TAG, "You need to add atleast one SQL file in your assets/" + migrationsPath + " folder");
        } else {
            Migrations migrations = new Migrations(-1);
            for (String sqlfile : sqls) {
                migrations.add(sqlfile);
            }
            version = (migrations.extractDate(migrations.getMigrationsFiles().last()));
            if (infoLoggingEnabled()) {
                Log.i(TAG, "current migration file version is: " + version);
            }
        }
        return version;
    }
}