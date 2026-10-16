package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MaterialCategory(val displayName: String) {
    CIVIL("Civil"),
    PLUMBING("Plumbing"),
    ELECTRICAL("Electrical"),
    FINISHING("Finishing"),
    HARDWARE("Hardware"),
    OTHER("Other")
}

enum class MaterialUnit(val displayName: String) {
    BAGS("Bags"),
    KG("Kg"),
    TON("Ton"),
    SQ_FT("Sq Ft"),
    CU_FT("Cu Ft"),
    PCS("Pcs"),
    MTR("Mtr"),
    LTR("Ltr"),
    OTHER("Other")
}

@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: MaterialCategory = MaterialCategory.OTHER,
    val unit: MaterialUnit = MaterialUnit.BAGS,
    val minStockLevel: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
