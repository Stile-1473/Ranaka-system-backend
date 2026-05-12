package Ranaka.ranaka.request.repository;

import Ranaka.ranaka.request.entity.RequestAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestAttachmentRepository extends JpaRepository<RequestAttachment, Long> {

    List<RequestAttachment> findByRequestIdOrderByUploadedAtDesc(Long requestId);

    List<RequestAttachment> findByUploadedByIdOrderByUploadedAtDesc(Long uploadedById);

    Optional<RequestAttachment> findByIdAndRequestId(Long id, Long requestId);
}
