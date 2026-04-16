package com.esprit.lms.users.dto;

import com.esprit.lms.users.entity.LmsRole;
import lombok.*;

import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserProfileDTO {
    private UUID id;
    private String email;
    private String fullName;
    private LmsRole role;
    private String matricule;
    private int effectiveBorrowLimit;
}
