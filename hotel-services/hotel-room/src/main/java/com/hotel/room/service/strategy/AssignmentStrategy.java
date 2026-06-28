package com.hotel.room.service.strategy;

import com.hotel.room.model.entity.Room;

import java.util.List;

@FunctionalInterface
public interface AssignmentStrategy {
    Room select(List<Room> availableRooms);
}
