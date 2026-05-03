package Ranaka.ranaka.authentication.service;


import Ranaka.ranaka.authentication.dto.request.LoginRequest;
import Ranaka.ranaka.authentication.dto.request.RegisterRequest;
import Ranaka.ranaka.authentication.dto.request.UpdateCurrentUserRequest;
import Ranaka.ranaka.authentication.dto.response.AuthResponse;
import Ranaka.ranaka.user.dto.request.ChangePasswordRequest;
import Ranaka.ranaka.user.entity.User;

public interface AuthService {

    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest loginRequest);
    void changePassword(ChangePasswordRequest request);
    User getCurrentUser();
    User updateCurrentUser(UpdateCurrentUserRequest request);

}
