package com.example.data.repository

import com.example.data.local.LeadDao
import com.example.data.model.Lead
import kotlinx.coroutines.flow.Flow

class LeadRepository(private val leadDao: LeadDao) {
    val allLeads: Flow<List<Lead>> = leadDao.getAllLeads()
    val savedLeads: Flow<List<Lead>> = leadDao.getSavedLeads()

    suspend fun getLeadById(id: Int): Lead? {
        return leadDao.getLeadById(id)
    }

    suspend fun insertLeads(leads: List<Lead>) {
        leadDao.insertLeads(leads)
    }

    suspend fun insertLead(lead: Lead): Long {
        return leadDao.insertLead(lead)
    }

    suspend fun updateLead(lead: Lead) {
        leadDao.updateLead(lead)
    }

    suspend fun deleteLead(lead: Lead) {
        leadDao.deleteLead(lead)
    }

    suspend fun clearUnsavedLeads() {
        leadDao.clearUnsavedLeads()
    }

    suspend fun clearAll() {
        leadDao.clearAll()
    }
}
