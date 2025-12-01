
package de.frachtwerk.essencium.backend.model.dto



class UserDto : BaseUserDto<Long?>() {
    var phone: String? = null
    var mobile: String? = null
}