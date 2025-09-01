package br.edu.fiap.soa.estacionamento.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Entidade para configurações do estacionamento
 */
@Entity
@Table(name = "estacionamento_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EstacionamentoConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "total_vagas", nullable = false)
    private Integer totalVagas;
    
    @Column(name = "tarifa_30min", precision = 10, scale = 2)
    private BigDecimal tarifa30Min;
    
    @Column(name = "tarifa_hora", precision = 10, scale = 2)
    private BigDecimal tarifaHora;
    
    @Column(name = "tarifa_diaria", precision = 10, scale = 2)
    private BigDecimal tarifaDiaria;
    
    @Column(name = "tarifa_mensal", precision = 10, scale = 2)
    private BigDecimal tarifaMensal;
      @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
