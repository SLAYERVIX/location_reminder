package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.util.MainCoroutineRule
import junit.framework.TestCase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var remindersList:MutableList<ReminderDTO>
    private var applicationContext=ApplicationProvider.getApplicationContext<MyApp>()
    private lateinit var dataSource:FakeDataSource
    private lateinit var remindersListViewModel:RemindersListViewModel

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule=MainCoroutineRule()

    @Before
    fun setup(){
        dataSource=FakeDataSource()
        remindersListViewModel=RemindersListViewModel(applicationContext,dataSource)
        remindersList= mutableListOf(
            ReminderDTO("Test1", "Test1", "Test1", 1.0, 2.0,)
            ,ReminderDTO("Test2", "Test2", "Test2", 3.0, 4.0)
            ,ReminderDTO("Test3", "Test3", "Test3", 5.0, 6.0)
            ,ReminderDTO("Test4", "Test4", "Test4", 7.0, 8.0)
        )
    }

    @After
    fun cleanResources() {
        applicationContext=null
        remindersList.clear()
        stopKoin()
    }
    @Test
    fun `loadReminders with emptyList then showSnackBar with empty list message`(){
        //act
        remindersListViewModel.loadReminders()

        //assert
        val expected= emptyList<ReminderDTO>()
        val actualResult=remindersListViewModel.remindersList.getOrAwaitValue()
        assertEquals(expected,actualResult)
    }
    @Test
    fun `loadReminders with valid remindersList then return the same inserted list`(){
        //arrange
        dataSource=FakeDataSource(remindersList)
        remindersListViewModel=RemindersListViewModel(applicationContext,dataSource)

        //act
        remindersListViewModel.loadReminders()

        //assert
        val result =remindersListViewModel.remindersList.getOrAwaitValue()
        assertEquals(remindersList,result.asReminderDTO())
    }

    @Test
    fun `loadReminders with setting error then show showSnackBar error message`(){
        //arrange
        dataSource.hasError(true)

        //act
        remindersListViewModel.loadReminders()

        //assert
        val expected="Error occurred please try again"
        val actual =remindersListViewModel.showSnackBar.getOrAwaitValue()
        assertEquals(expected,actual)
    }


    @Test
    fun `loadReminders then show loading state and hide it after returning result`(){

        mainCoroutineRule.pauseDispatcher()

        //act
        remindersListViewModel.loadReminders()

        //assert
        assertTrue(remindersListViewModel.showLoading.getOrAwaitValue())

        mainCoroutineRule.resumeDispatcher()

        //assert
        assertFalse(remindersListViewModel.showLoading.getOrAwaitValue())
    }

}