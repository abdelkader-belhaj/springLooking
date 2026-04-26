package tn.hypercloud.dto.transport;

import java.time.LocalDateTime;

public record EtatDesLieuxPhotoDto(
        Long id,
        String photoUrl,
        String type,
        LocalDateTime dateUpload
) {}