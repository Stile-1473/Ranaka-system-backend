package Ranaka.ranaka.request.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDownloadDto {

    private Long id;
    private Long requestId;
    private String fileName;
    private String filePath;
    private String contentType;
    private Long fileSize;
}
