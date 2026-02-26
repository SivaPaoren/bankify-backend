package seniorproject.bankifycore.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

//to help differentiate between who is doing what , since we have 3 , ADMIN USER JWT | ATM JWT | Partner porrtal
//GEt the USER and their ID to use in audit log
public class ActorContext {

    private ActorContext() {}

    public static String actorType() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return "SYSTEM";
        if (a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"))) return "ADMIN";
        if (a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ATM"))) return "ATM";
        if (a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_PARTNER"))) return "PARTNER";
        return "USER";
    }

    public static String actorId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getPrincipal() == null) return "SYSTEM";
        Object principal = a.getPrincipal();
        // Extract username cleanly, avoiding toString() which gives BankifyUserDetails@hashcode
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return ud.getUsername();
        }
        return String.valueOf(principal);
    }


}
