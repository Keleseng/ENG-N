package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculationRepository(private val dao: CalculationDao) {

  val allAnchorRecords: Flow<List<AnchorCalculationRecord>> = dao.getAllAnchorRecords()
  val allTideRecords: Flow<List<TideCalculationRecord>> = dao.getAllTideRecords()

  suspend fun saveAnchorCalculation(record: AnchorCalculationRecord): Long {
    val existing = dao.findDuplicateAnchorRecord(
      vesselName = record.vesselName,
      chainScopeMeters = record.chainScopeMeters,
      depthMeters = record.depthMeters,
      metersPerShackle = record.metersPerShackle
    )
    val recordToSave = if (existing != null) {
      record.copy(id = existing.id, timestamp = System.currentTimeMillis())
    } else {
      record
    }
    return dao.insertAnchorRecord(recordToSave)
  }

  suspend fun deleteAnchorRecord(id: Long) {
    dao.deleteAnchorRecordById(id)
  }

  suspend fun clearAnchorHistory() {
    dao.clearAllAnchorRecords()
  }

  suspend fun saveTideCalculation(record: TideCalculationRecord): Long {
    val existing = dao.findDuplicateTideRecord(
      portName = record.portName,
      chartDatumDepthMeters = record.chartDatumDepthMeters,
      shipDraftMeters = record.shipDraftMeters,
      requiredUkcMeters = record.requiredUkcMeters
    )
    val recordToSave = if (existing != null) {
      record.copy(id = existing.id, timestamp = System.currentTimeMillis())
    } else {
      record
    }
    return dao.insertTideRecord(recordToSave)
  }

  suspend fun deleteTideRecord(id: Long) {
    dao.deleteTideRecordById(id)
  }

  suspend fun clearTideHistory() {
    dao.clearAllTideRecords()
  }
}
