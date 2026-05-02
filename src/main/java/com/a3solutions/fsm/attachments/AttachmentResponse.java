package com.a3solutions.fsm.attachments;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Metadata describing a file attached to a work order.")
public record AttachmentResponse(
        @Schema(description = "Attachment identifier.", example = "15")
        Long id,
        @Schema(description = "Original uploaded filename.", example = "generator-panel-photo.jpg")
        String filename,
        @Schema(description = "Stored file URL or path.", example = "uploads/2026/05/generator-panel-photo.jpg")
        String url,
        @Schema(description = "Resolved MIME type.", example = "image/jpeg")
        String contentType,
        @Schema(description = "File size in bytes.", example = "245981")
        long size,
        @Schema(description = "Timestamp when the file was uploaded.", example = "2026-05-02T14:30:00Z")
        Instant uploadedAt,
        @Schema(description = "Authenticated actor who uploaded the file.", example = "debs@a3fsm.com")
        String uploadedBy
) {
}
