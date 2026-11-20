package com.academy.coworkhub.business.service.impl;

import com.academy.coworkhub.business.exception.BusinessRuleException;
import com.academy.coworkhub.business.exception.ResourceNotFoundException;
import com.academy.coworkhub.business.service.MemberService;
import com.academy.coworkhub.persistence.entity.*;
import com.academy.coworkhub.persistence.repository.BookingRepository;
import com.academy.coworkhub.persistence.repository.MemberRepository;
import com.academy.coworkhub.persistence.repository.MembershipPlanRepository;
import com.academy.coworkhub.persistence.repository.UserAccountRepository;
import com.academy.coworkhub.presentation.dto.request.MemberCreateRequest;
import com.academy.coworkhub.presentation.dto.request.MemberUpdateRequest;
import com.academy.coworkhub.presentation.dto.response.MemberResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final UserAccountRepository userAccountRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberServiceImpl(MemberRepository memberRepository, MembershipPlanRepository planRepository,
                             UserAccountRepository userAccountRepository, BookingRepository bookingRepository,
                             PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.planRepository = planRepository;
        this.userAccountRepository = userAccountRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<MemberResponse> search(String name, MemberStatus status) {
        String filter = name == null || name.isBlank() ? null : name.trim();
        return memberRepository.search(filter, status).stream().map(MemberResponse::from).toList();
    }

    @Override
    public MemberResponse findById(Long id) {
        return MemberResponse.from(get(id));
    }

    // Miembro y cuenta en la misma transacción: si falla uno, no se guarda ninguno
    @Override
    @Transactional
    public MemberResponse create(MemberCreateRequest request) {
        String document = request.documento().trim();
        String email = request.email().trim();
        String username = request.usuario().trim();
        if (memberRepository.existsByDocumentNumber(document)) {
            throw new BusinessRuleException("RN-11", "Ya existe un miembro con el documento " + document);
        }
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw duplicateEmail(email);
        }
        if (userAccountRepository.existsByUsername(username)) {
            throw new BusinessRuleException("RN-11", "El usuario '" + username + "' ya existe");
        }

        Member member = new Member();
        member.setDocumentNumber(document);
        member.setFullName(request.nombreCompleto().trim());
        member.setEmail(email);
        member.setStatus(MemberStatus.ACTIVE);
        member.setPlan(getPlan(request.planId()));
        memberRepository.save(member);

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setRole(Role.MEMBER);
        account.setMember(member);
        userAccountRepository.save(account);

        return MemberResponse.from(member);
    }

    @Override
    @Transactional
    public MemberResponse update(Long id, MemberUpdateRequest request) {
        Member member = get(id);
        String email = request.email().trim();
        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw duplicateEmail(email);
        }
        member.setFullName(request.nombreCompleto().trim());
        member.setEmail(email);
        member.setPlan(getPlan(request.planId()));
        return MemberResponse.from(member);
    }

    @Override
    @Transactional
    public MemberResponse changeStatus(Long id, MemberStatus status) {
        Member member = get(id);
        member.setStatus(status);
        return MemberResponse.from(member);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Member member = get(id);
        if (bookingRepository.existsByMemberId(id)) {
            throw new BusinessRuleException("RN-12", "No se puede eliminar el miembro: tiene reservas");
        }
        // La cuenta apunta al miembro: se borra primero
        userAccountRepository.findByMemberId(id).ifPresent(userAccountRepository::delete);
        memberRepository.delete(member);
    }

    private Member get(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Miembro", id));
    }

    private MembershipPlan getPlan(Long planId) {
        return planRepository.findById(planId).orElseThrow(() -> new ResourceNotFoundException("Plan", planId));
    }

    private BusinessRuleException duplicateEmail(String email) {
        return new BusinessRuleException("RN-11", "Ya existe un miembro con el email " + email);
    }
}
