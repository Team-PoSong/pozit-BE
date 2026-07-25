package com.pozit.pozitserver.travel.validator;

import com.pozit.pozitserver.travel.dto.request.TravelCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TravelPeriodValidator implements ConstraintValidator<ValidTravelPeriod, TravelCreateRequest> {
    private static final long MAX_NIGHTS=3;

    @Override
    public boolean isValid(
            TravelCreateRequest request,
            ConstraintValidatorContext context
    ){
        if(request==null){
            return true;
        }

        LocalDate startDate=request.startDate();
        LocalDate endDate=request.endDate();

        long nights= ChronoUnit.DAYS.between(startDate,endDate);
        if(nights<0){
            addViolation(
                    context,
                    "endDate",
                    "종료일은 시작일보다 이전일 수 없습니다."
            );
            return false;
        }

        if(nights>MAX_NIGHTS){
            addViolation(
                    context,
                    "endDate",
                    "여행 기간은 최대 3박 4일까지 가능합니다."
            );
            return false;
        }
        return true;
    }

    private void addViolation(
            ConstraintValidatorContext context,
            String field,
            String message
    ){
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
