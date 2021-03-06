package id.careerfund.api.domains.models.responses;

import id.careerfund.api.domains.entities.Interest;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MyInterests {
    private final Long user;
    private final List<Interest> interests;
}
