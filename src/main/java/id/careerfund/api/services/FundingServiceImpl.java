package id.careerfund.api.services;

import id.careerfund.api.domains.entities.*;
import id.careerfund.api.domains.models.requests.WithdrawRequest;
import id.careerfund.api.repositories.BankRepository;
import id.careerfund.api.repositories.FinancialTransactionRepository;
import id.careerfund.api.repositories.FundingRepository;
import id.careerfund.api.repositories.WithdrawalsRepository;
import id.careerfund.api.utils.helpers.PageableHelper;
import id.careerfund.api.utils.mappers.FundingMapper;
import id.careerfund.api.utils.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.web.firewall.RequestRejectedException;
import id.careerfund.api.domains.entities.Funding;
import id.careerfund.api.domains.entities.Loan;
import id.careerfund.api.domains.models.responses.FundingDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FundingServiceImpl implements FundingService {
    private final FundingRepository fundingRepo;
    private final WithdrawalsRepository withdrawRepo;
    private final BankRepository bankRepo;
    private final FinancialTransactionRepository financialTransactionRepo;
    private final CashService cashService;
    private final FundingMapper fundingMapper;

    @Override
    public Page<FundingDto> getMyFundings(Principal principal, String sort, String order) {
        User user = UserMapper.principalToUser(principal);
        Pageable pageable = PageableHelper.getPageable(sort, order);
        Page<Funding> fundingPage = fundingRepo.findDistinctByLender_Id(user.getId(), pageable);

        return fundingPage.map(fundingMapper::entityToDto);
    }

    @Override
    public List<FundingDto> getWithdrawableFundings(Principal principal) {
        Page<FundingDto> fundingPage = getMyFundings(principal, null, null);
        return fundingPage.getContent().stream().filter(fundingDto -> fundingDto.getLoan().getMonthsPaid() >= fundingDto.getLoan().getLoanPayments().size() - 1 && fundingDto.getWithdrawals() == null && fundingDto.getLoan().getLoanPayments().size() - 1 >= fundingDto.getLoan().getTenorMonth()).collect(Collectors.toList());
    }

    @Override
    public FundingDto getMyFundingById(Principal principal, Long id) throws EntityNotFoundException {
        User user = UserMapper.principalToUser(principal);
        Funding funding = fundingRepo.findByIdAndLender_Id(id, user.getId());
        if (funding == null) throw new EntityNotFoundException("FUNDING_NOT_FOUND");
        return fundingMapper.entityToDto(funding);
    }

    @Override
    public Long getTotalLoanFund(Loan loan) {
        long totalFund = 0L;
        for (Funding funding : loan.getFundings()) {
            totalFund += funding.getFinancialTransaction().getNominal().longValue();
        }
        return totalFund;
    }

    @Override
    public Double getMonthlyCapital(Funding funding) {
        return funding.getFinancialTransaction().getNominal() / (double) funding.getLoan().getTenorMonth();
    }

    @Override
    public void withdrawLoan(Principal principal, Long fundingId, WithdrawRequest withdrawRequest) throws EntityNotFoundException, RequestRejectedException {
        User user = UserMapper.principalToUser(principal);
        Funding funding = fundingRepo.findByIdAndLender_Id(fundingId, user.getId());
        if (funding == null) throw new EntityNotFoundException("FUNDING_NOT_FOUND");
        if (funding.getLoan().getLoanPayments().size() - 1 < funding.getLoan().getTenorMonth())
            throw new RequestRejectedException("LOAN_UNFINISHED");
        if (funding.getWithdrawals() != null) throw new RequestRejectedException("WITHDRAWN");

        Withdrawals withdrawals = new Withdrawals();
        withdrawals.setFunding(funding);
        withdrawals.setBank(bankRepo.getById(withdrawRequest.getBankId()));
        withdrawals.setAccountNumber(withdrawRequest.getAccountNumber());
        withdrawRepo.save(withdrawals);

        FinancialTransaction financialTransaction = new FinancialTransaction();
        financialTransaction.setNominal(funding.getFinancialTransaction().getNominal());
        financialTransactionRepo.save(financialTransaction);

        cashService.doCredit(financialTransaction);
    }
}
