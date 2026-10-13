package br.com.conde.plano;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** As pecas, uma a uma, antes de qualquer medida. */
class PlanoTest {

    // ───────────────────── o sorteio ─────────────────────

    @Nested
    @DisplayName("o sorteio da casa")
    class OSorteio {

        /** O mesmo gerador sem o deslocamento, para o teste poder mostrar a diferenca. */
        private int ateSemDeslocamento(long[] estado, int limite) {
            estado[0] = estado[0] * 6364136223846793005L + 1442695040888963407L;

            return Math.floorMod((int) estado[0], limite);
        }

        /**
         * O DEFEITO DOS BITS DE BAIXO: com limite potencia de dois, vira rodizio.
         *
         * <p>Os tamanhos das tabelas e a escolha de onde pendurar cada tabela do floco saem
         * de sorteios com limite pequeno. Com os bits de baixo, dezesseis sorteios
         * percorrem os dezesseis valores na mesma ordem, sempre, e consultas "sorteadas"
         * diferentes passariam a ser a mesma consulta deslocada.
         */
        @Test
        @DisplayName("sem o deslocamento, dezesseis sorteios dao os dezesseis valores em ordem fixa")
        void semODeslocamentoViraRodizio() {
            long[] estado = {Medidas.SEMENTE};

            java.util.List<Integer> primeiros = new java.util.ArrayList<>();

            for (int i = 0; i < 16; i++) primeiros.add(ateSemDeslocamento(estado, 16));

            assertThat(primeiros)
                    .as("dezesseis sorteios, dezesseis valores distintos: nao sobrou acaso")
                    .doesNotHaveDuplicates()
                    .hasSize(16);

            java.util.List<Integer> segundos = new java.util.ArrayList<>();

            for (int i = 0; i < 16; i++) segundos.add(ateSemDeslocamento(estado, 16));

            assertThat(segundos).as("e a segunda volta repete a primeira").isEqualTo(primeiros);

            Sorteio bom = new Sorteio(Medidas.SEMENTE);

            java.util.List<Integer> comDeslocamento = new java.util.ArrayList<>();

            for (int i = 0; i < 16; i++) comDeslocamento.add(bom.ate(16));

            assertThat(new java.util.HashSet<>(comDeslocamento))
                    .as("com os bits de cima ha repetidos, que e o que acaso faz")
                    .hasSizeLessThan(16);
        }

        @Test
        @DisplayName("o sorteio da os mesmos numeros em qualquer maquina")
        void oSorteioEhFixo() {
            Sorteio sorteio = new Sorteio(1);

            assertThat(List.of(sorteio.proximo(), sorteio.proximo(), sorteio.proximo()))
                    .containsExactly(1817669548, -2107078989, -1510284903);
        }

        @Test
        @DisplayName("a escala logaritmica fica entre as duas pontas pedidas")
        void aEscalaLogaritmica() {
            Sorteio sorteio = new Sorteio(Medidas.SEMENTE);

            for (int i = 0; i < 10_000; i++)
                assertThat(sorteio.entreEmEscalaLogaritmica(100, 2_000_000))
                        .isBetween(100.0, 2_000_000.0);
        }
    }

    // ───────────────────── a consulta ─────────────────────

    @Nested
    @DisplayName("a consulta")
    class AConsulta {

        private final Consulta tresTabelas = new Consulta("mao", new double[] {100, 200, 400},
                new double[][] {
                        {1.0, 0.01, 1.0},
                        {0.01, 1.0, 0.5},
                        {1.0, 0.5, 1.0}});

        /**
         * A CARDINALIDADE DE UM CONJUNTO NAO DEPENDE DA ORDEM, e e isso que permite
         * trocar de plano sem trocar de resposta.
         */
        @Test
        @DisplayName("o tamanho do resultado e o produto dos tamanhos vezes as seletividades")
        void aCardinalidadeEhOProdutoVezesAsSeletividades() {
            assertThat(tresTabelas.cardinalidade(0b001)).isEqualTo(100);
            assertThat(tresTabelas.cardinalidade(0b011)).isEqualTo(100 * 200 * 0.01);

            assertThat(tresTabelas.cardinalidade(0b111))
                    .as("o predicado de 0 com 2 nao existe, entao so entram os outros dois")
                    .isCloseTo(100 * 200 * 400 * 0.01 * 0.5, within(1e-9));

            assertThat(tresTabelas.cardinalidade(0)).as("o conjunto vazio nao corta nada").isEqualTo(1);
        }

        @Test
        @DisplayName("todo plano da mesma consulta produz exatamente o mesmo numero de linhas")
        void todoPlanoProduzOMesmo() {
            double certo = tresTabelas.cardinalidade(tresTabelas.todas());

            for (Plano plano : ForcaBruta.todos(tresTabelas.todas()))
                assertThat(plano.linhas(tresTabelas))
                        .as(plano.desenho())
                        .isCloseTo(certo, within(certo * 1e-12));
        }

        @Test
        @DisplayName("ligadas quer dizer que ha predicado, e sem predicado e produto cartesiano")
        void oQueEhEstarLigada() {
            assertThat(tresTabelas.ligadas(0, 1)).isTrue();
            assertThat(tresTabelas.ligadas(0, 2)).isFalse();

            assertThat(tresTabelas.ligados(0b001, 0b110))
                    .as("a tabela 0 liga com a 1, que esta do outro lado")
                    .isTrue();

            assertThat(tresTabelas.ligados(0b001, 0b100))
                    .as("e com a 2 sozinha nao ha predicado nenhum")
                    .isFalse();
        }

        @Test
        @DisplayName("um predicado com dois valores diferentes e um erro de quem montou")
        void predicadoComDoisValores() {
            assertThatThrownBy(() -> new Consulta("torta", new double[] {10, 10},
                    new double[][] {{1.0, 0.1}, {0.2, 1.0}}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dois valores");
        }

        @Test
        @DisplayName("seletividade fora de zero a um nao e seletividade")
        void seletividadeInvalida() {
            assertThatThrownBy(() -> new Consulta("torta", new double[] {10, 10},
                    new double[][] {{1.0, 1.5}, {1.5, 1.0}}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("entre zero e um");
        }

        @Test
        @DisplayName("uma consulta de uma tabela so nao tem juncao para ordenar")
        void umaTabelaSo() {
            assertThatThrownBy(() -> new Consulta("sozinha", new double[] {10},
                    new double[][] {{1.0}}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duas tabelas ou mais");
        }
    }

    // ───────────────────── o plano ─────────────────────

    @Nested
    @DisplayName("o plano")
    class OPlano {

        private final Consulta consulta = Consultas.cadeia(4, new Sorteio(7));

        @Test
        @DisplayName("a folha nao custa nada, porque a tabela ja existe")
        void aFolhaNaoCusta() {
            Plano folha = new Plano.Folha(2);

            assertThat(folha.custo(consulta)).isZero();
            assertThat(folha.juncoes()).isZero();
            assertThat(folha.conjunto()).isEqualTo(0b100);
            assertThat(folha.linhas(consulta)).isEqualTo(consulta.linhasDe(2));
        }

        @Test
        @DisplayName("o custo e a soma dos resultados intermediarios, e a raiz entra na conta")
        void oCustoEhASomaDosIntermediarios() {
            Plano meio = new Plano.Juncao(new Plano.Folha(0), new Plano.Folha(1));
            Plano raiz = new Plano.Juncao(meio, new Plano.Folha(2));

            assertThat(raiz.custo(consulta))
                    .isCloseTo(consulta.cardinalidade(0b011) + consulta.cardinalidade(0b111),
                            within(1e-6));
        }

        @Test
        @DisplayName("juntar um plano com ele mesmo nao e um plano")
        void naoDaParaJuntarComSigoMesmo() {
            Plano folha = new Plano.Folha(1);

            assertThatThrownBy(() -> new Plano.Juncao(folha, folha))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("compartilham tabela");
        }

        @Test
        @DisplayName("a arvore e a esquerda quando todo lado direito e uma folha")
        void oQueEhSerAEsquerda() {
            Plano aEsquerda = new Plano.Juncao(
                    new Plano.Juncao(new Plano.Folha(0), new Plano.Folha(1)),
                    new Plano.Folha(2));

            Plano cabeluda = new Plano.Juncao(
                    new Plano.Juncao(new Plano.Folha(0), new Plano.Folha(1)),
                    new Plano.Juncao(new Plano.Folha(2), new Plano.Folha(3)));

            assertThat(aEsquerda.aEsquerda()).isTrue();
            assertThat(cabeluda.aEsquerda()).isFalse();

            assertThat(aEsquerda.desenho()).isEqualTo("((0 1) 2)");
        }

        @Test
        @DisplayName("toda arvore com n folhas tem exatamente n menos uma juncoes")
        void aContagemDeJuncoes() {
            for (Plano plano : ForcaBruta.todos(0b1111))
                assertThat(plano.juncoes()).as(plano.desenho()).isEqualTo(3);
        }
    }

    // ───────────────────── a forca bruta ─────────────────────

    @Nested
    @DisplayName("a forca bruta")
    class AForcaBruta {

        /**
         * QUANTOS PLANOS SAO, conferido contra o duplo fatorial.
         *
         * <p>Se a enumeracao esquecesse uma forma de arvore, a forca bruta deixaria de ser
         * exaustiva e o juiz deixaria de ser juiz, sem nenhum sintoma.
         */
        @Test
        @DisplayName("sao 1, 3, 15, 105, 945 e 10.395 planos, de duas a sete tabelas")
        void aContagemDePlanos() {
            long[] esperado = {1, 3, 15, 105, 945, 10_395};

            for (int tabelas = 2; tabelas <= 7; tabelas++)
                assertThat(ForcaBruta.todos((1 << tabelas) - 1).size())
                        .as(tabelas + " tabelas")
                        .isEqualTo(esperado[tabelas - 2]);
        }

        @Test
        @DisplayName("todo plano montado usa todas as tabelas, cada uma uma vez so")
        void todoPlanoCobreTudo() {
            for (Plano plano : ForcaBruta.todos(0b11111)) {
                assertThat(plano.conjunto()).as(plano.desenho()).isEqualTo(0b11111);
                assertThat(plano.juncoes()).as(plano.desenho()).isEqualTo(4);
            }
        }

        @Test
        @DisplayName("nenhum plano aparece duas vezes na enumeracao")
        void semPlanoRepetido() {
            List<Plano> planos = ForcaBruta.todos(0b111111);

            assertThat(planos).extracting(Plano::desenho).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("acima de oito tabelas a forca bruta recusa, em vez de nao terminar")
        void acimaDeOitoRecusa() {
            Consulta grande = Consultas.cadeia(10, new Sorteio(1));

            assertThatThrownBy(() -> ForcaBruta.melhor(grande))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vai ate 8 tabelas");
        }
    }

    // ───────────────────── os otimizadores ─────────────────────

    @Nested
    @DisplayName("os otimizadores")
    class OsOtimizadores {

        /**
         * A DINAMICA E A FORCA BRUTA DAO O MESMO CUSTO, consulta por consulta.
         *
         * <p>Nao o mesmo plano: pode haver empate e cada uma desempata do seu jeito. O
         * mesmo CUSTO, que e o que o principio da otimalidade garante.
         */
        @Test
        @DisplayName("a dinamica acha o otimo em todas as 500 consultas, e a forca bruta confirma")
        void aDinamicaAchaOOtimo() {
            int conferidas = 0;

            for (String formato : Consultas.FORMATOS)
                for (int tabelas : new int[] {3, 5, 7}) {
                    for (Consulta consulta : Medidas.populacao(formato, tabelas, 10)) {
                        double bruto = ForcaBruta.melhor(consulta).custo(consulta);
                        double dinamico = new Dinamica().planejar(consulta).custo(consulta);

                        assertThat(dinamico)
                                .as(formato + " com " + tabelas + " tabelas")
                                .isCloseTo(bruto, within(bruto * 1e-9));

                        conferidas++;
                    }
                }

            assertThat(conferidas).isEqualTo(150);
        }

        @Test
        @DisplayName("a arvore a esquerda devolve sempre uma arvore a esquerda")
        void aEsquerdaDevolveAEsquerda() {
            for (String formato : Consultas.FORMATOS)
                for (Consulta consulta : Medidas.populacao(formato, 6, 20))
                    assertThat(new AEsquerda().planejar(consulta).aEsquerda())
                            .as(formato)
                            .isTrue();
        }

        @Test
        @DisplayName("a ordem escrita junta na ordem dos indices, e sempre a esquerda")
        void aOrdemEscritaEhLiteral() {
            Consulta consulta = Consultas.cadeia(4, new Sorteio(3));

            assertThat(new NaOrdemEscrita().planejar(consulta).desenho())
                    .isEqualTo("(((0 1) 2) 3)");
        }

        @Test
        @DisplayName("a gulosa olha so os pares que restam, e sao muito menos que os conjuntos")
        void aGulosaOlhaMenos() {
            Consulta consulta = Consultas.cadeia(10, new Sorteio(3));

            assertThat(new Gulosa().conjuntosExaminados(consulta)).isEqualTo(165);
            assertThat(new Dinamica().conjuntosExaminados(consulta)).isEqualTo(1_023);
        }

        @Test
        @DisplayName("nenhum otimizador devolve plano invalido, em consulta nenhuma")
        void todosDevolvemPlanoValido() {
            for (String formato : Consultas.FORMATOS)
                for (int tabelas : new int[] {2, 4, 6}) {
                    for (Consulta consulta : Medidas.populacao(formato, tabelas, 20)) {
                        double otimo = ForcaBruta.melhor(consulta).custo(consulta);

                        for (Otimizador otimizador : Juiz.todos()) {
                            Juiz.Veredito veredito = Juiz.julgar(otimizador, consulta, otimo);

                            assertThat(veredito.valido())
                                    .as(formato + " / " + otimizador.nome() + ": "
                                            + veredito.problema())
                                    .isTrue();
                        }
                    }
                }
        }
    }

    // ───────────────────── o juiz ─────────────────────

    @Nested
    @DisplayName("o juiz")
    class OJuiz {

        private final Consulta consulta = Consultas.cadeia(5, new Sorteio(11));

        private Otimizador queEsquece() {
            return new Otimizador() {
                @Override
                public String nome() {
                    return "esquece uma tabela";
                }

                @Override
                public Plano planejar(Consulta qual) {
                    Plano atual = new Plano.Folha(0);

                    for (int i = 1; i < qual.tabelas() - 1; i++)
                        atual = new Plano.Juncao(atual, new Plano.Folha(i));

                    return atual;
                }
            };
        }

        @Test
        @DisplayName("o juiz pega um plano que deixa uma tabela de fora")
        void pegaOQueEsquece() {
            double otimo = ForcaBruta.melhor(consulta).custo(consulta);

            Juiz.Veredito veredito = Juiz.julgar(queEsquece(), consulta, otimo);

            assertThat(veredito.valido()).isFalse();
            assertThat(veredito.problema()).contains("o plano cobre");
        }

        @Test
        @DisplayName("o juiz pega um otimizador que alega custo abaixo do otimo")
        void pegaOQueMente() {
            Otimizador mentiroso = new Otimizador() {
                @Override
                public String nome() {
                    return "mentiroso";
                }

                @Override
                public Plano planejar(Consulta qual) {
                    return new Dinamica().planejar(qual);
                }
            };

            double otimoInflado = ForcaBruta.melhor(consulta).custo(consulta) * 10;

            Juiz.Veredito veredito = Juiz.julgar(mentiroso, consulta, otimoInflado);

            assertThat(veredito.valido()).isFalse();
            assertThat(veredito.problema()).contains("abaixo do otimo");
        }

        @Test
        @DisplayName("o juiz nao olha desempenho: a ordem escrita passa igual")
        void oJuizNaoOlhaDesempenho() {
            double otimo = ForcaBruta.melhor(consulta).custo(consulta);

            Juiz.Veredito veredito = Juiz.julgar(new NaOrdemEscrita(), consulta, otimo);

            assertThat(veredito.valido()).isTrue();
            assertThat(veredito.vezesOOtimo()).isGreaterThanOrEqualTo(1.0);
        }
    }
}
