package com.academy.coworkhub.business.service.impl;

import com.academy.coworkhub.business.exception.BusinessRuleException;
import com.academy.coworkhub.business.exception.ResourceNotFoundException;
import com.academy.coworkhub.business.service.LocationService;
import com.academy.coworkhub.persistence.entity.Location;
import com.academy.coworkhub.persistence.repository.LocationRepository;
import com.academy.coworkhub.persistence.repository.RoomRepository;
import com.academy.coworkhub.presentation.dto.request.LocationRequest;
import com.academy.coworkhub.presentation.dto.response.LocationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final RoomRepository roomRepository;

    public LocationServiceImpl(LocationRepository locationRepository, RoomRepository roomRepository) {
        this.locationRepository = locationRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public List<LocationResponse> findAll() {
        return locationRepository.findAll().stream().map(LocationResponse::from).toList();
    }

    @Override
    public LocationResponse findById(Long id) {
        return LocationResponse.from(get(id));
    }

    @Override
    @Transactional
    public LocationResponse create(LocationRequest request) {
        Location location = new Location();
        apply(location, request);
        return LocationResponse.from(locationRepository.save(location));
    }

    @Override
    @Transactional
    public LocationResponse update(Long id, LocationRequest request) {
        Location location = get(id);
        apply(location, request);
        return LocationResponse.from(location);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Location location = get(id);
        if (roomRepository.existsByLocationId(id)) {
            throw new BusinessRuleException("RN-12", "No se puede eliminar la sede: tiene salas");
        }
        locationRepository.delete(location);
    }

    private Location get(Long id) {
        return locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sede", id));
    }

    private void apply(Location location, LocationRequest request) {
        location.setName(request.nombre().trim());
        location.setCity(request.ciudad().trim());
        location.setAddress(request.direccion().trim());
        location.setOpeningTime(request.horaApertura());
        location.setClosingTime(request.horaCierre());
    }
}
