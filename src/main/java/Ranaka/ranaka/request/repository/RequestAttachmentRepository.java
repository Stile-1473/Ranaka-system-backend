package Ranaka.ranaka.request.repository;

import Ranaka.ranaka.request.entity.RequestAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestAttachmentRepository extends JpaRepository<RequestAttachment, Long> {

    List<RequestAttachment> findByRequestIdOrderByUploadedAtDesc(Long requestId);

    List<RequestAttachment> findByUploadedByIdOrderByUploadedAtDesc(Long uploadedById);
}

