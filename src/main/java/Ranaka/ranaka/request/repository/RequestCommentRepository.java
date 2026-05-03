package Ranaka.ranaka.request.repository;

import Ranaka.ranaka.request.entity.RequestComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestCommentRepository extends JpaRepository<RequestComment, Long> {

    List<RequestComment> findByRequestIdOrderByCreatedAtDesc(Long requestId);

    List<RequestComment> findByCommenterIdOrderByCreatedAtDesc(Long commenterId);
}

