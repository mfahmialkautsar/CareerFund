package id.careerfund.api.domains.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
public class UpdatePassword {
    @NotEmpty(message = "Otp is mandatory")
    public String otp;

    @NotEmpty(message = "NewPassword is mandatory")
    public String newpassword;
}
