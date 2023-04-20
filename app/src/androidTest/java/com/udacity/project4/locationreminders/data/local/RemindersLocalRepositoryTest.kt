package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.util.MainCoroutineRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private var applicationContext = ApplicationProvider.getApplicationContext<MyApp>()
    private lateinit var database: RemindersDatabase
    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var remindersList: MutableList<ReminderDTO>


    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            applicationContext,
            RemindersDatabase::class.java
        ).build()

        remindersLocalRepository =
            RemindersLocalRepository(database.reminderDao(), mainCoroutineRule.dispatcher)
    }

    @Before
    fun initRemindersList() {
        remindersList = mutableListOf(
            ReminderDTO("test1", "test1", "test1", 1.0, 2.0, "1"),
            ReminderDTO("test2", "test2", "test2", 3.0, 4.0, "2"),
            ReminderDTO("test3", "test3", "test3", 5.0, 6.0, "3")
        )
    }

    @After
    fun cleanResources() {
        database.close()
        applicationContext = null
        remindersList.clear()
    }

    @Test
    fun saveReminderWithValidDataThenReturnSuccessResultWithTheSameInsertedData() =
        mainCoroutineRule.runBlockingTest {
            //arrange
            val reminder = remindersList[0]

            //act
            remindersLocalRepository.saveReminder(reminder)
            val actual = remindersLocalRepository.getReminder(reminder.id)

            //assert
            var result = actual is Result.Success
            assertTrue(result)

            val actualResult = actual as Result.Success
            assertEquals(reminder, actualResult.data)
        }

    @Test
    fun getRemindersWithValidListOfRemindersThenReturnSuccessResultWithTheSameInsertedList() =
        mainCoroutineRule.runBlockingTest {
            //act
            remindersList.forEach { reminderDTO ->
                remindersLocalRepository.saveReminder(reminderDTO)
            }
            val actual = remindersLocalRepository.getReminders()

            //assert
            val result = actual is Result.Success
            assertTrue(result)

            val actualResult = actual as Result.Success
            assertEquals(remindersList, actualResult.data)
        }

    @Test
    fun getReminderByIdWithValidDataThenReturnTheSameInsertedData() =
        mainCoroutineRule.runBlockingTest {
            //arrange
            val reminder = remindersList[0]

            //act
            remindersLocalRepository.saveReminder(reminder)
            val actual =remindersLocalRepository.getReminder(reminder.id)

            //assert
            val actualResult = actual as Result.Success
            assertEquals(reminder, actualResult.data)
        }

    @Test
    fun getReminderByIdWithNonValidIdThenReturnErrorResultWithErrorMessage() =
        mainCoroutineRule.runBlockingTest {
            //act
            val actual =remindersLocalRepository.getReminder("1")

            //assert
            val result = actual is Result.Error
            assertTrue(result)

            val actualResult = actual as Result.Error
            val errorMessage="Reminder not found!"
            assertEquals(errorMessage, actualResult.message)
        }

    @Test
    fun deleteAllRemindersThenReturnResultSuccessAndDatabaseIsEmpty()=mainCoroutineRule.runBlockingTest {
        //act
        remindersList.forEach { reminderDTO ->
            remindersLocalRepository.saveReminder(reminderDTO)
        }

        remindersLocalRepository.deleteAllReminders()

        val actual=remindersLocalRepository.getReminders()

        //assert
        val result = actual is Result.Success
        assertTrue(result)

        val actualResult = actual as Result.Success
        assertTrue(actualResult.data.isEmpty())
    }
}