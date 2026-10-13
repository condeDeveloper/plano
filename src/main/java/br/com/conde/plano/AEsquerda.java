package br.com.conde.plano;

/**
 * PROGRAMACAO DINAMICA RESTRITA A ARVORE TODA PARA A ESQUERDA.
 *
 * <p>Arvore a esquerda quer dizer que todo lado direito de uma juncao e uma tabela base:
 * junta-se o que ja se tem com mais uma, e nunca dois resultados intermediarios entre si.
 *
 * <p>Essa restricao e classica e tem um motivo pratico forte: num plano a esquerda o
 * resultado parcial pode ir fluindo pela cadeia sem nunca ser guardado inteiro, enquanto
 * juntar dois intermediarios exige materializar os dois. Boa parte dos otimizadores de
 * verdade so considera planos assim.
 *
 * <p>Ela tambem corta a busca: em vez de todos os jeitos de partir um conjunto em dois, sao
 * n jeitos de tirar uma tabela dele.
 *
 * <p>O que ela custa esta na tabela da forma. A pergunta nao e se ela perde do otimo, e
 * quanto e em que formato de consulta, e a resposta nao e a mesma para cadeia e para
 * estrela.
 */
public final class AEsquerda implements Otimizador {

    @Override
    public String nome() {
        return "a esquerda";
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

            for (int i = 0; i < consulta.tabelas(); i++) {
                int ultima = 1 << i;

                if ((conjunto & ultima) == 0) continue;

                int resto = conjunto & ~ultima;

                if (melhor[resto] == null) continue;

                double candidato = custo[resto] + consulta.cardinalidade(conjunto);

                if (candidato >= custo[conjunto]) continue;

                custo[conjunto] = candidato;
                melhor[conjunto] = new Plano.Juncao(melhor[resto], new Plano.Folha(i));
            }
        }

        return melhor[todas];
    }

    @Override
    public long conjuntosExaminados(Consulta consulta) {
        return (1L << consulta.tabelas()) - 1;
    }
}
