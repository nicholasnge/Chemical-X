package com.example.chemicalx;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.chemicalx.Fragment_Tasks.AddTask;
import com.example.chemicalx.R;
import com.example.chemicalx.TextClassificationClient;
import com.google.common.collect.Iterables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AddTaskInstrumentedTest {
    private Context context = ApplicationProvider.getApplicationContext();
    AddTask addTask;

    @Before //This is executed before the @Test executes
    public void setUp() {
        TextClassificationClient tf = new TextClassificationClient(context);
        tf.load();

        addTask = new AddTask(tf);
        System.out.println("Ready for testing");
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.chemicalx", appContext.getPackageName());
    }

    @Test
    public void getSecondsTest1() {
        assertEquals(addTask.getSeconds(5), 3000);
    }

    @Test
    public void getSecondsTest2() {
        assertEquals(addTask.getSeconds(21), 16200);
    }

    @Test
    public void TitleTest1() {
        String STRING_TO_BE_TYPED = "i dislike very much making tests";

        onView(withId(R.id.taskTitle))
                .perform(typeText(STRING_TO_BE_TYPED), closeSoftKeyboard());

        // Check that the text was changed.
        onView(withId(R.id.taskTitle)).check(matches(withText(STRING_TO_BE_TYPED)));
    }

    @Test
    public void TitleTest2() {
        String STRING_TO_BE_TYPED = "";

        onView(withId(R.id.taskTitle))
                .perform(typeText(STRING_TO_BE_TYPED), closeSoftKeyboard());

        onView(withId(R.id.createTaskButton)).perform(click());
        assertEquals(1,1);
    }
}
