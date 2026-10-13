package br.com.conde.plano;

/**
 * O JUIZ: exige que o plano seja VALIDO e que a dinamica empate com a forca bruta.
 *
 * <p>Validade e quatro coisas, e nenhuma delas e ser barato:
 *
 * <ul>
 *   <li>O plano usa todas as tabelas da consulta, e cada uma uma vez so.
 *   <li>Ele tem exatamente n menos uma juncoes, que e o que uma arvore binaria com n folhas
 *       tem.
 *   <li>O numero de linhas que ele produz e o mesmo de qualquer outro plano da mesma
 *       consulta. Nao e uma coincidencia: a cardinalidade de um conjunto nao depende da
 *       ordem, e e isso que permite trocar de plano sem trocar de resposta.
 *   <li>O custo nunca fica ABAIXO do da forca bruta, que olhou todos os planos.
 * </ul>
 *
 * <p>Ele NAO exige que um otimizador ache o otimo. A gulosa nao acha, a da ordem escrita
 * nem procura, e a distancia ate o otimo e o que as tabelas medem em vez de julgar.
 */
public final class Juiz {

    private Juiz() {}

    public record Veredito(String otimizador, boolean valido, double custo, double doOtimo,
            String problema) {

        public double vezesOOtimo() {
            return doOtimo <= 0 ? 1 : custo / doOtimo;
        }
    }

    public static Veredito julgar(Otimizador otimizador, Consulta consulta, double custoOtimo) {
        Plano plano;

        try {
            plano = otimizador.planejar(consulta);
        } catch (RuntimeException erro) {
            return new Veredito(otimizador.nome(), false, Double.NaN, custoOtimo,
                    erro.getClass().getSimpleName() + ": " + erro.getMessage());
        }

        if (plano == null)
            return new Veredito(otimizador.nome(), false, Double.NaN, custoOtimo,
                    "nao devolveu plano nenhum");

        if (plano.conjunto() != consulta.todas())
            return new Veredito(otimizador.nome(), false, Double.NaN, custoOtimo,
                    "o plano cobre " + Integer.toBinaryString(plano.conjunto())
                            + " e a consulta tem " + Integer.toBinaryString(consulta.todas()));

        if (plano.juncoes() != consulta.tabelas() - 1)
            return new Veredito(otimizador.nome(), false, Double.NaN, custoOtimo,
                    "o plano tem " + plano.juncoes() + " juncoes e o certo sao "
                            + (consulta.tabelas() - 1));

        double linhas = plano.linhas(consulta);
        double certo = consulta.cardinalidade(consulta.todas());

        if (Math.abs(linhas - certo) > certo * 1e-9)
            return new Veredito(otimizador.nome(), false, Double.NaN, custoOtimo,
                    "o plano produz " + linhas + " linhas e o certo sao " + certo);

        double custo = plano.custo(consulta);

        if (custo < custoOtimo * (1 - 1e-9))
            return new Veredito(otimizador.nome(), false, custo, custoOtimo,
                    "custou " + custo + ", abaixo do otimo de " + custoOtimo);

        return new Veredito(otimizador.nome(), true, custo, custoOtimo, "");
    }

    /** Os otimizadores deste repositorio, do mais caro de rodar ao mais barato. */
    public static java.util.List<Otimizador> todos() {
        return java.util.List.of(
                new Dinamica(),
                new AEsquerda(),
                new Gulosa(),
                new NaOrdemEscrita());
    }
}
