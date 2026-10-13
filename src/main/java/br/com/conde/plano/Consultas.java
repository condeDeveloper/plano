package br.com.conde.plano;

/**
 * OS FORMATOS DE CONSULTA, que sao a metade que importa.
 *
 * <p>Nao existe "o quanto um otimizador ganha". Existe o quanto ele ganha NAQUELE formato
 * de grafo de juncao, e os formatos daqui nao foram escolhidos para serem justos: cada um
 * e o melhor caso de alguma decisao e o pior de outra.
 *
 * <ul>
 *   <li>CADEIA: cada tabela liga com a seguinte. E a consulta de um sistema normalizado
 *       subindo uma hierarquia, e e onde a arvore de qualquer forma tem mais a ganhar.
 *   <li>ESTRELA: uma tabela grande no meio e varias pequenas em volta. E a consulta de
 *       armazem de dados, e e onde a ordem importa muito e a forma nao.
 *   <li>CICLO: a cadeia que fecha. O predicado a mais muda o otimo inteiro.
 *   <li>FLOCO: uma arvore qualquer. E o formato mais comum em sistema de verdade e nao
 *       tem nome bonito.
 *   <li>COMPLETA: todas ligadas com todas. Nao acontece na pratica e esta aqui porque e o
 *       caso em que a busca mais tem onde escolher.
 * </ul>
 *
 * <h2>De onde saem as seletividades</h2>
 *
 * <p>Nao sao sorteadas soltas. A seletividade de uma juncao por chave e, pela estimativa
 * classica, um sobre o numero de valores distintos do lado maior, o que aqui vira um sobre
 * o numero de linhas da maior das duas tabelas. Um fator sorteado entre 0,2 e 5 mexe nisso
 * para as consultas nao ficarem todas iguais.
 *
 * <p>Isso importa: com seletividades sorteadas soltas, uma juncao de duas tabelas de um
 * milhao de linhas sairia com um bilhao de linhas, o que nao acontece em sistema nenhum, e
 * a tabela inteira mediria um mundo que nao existe.
 */
public final class Consultas {

    private Consultas() {}

    private static final double MENOR_TABELA = 100;
    private static final double MAIOR_TABELA = 2_000_000;

    private static double[] tamanhos(int tabelas, Sorteio sorteio) {
        double[] linhas = new double[tabelas];

        for (int i = 0; i < tabelas; i++)
            linhas[i] = Math.round(sorteio.entreEmEscalaLogaritmica(MENOR_TABELA, MAIOR_TABELA));

        return linhas;
    }

    private static double[][] semPredicado(int tabelas) {
        double[][] seletividade = new double[tabelas][tabelas];

        for (double[] linha : seletividade) java.util.Arrays.fill(linha, 1.0);

        return seletividade;
    }

    private static void ligar(double[][] seletividade, double[] linhas, int a, int b,
            Sorteio sorteio) {
        double fator = sorteio.entreEmEscalaLogaritmica(0.2, 5.0);

        double valor = Math.min(1.0, fator / Math.max(linhas[a], linhas[b]));

        seletividade[a][b] = valor;
        seletividade[b][a] = valor;
    }

    /** Cada tabela liga com a seguinte. */
    public static Consulta cadeia(int tabelas, Sorteio sorteio) {
        double[] linhas = tamanhos(tabelas, sorteio);
        double[][] seletividade = semPredicado(tabelas);

        for (int i = 0; i + 1 < tabelas; i++) ligar(seletividade, linhas, i, i + 1, sorteio);

        return new Consulta("cadeia", linhas, seletividade);
    }

    /** A tabela zero no meio, todas as outras ligadas so a ela. */
    public static Consulta estrela(int tabelas, Sorteio sorteio) {
        double[] linhas = tamanhos(tabelas, sorteio);

        /* O centro e grande e as pontas sao pequenas, que e o que faz uma estrela. */
        linhas[0] = Math.round(sorteio.entreEmEscalaLogaritmica(500_000, MAIOR_TABELA));

        for (int i = 1; i < tabelas; i++)
            linhas[i] = Math.round(sorteio.entreEmEscalaLogaritmica(MENOR_TABELA, 50_000));

        double[][] seletividade = semPredicado(tabelas);

        for (int i = 1; i < tabelas; i++) ligar(seletividade, linhas, 0, i, sorteio);

        return new Consulta("estrela", linhas, seletividade);
    }

    /** A cadeia que fecha: a ultima liga de volta na primeira. */
    public static Consulta ciclo(int tabelas, Sorteio sorteio) {
        double[] linhas = tamanhos(tabelas, sorteio);
        double[][] seletividade = semPredicado(tabelas);

        for (int i = 0; i + 1 < tabelas; i++) ligar(seletividade, linhas, i, i + 1, sorteio);

        ligar(seletividade, linhas, tabelas - 1, 0, sorteio);

        return new Consulta("ciclo", linhas, seletividade);
    }

    /** Uma arvore qualquer: cada tabela nova se pendura numa que ja esta. */
    public static Consulta floco(int tabelas, Sorteio sorteio) {
        double[] linhas = tamanhos(tabelas, sorteio);
        double[][] seletividade = semPredicado(tabelas);

        for (int i = 1; i < tabelas; i++) ligar(seletividade, linhas, sorteio.ate(i), i, sorteio);

        return new Consulta("floco", linhas, seletividade);
    }

    /** Todas ligadas com todas. */
    public static Consulta completa(int tabelas, Sorteio sorteio) {
        double[] linhas = tamanhos(tabelas, sorteio);
        double[][] seletividade = semPredicado(tabelas);

        for (int i = 0; i < tabelas; i++)
            for (int j = i + 1; j < tabelas; j++) ligar(seletividade, linhas, i, j, sorteio);

        return new Consulta("completa", linhas, seletividade);
    }

    /** Os cinco formatos, na ordem em que as tabelas os mostram. */
    public static final String[] FORMATOS = {"cadeia", "estrela", "ciclo", "floco", "completa"};

    public static Consulta doFormato(String formato, int tabelas, Sorteio sorteio) {
        return switch (formato) {
            case "cadeia" -> cadeia(tabelas, sorteio);
            case "estrela" -> estrela(tabelas, sorteio);
            case "ciclo" -> ciclo(tabelas, sorteio);
            case "floco" -> floco(tabelas, sorteio);
            case "completa" -> completa(tabelas, sorteio);
            default -> throw new IllegalArgumentException("formato desconhecido: " + formato);
        };
    }

    /**
     * A mesma consulta com cada seletividade errada por ate um fator.
     *
     * <p>E assim que um otimizador de verdade enxerga: ele nao tem a seletividade, tem uma
     * ESTIMATIVA dela, tirada de histogramas e de hipoteses que quase sempre nao valem.
     * Errar por um fator dez numa juncao e comum; o erro se multiplica ao longo da cadeia.
     */
    public static Consulta comErroDeEstimativa(Consulta certa, double fator, Sorteio sorteio) {
        if (fator < 1) throw new IllegalArgumentException("o fator de erro comeca em um");

        double[][] estimada = certa.seletividades();

        for (int i = 0; i < certa.tabelas(); i++)
            for (int j = i + 1; j < certa.tabelas(); j++) {
                if (estimada[i][j] >= 1.0) continue;

                double erro = fator == 1.0 ? 1.0
                        : sorteio.entreEmEscalaLogaritmica(1 / fator, fator);

                double valor = Math.min(1.0, estimada[i][j] * erro);

                estimada[i][j] = valor;
                estimada[j][i] = valor;
            }

        return certa.com(estimada);
    }
}
