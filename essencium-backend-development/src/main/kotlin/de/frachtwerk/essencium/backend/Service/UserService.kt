package de.frachtwerk.essencium.backend.service

import de.frachtwerk.essencium.backend.model.User
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails
import de.frachtwerk.essencium.backend.model.dto.UserDto
import de.frachtwerk.essencium.backend.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.Optional

@Service
class UserService(
    userRepository: UserRepository,
    passwordEncoder: PasswordEncoder,
    userMailService: UserMailService,
    private val roleService: RoleService,
    adminRightRoleCache: AdminRightRoleCache,
    jwtTokenService: JwtTokenService
) : AbstractUserService<User, EssenciumUserDetails<Long>, Long, UserDto>(
    userRepository,
    passwordEncoder,
    userMailService,
    roleService,
    adminRightRoleCache,
    jwtTokenService
) {

    override fun <E : UserDto> convertDtoToEntity(
        dto: E,
        currentEntityOpt: Optional<User>
    ): User {
        val roles = dto.roles.map { roleName -> roleService.getByName(roleName) }.toSet()

        val user = currentEntityOpt.orElseGet { User() }

        if (dto.id != null) user.id = dto.id


        user.email = dto.email
        user.isEnabled = dto.isEnabled
        user.firstName = dto.firstName
        user.lastName = dto.lastName
        user.locale = dto.locale
        user.setMobile(dto.mobile)
        user.setPhone(dto.phone)
        user.source = dto.source
        user.isLoginDisabled = dto.isLoginDisabled
        user.roles = roles

        return user
    }


    override fun selfUpdate(user: User, updateInformation: UserDto): User {
        user.setPhone(updateInformation.phone)
        user.setMobile(updateInformation.mobile)
        return super.selfUpdate(user, updateInformation)
    }

    override fun selfUpdatePermittedFields(): Set<String> =
        super.selfUpdatePermittedFields().toMutableSet().apply {
            add("phone")
            add("mobile")
        }

    override fun getAllPreProcessing(pageable: Pageable): Pageable {
        val nameSortOrder = pageable.sort.getOrderFor("name") ?: return pageable
        val orders = pageable.sort.toList().toMutableList()
        val idx = orders.indexOf(nameSortOrder)

        val firstNameSortOrder = nameSortOrder.withProperty("firstName")
        val lastNameSortOrder = nameSortOrder.withProperty("lastName")

        orders.removeAt(idx)
        orders.add(idx, firstNameSortOrder)
        orders.add(idx + 1, lastNameSortOrder)

        return PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(orders))
    }

    override fun getNewUser(): UserDto = UserDto()
}
