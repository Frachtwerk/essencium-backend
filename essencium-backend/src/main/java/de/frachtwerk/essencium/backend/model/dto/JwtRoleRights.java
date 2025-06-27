package de.frachtwerk.essencium.backend.model.dto;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtRoleRights {

  private String role;
  private Set<String> rights;

  //    public String getRole() {
  //        return role;
  //    }
  //
  //    public void setRole(String role) {
  //        this.role = role;
  //    }
  //
  //    public List<String> getRights() {
  //        return rights != null ? rights : Collections.emptyList();
  //    }
  //
  //    public void setRights(List<String> rights) {
  //        this.rights = rights;
  //    }
}
