package br.com.conde.plano;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros do README, afirmados.
 *
 * <p>Um README e texto: ele nao quebra quando deixa de ser verdade. Cada afirmacao das
 * tabelas esta repetida aqui para quebrar.
 */
class MedidasTest {

    /* Cada tabela planeja milhares de consultas: calcular uma vez so importa. */
    private static final List<Medidas.LinhaOrdem> ORDEM = Medidas.aOrdem();
    private static final List<Medidas.LinhaForma> FORMA = Medidas.aForma();
    private static final List<Medidas.LinhaGuloso> GULOSO = Medidas.oGuloso();
    private static final List<Medidas.LinhaEstimativa> ESTIMATIVA = Medidas.aEstimativa();

    // ───────────────────── 1. a correcao ─────────────────────

    /**
     * A DINAMICA EMPATA COM A FORCA BRUTA em mil consultas, e e isso que permite usar a
     * dinamica como regua nas outras quatro tabelas.
     */
    @Test
    @DisplayName("213.800 planos montados, e a dinamica nao discordou em nenhuma das 1.000 consultas")
    void aDinamicaEmpataComAForcaBruta() {
        List<Medidas.LinhaCorrecao> linhas = Medidas.aCorrecao();

        assertThat(linhas).hasSize(25);

        assertThat(linhas.stream().mapToLong(Medidas.LinhaCorrecao::planosMontados).sum())
                .isEqualTo(213_800);

        assertThat(linhas.stream().mapToLong(Medidas.LinhaCorrecao::conjuntosDaDinamica).sum())
                .as("contra 23.800 conjuntos: nove vezes menos trabalho para o mesmo resultado")
                .isEqualTo(23_800);

        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.invalidos()).as(l.formato() + " " + l.tabelas()).isZero();
            assertThat(l.dinamicaDiscordou()).as(l.formato() + " " + l.tabelas()).isZero();
        });

        assertThat(linhas).filteredOn(l -> l.tabelas() == 6)
                .allSatisfy(l -> assertThat(l.planosMontados())
                        .as(l.formato() + ": 945 planos por consulta, vezes 40")
                        .isEqualTo(37_800));
    }

    // ───────────────────── 2. a ordem ─────────────────────

    /**
     * O PRECO DE NAO OTIMIZAR, e a diferenca entre media e mediana e metade do assunto.
     *
     * <p>A media passa de 400 vezes o otimo e a mediana fica em 19. O estrago nao esta
     * espalhado: ele esta numa cauda.
     */
    @Test
    @DisplayName("nao otimizar custa 491 vezes na media e 19 na mediana, numa cadeia de 10")
    void naoOtimizarCustaCaro() {
        assertThat(ordem("cadeia", 10).media()).isCloseTo(490.9, within(0.1));
        assertThat(ordem("cadeia", 10).mediana()).isCloseTo(19.1, within(0.1));

        assertThat(ordem("cadeia", 10).media())
                .as("a media e vinte e cinco vezes a mediana: o estrago mora na cauda")
                .isGreaterThan(ordem("cadeia", 10).mediana() * 20);

        assertThat(ordem("completa", 6).media()).isCloseTo(903.2, within(0.1));
        assertThat(ordem("estrela", 4).media()).isCloseTo(40.1, within(0.1));

        assertThat(ORDEM).allSatisfy(l -> {
            assertThat(l.media()).as(l.formato() + " " + l.tabelas()).isGreaterThan(1.0);
            assertThat(l.pior()).as(l.formato() + " " + l.tabelas()).isGreaterThan(l.media());
        });
    }

    private static Medidas.LinhaOrdem ordem(String formato, int tabelas) {
        return ORDEM.stream()
                .filter(l -> l.formato().equals(formato) && l.tabelas() == tabelas)
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 3. a forma ─────────────────────

    /**
     * A CORRECAO: o otimo quase nunca e uma arvore a esquerda, e proibir arvore que nao
     * seja a esquerda nao custa nada.
     *
     * <p>Numa cadeia de dez tabelas, o otimo que a dinamica devolve e a esquerda em 8,5%
     * das consultas. E restringir a busca so a arvores a esquerda custa 1,0000 vezes o
     * otimo, em duzentas consultas: zero.
     *
     * <p>Os dois numeros nao se contradizem. Eles dizem que ha MUITOS planos empatados no
     * otimo e que sempre ha um a esquerda entre eles; a dinamica so devolve outro porque a
     * ordem de varredura dela chega primeiro num plano de qualquer forma.
     */
    @Test
    @DisplayName("o otimo e a esquerda em 8,5% das cadeias de 10, e so olhar a esquerda custa zero")
    void proibirArvoreCabeludaNaoCustaNada() {
        assertThat(forma("cadeia", 10).fracaoDeOtimosAEsquerda()).isCloseTo(0.085, within(0.001));

        assertThat(forma("cadeia", 10).media())
                .as("e mesmo assim a arvore a esquerda empata com o otimo")
                .isEqualTo(1.0);

        assertThat(forma("cadeia", 10).fracaoQuePerde()).isZero();

        assertThat(FORMA).allSatisfy(l -> {
            assertThat(l.media()).as(l.formato() + " " + l.tabelas()).isLessThan(1.005);
            assertThat(l.pior()).as(l.formato() + " " + l.tabelas()).isLessThan(1.6);
            assertThat(l.fracaoQuePerde()).as(l.formato() + " " + l.tabelas()).isLessThan(0.03);
        });

        assertThat(forma("cadeia", 8).pior())
                .as("a pior consulta de todas perde 33%, e e uma em duzentas")
                .isCloseTo(1.330, within(0.001));
    }

    /**
     * NA ESTRELA O OTIMO E SEMPRE A ESQUERDA, nas oitocentas consultas.
     *
     * <p>Faz sentido depois de medido: as pontas de uma estrela so ligam com o centro, e
     * juntar duas pontas entre si e produto cartesiano. Nao ha arvore cabeluda util.
     */
    @Test
    @DisplayName("na estrela o otimo e a esquerda em 100% das consultas, em todo tamanho")
    void naEstrelaOOtimoEhSempreAEsquerda() {
        for (int tabelas : Medidas.TAMANHOS) {
            assertThat(forma("estrela", tabelas).fracaoDeOtimosAEsquerda())
                    .as("" + tabelas)
                    .isEqualTo(1.0);

            assertThat(forma("estrela", tabelas).media()).as("" + tabelas).isEqualTo(1.0);
        }
    }

    /**
     * E O OTIMO NUNCA USOU PRODUTO CARTESIANO, em quatro mil consultas.
     *
     * <p>Essa e a heuristica mais antiga do assunto, de 1979, e ela nao e teorema. Aqui ela
     * se confirmou em todas, em cinco formatos e quatro tamanhos.
     */
    @Test
    @DisplayName("em 4.000 consultas, o melhor plano nunca juntou dois lados sem predicado")
    void oOtimoNuncaUsaProdutoCartesiano() {
        assertThat(FORMA).allSatisfy(l ->
                assertThat(l.fracaoDeOtimosComProduto())
                        .as(l.formato() + " com " + l.tabelas() + " tabelas")
                        .isZero());

        assertThat(FORMA).hasSize(20);
    }

    private static Medidas.LinhaForma forma(String formato, int tabelas) {
        return FORMA.stream()
                .filter(l -> l.formato().equals(formato) && l.tabelas() == tabelas)
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 4. o guloso ─────────────────────

    @Test
    @DisplayName("a gulosa e exatamente otima na estrela e na completa, e perde 26% no floco")
    void aGulosaDependeDoFormato() {
        for (int tabelas : Medidas.TAMANHOS)
            for (String formato : List.of("estrela", "completa")) {
                assertThat(guloso(formato, tabelas).media()).as(formato + " " + tabelas)
                        .isEqualTo(1.0);

                assertThat(guloso(formato, tabelas).fracaoQueAcerta())
                        .as(formato + " " + tabelas)
                        .isEqualTo(1.0);
            }

        assertThat(guloso("floco", 10).media()).isCloseTo(1.2592, within(0.0001));
        assertThat(guloso("cadeia", 6).media()).isCloseTo(1.2477, within(0.0001));

        assertThat(guloso("cadeia", 6).pior())
                .as("e numa consulta ela custou catorze vezes o otimo")
                .isCloseTo(14.375, within(0.001));

        assertThat(guloso("floco", 10).fracaoQueAcerta())
                .as("com dez tabelas ela acha o otimo em menos da metade das vezes")
                .isCloseTo(0.445, within(0.001));
    }

    @Test
    @DisplayName("a gulosa olha 165 pares onde a dinamica olha 1.023 conjuntos")
    void aGulosaOlhaMuitoMenos() {
        assertThat(guloso("cadeia", 10).conjuntosDaGulosa()).isEqualTo(165);
        assertThat(guloso("cadeia", 10).conjuntosDaDinamica()).isEqualTo(1_023);

        assertThat(guloso("cadeia", 4).conjuntosDaGulosa()).isEqualTo(10);
        assertThat(guloso("cadeia", 4).conjuntosDaDinamica()).isEqualTo(15);
    }

    private static Medidas.LinhaGuloso guloso(String formato, int tabelas) {
        return GULOSO.stream()
                .filter(l -> l.formato().equals(formato) && l.tabelas() == tabelas)
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 5. a estimativa ─────────────────────

    /**
     * A CORRECAO PRINCIPAL: errar a estimativa custa mais do que errar o algoritmo.
     *
     * <p>Eu montei o repositorio para comparar algoritmos. Com dez tabelas e mil consultas,
     * trocar a busca exata pela gulosa custa 1,118 vezes o otimo. Manter a busca exata e
     * errar as seletividades por um fator dez custa 2,426.
     *
     * <p>Um fator dois de erro, que e otimismo num sistema de verdade, ja custa 1,120, que
     * e o mesmo que jogar a busca exata fora.
     */
    @Test
    @DisplayName("errar a estimativa por 10 custa o dobro de trocar o algoritmo exato pelo guloso")
    void errarAEstimativaCustaMaisQueErrarOAlgoritmo() {
        assertThat(estimativa("dinamica", 1, 10).media()).isEqualTo(1.0);

        assertThat(estimativa("dinamica", 2, 10).media()).isCloseTo(1.120, within(0.001));
        assertThat(estimativa("dinamica", 10, 10).media()).isCloseTo(2.426, within(0.001));
        assertThat(estimativa("dinamica", 100, 10).media()).isCloseTo(9.288, within(0.001));

        assertThat(estimativa("gulosa", 1, 10).media()).isCloseTo(1.118, within(0.001));
        assertThat(estimativa("a esquerda", 1, 10).media()).isCloseTo(1.000, within(0.001));

        assertThat(estimativa("dinamica", 2, 10).media())
                .as("um fator dois de erro ja empata com jogar a busca exata fora")
                .isCloseTo(estimativa("gulosa", 1, 10).media(), within(0.01));

        assertThat(estimativa("dinamica", 10, 10).media())
                .as("e um fator dez custa o dobro disso")
                .isGreaterThan(estimativa("gulosa", 1, 10).media() * 2);
    }

    @Test
    @DisplayName("com estimativa perfeita a dinamica e exatamente o otimo, por definicao")
    void comEstimativaPerfeitaADinamicaEhOOtimo() {
        for (int tabelas : new int[] {6, 10}) {
            assertThat(estimativa("dinamica", 1, tabelas).media()).as("" + tabelas).isEqualTo(1.0);
            assertThat(estimativa("dinamica", 1, tabelas).pior()).as("" + tabelas).isEqualTo(1.0);
        }
    }

    @Test
    @DisplayName("a mediana do erro de estimativa fica perto de um, e a cauda e que machuca")
    void aCaudaDoErroDeEstimativa() {
        assertThat(estimativa("dinamica", 10, 10).mediana()).isCloseTo(1.266, within(0.001));

        assertThat(estimativa("dinamica", 10, 10).pior())
                .as("e numa consulta o plano escolhido custou 66 vezes o otimo")
                .isCloseTo(66.63, within(0.1));

        assertThat(estimativa("dinamica", 100, 10).pior()).isGreaterThan(900.0);
    }

    private static Medidas.LinhaEstimativa estimativa(String otimizador, double erro,
            int tabelas) {
        return ESTIMATIVA.stream()
                .filter(l -> l.otimizador().equals(otimizador) && l.fatorDeErro() == erro
                        && l.tabelas() == tabelas)
                .findFirst()
                .orElseThrow();
    }
}
