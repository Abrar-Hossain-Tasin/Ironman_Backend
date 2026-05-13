package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.ironman.dto.user.AddressRequest;
import com.ironman.dto.user.AddressResponse;
import com.ironman.dto.user.ChangePasswordRequest;
import com.ironman.dto.user.UpdateProfileRequest;
import com.ironman.dto.user.UserSummary;
import com.ironman.model.Address;
import com.ironman.model.User;
import com.ironman.repository.AddressRepository;
import com.ironman.repository.CustomerProfileRepository;
import com.ironman.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
  private final PrincipalService principalService;
  private final UserRepository userRepository;
  private final AddressRepository addressRepository;
  private final CustomerProfileRepository customerProfileRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  public UserSummary me() {
    return UserSummary.from(principalService.currentUser());
  }

  @Transactional
  public UserSummary updateMe(UpdateProfileRequest request) {
    User user = principalService.currentUser();
    user.setFullName(request.fullName());
    user.setPhone(request.phone());
    user.setProfilePictureUrl(request.profilePictureUrl());
    return UserSummary.from(userRepository.save(user));
  }

  public List<AddressResponse> myAddresses() {
    return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(principalService.currentUser().getId())
        .stream()
        .map(AddressResponse::from)
        .toList();
  }

  @Transactional
  public AddressResponse addAddress(AddressRequest request) {
    User user = principalService.currentUser();
    var address = new Address();
    apply(address, request);
    address.setUser(user);
    address = addressRepository.save(address);
    if (request.defaultAddress()) {
      setOnlyDefault(user.getId(), address.getId());
      setProfileDefault(user.getId(), address);
    }
    return AddressResponse.from(address);
  }

  @Transactional
  public AddressResponse updateAddress(UUID id, AddressRequest request) {
    User user = principalService.currentUser();
    Address address = addressRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new NotFoundException("Address not found"));
    apply(address, request);
    address = addressRepository.save(address);
    if (request.defaultAddress()) {
      setOnlyDefault(user.getId(), address.getId());
      setProfileDefault(user.getId(), address);
    }
    return AddressResponse.from(address);
  }

  @Transactional
  public void deleteAddress(UUID id) {
    User user = principalService.currentUser();
    Address address = addressRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new NotFoundException("Address not found"));
    addressRepository.delete(address);
  }

  /**
   * Logged-in password change. Requires the current password to be supplied to
   * prevent session-hijack pivots, and emails the user once the change lands so
   * they have an audit trail if it wasn't them.
   */
  @Transactional
  public void changePassword(ChangePasswordRequest request) {
    User user = principalService.currentUser();
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new BadRequestException("Current password is incorrect");
    }
    if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
      throw new BadRequestException("New password must be different from current password");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    emailService.sendPasswordChanged(user.getEmail(), user.getFullName());
  }

  private void apply(Address address, AddressRequest request) {
    address.setLabel(request.label());
    address.setAddressLine1(request.addressLine1());
    address.setAddressLine2(request.addressLine2());
    address.setArea(request.area());
    address.setCity(request.city());
    address.setPostalCode(request.postalCode());
    address.setLatitude(request.latitude());
    address.setLongitude(request.longitude());
    address.setDefaultAddress(request.defaultAddress());
  }

  private void setOnlyDefault(UUID userId, UUID selectedId) {
    addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).forEach(address -> {
      address.setDefaultAddress(address.getId().equals(selectedId));
      addressRepository.save(address);
    });
  }

  private void setProfileDefault(UUID userId, Address address) {
    customerProfileRepository.findByUserId(userId).ifPresent(profile -> {
      profile.setDefaultAddress(address);
      customerProfileRepository.save(profile);
    });
  }
}
