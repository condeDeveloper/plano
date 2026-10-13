package br.com.conde.plano;

/**
 * PROGRAMACAO DINAMICA SOBRE CONJUNTOS, com arvore de qualquer forma.
 *
 * <p>Ela e o algoritmo de verdade deste repositorio e e a que todo banco de dados serio
 * usa, de 1979 para ca, com os nomes de Selinger e companhia em cima.
 *
 * <p>A ideia e uma: o melhor plano de um conjunto de tabelas usa os melhores planos dos
 * dois pedacos em que ele se divide. Entao da para guardar um plano por conjunto, subir do
 * menor para o maior, e nunca mais montar arvore nenhuma.
 *
 * <p>Isso derruba o numero de coisas examinadas de "todas as arvores" para "todos os
 * conjuntos", que com n tabelas e 2 elevado a n em vez de fatorial. Com sete tabelas sao
 * 127 conjuntos contra centenas de milhares de arvores.
 *
 * <h2>Por que ela precisa de juiz mesmo sendo provadamente otima</h2>
 *
 * <p>Porque a prova e sobre o PRINCIPIO DA OTIMALIDADE, que vale neste modelo de custo e
 * pode nao valer em outro. Se um dia o custo passar a depender da ordem em que as linhas
 * saem de uma juncao, a prova cai e o codigo continua compilando. A forca bruta nota; a
 * prova no papel nao.
 */
public final class Dinamica implements Otimizador {

    @Override
    public String nome() {
        return "dinamica";
    }

    @Override
    public Plano planejar(Consulta consulta) {
        int todas = consulta.todas();

        Plano[] melhor = new Plano[todas + 1];
        double[] custo = new double[todas + 1];

        java.util.Arrays.fill(custo, Double.POSITIVE_INFINITY);

        for (int i = 0; i < consulta.tabelas(); i++) {
            melhor[1 << i] = new Plano.Folha(i);
            custo[1 << i] = 0;
        }

        for (int conjunto = 1; conjunto <= todas; conjunto++) {
            if (Integer.bitCount(conjunto) < 2) continue;

            int primeira = conjunto & -conjunto;

            for (int esquerda = (conjunto - 1) & conjunto; esquerda > 0;
                    esquerda = (esquerda - 1) & conjunto) {

                /* A primeira tabela fica sempre a esquerda: a outra metade e o espelho. */
                if ((esquerda & primeira) == 0) continue;

                int direita = conjunto & ~esquerda;

                if (direita == 0 || melhor[esquerda] == null || melhor[direita] == null) continue;

                double candidato = custo[esquerda] + custo[direita]
                        + consulta.cardinalidade(conjunto);

                if (candidato >= custo[conjunto]) continue;

                custo[conjunto] = candidato;
                melhor[conjunto] = new Plano.Juncao(melhor[esquerda], melhor[direita]);
            }
        }

        return melhor[todas];
    }

    @Override
    public long conjuntosExaminados(Consulta consulta) {
        return (1L << consulta.tabelas()) - 1;
    }
}
