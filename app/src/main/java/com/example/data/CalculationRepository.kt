package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculationRepository(private val dao: CalculationDao) {

  val allAnchorRecords: Flow<List<AnchorCalculationRecord>> = dao.getAllAnchorRecords()
  val allTideRecords: Flow<List<TideCalculationRecord>> = dao.getAllTideRecords()

  suspend fun saveAnchorCalculation(record: AnchorCalculationRecord): Long {
    return dao.insertAnchorRecord(record)
  }

  suspend fun deleteAnchorRecord(id: Long) {
    dao.deleteAnchorRecordById(id)
  }

  suspend fun clearAnchorHistory() {
    dao.clearAllAnchorRecords()
  }

  suspend fun saveTideCalculation(record: TideCalculationRecord): Long {
    return dao.insertTideRecord(record)
  }

  suspend fun deleteTideRecord(id: Long) {
    dao.deleteTideRecordById(id)
  }

  suspend fun clearTideHistory() {
    dao.clearAllTideRecords()
  }
}
