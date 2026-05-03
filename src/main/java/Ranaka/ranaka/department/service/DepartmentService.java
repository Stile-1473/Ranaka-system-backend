package Ranaka.ranaka.department.service;

import Ranaka.ranaka.department.dto.request.CreateDepartmentRequest;
import Ranaka.ranaka.department.dto.response.DepartmentResponse;
import Ranaka.ranaka.department.dto.request.UpdateDepartmentRequest;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(CreateDepartmentRequest request);

    DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request);

    DepartmentResponse getDepartmentById(Long id);

    List<DepartmentResponse> getAllDepartments();

    List<DepartmentResponse> getActiveDepartments();

    void deleteDepartment(Long id);

    boolean existsByName(String name);
}

