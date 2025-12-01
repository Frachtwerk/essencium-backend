package de.frachtwerk.essencium.backend.model.representation

import java.util.*
import de.frachtwerk.essencium.backend.model.Role

class UserRepresentation(
    id: Long? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var phone: String? = null,
    var mobile: String? = null,
    var email: String? = null,
    var locale: Locale? = null,
    var roles: Set<Role> = emptySet(),
    var enabled: Boolean = false,
    var loginDisabled: Boolean = false,
    var source: String? = null
) : ModelRepresentation()