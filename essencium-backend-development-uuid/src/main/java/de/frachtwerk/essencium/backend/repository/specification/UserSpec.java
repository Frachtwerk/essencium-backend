package de.frachtwerk.essencium.backend.repository.specification;

import de.frachtwerk.essencium.backend.model.User;
import net.kaczmarzyk.spring.data.jpa.domain.GreaterThanOrEqual;
import net.kaczmarzyk.spring.data.jpa.domain.LessThanOrEqual;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;

import java.util.UUID;

@Spec(path = "dateOfBirth", params = "dateOfBirthFrom", spec = GreaterThanOrEqual.class)
interface UserSpecDateOfBirthFrom extends BaseUserSpec<User, UUID> {}

@Spec(path = "dateOfBirth", params = "dateOfBirthTo", spec = LessThanOrEqual.class)
interface UserSpecDateOfBirthTo extends BaseUserSpec<User, UUID> {}

public interface UserSpec extends UserSpecDateOfBirthFrom, UserSpecDateOfBirthTo {}
