package de.frachtwerk.essencium.backend.model.representation.assembler

import de.frachtwerk.essencium.backend.model.User
import de.frachtwerk.essencium.backend.model.representation.UserRepresentation
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class UserAssembler :
    AbstractRepresentationAssembler<User, UserRepresentation>() {

    override fun toModel(entity: User): UserRepresentation {
        return UserRepresentation(
            id = entity.id,
            firstName = entity.firstName,
            lastName = entity.lastName,
            phone = entity.getPhone(),
            mobile = entity.getMobile(),
            email = entity.email,
            locale = entity.locale,
            roles = entity.roles ?: emptySet(),
            enabled = entity.isEnabled,
            loginDisabled = entity.isLoginDisabled,
            source = entity.source
        )
    }
}
