package com.hotel.room.service.strategy;

import com.hotel.room.model.entity.Room;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component("highFloorStrategy")
public class HighFloorStrategy implements AssignmentStrategy {

    @Override
    public Room select(List<Room> availableRooms) {
        return availableRooms.stream()
                .sorted(Comparator.comparing(Room::getFloorNo).reversed()
                        .thenComparing(Room::getSortOrder)
                        .thenComparing(Room::getRoomNo))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("无可用房间"));
    }
}
