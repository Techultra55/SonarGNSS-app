package com.nio.SonarGnss;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

/**
 * This is the MBTilesProvider class and supplies offline map tiles from an SQLite database to Google Maps.
 */
public class MBTilesProvider implements TileProvider {

    // This is the db field and holds reference to the SQLite database containing map tiles
    SQLiteDatabase db;

    // This is the MBTilesProvider constructor and initializes the database connection
    public MBTilesProvider(SQLiteDatabase database){
        // This line assigns the passed database reference taking insights from database parameter
        db = database;
    }

    // This is the getTile function and fetches a specific map tile image using coordinates and zoom level
    @Override
    public Tile getTile(int x, int y, int zoom){
        // This line converts standard Y coordinate to TMS Y coordinate taking insights from zoom and y parameters
        int tmsY = (1 << zoom) - 1 - y;

        // This line logs the incoming tile request taking insights from zoom, x, y, and tmsY variables
        Log.d("MBTILES", "Request tile z=" + zoom + " x=" + x + " y=" + y + " tmsY=" + tmsY);

        // This line queries the SQLite database for matching tile image data taking insights from zoom, x, and tmsY
        Cursor c = db.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?",
                new String[]{
                        String.valueOf(zoom),
                        String.valueOf(x),
                        String.valueOf(tmsY)
                }
        );

        // This line checks if the query found a tile result taking insights from cursor c
        if(c.moveToFirst()){
            // This line extracts binary tile image bytes taking insights from cursor column index 0
            byte[] data = c.getBlob(0);

            // This line logs successful tile retrieval taking insights from data length
            Log.d("MBTILES", "Tile FOUND size=" + data.length);

            // This line closes the cursor to prevent memory leaks taking insights from c
            c.close();

            // This line returns a new map Tile object taking insights from dimensions 256x256 and byte data
            return new Tile(256, 256, data);
        }

        // This line logs when a tile is missing taking insights from query results
        Log.d("MBTILES", "Tile NOT FOUND");

        // This line closes the cursor taking insights from c
        c.close();

        // This line returns NO_TILE indicator taking insights from TileProvider interface
        return NO_TILE;
    }
}
