package com.hotel.room.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomStatusChangeEvent implements Serializable {
    private String hotelId;
    private String roomTypeId;
    private String roomId;
    private String roomNo;
    private String orderId;
    private String changeType;
    private long timestamp;
}
