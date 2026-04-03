package com.trader.salesmanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.trader.salesmanager.data.remote.FirebaseActivationService
import com.trader.salesmanager.domain.repository.ActivationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ActivationRepositoryImpl(
    private val context: Context,
    private val firebaseService: FirebaseActivationService
) : ActivationRepository {

    private val IS_ACTIVATED = booleanPreferencesKey("is_activated")

    override suspend fun validateCode(code: String): Boolean = firebaseService.validateCode(code)

    override suspend fun isActivated(): Boolean =
        context.dataStore.data.map { it[IS_ACTIVATED] ?: false }.first()

    override suspend fun saveActivationStatus(activated: Boolean) {
        context.dataStore.edit { it[IS_ACTIVATED] = activated }
    }
}