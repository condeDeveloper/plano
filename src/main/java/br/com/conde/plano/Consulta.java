package br.com.conde.plano;

/**
 * UMA CONSULTA: tabelas e os predicados que ligam pares delas.
 *
 * <p>Nao ha SQL aqui e isso e proposital. Para escolher a ordem das juncoes so importam
 * duas coisas: quantas linhas cada tabela tem e quanto cada predicado corta. Colunas,
 * tipos, nomes de campo e sintaxe nao entram na conta, e carrega-los so esconderia o
 * assunto.
 *
 * <h2>A cardinalidade de um conjunto</h2>
 *
 * <p>O tamanho do resultado de juntar um CONJUNTO de tabelas e o produto dos tamanhos
 * delas vezes a seletividade de cada predicado cujas duas pontas estao no conjunto.
 *
 * <p>Repare no que essa formula NAO tem: a ordem. O resultado de juntar um conjunto de
 * tabelas e o mesmo seja qual for a ordem das juncoes, e isso nao e uma aproximacao, e
 * algebra. E por isso que escolher a ordem nao muda a RESPOSTA, so o trabalho para chegar
 * nela, e e por isso que existe um otimizador.
 *
 * <p>Os conjuntos sao mascaras de bits num int, entao esta implementacao vai ate 31
 * tabelas. Nenhuma tabela deste repositorio chega perto disso: o juiz e exaustivo e nao
 * aguentaria.
 */
public final class Consulta {

    private final String nome;
    private final double[] linhas;
    private final double[][] seletividade;

    public Consulta(String nome, double[] linhas, double[][] seletividade) {
        if (linhas.length < 2) throw new IllegalArgumentException("uma consulta tem duas tabelas ou mais");
        if (linhas.length > 20)
            throw new IllegalArgumentException(
                    "o juiz e exaustivo e " + linhas.length + " tabelas nao terminam");

        for (double quantas : linhas)
            if (quantas < 1) throw new IllegalArgumentException("uma tabela tem pelo menos uma linha");

        this.nome = nome;
        this.linhas = linhas.clone();
        this.seletividade = new double[linhas.length][linhas.length];

        for (int i = 0; i < linhas.length; i++)
            for (int j = 0; j < linhas.length; j++) {
                double valor = seletividade[i][j];

                if (valor <= 0 || valor > 1)
                    throw new IllegalArgumentException(
                            "seletividade de " + i + " com " + j + " vale " + valor
                                    + ", e ela fica entre zero e um");

                this.seletividade[i][j] = valor;
            }

        for (int i = 0; i < linhas.length; i++)
            for (int j = i + 1; j < linhas.length; j++)
                if (this.seletividade[i][j] != this.seletividade[j][i])
                    throw new IllegalArgumentException(
                            "o predicado de " + i + " com " + j + " tem dois valores");
    }

    public String nome() {
        return nome;
    }

    public int tabelas() {
        return linhas.length;
    }

    public double linhasDe(int tabela) {
        return linhas[tabela];
    }

    public double seletividadeEntre(int a, int b) {
        return seletividade[a][b];
    }

    /** Ha predicado entre as duas, ou juntar as duas seria um produto cartesiano. */
    public boolean ligadas(int a, int b) {
        return seletividade[a][b] < 1.0;
    }

    /** Todas as tabelas, como mascara. */
    public int todas() {
        return (1 << tabelas()) - 1;
    }

    /**
     * Quantas linhas saem de juntar este conjunto de tabelas.
     *
     * <p>Nao depende de ordem nenhuma, e e isso que faz o otimizador ser sobre custo e nao
     * sobre resultado.
     */
    public double cardinalidade(int conjunto) {
        if (conjunto == 0) return 1;

        double total = 1;

        for (int i = 0; i < tabelas(); i++) {
            if ((conjunto & (1 << i)) == 0) continue;

            total *= linhas[i];

            for (int j = i + 1; j < tabelas(); j++)
                if ((conjunto & (1 << j)) != 0) total *= seletividade[i][j];
        }

        return total;
    }

    /** Ha ao menos um predicado entre os dois conjuntos, ou junta-los e produto cartesiano. */
    public boolean ligados(int esquerda, int direita) {
        for (int i = 0; i < tabelas(); i++) {
            if ((esquerda & (1 << i)) == 0) continue;

            for (int j = 0; j < tabelas(); j++)
                if ((direita & (1 << j)) != 0 && ligadas(i, j)) return true;
        }

        return false;
    }

    /** A mesma consulta com cada seletividade multiplicada por um erro de estimativa. */
    public Consulta com(double[][] outrasSeletividades) {
        return new Consulta(nome, linhas, outrasSeletividades);
    }

    public double[][] seletividades() {
        double[][] copia = new double[tabelas()][];

        for (int i = 0; i < tabelas(); i++) copia[i] = seletividade[i].clone();

        return copia;
    }
}
