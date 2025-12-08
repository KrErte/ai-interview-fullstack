package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.dto.UserProfileDto;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserProfile;
import ee.kerrete.ainterview.repository.AppUserRepository;
import ee.kerrete.ainterview.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String email) {
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseGet(() -> createShellProfile(email));
        return toDto(profile);
    }

    @Transactional
    public UserProfileDto saveProfile(String email, UserProfileDto dto) {
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseGet(() -> createShellProfile(email));

        profile.setFullName(dto.getFullName());
        profile.setCurrentRole(dto.getCurrentRole());
        profile.setTargetRole(dto.getTargetRole());
        profile.setYearsOfExperience(dto.getYearsOfExperience());
        profile.setSkills(dto.getSkills());
        profile.setBio(dto.getBio());

        userProfileRepository.save(profile);

        // keep AppUser fullName in sync
        appUserRepository.findByEmail(email).ifPresent(user -> {
            if (StringUtils.hasText(dto.getFullName())) {
                user.setFullName(dto.getFullName());
            }
            appUserRepository.save(user);
        });

        return toDto(profile);
    }

    public int calculateCompleteness(UserProfile profile) {
        int filled = 0;
        int total = 6; // fields counted below
        if (StringUtils.hasText(profile.getFullName())) filled++;
        if (StringUtils.hasText(profile.getCurrentRole())) filled++;
        if (StringUtils.hasText(profile.getTargetRole())) filled++;
        if (profile.getYearsOfExperience() != null) filled++;
        if (StringUtils.hasText(profile.getSkills())) filled++;
        if (StringUtils.hasText(profile.getBio())) filled++;

        return Math.round((filled * 100f) / total);
    }

    private UserProfile createShellProfile(String email) {
        String fullName = appUserRepository.findByEmail(email)
                .map(AppUser::getFullName)
                .orElse(null);
        return userProfileRepository.save(UserProfile.builder()
                .email(email)
                .fullName(fullName)
                .build());
    }

    private UserProfileDto toDto(UserProfile profile) {
        return UserProfileDto.builder()
                .email(profile.getEmail())
                .fullName(profile.getFullName())
                .currentRole(profile.getCurrentRole())
                .targetRole(profile.getTargetRole())
                .yearsOfExperience(profile.getYearsOfExperience())
                .skills(profile.getSkills())
                .bio(profile.getBio())
                .profileCompleteness(calculateCompleteness(profile))
                .build();
    }

    /**
     * Helper for skill overlap computations: returns lowercase trimmed skill array.
     */
    public String[] splitSkills(String skills) {
        if (!StringUtils.hasText(skills)) {
            return new String[0];
        }
        return Arrays.stream(skills.split("[,\\n]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .distinct()
                .toArray(String[]::new);
    }
}













