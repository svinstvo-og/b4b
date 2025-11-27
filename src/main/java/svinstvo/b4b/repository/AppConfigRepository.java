package svinstvo.b4b.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import svinstvo.b4b.model.AppConfig;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, String> {
}
