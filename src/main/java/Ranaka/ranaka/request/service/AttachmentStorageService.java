package Ranaka.ranaka.request.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AttachmentStorageService {

    String store(MultipartFile file) throws IOException;

    Resource loadAsResource(String filePath);
}
