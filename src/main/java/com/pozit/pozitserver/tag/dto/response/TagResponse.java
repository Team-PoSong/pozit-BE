package com.pozit.pozitserver.tag.dto.response;

import com.pozit.pozitserver.tag.domain.Tag;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "태그 응답")
public record TagResponse (
        @Schema(description = "태그 ID", example = "1")
        Long id,

        @Schema(description = "태그명", example = "맛집")
        String name
){
    public static TagResponse from(Tag tag){
        return new TagResponse(
                tag.getId(),
                tag.getName()
        );
    }
}
