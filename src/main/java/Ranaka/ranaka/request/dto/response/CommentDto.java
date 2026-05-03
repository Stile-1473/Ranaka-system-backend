package Ranaka.ranaka.request.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long id;
    private String comment;
    private boolean isInternal;
    private LocalDateTime createdAt;
    private String commenterName;
    private String commenterRole;
}

