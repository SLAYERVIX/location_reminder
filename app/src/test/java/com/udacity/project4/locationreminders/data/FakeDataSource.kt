package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private val remindersList:MutableList<ReminderDTO> = mutableListOf()): ReminderDataSource {

    private var errorOccurred=false
    fun hasError(value:Boolean){
        errorOccurred=true
    }
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if(errorOccurred)return Result.Error("Error occurred please try again")
        else if(remindersList.isEmpty())return Result.Success(emptyList())
        return Result.Success(remindersList as List<ReminderDTO>)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        remindersList.forEach { reminderDTO ->
            if(reminderDTO.id==id)return Result.Success(reminderDTO)
        }
        return Result.Error("The id not found")
    }

    override suspend fun deleteAllReminders() {
        remindersList.clear()
    }


}