package com.academy.coworkhub.business.service.impl;

import com.academy.coworkhub.business.exception.BusinessRuleException;
import com.academy.coworkhub.business.exception.ResourceNotFoundException;
import com.academy.coworkhub.business.service.RoomService;
import com.academy.coworkhub.persistence.entity.Equipment;
import com.academy.coworkhub.persistence.entity.Location;
import com.academy.coworkhub.persistence.entity.Room;
import com.academy.coworkhub.persistence.repository.BookingRepository;
import com.academy.coworkhub.persistence.repository.EquipmentRepository;
import com.academy.coworkhub.persistence.repository.LocationRepository;
import com.academy.coworkhub.persistence.repository.RoomRepository;
import com.academy.coworkhub.presentation.dto.request.RoomRequest;
import com.academy.coworkhub.presentation.dto.response.RoomResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final LocationRepository locationRepository;
    private final EquipmentRepository equipmentRepository;
    private final BookingRepository bookingRepository;

    public RoomServiceImpl(RoomRepository roomRepository, LocationRepository locationRepository,
                           EquipmentRepository equipmentRepository, BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.locationRepository = locationRepository;
        this.equipmentRepository = equipmentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<RoomResponse> findAll(Long locationId) {
        List<Room> rooms = locationId == null ? roomRepository.findAll() : roomRepository.findByLocationId(locationId);
        return rooms.stream().map(RoomResponse::from).toList();
    }

    @Override
    public RoomResponse findById(Long id) {
        return RoomResponse.from(get(id));
    }

    @Override
    @Transactional
    public RoomResponse create(RoomRequest request) {
        String name = request.nombre().trim();
        if (roomRepository.existsByLocationIdAndNameIgnoreCase(request.sedeId(), name)) {
            throw duplicate(name);
        }
        Room room = new Room();
        apply(room, name, request);
        return RoomResponse.from(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = get(id);
        String name = request.nombre().trim();
        if (roomRepository.existsByLocationIdAndNameIgnoreCaseAndIdNot(request.sedeId(), name, id)) {
            throw duplicate(name);
        }
        apply(room, name, request);
        return RoomResponse.from(room);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Room room = get(id);
        if (bookingRepository.existsByRoomId(id)) {
            throw new BusinessRuleException("RN-12", "No se puede eliminar la sala: tiene reservas");
        }
        roomRepository.delete(room);
    }

    private Room get(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sala", id));
    }

    private void apply(Room room, String name, RoomRequest request) {
        Location location = locationRepository.findById(request.sedeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede", request.sedeId()));
        room.setName(name);
        room.setType(request.tipo());
        room.setCapacity(request.capacidad());
        room.setHourlyRate(request.tarifaHora());
        room.setActive(request.activa() == null || request.activa());
        room.setLocation(location);
        room.setEquipment(findEquipment(request.equipamientoIds()));
    }

    private Set<Equipment> findEquipment(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Equipment> found = equipmentRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            Long missing = ids.stream()
                    .filter(id -> found.stream().noneMatch(e -> e.getId().equals(id)))
                    .findFirst().orElseThrow();
            throw new ResourceNotFoundException("Equipamiento", missing);
        }
        return new HashSet<>(found);
    }

    private BusinessRuleException duplicate(String name) {
        return new BusinessRuleException("RN-11", "Ya existe una sala llamada '" + name + "' en esa sede");
    }
}
