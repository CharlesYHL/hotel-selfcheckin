package com.hotel.room.service;

import com.hotel.common.cache.CacheService;
import com.hotel.common.core.BusinessException;
import com.hotel.room.model.dto.RoomDTO;
import com.hotel.room.model.dto.RoomTypeDTO;
import com.hotel.room.model.entity.Room;
import com.hotel.room.model.entity.RoomType;
import com.hotel.room.model.enums.RoomStatus;
import com.hotel.room.repository.RoomMapper;
import com.hotel.room.repository.RoomTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomQueryService {

    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomInventoryService inventoryService;
    private final CacheService cacheService;

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration CACHE_JITTER = Duration.ofMinutes(2);

    public List<RoomTypeDTO> listRoomTypes(String hotelId) {
        String cacheKey = "room:types:" + hotelId;
        List<RoomTypeDTO> cached = cacheService.get(cacheKey);
        if (cached != null) return cached;

        List<RoomType> types = roomTypeMapper.selectByHotelId(hotelId);
        List<RoomTypeDTO> dtos = types.stream().map(t -> {
            int available = inventoryService.getInventory(t.getHotelId(), t.getRoomTypeId());
            return RoomTypeDTO.builder()
                    .roomTypeId(t.getRoomTypeId())
                    .hotelId(t.getHotelId())
                    .roomTypeCode(t.getRoomTypeCode())
                    .roomTypeName(t.getRoomTypeName())
                    .baseCapacity(t.getBaseCapacity())
                    .maxCapacity(t.getMaxCapacity())
                    .bedType(t.getBedType())
                    .availableCount(available)
                    .maxRooms(t.getMaxRooms())
                    .build();
        }).toList();

        cacheService.setWithJitter(cacheKey, dtos, CACHE_TTL, CACHE_JITTER);
        return dtos;
    }

    public RoomTypeDTO getRoomType(String hotelId, String roomTypeId) {
        RoomType t = roomTypeMapper.selectById(roomTypeId);
        if (t == null || !t.getHotelId().equals(hotelId)) {
            throw new BusinessException("房型不存在");
        }
        int available = inventoryService.getInventory(t.getHotelId(), t.getRoomTypeId());
        return RoomTypeDTO.builder()
                .roomTypeId(t.getRoomTypeId())
                .hotelId(t.getHotelId())
                .roomTypeCode(t.getRoomTypeCode())
                .roomTypeName(t.getRoomTypeName())
                .baseCapacity(t.getBaseCapacity())
                .maxCapacity(t.getMaxCapacity())
                .bedType(t.getBedType())
                .availableCount(available)
                .maxRooms(t.getMaxRooms())
                .build();
    }

    public List<RoomDTO> listRooms(String hotelId, String roomTypeId) {
        List<Room> rooms = roomMapper.selectAvailable(hotelId, roomTypeId);
        return rooms.stream().map(this::toDTO).toList();
    }

    public List<RoomDTO> listRoomsByStatus(String hotelId, int status) {
        List<Room> rooms = roomMapper.selectByStatus(hotelId, status);
        return rooms.stream().map(this::toDTO).toList();
    }

    public RoomDTO getRoom(String roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) throw new BusinessException("房间不存在");
        return toDTO(room);
    }

    private RoomDTO toDTO(Room r) {
        return RoomDTO.builder()
                .roomId(r.getRoomId())
                .roomNo(r.getRoomNo())
                .hotelId(r.getHotelId())
                .roomTypeId(r.getRoomTypeId())
                .floorNo(r.getFloorNo())
                .roomStatus(r.getRoomStatus())
                .roomStatusDesc(RoomStatus.of(r.getRoomStatus()).getDesc())
                .direction(r.getDirection())
                .maxGuest(r.getMaxGuest())
                .isSmokeFree(r.getIsSmokeFree())
                .createdTime(r.getCreatedTime())
                .build();
    }
}
