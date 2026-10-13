package br.com.conde.plano;

/**
 * UM PLANO: a arvore que diz em que ordem as juncoes acontecem.
 *
 * <p>A folha e uma tabela e nao custa nada: ela ja existe. O no interno e uma juncao, e o
 * que ela custa e o TAMANHO DO RESULTADO dela.
 *
 * <h2>Por que o custo e a soma dos resultados intermediarios</h2>
 *
 * <p>Esse modelo chama-se C-out e e o modelo certo para este repositorio, e nao so o
 * conveniente. Ele nao supoe algoritmo de juncao nenhum: nao diz se e laco aninhado, se e
 * espalhamento, se cabe na memoria. Ele diz so que produzir n linhas custa n.
 *
 * <p>Isso e uma simplificacao e ela tem um nome. O que ela captura e exatamente o que a
 * ordem das juncoes controla, que e o tamanho do que passa de uma juncao para a proxima; e
 * o que ela deixa de fora e a escolha do algoritmo, que e outra decisao e esta em outro
 * repositorio. Misturar as duas numa conta so e o jeito de nao conseguir explicar nenhuma.
 *
 * <p>A folha custar zero tambem e escolha: ler as tabelas base custa o mesmo em todo plano,
 * entao somar isso deslocaria todas as colunas pelo mesmo valor e esconderia as razoes.
 */
public sealed interface Plano {

    /** As tabelas deste plano, como mascara de bits. */
    int conjunto();

    /** Quantas linhas este plano produz. Igual em todo plano com o mesmo conjunto. */
    double linhas(Consulta consulta);

    /** A soma dos tamanhos de todos os resultados intermediarios. */
    double custo(Consulta consulta);

    /** Quantas juncoes ha neste plano. Sempre o numero de tabelas menos um. */
    int juncoes();

    /** A arvore e toda para a esquerda: todo lado direito e uma folha. */
    boolean aEsquerda();

    /** Alguma juncao deste plano junta dois lados sem predicado entre eles. */
    boolean temProdutoCartesiano(Consulta consulta);

    String desenho();

    /** Uma tabela base. Ela ja existe, entao nao custa nada. */
    record Folha(int tabela) implements Plano {

        public Folha {
            if (tabela < 0 || tabela > 30) throw new IllegalArgumentException("tabela " + tabela);
        }

        @Override
        public int conjunto() {
            return 1 << tabela;
        }

        @Override
        public double linhas(Consulta consulta) {
            return consulta.linhasDe(tabela);
        }

        @Override
        public double custo(Consulta consulta) {
            return 0;
        }

        @Override
        public int juncoes() {
            return 0;
        }

        @Override
        public boolean aEsquerda() {
            return true;
        }

        @Override
        public boolean temProdutoCartesiano(Consulta consulta) {
            return false;
        }

        @Override
        public String desenho() {
            return String.valueOf(tabela);
        }
    }

    /** Uma juncao de dois planos. Custa o que ela produz, mais o que os dois lados custaram. */
    record Juncao(Plano esquerda, Plano direita) implements Plano {

        public Juncao {
            if ((esquerda.conjunto() & direita.conjunto()) != 0)
                throw new IllegalArgumentException(
                        "os dois lados compartilham tabela: " + esquerda.desenho()
                                + " e " + direita.desenho());
        }

        @Override
        public int conjunto() {
            return esquerda.conjunto() | direita.conjunto();
        }

        @Override
        public double linhas(Consulta consulta) {
            return consulta.cardinalidade(conjunto());
        }

        @Override
        public double custo(Consulta consulta) {
            return esquerda.custo(consulta) + direita.custo(consulta) + linhas(consulta);
        }

        @Override
        public int juncoes() {
            return 1 + esquerda.juncoes() + direita.juncoes();
        }

        @Override
        public boolean aEsquerda() {
            return direita instanceof Folha && esquerda.aEsquerda();
        }

        @Override
        public boolean temProdutoCartesiano(Consulta consulta) {
            if (!consulta.ligados(esquerda.conjunto(), direita.conjunto())) return true;

            return esquerda.temProdutoCartesiano(consulta)
                    || direita.temProdutoCartesiano(consulta);
        }

        @Override
        public String desenho() {
            return "(" + esquerda.desenho() + " " + direita.desenho() + ")";
        }
    }
}
