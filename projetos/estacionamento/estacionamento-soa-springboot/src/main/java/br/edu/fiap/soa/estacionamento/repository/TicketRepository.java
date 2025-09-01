package br.edu.fiap.soa.estacionamento.repository;
import br.edu.fiap.soa.estacionamento.domain.Ticket;
import br.edu.fiap.soa.estacionamento.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByStatus(TicketStatus status);
    boolean existsByVeiculoIdAndStatus(Long veiculoId, TicketStatus status);
    
    // Novos métodos para relatórios e controle de lotação
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = 'ABERTO'")
    Long countTicketsAbertos();
    
    @Query("SELECT t FROM Ticket t WHERE t.entrada BETWEEN :inicio AND :fim")
    List<Ticket> findByPeriodo(@Param("inicio") LocalDateTime inicio, 
                              @Param("fim") LocalDateTime fim);
    
    @Query("SELECT t.vaga, COUNT(t) FROM Ticket t WHERE t.entrada BETWEEN :inicio AND :fim GROUP BY t.vaga")
    List<Object[]> getRelatorioUsoVagas(@Param("inicio") LocalDateTime inicio, 
                                       @Param("fim") LocalDateTime fim);
    
    List<Ticket> findByVaga(String vaga);
}
