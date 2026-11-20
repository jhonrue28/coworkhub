package com.academy.coworkhub.business.service.impl;

import com.academy.coworkhub.business.exception.BusinessRuleException;
import com.academy.coworkhub.business.exception.ResourceNotFoundException;
import com.academy.coworkhub.business.service.MembershipPlanService;
import com.academy.coworkhub.persistence.entity.MembershipPlan;
import com.academy.coworkhub.persistence.repository.MemberRepository;
import com.academy.coworkhub.persistence.repository.MembershipPlanRepository;
import com.academy.coworkhub.presentation.dto.request.MembershipPlanRequest;
import com.academy.coworkhub.presentation.dto.response.MembershipPlanResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MembershipPlanServiceImpl implements MembershipPlanService {

    private final MembershipPlanRepository planRepository;
    private final MemberRepository memberRepository;

    public MembershipPlanServiceImpl(MembershipPlanRepository planRepository, MemberRepository memberRepository) {
        this.planRepository = planRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public List<MembershipPlanResponse> findAll() {
        return planRepository.findAll().stream().map(MembershipPlanResponse::from).toList();
    }

    @Override
    public MembershipPlanResponse findById(Long id) {
        return MembershipPlanResponse.from(get(id));
    }

    @Override
    @Transactional
    public MembershipPlanResponse create(MembershipPlanRequest request) {
        String name = request.nombre().trim();
        if (planRepository.existsByNameIgnoreCase(name)) {
            throw duplicate(name);
        }
        MembershipPlan plan = new MembershipPlan();
        apply(plan, name, request);
        return MembershipPlanResponse.from(planRepository.save(plan));
    }

    @Override
    @Transactional
    public MembershipPlanResponse update(Long id, MembershipPlanRequest request) {
        MembershipPlan plan = get(id);
        String name = request.nombre().trim();
        if (planRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicate(name);
        }
        apply(plan, name, request);
        return MembershipPlanResponse.from(plan);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MembershipPlan plan = get(id);
        if (memberRepository.existsByPlanId(id)) {
            throw new BusinessRuleException("RN-12", "No se puede eliminar el plan: tiene miembros");
        }
        planRepository.delete(plan);
    }

    private MembershipPlan get(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plan", id));
    }

    private void apply(MembershipPlan plan, String name, MembershipPlanRequest request) {
        plan.setName(name);
        plan.setIncludedHoursPerMonth(request.horasIncluidasMes());
        plan.setOverageDiscount(request.descuentoExcedente());
        plan.setMaxActiveBookings(request.maxReservasActivas());
    }

    private BusinessRuleException duplicate(String name) {
        return new BusinessRuleException("RN-11", "Ya existe un plan llamado '" + name + "'");
    }
}
