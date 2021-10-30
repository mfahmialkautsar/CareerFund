package id.careerfund.api.domains.models;

import id.careerfund.api.domains.ERole;
import id.careerfund.api.utils.validators.ValidEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegister {

    @NotEmpty(message = "Name is mandatory")
    private String name;

    @NotEmpty(message = "Email is mandatory")
    @Email(message = "Please input email")
    private String email;

    @NotEmpty(message = "Password is mandatory")
    private String password;

    @NotNull(message = "Role is mandatory")
    @ValidEnum(enumClass = ERole.class, groups = ERole.class, message = "Role is not correct")
    @Enumerated(EnumType.STRING)
    private ERole role;
}
