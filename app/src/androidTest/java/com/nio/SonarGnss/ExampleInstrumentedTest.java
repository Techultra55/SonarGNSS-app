package com.nio.SonarGnss;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    // This is the useAppContext function and tests app components on an actual Android device
    @Test
    public void useAppContext() {
        // This line gets the application context using the testing instrumentation registry
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // This line does package verification taking insights from the app context to ensure the correct package name "com.nio.SonarGnss" is loaded
        assertEquals("com.nio.SonarGnss", appContext.getPackageName());
    }
}
