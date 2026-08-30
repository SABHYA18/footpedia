package com.footpedia.account;

import com.footpedia.account.dto.ChangePasswordRequest;
import com.footpedia.account.dto.DeleteAccountRequest;
import com.footpedia.account.dto.UpdateProfileRequest;
import com.footpedia.auth.dto.UserResponse;

public interface AccountService {

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void deleteAccount(Long userId, DeleteAccountRequest request);
}
