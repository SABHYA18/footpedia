package com.footpedia.auth;

import com.footpedia.auth.dto.PasswordResetConfirmRequest;
import com.footpedia.auth.dto.PasswordResetRequest;

public interface PasswordResetService {

    /** Always succeeds from the caller's point of view, even for an unknown email -- avoids account enumeration. */
    void requestReset(PasswordResetRequest request);

    void confirmReset(PasswordResetConfirmRequest request);
}
