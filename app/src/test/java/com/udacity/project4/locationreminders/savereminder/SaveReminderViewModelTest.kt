package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MyApp
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.util.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.asReminderDTO
import junit.framework.TestCase.*

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private var applicationContext= ApplicationProvider.getApplicationContext<MyApp>()
    private lateinit var dataSource:FakeDataSource
    private lateinit var saveReminderViewModel:SaveReminderViewModel
    private lateinit var reminderData:ReminderDataItem

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule= MainCoroutineRule()

    @Before
    fun setup(){
        dataSource= FakeDataSource()
        saveReminderViewModel= SaveReminderViewModel(applicationContext,dataSource)
        reminderData=ReminderDataItem("test","test","test",1.0,2.0,"1")
    }
    @After
    fun cleanResources() {
        applicationContext=null
        saveReminderViewModel.onClear()
        stopKoin()
    }

    @Test
    fun `validateEnteredData with null title then show snackbar with error message and return false`(){
        //arrange
        val reminderData=ReminderDataItem(null,"test","test",1.0,2.0)

        //act
        val actualValidationResult=saveReminderViewModel.validateEnteredData(reminderData)

        val actualSnackbarMessage=saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        val expectedSnackbarMessage= R.string.err_enter_title

        //assert
        assertFalse(actualValidationResult)
        assertEquals(expectedSnackbarMessage,actualSnackbarMessage)
    }

    @Test
    fun `validateEnteredData with null location then show snackbar with error message and return false`(){
        //arrange
        val reminderData=ReminderDataItem("test","test",null,1.0,2.0)

        //act
        val actualValidationResult=saveReminderViewModel.validateEnteredData(reminderData)

        val actualSnackbarMessage=saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        val expectedSnackbarMessage= R.string.err_select_location

        //assert
        assertFalse(actualValidationResult)
        assertEquals(expectedSnackbarMessage,actualSnackbarMessage)
    }

    @Test
    fun `validateEnteredData with valid data then return true`(){
        //act
        val actualValidationResult=saveReminderViewModel.validateEnteredData(reminderData)

        //assert
        assertTrue(actualValidationResult)
    }

    @Test
    fun `saveReminder then show loading state and hide it after showing toast with reminder saved message `()
    =mainCoroutineRule.runBlockingTest{

        mainCoroutineRule.pauseDispatcher()

        //act
        saveReminderViewModel.saveReminder(reminderData)

        //assert
        assertTrue(saveReminderViewModel.showLoading.getOrAwaitValue())

        mainCoroutineRule.resumeDispatcher()

        //assert
        assertFalse(saveReminderViewModel.showLoading.getOrAwaitValue())

        val actualToastMessage=saveReminderViewModel.showToast.getOrAwaitValue()
        val expectedToastMessage=applicationContext.getString(R.string.reminder_saved)

        //assert
        assertEquals(expectedToastMessage,actualToastMessage)
    }

    @Test
    fun `saveReminder with valid data then return the same inserted data`()=mainCoroutineRule.runBlockingTest{
        //act
        saveReminderViewModel.saveReminder(reminderData)

        val actualData=dataSource.getReminder(reminderData.id) as Result.Success

        assertEquals(reminderData.asReminderDTO(),actualData.data)
    }
}