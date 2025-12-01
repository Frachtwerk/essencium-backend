package de.frachtwerk.essencium.backend.model.representation.assembler

import de.frachtwerk.essencium.backend.model.ExampleEntity
import de.frachtwerk.essencium.backend.model.representation.ExampleRepresentation
import org.springframework.stereotype.Component

@Component
class ExampleAssembler :
    AbstractRepresentationAssembler<ExampleEntity, ExampleRepresentation>() {

    override fun toModel(entity: ExampleEntity): ExampleRepresentation {
        return ExampleRepresentation(
            id = entity.id,
            content = entity.content
        )
    }
}
