package seniorproject.bankifycore.web.v1.partner;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import seniorproject.bankifycore.consants.ApiPaths;
import seniorproject.bankifycore.dto.RotationKeyResponse;
import seniorproject.bankifycore.dto.partner.*;
import seniorproject.bankifycore.dto.rotation.RotateKeyRequest;
import seniorproject.bankifycore.dto.rotation.RotateKeyResponse;
import seniorproject.bankifycore.dto.rotation.RotationRequestItem;


import seniorproject.bankifycore.service.partner.PartnerPortalService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.PARTNER +"/portal")
public class PartnerPortalController {

    private final PartnerPortalService partnerPortalService;
    private final PartnerKeyVaultService partnerKeyVaultService;



    // ✅ requires PARTNER_PORTAL JWT
    @GetMapping("/me")
    @PreAuthorize("hasRole('PARTNER')")
    public PartnerPortalMeResponse me() {
        return partnerPortalService.me();
    }

    @PostMapping("/keys/rotate-request")
    @PreAuthorize("hasRole('PARTNER')")
    public RotateKeyResponse requestRotation(@RequestBody RotateKeyRequest req) {
        return partnerPortalService.requestRotation(req);
    }

    @GetMapping("/keys/rotation-requests")
    @PreAuthorize("hasRole('PARTNER')")
    public List<RotationRequestItem> rotationRequests() {
        return partnerPortalService.myRotationRequests();
    }





    @GetMapping("/key/retrieve")
    @PreAuthorize("hasRole('PARTNER')")
    public RotationKeyResponse getRotatedKey(Authentication auth) {

        // ✅ Pull partnerAppId from JWT claims or principal
        UUID partnerAppId = partnerPortalService.extractPartnerAppId(auth);

        String key = partnerKeyVaultService.retrieve(partnerAppId);

        if (key == null) {
            // either not approved, already retrieved, or TTL expired
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No rotated key available (already retrieved or expired)"
            );
        }

        return new RotationKeyResponse(
                partnerAppId,
                key,
                "Show this once and store it safely. You won't be able to fetch it again."
        );
    }



}
