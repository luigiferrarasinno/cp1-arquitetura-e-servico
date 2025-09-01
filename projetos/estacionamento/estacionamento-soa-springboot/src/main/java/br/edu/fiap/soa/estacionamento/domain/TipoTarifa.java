package br.edu.fiap.soa.estacionamento.domain;

/**
 * Enum para definir os tipos de tarifa do estacionamento
 */
public enum TipoTarifa {
    FRACAO_30MIN("Fração de 30 minutos"),
    HORARIA("Por hora completa"),
    DIARIA("Diária (até 24h)"),
    MENSAL("Mensalista");

    private final String descricao;

    TipoTarifa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
