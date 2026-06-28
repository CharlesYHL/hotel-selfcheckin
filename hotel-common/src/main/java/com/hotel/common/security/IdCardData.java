package com.hotel.common.security;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
public class IdCardData implements Serializable {
    private String idCardHash;
    private String idCardEncrypted;
    private Integer keyVersion;
    private String salt;
    private String idCardMasked;
    private LocalDate birthDate;
    private Integer gender;
}
