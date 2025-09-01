package br.edu.fiap.soa.estacionamento.web;

import br.edu.fiap.soa.estacionamento.domain.Reserva;
import br.edu.fiap.soa.estacionamento.service.ReservaService;
import br.edu.fiap.soa.estacionamento.web.dto.ReservaRequest;
import br.edu.fiap.soa.estacionamento.web.dto.ReservaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "Gerenciamento de reservas de vagas")
public class ReservaController {
    
    private final ReservaService reservaService;
    
    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }
    
    @PostMapping
    @Operation(summary = "Criar reserva de vaga")
    public ResponseEntity<ReservaResponse> criarReserva(@RequestBody @Valid ReservaRequest request) {
        Reserva reserva = reservaService.criarReserva(
            request.getPlaca(),
            request.getVaga(),
            request.getDataInicio(),
            request.getDataFim()
        );
        
        ReservaResponse response = mapToResponse(reserva);
        
        return ResponseEntity.created(URI.create("/api/reservas/" + reserva.getId()))
                .body(response);
    }
    
    @GetMapping("/ativas")
    @Operation(summary = "Listar reservas ativas")
    public List<ReservaResponse> listarReservasAtivas() {
        return reservaService.listarReservasAtivas()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/veiculo/{veiculoId}")
    @Operation(summary = "Listar reservas por veículo")
    public List<ReservaResponse> listarReservasPorVeiculo(@PathVariable Long veiculoId) {
        return reservaService.listarReservasPorVeiculo(veiculoId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar reserva")
    public ResponseEntity<Void> cancelarReserva(@PathVariable Long id) {
        reservaService.cancelarReserva(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/utilizar")
    @Operation(summary = "Marcar reserva como utilizada")
    public ResponseEntity<Void> utilizarReserva(@PathVariable Long id) {
        reservaService.utilizarReserva(id);
        return ResponseEntity.ok().build();
    }
    
    private ReservaResponse mapToResponse(Reserva reserva) {
        return ReservaResponse.builder()
                .id(reserva.getId())
                .placa(reserva.getVeiculo().getPlaca())
                .vaga(reserva.getVaga())
                .dataReserva(reserva.getDataReserva())
                .dataInicio(reserva.getDataInicio())
                .dataFim(reserva.getDataFim())
                .status(reserva.getStatus())
                .build();
    }
}
