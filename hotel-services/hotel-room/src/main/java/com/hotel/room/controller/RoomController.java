package com.hotel.room.controller;

import com.hotel.common.core.Result;
import com.hotel.common.idempotent.Idempotent;
import com.hotel.room.model.dto.*;
import com.hotel.room.service.*;
import com.hotel.room.service.strategy.HighFloorStrategy;
import com.hotel.room.service.strategy.SequentialStrategy;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomQueryService queryService;
    private final RoomAssignmentService assignmentService;
    private final RoomInventoryService inventoryService;
    private final InventorySyncService syncService;

    @Qualifier("sequentialStrategy")
    private final SequentialStrategy sequentialStrategy;

    @Qualifier("highFloorStrategy")
    private final HighFloorStrategy highFloorStrategy;

    // ======================== 查询 ========================

    @GetMapping("/types/{hotelId}")
    public Result<List<RoomTypeDTO>> listRoomTypes(@PathVariable String hotelId) {
        return Result.success(queryService.listRoomTypes(hotelId));
    }

    @GetMapping("/types/{hotelId}/{roomTypeId}")
    public Result<RoomTypeDTO> getRoomType(@PathVariable String hotelId, @PathVariable String roomTypeId) {
        return Result.success(queryService.getRoomType(hotelId, roomTypeId));
    }

    @GetMapping("/list/{hotelId}/{roomTypeId}")
    public Result<List<RoomDTO>> listRooms(@PathVariable String hotelId, @PathVariable String roomTypeId) {
        return Result.success(queryService.listRooms(hotelId, roomTypeId));
    }

    @GetMapping("/status/{hotelId}/{status}")
    public Result<List<RoomDTO>> listByStatus(@PathVariable String hotelId, @PathVariable int status) {
        return Result.success(queryService.listRoomsByStatus(hotelId, status));
    }

    @GetMapping("/{roomId}")
    public Result<RoomDTO> getRoom(@PathVariable String roomId) {
        return Result.success(queryService.getRoom(roomId));
    }

    // ======================== 排房 ========================

    @PostMapping("/assign")
    @Idempotent(keyPrefix = "room:assign", fields = {"orderId"}, expireSeconds = 3600,
            deleteOnSuccess = true, concurrentStrategy = Idempotent.ConcurrentStrategy.THROW)
    public Result<RoomAssignResponse> assign(@RequestBody @Valid RoomAssignRequest request) {
        var strategy = "HIGH_FLOOR".equalsIgnoreCase(request.getStrategy())
                ? highFloorStrategy : sequentialStrategy;

        var result = assignmentService.assignWithStrategy(
                request.getOrderId(), request.getHotelId(), request.getRoomTypeId(), strategy);

        RoomAssignResponse resp = RoomAssignResponse.builder()
                .success(result.success())
                .message(result.message())
                .build();
        if (result.success() && result.room() != null) {
            var room = result.room();
            resp.setRoomId(room.getRoomId());
            resp.setRoomNo(room.getRoomNo());
            resp.setRoomTypeId(room.getRoomTypeId());
            resp.setFloorNo(room.getFloorNo());
            resp.setHotelId(room.getHotelId());
        }
        return Result.success(resp);
    }

    // ======================== 释放房间 ========================

    @PostMapping("/release")
    @Idempotent(keyPrefix = "room:release", fields = {"orderId"}, expireSeconds = 3600)
    public Result<Void> release(@RequestBody @Valid RoomReleaseRequest request) {
        assignmentService.release(request.getRoomId(), request.getOrderId());
        return Result.success();
    }

    // ======================== 库存 ========================

    @GetMapping("/inventory/{hotelId}/{roomTypeId}")
    public Result<Integer> getInventory(@PathVariable String hotelId, @PathVariable String roomTypeId) {
        return Result.success(inventoryService.getInventory(hotelId, roomTypeId));
    }

    @PostMapping("/inventory/sync/{hotelId}")
    public Result<Void> syncInventory(@PathVariable String hotelId) {
        syncService.reload(hotelId);
        return Result.success();
    }
}
