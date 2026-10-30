package com.nio.SonarGnss;

/**
 * This is the RtkString class and formats raw GNSS/RTK data numbers into clean human-readable strings.
 */
public class RtkString {
    
    // This is the DATE function and formats year, month, and day into a standard date string
    String DATE(int year, int month, int day){
        // This line returns the formatted date taking insights from year, month, and day parameters
        return String.format("%02d/%02d/%04d", day, month, year);
    }
    
    // This is the TIME function and formats second, minute, and hour into a standard time string
    String TIME(int second, int minute, int hour){
        // This line returns the formatted time taking insights from hour, minute, and second parameters
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }
    
    // This is the LAT function and converts raw latitude double into a string value
    String LAT(double latitude){
        // This line checks if latitude is negative and returns its string representation taking insights from latitude
        if(latitude < 0){ return String.valueOf(latitude * 1); }
        // This line returns the positive latitude string taking insights from latitude
        return String.valueOf(latitude);
    }
    
    // This is the LON function and converts raw longitude double into a string value
    String LON(double longitude){
        // This line checks if longitude is negative and returns its string representation taking insights from longitude
        if(longitude < 0){ return String.valueOf(longitude * 1); }
        // This line returns the positive longitude string taking insights from longitude
        return String.valueOf(longitude);
    }
    
    // This is the LATS function and returns North or South hemisphere indicator for latitude
    String LATS(double latitude){
        // This line returns South or North taking insights from whether latitude is below zero
        if(latitude < 0){ return " °S"; } return " °N";
    }
    
    // This is the LONS function and returns East or West hemisphere indicator for longitude
    String LONS(double longitude){
        // This line returns West or East taking insights from whether longitude is below zero
        if(longitude < 0){ return " °W"; } return " °E";
    }
    
    // This is the ALTITUDE function and converts altitude value to a string
    String ALTITUDE(double altitude){
        // This line returns the altitude string taking insights from altitude
        return String.valueOf(altitude);
    }
    
    // This is the SPEED function and converts speed value to a string
    String SPEED(double speed){
        // This line returns the speed string taking insights from speed
        return String.valueOf(speed);
    }
    
    // This is the COG function and converts course over ground (COG) value to a string
    String COG(double cog){
        // This line returns the COG string taking insights from cog
        return String.valueOf(cog);
    }
}
