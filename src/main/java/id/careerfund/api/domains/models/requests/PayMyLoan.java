package id.careerfund.api.domains.models.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PayMyLoan {
    @NotNull
    @Positive
    private Long paymentAmount;
    @NotNull
    @Positive
    private Long paymentAccountId;
}
