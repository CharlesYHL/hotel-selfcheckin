package com.hotel.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdCardHolder {

    private final IdCardEncryptService encryptService;

    public IdCardData save(String rawIdCard) {
        return encryptService.encrypt(rawIdCard);
    }

    public boolean verify(String raw, String hash, String salt) {
        return encryptService.verify(raw, hash, salt);
    }

    public String masked(String raw) {
        return encryptService.mask(raw);
    }

    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public String decrypt(String encrypted, Integer keyVersion) {
        return encryptService.decrypt(encrypted, keyVersion);
    }
}
