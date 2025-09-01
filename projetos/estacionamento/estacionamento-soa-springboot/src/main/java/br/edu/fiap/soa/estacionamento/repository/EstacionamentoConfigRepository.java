package br.edu.fiap.soa.estacionamento.repository;

import br.edu.fiap.soa.estacionamento.domain.EstacionamentoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EstacionamentoConfigRepository extends JpaRepository<EstacionamentoConfig, Long> {
    
    @Query("SELECT e FROM EstacionamentoConfig e WHERE e.ativo = true ORDER BY e.id DESC")
    Optional<EstacionamentoConfig> findActiveConfig();
}
