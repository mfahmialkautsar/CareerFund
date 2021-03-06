package id.careerfund.api.domains.entities;

import id.careerfund.api.domains.EPaymentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Table(name = "payment_types")
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class PaymentType extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true)
    private EPaymentType name;

    @OneToMany(mappedBy = "paymentType", orphanRemoval = true)
    private List<PaymentAccount> paymentAccounts = new ArrayList<>();

}