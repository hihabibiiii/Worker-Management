package com.example.workermanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ContractorMaterialCategory(val displayName: String) {
    SHUTTERING_ROOF_CASTING("Shuttering Materials"),
    CONSTRUCTION_TOOLS_MATERIALS("Construction Materials"),
    MASON_TOOLS("Mason Tools"),
    OTHER_SITE_EQUIPMENT("Other Site Equipment"),
    OTHER("Other")
}

enum class ContractorMaterialUnit(val displayName: String) {
    PCS("Pcs"),
    BAGS("Bags"),
    KG("Kg"),
    TON("Ton"),
    SQ_FT("Sq Ft"),
    CU_FT("Cu Ft"),
    MTR("Mtr"),
    LTR("Ltr"),
    SETS("Sets"),
    OTHER("Other")
}

@Entity(tableName = "contractor_materials")
data class ContractorMaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: ContractorMaterialCategory = ContractorMaterialCategory.OTHER,
    val unit: ContractorMaterialUnit = ContractorMaterialUnit.PCS,
    val minStockLevel: Double = 0.0,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncOrigin: Int = SyncOrigin.NEW_LOCAL,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isPendingSync: Boolean = true
)
