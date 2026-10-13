package br.com.conde.plano;

/**
 * UM OTIMIZADOR: de uma consulta, um plano.
 *
 * <p>Todos aqui devolvem um plano VALIDO para a mesma consulta, e todo plano valido produz
 * exatamente as mesmas linhas, porque a cardinalidade de um conjunto de tabelas nao depende
 * da ordem. Entao nenhum deles pode estar "errado" no sentido de dar outra resposta.
 *
 * <p>O que muda entre eles e so o CUSTO do plano que escolhem, e quanto trabalho eles
 * gastam escolhendo. As duas colunas andam ao contrario e e disso que as tabelas tratam.
 */
public interface Otimizador {

    String nome();

    Plano planejar(Consulta consulta);

    /** Quantos conjuntos de tabelas este otimizador considerou. Zero quando ele nao busca. */
    default long conjuntosExaminados(Consulta consulta) {
        return 0;
    }
}
