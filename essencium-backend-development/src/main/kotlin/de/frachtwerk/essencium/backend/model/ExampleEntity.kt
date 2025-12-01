package de.frachtwerk.essencium.backend.model

import jakarta.persistence.Entity



@Entity
class ExampleEntity(
    var content: String? = null
) : AbstractModel() {

    override fun getTitle(): String? = content
}