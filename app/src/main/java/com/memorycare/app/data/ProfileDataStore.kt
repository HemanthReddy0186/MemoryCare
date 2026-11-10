package com.memorycare.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.memorycare.app.model.UserProfile
import com.memorycare.app.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "memory_care_profile_prefs")

/**
 * DataStore repository for persisting user profiles and tracking the last selected profile.
 */
class ProfileDataStore(private val context: Context) {

    private val gson = Gson()

    companion object {
        private val KEY_PROFILES_JSON = stringPreferencesKey("key_profiles_json")
        private val KEY_LAST_SELECTED_PROFILE_ID = stringPreferencesKey("key_last_selected_profile_id")
    }

    /**
     * Default starting profiles for zero-friction initial experience.
     */
    private val defaultProfiles = listOf(
        UserProfile(
            id = "profile_default_patient",
            name = "Grandpa John",
            age = 78,
            defaultRole = UserRole.PATIENT,
            avatarEmoji = "👴",
            linkedPatientId = null
        ),
        UserProfile(
            id = "profile_default_caretaker",
            name = "Nurse Sarah",
            age = 38,
            defaultRole = UserRole.CARETAKER,
            avatarEmoji = "👩‍⚕️",
            linkedPatientId = "profile_default_patient"
        )
    )

    /**
     * Flow of all local profiles from DataStore.
     */
    val profilesFlow: Flow<List<UserProfile>> = context.profileDataStore.data.map { preferences ->
        val json = preferences[KEY_PROFILES_JSON]
        if (json.isNullOrEmpty()) {
            defaultProfiles
        } else {
            try {
                val type = object : TypeToken<List<UserProfile>>() {}.type
                val list = gson.fromJson<List<UserProfile>>(json, type) ?: defaultProfiles
                // Ensure any caretaker profile has a linked patient fallback if available
                val firstPatient = list.firstOrNull { it.defaultRole == UserRole.PATIENT } ?: defaultProfiles.first()
                list.map { profile ->
                    if (profile.defaultRole == UserRole.CARETAKER && profile.linkedPatientId.isNullOrBlank()) {
                        profile.copy(linkedPatientId = firstPatient.id)
                    } else {
                        profile
                    }
                }
            } catch (e: Exception) {
                defaultProfiles
            }
        }
    }

    /**
     * Flow of last selected profile ID from DataStore.
     */
    val lastSelectedProfileIdFlow: Flow<String?> = context.profileDataStore.data.map { preferences ->
        preferences[KEY_LAST_SELECTED_PROFILE_ID]
    }

    /**
     * Saves a new or updated profile in DataStore.
     */
    suspend fun saveProfile(profile: UserProfile) {
        context.profileDataStore.edit { preferences ->
            val currentJson = preferences[KEY_PROFILES_JSON]
            val currentList = if (currentJson.isNullOrEmpty()) {
                defaultProfiles.toMutableList()
            } else {
                try {
                    val type = object : TypeToken<List<UserProfile>>() {}.type
                    gson.fromJson<List<UserProfile>>(currentJson, type)?.toMutableList() ?: defaultProfiles.toMutableList()
                } catch (e: Exception) {
                    defaultProfiles.toMutableList()
                }
            }

            val existingIndex = currentList.indexOfFirst { it.id == profile.id }
            if (existingIndex != -1) {
                currentList[existingIndex] = profile
            } else {
                currentList.add(profile)
            }

            preferences[KEY_PROFILES_JSON] = gson.toJson(currentList)
            preferences[KEY_LAST_SELECTED_PROFILE_ID] = profile.id
        }
    }

    /**
     * Deletes a profile from DataStore.
     */
    suspend fun deleteProfile(profileId: String) {
        context.profileDataStore.edit { preferences ->
            val currentJson = preferences[KEY_PROFILES_JSON]
            if (!currentJson.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<List<UserProfile>>() {}.type
                    val list = gson.fromJson<List<UserProfile>>(currentJson, type)?.toMutableList() ?: mutableListOf()
                    list.removeAll { it.id == profileId }
                    preferences[KEY_PROFILES_JSON] = gson.toJson(list)
                } catch (e: Exception) {
                    // Ignore error on delete
                }
            }
            if (preferences[KEY_LAST_SELECTED_PROFILE_ID] == profileId) {
                preferences.remove(KEY_LAST_SELECTED_PROFILE_ID)
            }
        }
    }

    /**
     * Updates the last selected profile ID in DataStore.
     */
    suspend fun setLastSelectedProfileId(profileId: String) {
        context.profileDataStore.edit { preferences ->
            preferences[KEY_LAST_SELECTED_PROFILE_ID] = profileId
        }
    }

    /**
     * Clears the active/last selected profile ID in DataStore for sign-out.
     */
    suspend fun clearLastSelectedProfileId() {
        context.profileDataStore.edit { preferences ->
            preferences.remove(KEY_LAST_SELECTED_PROFILE_ID)
        }
    }

    /**
     * Updates the linked patient for a specific caretaker profile.
     */
    suspend fun updateLinkedPatient(caretakerId: String, patientId: String) {
        context.profileDataStore.edit { preferences ->
            val currentJson = preferences[KEY_PROFILES_JSON]
            if (!currentJson.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<List<UserProfile>>() {}.type
                    val list = gson.fromJson<List<UserProfile>>(currentJson, type)?.toMutableList() ?: mutableListOf()
                    val index = list.indexOfFirst { it.id == caretakerId }
                    if (index != -1) {
                        list[index] = list[index].copy(linkedPatientId = patientId)
                        preferences[KEY_PROFILES_JSON] = gson.toJson(list)
                    }
                } catch (_: Exception) { }
            }
        }
    }
}
