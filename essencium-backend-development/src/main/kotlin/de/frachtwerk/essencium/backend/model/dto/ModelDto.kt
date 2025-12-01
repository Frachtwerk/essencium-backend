package de.frachtwerk.essencium.backend.model.dto

import de.frachtwerk.essencium.backend.model.Identifiable
import jakarta.annotation.Nullable


abstract class ModelDto(
    @field:Nullable
    private var _id: Long? = null
) : Identifiable<Long> {

    @Nullable
    override fun getId(): Long? = _id

    override fun setId(@Nullable id: Long?) {
        _id = id
    }
}
