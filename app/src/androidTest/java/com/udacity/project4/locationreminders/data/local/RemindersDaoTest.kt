package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.util.MainCoroutineRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue

import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule= MainCoroutineRule()

    private lateinit var database: RemindersDatabase
    private var applicationContext=ApplicationProvider.getApplicationContext<MyApp>()
    private lateinit var remindersList:MutableList<ReminderDTO>
    @Before
    fun setupDatabase(){
        database= Room.inMemoryDatabaseBuilder(
            applicationContext,
            RemindersDatabase::class.java
        ).build()
    }

    @Before
    fun initRemindersList(){
        remindersList= mutableListOf(ReminderDTO("test1","test1","test1",1.0,2.0,"1")
            ,ReminderDTO("test2","test2","test2",3.0,4.0,"2"),
            ReminderDTO("test3","test3","test3",5.0,6.0,"3"))
    }
    @After
    fun cleanResources(){
        database.close()
        applicationContext=null
        remindersList.clear()
    }

    @Test
    fun saveReminderWithValidDataThenReturnTheSameInsertedData()=mainCoroutineRule.runBlockingTest {
        //arrange
        val reminder=remindersList[0]

        //act
        database.reminderDao().saveReminder(reminder)
        val actual=database.reminderDao().getReminderById(reminder.id)

        //arrange
        assertEquals(reminder,actual)
    }

    @Test
    fun getRemindersWithValidListOfRemindersThenReturnTheSameInsertedList()=mainCoroutineRule.runBlockingTest {
        //act
        remindersList.forEach { reminderDTO ->
            database.reminderDao().saveReminder(reminderDTO)
        }

        val actual=database.reminderDao().getReminders()

        //assert
        assertEquals(remindersList,actual)
    }

    @Test
    fun getReminderByIdWithValidDataThenReturnTheSameInsertedData()=mainCoroutineRule.runBlockingTest {
        //arrange
        val reminder=remindersList[0]

        //act
        database.reminderDao().saveReminder(reminder)
        val actual=database.reminderDao().getReminderById(reminder.id)

        //assert
        assertEquals(reminder,actual)
    }

    @Test
    fun deleteAllRemindersThenDatabaseIsEmpty()=mainCoroutineRule.runBlockingTest {
        //act
        remindersList.forEach { reminderDTO ->
            database.reminderDao().saveReminder(reminderDTO)
        }


        database.reminderDao().deleteAllReminders()

        val actual=database.reminderDao().getReminders()

        //assert
        assertTrue(actual.isEmpty())
    }
}