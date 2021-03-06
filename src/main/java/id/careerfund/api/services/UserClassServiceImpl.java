package id.careerfund.api.services;

import id.careerfund.api.domains.entities.*;
import id.careerfund.api.domains.entities.Class;
import id.careerfund.api.domains.models.requests.PayMyLoan;
import id.careerfund.api.domains.models.requests.UserClassRequest;
import id.careerfund.api.domains.models.responses.UserClassBorrowerDto;
import id.careerfund.api.repositories.*;
import id.careerfund.api.utils.mappers.UserClassMapper;
import id.careerfund.api.utils.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserClassServiceImpl implements UserClassService {
    private final LoanRepository loanRepo;
    private final UserClassRepository userClassRepo;
    private final ClassRepository classRepo;
    private final PaymentAccountRepository paymentAccountRepo;
    private final PaymentRepository paymentRepo;
    private final LoanPaymentRepository loanPaymentRepo;
    private final FinancialTransactionRepository financialTransactionRepo;
    private final LoanService loanService;
    private final CashService cashService;
    private final UserClassMapper userClassMapper;

    private boolean hasPaidDownPayment(Loan loan) {
        return !loan.getLoanPayments().isEmpty();
    }

    @Override
    public UserClassBorrowerDto registerClass(Principal principal, UserClassRequest userClassRequest)
            throws EntityNotFoundException, RequestRejectedException {
        User user = UserMapper.principalToUser(principal);
        Class aClass = classRepo.getById(userClassRequest.getClassId());
        if (aClass.getStartDate().isBefore(LocalDate.now().minusDays(7))) {
            throw new RequestRejectedException("CLASS_REGISTRATION_OVER");
        }
        if (userClassRequest.getDownPayment() > aClass.getPrice() * 0.3)
            throw new RequestRejectedException("DOWNPAYMENT_GREATER");
        if (userClassRequest.getDownPayment() < aClass.getPrice() * 0.1)
            throw new RequestRejectedException("DOWNPAYMENT_LESS");

        log.info("Registering class {}", userClassRequest.getClassId());
        Loan loan = new Loan();
        loan.setBorrower(user);
        loan.setDownPayment(userClassRequest.getDownPayment());
        loan.setTenorMonth(userClassRequest.getTenorMonth());
        loan.setInterestPercent(loanService.getInterestPercent(aClass, userClassRequest.getTenorMonth()));
        loan.setInterestNumber(loanService.getInterestNumber(aClass, userClassRequest.getTenorMonth(),
                userClassRequest.getDownPayment()));
        loan.setMonthlyFee(loanService.getMonthlyAdminFee(aClass, userClassRequest.getTenorMonth(),
                userClassRequest.getDownPayment()));
        loan.setFee(
                loanService.getAdminFee(aClass, userClassRequest.getTenorMonth(), userClassRequest.getDownPayment()));
        loan.setMonthlyPayment(loanService.getMonthlyPayment(aClass, userClassRequest.getTenorMonth(),
                userClassRequest.getDownPayment()));
        loan.setTotalPayment(loanService.getTotalPayment(aClass, userClassRequest.getTenorMonth(),
                userClassRequest.getDownPayment()));
        loan.setDpPaymentExpiredTime(LocalDateTime.now().plusDays(2));
        loanRepo.save(loan);

        UserClass userClass = new UserClass();
        userClass.setAClass(aClass);
        userClass.setUser(user);
        userClass.setLoan(loan);
        userClassRepo.save(userClass);

        return userClassMapper.entityToBorrowerDto(userClass);
    }

    @Override
    public List<UserClassBorrowerDto> getMyClasses(Principal principal) {
        List<UserClass> userClasses = userClassRepo.findByUser(UserMapper.principalToUser(principal));

        return userClasses.stream().map(userClassMapper::entityToBorrowerDto).collect(Collectors.toList());
    }

    @Override
    public UserClassBorrowerDto getMyClassById(Principal principal, Long id) throws AccessDeniedException {
        UserClass userClass = userClassRepo.getById(id);
        User user = UserMapper.principalToUser(principal);
        if (!userClass.getUser().getId().equals(user.getId()))
            throw new AccessDeniedException("USER_WRONG");
        return userClassMapper.entityToBorrowerDto(userClass);
    }

    @Override
    public UserClassBorrowerDto payMyClass(Principal principal, Long id, PayMyLoan payMyLoan)
            throws AccessDeniedException, RequestRejectedException, EntityNotFoundException {
        Optional<UserClass> optionalUserClass = userClassRepo.findById(id);
        if (!optionalUserClass.isPresent())
            throw new EntityNotFoundException();
        UserClass userClass = optionalUserClass.get();
        User user = UserMapper.principalToUser(principal);
        Loan loan = userClass.getLoan();

        Optional<PaymentAccount> paymentAccountOptional = paymentAccountRepo.findById(payMyLoan.getPaymentAccountId());
        if (!paymentAccountOptional.isPresent()) throw new EntityNotFoundException("PAYMENT_ACCOUNT_NOT_FOUND");
        PaymentAccount paymentAccount = paymentAccountOptional.get();

        FinancialTransaction financialTransaction = new FinancialTransaction();
        financialTransaction.setNominal(payMyLoan.getPaymentAmount().doubleValue());

        Payment payment = new Payment();
        payment.setPaymentAccount(paymentAccount);
        payment.setFinancialTransaction(financialTransaction);

        int paymentPeriod = loan.getLoanPayments().size();
        LoanPayment loanPayment = new LoanPayment();
        loanPayment.setLoan(loan);
        loanPayment.setPeriod(paymentPeriod);
        loanPayment.setPayment(payment);

        if (!userClass.getUser().getId().equals(user.getId()))
            throw new AccessDeniedException("USER_WRONG");
        if (loan.getLoanPayments().size() - 1 >= loan.getTenorMonth())
            throw new RequestRejectedException("LOAN_FINISHED");
        if (!hasPaidDownPayment(loan)) {
            if (loan.getDpPaymentExpiredTime().isBefore(LocalDateTime.now()))
                throw new RequestRejectedException("DOWNPAYMENT_EXPIRED");
            if (payMyLoan.getPaymentAmount().equals(loan.getDownPayment())) {
                onPaymentSuccess(loanPayment, financialTransaction, payment, userClass);
                // Add to company cash
                cashService.doDebit(financialTransaction);
                loan.setMonthlyPaymentDueDate(LocalDateTime.now().getDayOfMonth());
            } else if (payMyLoan.getPaymentAmount().equals(loan.getMonthlyPayment())) {
                throw new RequestRejectedException("SHOULD_PAY_DOWNPAYMENT");
            } else {
                throw new RequestRejectedException("WRONG_AMOUNT");
            }

        } else {
            if (payMyLoan.getPaymentAmount().equals(loan.getDownPayment())) {
                throw new RequestRejectedException("SHOULD_PAY_MONTHLYPAYMENT");
            } else if (payMyLoan.getPaymentAmount().equals(loan.getMonthlyPayment())) {
                onPaymentSuccess(loanPayment, financialTransaction, payment, userClass);
                // Add to company cash
                cashService.doDebit(financialTransaction);

                // Take fee
                cashService.takeMonthlyFee(financialTransaction,
                        loanService
                                .getMonthlyAdminFee(userClass.getAClass(), loan.getTenorMonth(), loan.getDownPayment())
                                .doubleValue());

                // Send payback to lender balance
//                loan.getFundings().forEach(funding -> balanceService.sendLenderPayback(funding, financialTransaction));
            } else {
                throw new RequestRejectedException("WRONG_AMOUNT");
            }
        }

        return userClassMapper.entityToBorrowerDto(userClass);
    }

    private void onPaymentSuccess(LoanPayment loanPayment, FinancialTransaction financialTransaction, Payment payment,
            UserClass userClass) {
        financialTransactionRepo.save(financialTransaction);
        paymentRepo.save(payment);
        loanPaymentRepo.save(loanPayment);
        userClass.getLoan().getLoanPayments().add(loanPayment);
    }
}
