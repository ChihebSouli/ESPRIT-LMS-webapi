package com.esprit.lms.users.security;

import com.esprit.lms.users.entity.LmsRole;

/**
 * Maps auth microservice role labels to LMS roles.
 *
 * Auth MS JWT "roles" claim contains labels like:
 *   ETUDIANT, ENSEIGNANT, ADMIN, GESTION, SUPERADMIN, etc.
 *
 * LMS uses 4 roles: STUDENT, TEACHER, LIBRARIAN, ADMIN
 */
public final class RoleMapper {

    private RoleMapper() {}

    public static LmsRole map(String authMsRole) {
        if (authMsRole == null) return LmsRole.STUDENT;

        return switch (authMsRole.toUpperCase().trim()) {
            case "ADMIN", "SUPERADMIN" -> LmsRole.ADMIN;
            case "GESTION"             -> LmsRole.LIBRARIAN;
            case "ENSEIGNANT"          -> LmsRole.TEACHER;
            case "ETUDIANT"            -> LmsRole.STUDENT;
            default                    -> LmsRole.STUDENT;
        };
    }

    /**
     * Given multiple roles from JWT, return the highest-privilege one.
     * Priority: ADMIN > LIBRARIAN > TEACHER > STUDENT
     */
    public static LmsRole mapHighest(java.util.List<String> roles) {
        if (roles == null || roles.isEmpty()) return LmsRole.STUDENT;

        LmsRole highest = LmsRole.STUDENT;
        for (String role : roles) {
            LmsRole mapped = map(role);
            if (mapped.ordinal() > highest.ordinal()) {
                highest = mapped;
            }
        }
        return highest;
    }
}
