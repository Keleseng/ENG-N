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

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTideRecord(record: TideCalculationRecord): Long

  @Query("DELETE FROM tide_calculations WHERE id = :id")
  suspend fun deleteTideRecordById(id: Long)

  @Query("DELETE FROM tide_calculations")
  suspend fun clearAllTideRecords()
}
