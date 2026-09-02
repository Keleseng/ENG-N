package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationDao {

  // ══════════════════════════════════════════════════════════════
  // DEMİRLEME HESAPLAMALARI
  // ══════════════════════════════════════════════════════════════

  @Query("SELECT * FROM anchor_calculations ORDER BY timestamp DESC")
  fun getAllAnchorRecords(): Flow<List<AnchorCalculationRecord>>

  @Query("SELECT * FROM anchor_calculations WHERE vesselName = :vesselName AND chainScopeMeters = :chainScopeMeters AND depthMeters = :depthMeters AND metersPerShackle = :metersPerShackle LIMIT 1")
  suspend fun findDuplicateAnchorRecord(vesselName: String, chainScopeMeters: Double, depthMeters: Double, metersPerShackle: Double): AnchorCalculationRecord?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAnchorRecord(record: AnchorCalculationRecord): Long

  @Query("DELETE FROM anchor_calculations WHERE id = :id")
  suspend fun deleteAnchorRecordById(id: Long)

  @Query("DELETE FROM anchor_calculations")
  suspend fun clearAllAnchorRecords()

  // ══════════════════════════════════════════════════════════════
  // GELGİT HESAPLAMALARI
  // ══════════════════════════════════════════════════════════════

  @Query("SELECT * FROM tide_calculations ORDER BY timestamp DESC")
  fun getAllTideRecords(): Flow<List<TideCalculationRecord>>

  @Query("SELECT * FROM tide_calculations WHERE portName = :portName AND chartDatumDepthMeters = :chartDatumDepthMeters AND shipDraftMeters = :shipDraftMeters AND requiredUkcMeters = :requiredUkcMeters LIMIT 1")
  suspend fun findDuplicateTideRecord(portName: String, chartDatumDepthMeters: Double, shipDraftMeters: Double, requiredUkcMeters: Double): TideCalculationRecord?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTideRecord(record: TideCalculationRecord): Long

  @Query("DELETE FROM tide_calculations WHERE id = :id")
  suspend fun deleteTideRecordById(id: Long)

  @Query("DELETE FROM tide_calculations")
  suspend fun clearAllTideRecords()
}
