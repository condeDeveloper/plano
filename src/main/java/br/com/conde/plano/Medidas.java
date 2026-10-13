package br.com.conde.plano;

import java.util.ArrayList;
import java.util.List;

/**
 * AS CINCO TABELAS que o medidor imprime e que os testes conferem numero por numero.
 *
 * <p>Tudo aqui e contado, nunca cronometrado: linhas intermediarias, conjuntos examinados e
 * planos montados. Os mesmos numeros saem em qualquer sistema.
 *
 * <h2>Quem e o otimo em cada tabela</h2>
 *
 * <p>Na tabela da correcao o otimo e a FORCA BRUTA, que monta todos os planos. Nas outras
 * quatro ele e a programacao dinamica, que e milhares de vezes mais rapida.
 *
 * <p>Essa troca so e honesta porque a primeira tabela existe: ela confere, consulta por
 * consulta, que as duas dao o mesmo custo. Sem ela, usar a dinamica como regua seria supor
 * o que se quer provar.
 */
public final class Medidas {

    public static final long SEMENTE = 20_261_013L;

    /** Quantas consultas de cada formato em cada tamanho. */
    public static final int CONSULTAS = 200;

    /** Os tamanhos de consulta das tabelas grandes. */
    public static final int[] TAMANHOS = {4, 6, 8, 10};

    private Medidas() {}

    private static final java.util.Map<String, List<Consulta>> POPULACOES =
            new java.util.HashMap<>();

    /**
     * Uma populacao de consultas de um formato e um tamanho, sempre a mesma.
     *
     * <p>Guardada porque as cinco tabelas usam as mesmas consultas, e porque o custo otimo
     * de cada uma fica guardado junto, pela identidade do objeto.
     */
    public static synchronized List<Consulta> populacao(String formato, int tabelas, int quantas) {
        return POPULACOES.computeIfAbsent(formato + " " + tabelas + " " + quantas, chave -> {
            Sorteio sorteio = new Sorteio(SEMENTE + formato.hashCode() * 31L + tabelas);

            List<Consulta> saida = new ArrayList<>();

            for (int i = 0; i < quantas; i++)
                saida.add(Consultas.doFormato(formato, tabelas, sorteio));

            return saida;
        });
    }

    private static final java.util.Map<Consulta, Double> OTIMOS = new java.util.IdentityHashMap<>();

    /**
     * O custo do melhor plano desta consulta, pela programacao dinamica.
     *
     * <p>Guardado por identidade: cada tabela deste arquivo pergunta o otimo das mesmas
     * consultas, e com dez tabelas a dinamica percorre 59.049 particoes por consulta.
     * Calcular uma vez e a diferenca entre a bateria de testes levar vinte segundos ou
     * noventa.
     */
    public static synchronized double custoOtimo(Consulta consulta) {
        return OTIMOS.computeIfAbsent(consulta,
                qual -> new Dinamica().planejar(qual).custo(qual));
    }

    private static double media(List<Double> valores) {
        double soma = 0;

        for (double valor : valores) soma += valor;

        return valores.isEmpty() ? 0 : soma / valores.size();
    }

    private static double maior(List<Double> valores) {
        double maior = 0;

        for (double valor : valores) maior = Math.max(maior, valor);

        return maior;
    }

    private static double mediana(List<Double> valores) {
        List<Double> ordenados = new ArrayList<>(valores);

        java.util.Collections.sort(ordenados);

        return ordenados.isEmpty() ? 0 : ordenados.get(ordenados.size() / 2);
    }

    // ───────────────────── 1. a correcao ─────────────────────

    public record LinhaCorrecao(String formato, int tabelas, int consultas, long planosMontados,
            long conjuntosDaDinamica, long invalidos, long dinamicaDiscordou) {}

    /**
     * A dinamica empata com a forca bruta em toda consulta, e todo plano e valido.
     *
     * <p>Vai ate seis tabelas porque e ate la que montar todos os planos termina.
     */
    public static List<LinhaCorrecao> aCorrecao() {
        List<LinhaCorrecao> saida = new ArrayList<>();

        for (String formato : Consultas.FORMATOS)
            for (int tabelas : new int[] {2, 3, 4, 5, 6}) {
                long planos = 0;
                long invalidos = 0;
                long discordou = 0;

                List<Consulta> consultas = populacao(formato, tabelas, 40);

                for (Consulta consulta : consultas) {
                    Plano bruto = ForcaBruta.melhor(consulta);

                    planos += ForcaBruta.quantosPlanos(consulta);

                    double otimo = bruto.custo(consulta);

                    for (Otimizador otimizador : Juiz.todos()) {
                        Juiz.Veredito veredito = Juiz.julgar(otimizador, consulta, otimo);

                        if (!veredito.valido()) invalidos++;
                    }

                    double daDinamica = new Dinamica().planejar(consulta).custo(consulta);

                    if (Math.abs(daDinamica - otimo) > otimo * 1e-9) discordou++;
                }

                saida.add(new LinhaCorrecao(formato, tabelas, consultas.size(), planos,
                        (long) consultas.size() * ((1L << tabelas) - 1), invalidos, discordou));
            }

        return saida;
    }

    // ───────────────────── 2. a ordem ─────────────────────

    public record LinhaOrdem(String formato, int tabelas, double media, double mediana,
            double pior) {}

    /** O preco de nao otimizar: a ordem escrita contra o otimo. */
    public static List<LinhaOrdem> aOrdem() {
        List<LinhaOrdem> saida = new ArrayList<>();

        for (String formato : Consultas.FORMATOS)
            for (int tabelas : TAMANHOS) {
                List<Double> razoes = new ArrayList<>();

                for (Consulta consulta : populacao(formato, tabelas, CONSULTAS)) {
                    double otimo = custoOtimo(consulta);
                    double escrita = new NaOrdemEscrita().planejar(consulta).custo(consulta);

                    razoes.add(escrita / otimo);
                }

                saida.add(new LinhaOrdem(formato, tabelas, media(razoes), mediana(razoes),
                        maior(razoes)));
            }

        return saida;
    }

    // ───────────────────── 3. a forma ─────────────────────

    public record LinhaForma(String formato, int tabelas, double media, double pior,
            double fracaoQuePerde, double fracaoDeOtimosAEsquerda,
            double fracaoDeOtimosComProduto) {}

    /** Arvore a esquerda contra arvore de qualquer forma. */
    public static List<LinhaForma> aForma() {
        List<LinhaForma> saida = new ArrayList<>();

        for (String formato : Consultas.FORMATOS)
            for (int tabelas : TAMANHOS) {
                List<Double> razoes = new ArrayList<>();

                long perde = 0;
                long otimosAEsquerda = 0;
                long otimosComProduto = 0;
                long total = 0;

                for (Consulta consulta : populacao(formato, tabelas, CONSULTAS)) {
                    Plano melhor = new Dinamica().planejar(consulta);

                    double otimo = melhor.custo(consulta);
                    double aEsquerda = new AEsquerda().planejar(consulta).custo(consulta);

                    razoes.add(aEsquerda / otimo);

                    if (aEsquerda > otimo * (1 + 1e-9)) perde++;
                    if (melhor.aEsquerda()) otimosAEsquerda++;
                    if (melhor.temProdutoCartesiano(consulta)) otimosComProduto++;

                    total++;
                }

                saida.add(new LinhaForma(formato, tabelas, media(razoes), maior(razoes),
                        (double) perde / total, (double) otimosAEsquerda / total,
                        (double) otimosComProduto / total));
            }

        return saida;
    }

    // ───────────────────── 4. o guloso ─────────────────────

    public record LinhaGuloso(String formato, int tabelas, double media, double pior,
            double fracaoQueAcerta, long conjuntosDaGulosa, long conjuntosDaDinamica) {}

    /** A gulosa contra o otimo, e quanto trabalho ela economiza. */
    public static List<LinhaGuloso> oGuloso() {
        List<LinhaGuloso> saida = new ArrayList<>();

        for (String formato : Consultas.FORMATOS)
            for (int tabelas : TAMANHOS) {
                List<Double> razoes = new ArrayList<>();

                long acerta = 0;
                long total = 0;

                long daGulosa = 0;
                long daDinamica = 0;

                for (Consulta consulta : populacao(formato, tabelas, CONSULTAS)) {
                    double otimo = custoOtimo(consulta);
                    double gulosa = new Gulosa().planejar(consulta).custo(consulta);

                    razoes.add(gulosa / otimo);

                    if (gulosa <= otimo * (1 + 1e-9)) acerta++;

                    daGulosa = new Gulosa().conjuntosExaminados(consulta);
                    daDinamica = new Dinamica().conjuntosExaminados(consulta);

                    total++;
                }

                saida.add(new LinhaGuloso(formato, tabelas, media(razoes), maior(razoes),
                        (double) acerta / total, daGulosa, daDinamica));
            }

        return saida;
    }

    // ───────────────────── 5. a estimativa ─────────────────────

    /** Os fatores de erro de estimativa, de nenhum a cem vezes. */
    public static final double[] ERROS = {1, 2, 10, 100};

    public record LinhaEstimativa(String otimizador, double fatorDeErro, int tabelas,
            double media, double mediana, double pior) {}

    /**
     * O otimo com estimativa errada, contra heuristicas com estimativa perfeita.
     *
     * <p>O plano e escolhido com a consulta ESTIMADA e depois cobrado com a consulta
     * VERDADEIRA, que e exatamente o que acontece na vida: o otimizador decide com o que
     * acha que sabe e a maquina paga o que e.
     */
    public static List<LinhaEstimativa> aEstimativa() {
        List<LinhaEstimativa> saida = new ArrayList<>();

        for (int tabelas : new int[] {6, 10}) {
            List<Consulta> consultas = new ArrayList<>();

            for (String formato : Consultas.FORMATOS)
                consultas.addAll(populacao(formato, tabelas, CONSULTAS));

            for (double fator : ERROS) {
                List<Double> razoes = new ArrayList<>();

                Sorteio sorteio = new Sorteio(SEMENTE + (long) (fator * 7) + tabelas);

                for (Consulta certa : consultas) {
                    Consulta estimada = Consultas.comErroDeEstimativa(certa, fator, sorteio);

                    Plano escolhido = new Dinamica().planejar(estimada);

                    razoes.add(escolhido.custo(certa) / custoOtimo(certa));
                }

                saida.add(new LinhaEstimativa("dinamica", fator, tabelas, media(razoes),
                        mediana(razoes), maior(razoes)));
            }

            for (Otimizador otimizador : List.of(new AEsquerda(), new Gulosa())) {
                List<Double> razoes = new ArrayList<>();

                for (Consulta certa : consultas)
                    razoes.add(otimizador.planejar(certa).custo(certa) / custoOtimo(certa));

                saida.add(new LinhaEstimativa(otimizador.nome(), 1, tabelas, media(razoes),
                        mediana(razoes), maior(razoes)));
            }
        }

        return saida;
    }
}
