package Ranaka.ranaka.department.repository;

import Ranaka.ranaka.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByIdAndDeletedAtIsNull(Long id);

    Optional<Department> findByNameAndDeletedAtIsNull(String name);

    Optional<Department> findByCodeAndDeletedAtIsNull(String code);

    List<Department> findByDeletedAtIsNullOrderByNameAsc();

    List<Department> findByIsActiveTrueAndDeletedAtIsNullOrderByNameAsc();

    boolean existsByNameAndDeletedAtIsNull(String name);

    boolean existsByCodeAndDeletedAtIsNull(String code);
}
