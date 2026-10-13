package br.com.conde.plano;

import java.util.ArrayList;
import java.util.List;

/**
 * A FORCA BRUTA: monta TODOS os planos que existem e fica com o mais barato.
 *
 * <p>E o juiz deste repositorio, e e o juiz certo e nao so o conveniente. Ele nao usa
 * programacao dinamica, nao usa poda, nao usa heuristica, nao supoe que o melhor plano de
 * um conjunto seja feito dos melhores planos dos pedacos. Ele monta a arvore inteira, de
 * todo jeito possivel, e compara.
 *
 * <p>Essa ultima frase e a que importa. A programacao dinamica funciona porque vale o
 * principio da otimalidade: o melhor plano de um conjunto usa os melhores planos dos dois
 * lados. Isso e verdade neste modelo de custo e PODE NAO SER em outro, por exemplo quando o
 * custo depende da ordem em que as linhas saem. Um juiz que ja supusesse o principio nao
 * teria como notar se ele deixasse de valer.
 *
 * <h2>Quantos planos sao</h2>
 *
 * <p>Com n tabelas sao (2n-3)!! planos, o duplo fatorial: 1, 3, 15, 105, 945, 10.395 e
 * 135.135 de duas a oito tabelas. Com nove ja sao dois milhoes, e e por isso que o juiz
 * para em oito.
 *
 * <p>Trocar os dois lados de uma juncao nao conta como plano novo: neste modelo de custo
 * (A B) e (B A) custam o mesmo, entao a primeira tabela do conjunto fica sempre a
 * esquerda. Contando os espelhos seria o dobro, e nao mudaria nada.
 *
 * <p>Esse crescimento e o motivo de o assunto existir: um otimizador de verdade nao pode
 * fazer o que este juiz faz.
 */
public final class ForcaBruta {

    /** Acima disto a varredura deixa de ser varredura e vira espera. */
    public static final int MAXIMO_DE_TABELAS = 8;

    private ForcaBruta() {}

    /** O plano mais barato, achado olhando todos. */
    public static Plano melhor(Consulta consulta) {
        exigirQueCaiba(consulta);

        Plano melhor = null;
        double menor = Double.POSITIVE_INFINITY;

        for (Plano plano : todos(consulta.todas())) {
            double custo = plano.custo(consulta);

            if (custo >= menor) continue;

            menor = custo;
            melhor = plano;
        }

        return melhor;
    }

    /** Quantos planos foram montados para achar o melhor. */
    public static long quantosPlanos(Consulta consulta) {
        exigirQueCaiba(consulta);

        return todos(consulta.todas()).size();
    }

    /**
     * Todos os planos sobre este conjunto de tabelas.
     *
     * <p>Sem memoria, de proposito: memorizar aqui seria programacao dinamica, que e
     * justamente o que este juiz precisa nao ser.
     */
    public static List<Plano> todos(int conjunto) {
        List<Plano> saida = new ArrayList<>();

        if (Integer.bitCount(conjunto) == 1) {
            saida.add(new Plano.Folha(Integer.numberOfTrailingZeros(conjunto)));

            return saida;
        }

        int primeira = conjunto & -conjunto;

        /* Para nao montar (A B) e (B A) como dois planos diferentes, a primeira tabela do
         * conjunto fica sempre a esquerda. Os dois teriam o mesmo custo neste modelo. */
        for (int esquerda = (conjunto - 1) & conjunto; esquerda > 0;
                esquerda = (esquerda - 1) & conjunto) {

            if ((esquerda & primeira) == 0) continue;

            int direita = conjunto & ~esquerda;

            if (direita == 0) continue;

            for (Plano umLado : todos(esquerda))
                for (Plano outroLado : todos(direita))
                    saida.add(new Plano.Juncao(umLado, outroLado));
        }

        return saida;
    }

    private static void exigirQueCaiba(Consulta consulta) {
        if (consulta.tabelas() > MAXIMO_DE_TABELAS)
            throw new IllegalArgumentException(
                    "a forca bruta vai ate " + MAXIMO_DE_TABELAS + " tabelas e pediram "
                            + consulta.tabelas());
    }
}
