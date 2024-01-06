package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.service.RightService;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Primary
@Configuration
public class RightInitializer extends DefaultRightInitializer {
  public RightInitializer(RightService rightService) {
    super(rightService);
  }

  @Override
  public Set<Right> getAdditionalApplicationRights() {
    Set<Right> rightSet = super.getAdditionalApplicationRights();

    Stream<Right> singleRights = Stream.of(new Right("READ", ""));
    Stream<Right> crudRights1 =
        getCombinedRights(Stream.of("CREATE", "READ", "UPDATE", "DELETE"), "EXAMPLE");
    Stream<Right> crudRights2 =
        getCombinedRights(
            Stream.of(
                "CREATE",
                "READ_ALL",
                "READ_OWN",
                "UPDATE_ALL",
                "UPDATE_OWN",
                "DELETE_ALL",
                "DELETE_OWN"),
            new Right("EXAMPLE_2", "TEST"));

    rightSet.addAll(
        Stream.of(singleRights, crudRights1, crudRights2)
            .flatMap(Function.identity())
            .collect(Collectors.toSet()));

    return rightSet;
  }
}
