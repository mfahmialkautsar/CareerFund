package id.careerfund.api.services;

import id.careerfund.api.domains.entities.Class;

public interface LoanService {
    Double getInterestPercent(Class aClass, Integer tenorMonth);

    Long getInterestNumber(Class aClass, Integer tenorMonth, Long downPayment);

    Long getMonthlyAdminFee(Class aClass, Integer tenorMonth, Long downPayment);

    Long getAdminFee(Class aClass, Integer tenorMonth, Long downPayment);

    Long getMonthlyPaymentWithoutAdminFee(Class aClass, Integer tenorMonth, Long downPayment);

    Long getMonthlyPayment(Class aClass, Integer tenorMonth, Long downPayment);

    Long getTotalPaymentWithoutAdminFeeAndInterest(Class aClass, Long downPayment);

    Long getTotalPaymentWithoutAdminFee(Class aClass, Integer tenorMonth, Long downPayment);

    Long getTotalPayment(Class aClass, Integer tenorMonth, Long downPayment);
}