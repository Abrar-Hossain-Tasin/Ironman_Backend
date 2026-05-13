package com.ironman.controller;

import com.ironman.dto.common.ApiMessage;
import com.ironman.dto.user.AddressRequest;
import com.ironman.dto.user.AddressResponse;
import com.ironman.dto.user.ChangePasswordRequest;
import com.ironman.dto.user.UpdateProfileRequest;
import com.ironman.dto.user.UserSummary;
import com.ironman.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  public UserSummary me() {
    return userService.me();
  }

  @PutMapping("/me")
  public UserSummary updateMe(@Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateMe(request);
  }

  @GetMapping("/me/addresses")
  public List<AddressResponse> addresses() {
    return userService.myAddresses();
  }

  @PostMapping("/me/addresses")
  public AddressResponse addAddress(@Valid @RequestBody AddressRequest request) {
    return userService.addAddress(request);
  }

  @PutMapping("/me/addresses/{id}")
  public AddressResponse updateAddress(@PathVariable UUID id, @Valid @RequestBody AddressRequest request) {
    return userService.updateAddress(id, request);
  }

  @DeleteMapping("/me/addresses/{id}")
  public ApiMessage deleteAddress(@PathVariable UUID id) {
    userService.deleteAddress(id);
    return new ApiMessage("Address deleted");
  }

  @PutMapping("/me/password")
  public ApiMessage changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(request);
    return new ApiMessage("Password updated");
  }
}
