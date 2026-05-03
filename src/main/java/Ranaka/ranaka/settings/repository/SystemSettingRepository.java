package Ranaka.ranaka.settings.repository;

import Ranaka.ranaka.settings.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingKeyAndDeletedAtIsNull(String settingKey);

    Optional<SystemSetting> findFirstBySettingKey(String settingKey);

    List<SystemSetting> findByDeletedAtIsNullOrderBySettingKeyAsc();

    List<SystemSetting> findByIsSystemSettingFalseAndDeletedAtIsNull();

    boolean existsBySettingKeyAndDeletedAtIsNull(String settingKey);
}
