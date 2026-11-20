package com.academy.coworkhub.business.service.impl;

import com.academy.coworkhub.business.exception.BusinessRuleException;
import com.academy.coworkhub.business.exception.ResourceNotFoundException;
import com.academy.coworkhub.business.service.AdditionalServiceService;
import com.academy.coworkhub.persistence.entity.AdditionalService;
import com.academy.coworkhub.persistence.repository.AdditionalServiceRepository;
import com.academy.coworkhub.persistence.repository.BookingItemRepository;
import com.academy.coworkhub.presentation.dto.request.AdditionalServiceRequest;
import com.academy.coworkhub.presentation.dto.response.AdditionalServiceResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdditionalServiceServiceImpl implements AdditionalServiceService {

    private final AdditionalServiceRepository serviceRepository;
    private final BookingItemRepository bookingItemRepository;

    public AdditionalServiceServiceImpl(AdditionalServiceRepository serviceRepository,
                                        BookingItemRepository bookingItemRepository) {
        this.serviceRepository = serviceRepository;
        this.bookingItemRepository = bookingItemRepository;
    }

    @Override
    public List<AdditionalServiceResponse> findAll() {
        return serviceRepository.findAll().stream().map(AdditionalServiceResponse::from).toList();
    }

    @Override
    public AdditionalServiceResponse findById(Long id) {
        return AdditionalServiceResponse.from(get(id));
    }

    @Override
    @Transactional
    public AdditionalServiceResponse create(AdditionalServiceRequest request) {
        String name = request.nombre().trim();
        if (serviceRepository.existsByNameIgnoreCase(name)) {
            throw duplicate(name);
        }
        AdditionalService service = new AdditionalService();
        service.setName(name);
        service.setUnitPrice(request.precioUnitario());
        return AdditionalServiceResponse.from(serviceRepository.save(service));
    }

    @Override
    @Transactional
    public AdditionalServiceResponse update(Long id, AdditionalServiceRequest request) {
        AdditionalService service = get(id);
        String name = request.nombre().trim();
        if (serviceRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicate(name);
        }
        service.setName(name);
        // Las reservas existentes conservan su appliedUnitPrice (RN-07)
        service.setUnitPrice(request.precioUnitario());
        return AdditionalServiceResponse.from(service);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AdditionalService service = get(id);
        if (bookingItemRepository.existsByServiceId(id)) {
            throw new BusinessRuleException("RN-12", "No se puede eliminar el servicio: está usado en reservas");
        }
        serviceRepository.delete(service);
    }

    private AdditionalService get(Long id) {
        return serviceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Servicio adicional", id));
    }

    private BusinessRuleException duplicate(String name) {
        return new BusinessRuleException("RN-11", "Ya existe un servicio adicional llamado '" + name + "'");
    }
}
