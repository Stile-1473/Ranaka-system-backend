package Ranaka.ranaka.request.serviceImpl;

import Ranaka.ranaka.request.service.AttachmentStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalAttachmentStorageService implements AttachmentStorageService {

    @Value("${attachments.storage.path:${java.io.tmpdir}/ranaka-attachments}")
    private String attachmentsStoragePath;

    @Override
    public String store(MultipartFile file) throws IOException {
        Path storageDirectory = Paths.get(attachmentsStoragePath).toAbsolutePath().normalize();
        Files.createDirectories(storageDirectory);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null
                ? "attachment"
                : file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "-" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path destinationPath = storageDirectory.resolve(storedFileName).normalize();

        file.transferTo(destinationPath);
        return destinationPath.toString();
    }

    @Override
    public Resource loadAsResource(String filePath) {
        try {
            return new UrlResource(Paths.get(filePath).toUri());
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Attachment file path is invalid");
        }
    }
}
