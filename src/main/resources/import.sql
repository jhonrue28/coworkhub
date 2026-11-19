-- Datos de prueba de CoWorkHub. Hibernate ejecuta este archivo al arrancar con ddl-auto=create.
-- Una sentencia por línea. Contraseñas en BCrypt: admin123, recepcion123, ana123, carlos123.
-- "Hoy" en hora de Colombia (UTC-5, sin horario de verano): el contenedor de MySQL corre en UTC.
SET @hoy = DATE(UTC_TIMESTAMP() - INTERVAL 5 HOUR);

-- Sedes
INSERT INTO locations (id, name, city, address, opening_time, closing_time) VALUES (1, 'Sede Centro', 'Bogotá', 'Carrera 7 # 12-34', '07:00:00', '21:00:00');
INSERT INTO locations (id, name, city, address, opening_time, closing_time) VALUES (2, 'Sede Norte', 'Bogotá', 'Calle 100 # 15-20', '08:00:00', '20:00:00');

-- Equipamiento
INSERT INTO equipment (id, name) VALUES (1, 'Proyector');
INSERT INTO equipment (id, name) VALUES (2, 'Pizarra');
INSERT INTO equipment (id, name) VALUES (3, 'Televisor');
INSERT INTO equipment (id, name) VALUES (4, 'Videoconferencia');

-- Salas (la 6 está inactiva)
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (1, 'Sala Andes', 'MEETING_ROOM', 8, 50000.00, true, 1);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (2, 'Oficina Caribe', 'PRIVATE_OFFICE', 4, 35000.00, true, 1);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (3, 'Escritorio Ventana', 'DESK', 1, 10000.00, true, 1);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (4, 'Sala Pacífico', 'MEETING_ROOM', 12, 60000.00, true, 2);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (5, 'Oficina Amazonas', 'PRIVATE_OFFICE', 6, 40000.00, true, 2);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (6, 'Sala Llanos', 'MEETING_ROOM', 10, 45000.00, false, 2);

-- Equipamiento de cada sala
INSERT INTO room_equipment (room_id, equipment_id) VALUES (1, 1);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (1, 2);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (1, 4);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (2, 3);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (3, 2);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (4, 1);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (4, 2);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (4, 3);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (4, 4);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (5, 2);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (5, 3);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (6, 1);

-- Servicios adicionales
INSERT INTO additional_services (id, name, unit_price) VALUES (1, 'Catering', 25000.00);
INSERT INTO additional_services (id, name, unit_price) VALUES (2, 'Pantalla', 15000.00);
INSERT INTO additional_services (id, name, unit_price) VALUES (3, 'Secretaria', 30000.00);

-- Planes de membresía
INSERT INTO membership_plans (id, name, included_hours_per_month, overage_discount, max_active_bookings) VALUES (1, 'Básico', 10, 10, 2);
INSERT INTO membership_plans (id, name, included_hours_per_month, overage_discount, max_active_bookings) VALUES (2, 'Profesional', 30, 20, 5);
INSERT INTO membership_plans (id, name, included_hours_per_month, overage_discount, max_active_bookings) VALUES (3, 'Empresarial', 80, 30, 10);

-- Miembros (Pedro está suspendido; Laura, Pedro y Sofía no tienen cuenta)
INSERT INTO members (id, document_number, full_name, email, status, plan_id) VALUES (1, '1010101010', 'Ana Gómez', 'ana.gomez@correo.com', 'ACTIVE', 1);
INSERT INTO members (id, document_number, full_name, email, status, plan_id) VALUES (2, '2020202020', 'Carlos Ruiz', 'carlos.ruiz@correo.com', 'ACTIVE', 2);
INSERT INTO members (id, document_number, full_name, email, status, plan_id) VALUES (3, '3030303030', 'Laura Martínez', 'laura.martinez@correo.com', 'ACTIVE', 3);
INSERT INTO members (id, document_number, full_name, email, status, plan_id) VALUES (4, '4040404040', 'Pedro Sánchez', 'pedro.sanchez@correo.com', 'SUSPENDED', 1);
INSERT INTO members (id, document_number, full_name, email, status, plan_id) VALUES (5, '5050505050', 'Sofía Torres', 'sofia.torres@correo.com', 'ACTIVE', 2);

-- Cuentas de usuario
INSERT INTO user_accounts (id, username, password, role, member_id) VALUES (1, 'admin', '$2a$10$Sku2tVBPQCIbuB1bWqmK2O07KLGsUyu7/vuh3t6XSg1v7Krw7gBSO', 'ADMIN', NULL);
INSERT INTO user_accounts (id, username, password, role, member_id) VALUES (2, 'recepcion', '$2a$10$lvQylyiQxGxhxJXZH7HVJOFeG58rbp.PGfSKj5S02J6cG1I9sxgM6', 'RECEPTIONIST', NULL);
INSERT INTO user_accounts (id, username, password, role, member_id) VALUES (3, 'ana', '$2a$10$PE/0dZZbejBka1PBNEcDcOZ0G2r/G/BQ7CNPWAzho.oKo.Qp4yxPu', 'MEMBER', 1);
INSERT INTO user_accounts (id, username, password, role, member_id) VALUES (4, 'carlos', '$2a$10$hdDso0Fs1walkjcWhinvv.FcZFpZel04FsNn36uOIXg6FF3vXMm8u', 'MEMBER', 2);

-- Reservas pasadas. Todas caben en las horas del plan, por eso room_cost = 0
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (1, 1, 1, TIMESTAMP(@hoy - INTERVAL 10 DAY, '09:00:00'), TIMESTAMP(@hoy - INTERVAL 10 DAY, '11:00:00'), 4, 'COMPLETED', 0.00, 50000.00, 50000.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (2, 2, 2, TIMESTAMP(@hoy - INTERVAL 9 DAY, '10:00:00'), TIMESTAMP(@hoy - INTERVAL 9 DAY, '12:00:00'), 3, 'COMPLETED', 0.00, 0.00, 0.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (3, 4, 3, TIMESTAMP(@hoy - INTERVAL 8 DAY, '14:00:00'), TIMESTAMP(@hoy - INTERVAL 8 DAY, '17:00:00'), 10, 'COMPLETED', 0.00, 50000.00, 50000.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (4, 3, 5, TIMESTAMP(@hoy - INTERVAL 7 DAY, '08:00:00'), TIMESTAMP(@hoy - INTERVAL 7 DAY, '12:00:00'), 1, 'COMPLETED', 0.00, 0.00, 0.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (5, 5, 2, TIMESTAMP(@hoy - INTERVAL 5 DAY, '09:00:00'), TIMESTAMP(@hoy - INTERVAL 5 DAY, '10:00:00'), 2, 'CANCELLED', 0.00, 0.00, 0.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (6, 1, 3, TIMESTAMP(@hoy - INTERVAL 3 DAY, '15:00:00'), TIMESTAMP(@hoy - INTERVAL 3 DAY, '16:30:00'), 6, 'COMPLETED', 0.00, 15000.00, 15000.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (7, 4, 4, TIMESTAMP(@hoy - INTERVAL 20 DAY, '10:00:00'), TIMESTAMP(@hoy - INTERVAL 20 DAY, '12:00:00'), 5, 'COMPLETED', 0.00, 0.00, 0.00, 0.00);

-- Reservas futuras. La 9 empieza justo cuando termina la 8 (no se cruzan). Ana (plan Básico, máx. 2) ya tiene 2 activas
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (8, 1, 1, TIMESTAMP(@hoy + INTERVAL 2 DAY, '10:00:00'), TIMESTAMP(@hoy + INTERVAL 2 DAY, '12:00:00'), 4, 'CONFIRMED', 0.00, 50000.00, 50000.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (9, 1, 2, TIMESTAMP(@hoy + INTERVAL 2 DAY, '12:00:00'), TIMESTAMP(@hoy + INTERVAL 2 DAY, '13:00:00'), 2, 'PENDING', 0.00, 0.00, 0.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (10, 2, 1, TIMESTAMP(@hoy + INTERVAL 4 DAY, '09:00:00'), TIMESTAMP(@hoy + INTERVAL 4 DAY, '10:00:00'), 2, 'PENDING', 0.00, 15000.00, 15000.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (11, 4, 3, TIMESTAMP(@hoy + INTERVAL 3 DAY, '14:00:00'), TIMESTAMP(@hoy + INTERVAL 3 DAY, '18:00:00'), 12, 'CONFIRMED', 0.00, 105000.00, 105000.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (12, 5, 5, TIMESTAMP(@hoy + INTERVAL 5 DAY, '09:00:00'), TIMESTAMP(@hoy + INTERVAL 5 DAY, '11:00:00'), 4, 'PENDING', 0.00, 0.00, 0.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (13, 3, 2, TIMESTAMP(@hoy + INTERVAL 6 DAY, '08:00:00'), TIMESTAMP(@hoy + INTERVAL 6 DAY, '16:00:00'), 1, 'CONFIRMED', 0.00, 0.00, 0.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (14, 4, 5, TIMESTAMP(@hoy + INTERVAL 7 DAY, '10:00:00'), TIMESTAMP(@hoy + INTERVAL 7 DAY, '11:30:00'), 8, 'CANCELLED', 0.00, 0.00, 0.00, 0.00);
INSERT INTO bookings (id, room_id, member_id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee) VALUES (15, 2, 3, TIMESTAMP(@hoy + INTERVAL 1 DAY, '16:00:00'), TIMESTAMP(@hoy + INTERVAL 1 DAY, '17:00:00'), 3, 'CONFIRMED', 0.00, 30000.00, 30000.00, 0.00);

-- Detalles: services_cost de cada reserva = suma de quantity × applied_unit_price
INSERT INTO booking_items (id, booking_id, service_id, quantity, applied_unit_price) VALUES (1, 1, 1, 2, 25000.00);
INSERT INTO booking_items (id, booking_id, service_id, quantity, applied_unit_price) VALUES (2, 3, 1, 2, 25000.00);
INSERT INTO booking_items (id, booking_id, service_id, quantity, applied_unit_price) VALUES (3, 6, 2, 1, 15000.00);
INSERT INTO booking_items (id, booking_id, service_id, quantity, applied_unit_price) VALUES (4, 8, 1, 2, 25000.00);
INSERT INTO booking_items (id, booking_id, service_id, quantity, applied_unit_price) VALUES (5, 10, 2, 1, 15000.00);
INSERT INTO booking_items (id, booking_id, service_id, quantity, applied_unit_price) VALUES (6, 11, 1, 3, 25000.00);
INSERT INTO booking_items (id, booking_id, service_id, quantity, applied_unit_price) VALUES (7, 11, 3, 1, 30000.00);
INSERT INTO booking_items (id, booking_id, service_id, quantity, applied_unit_price) VALUES (8, 15, 3, 1, 30000.00);
