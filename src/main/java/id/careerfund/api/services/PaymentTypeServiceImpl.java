package id.careerfund.api.services;

import id.careerfund.api.domains.entities.PaymentType;
import id.careerfund.api.repositories.PaymentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentTypeServiceImpl implements PaymentTypeService {
    private final PaymentTypeRepository paymentTypeRepo;

    @Override
    public List<PaymentType> getPaymentTypes() {
        return paymentTypeRepo.findAll();
    }
}
