package com.academy.coworkhub.business.service.impl;

import com.academy.coworkhub.business.exception.BusinessRuleException;
import com.academy.coworkhub.business.exception.ResourceNotFoundException;
import com.academy.coworkhub.business.service.EquipmentService;
import com.academy.coworkhub.persistence.entity.Equipment;
import com.academy.coworkhub.persistence.repository.EquipmentRepository;
import com.academy.coworkhub.persistence.repository.RoomRepository;
import com.academy.coworkhub.presentation.dto.request.EquipmentRequest;
import com.academy.coworkhub.presentation.dto.response.EquipmentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final RoomRepository roomRepository;

    public EquipmentServiceImpl(EquipmentRepository equipmentRepository, RoomRepository roomRepository) {
        this.equipmentRepository = equipmentRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public List<EquipmentResponse> findAll() {
        return equipmentRepository.findAll().stream().map(EquipmentResponse::from).toList();
    }

    @Override
    public EquipmentResponse findById(Long id) {
        return EquipmentResponse.from(get(id));
    }

    @Override
    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        String name = request.nombre().trim();
        if (equipmentRepository.existsByNameIgnoreCase(name)) {
            throw duplicate(name);
        }
        Equipment equipment = new Equipment();
        equipment.setName(name);
        return EquipmentResponse.from(equipmentRepository.save(equipment));
    }

    @Override
    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest request) {
        Equipment equipment = get(id);
        String name = request.nombre().trim();
        if (equipmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicate(name);
        }
        equipment.setName(name);
        return EquipmentResponse.from(equipment);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Equipment equipment = get(id);
        if (roomRepository.existsByEquipment_Id(id)) {
            throw new BusinessRuleException("RN-12", "No se puede eliminar el equipamiento: está asignado a salas");
        }
        equipmentRepository.delete(equipment);
    }

    private Equipment get(Long id) {
        return equipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Equipamiento", id));
    }

    private BusinessRuleException duplicate(String name) {
        return new BusinessRuleException("RN-11", "Ya existe un equipamiento llamado '" + name + "'");
    }
}
