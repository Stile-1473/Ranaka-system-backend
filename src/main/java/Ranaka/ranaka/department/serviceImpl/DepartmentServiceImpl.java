package Ranaka.ranaka.department.serviceImpl;

import Ranaka.ranaka.common.exception.ResourceNotFoundException;
import Ranaka.ranaka.department.dto.request.CreateDepartmentRequest;
import Ranaka.ranaka.department.dto.response.DepartmentResponse;
import Ranaka.ranaka.department.dto.request.UpdateDepartmentRequest;
import Ranaka.ranaka.department.entity.Department;
import Ranaka.ranaka.department.repository.DepartmentRepository;
import Ranaka.ranaka.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.existsByNameAndDeletedAtIsNull(request.getName())) {
            throw new IllegalArgumentException("Department with this name already exists");
        }

        // The code is generated from the name so the frontend and reports have a stable identifier.
        Department department = Department.builder()
                .name(request.getName())
                .code(generateUniqueDepartmentCode(request.getName(), null))
                .description(request.getDescription())
                .isActive(true)
                .build();

        Department savedDepartment = departmentRepository.save(department);
        return mapToResponse(savedDepartment);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (request.getName() != null && !request.getName().equals(department.getName())) {
            if (departmentRepository.existsByNameAndDeletedAtIsNull(request.getName())) {
                throw new IllegalArgumentException("Department with this name already exists");
            }
            department.setName(request.getName());
            department.setCode(generateUniqueDepartmentCode(request.getName(), department.getId()));
        }

        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }

        if (request.getIsActive() != null) {
            department.setActive(request.getIsActive());
        }

        Department updatedDepartment = departmentRepository.save(department);
        return mapToResponse(updatedDepartment);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return mapToResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findByDeletedAtIsNullOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getActiveDepartments() {
        return departmentRepository.findByIsActiveTrueAndDeletedAtIsNullOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        department.setActive(false);
        department.setDeletedAt(LocalDateTime.now());
        departmentRepository.save(department);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return departmentRepository.existsByNameAndDeletedAtIsNull(name);
    }

    private DepartmentResponse mapToResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .description(department.getDescription())
                .isActive(department.isActive())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }

    private String generateUniqueDepartmentCode(String name, Long currentDepartmentId) {
        // Example:
        // "Community Outreach" becomes "COMMUNITY_OUTREACH".
        // If that code already exists, the service tries COMMUNITY_OUTREACH_2, then _3, and so on.
        String baseCode = name == null
                ? "DEPARTMENT"
                : name.trim()
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        if (baseCode.isBlank()) {
            baseCode = "DEPARTMENT";
        }

        String candidate = baseCode;
        int suffix = 2;

        while (true) {
            Department existingDepartment = departmentRepository.findByCodeAndDeletedAtIsNull(candidate).orElse(null);
            if (existingDepartment == null || existingDepartment.getId().equals(currentDepartmentId)) {
                return candidate;
            }
            candidate = baseCode + "_" + suffix++;
        }
    }
}
