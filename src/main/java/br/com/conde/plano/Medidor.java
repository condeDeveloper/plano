package br.com.conde.plano;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("correcao") || comando.equals("tudo")) correcao();
        if (comando.equals("ordem") || comando.equals("tudo")) ordem();
        if (comando.equals("forma") || comando.equals("tudo")) forma();
        if (comando.equals("guloso") || comando.equals("tudo")) guloso();
        if (comando.equals("estimativa") || comando.equals("tudo")) estimativa();
    }

    private static void correcao() {
        System.out.println();
        System.out.println("A DINAMICA EMPATA COM A FORCA BRUTA, que monta todos os planos?");
        System.out.println();

        var linhas = Medidas.aCorrecao();

        System.out.printf("  %-10s %8s %10s %16s %16s %10s %12s%n",
                "formato", "tabelas", "consultas", "planos montados", "conjuntos da DP",
                "invalidos", "discordou");

        long planos = 0;
        long conjuntos = 0;
        long invalidos = 0;
        long discordou = 0;

        for (var l : linhas) {
            System.out.printf("  %-10s %8d %10d %16d %16d %10d %12d%n",
                    l.formato(), l.tabelas(), l.consultas(), l.planosMontados(),
                    l.conjuntosDaDinamica(), l.invalidos(), l.dinamicaDiscordou());

            planos += l.planosMontados();
            conjuntos += l.conjuntosDaDinamica();
            invalidos += l.invalidos();
            discordou += l.dinamicaDiscordou();
        }

        System.out.println();
        System.out.printf("  %d planos montados contra %d conjuntos da dinamica.%n",
                planos, conjuntos);
        System.out.printf("  %d planos invalidos, %d consultas em que a dinamica discordou.%n",
                invalidos, discordou);
        System.out.println();
    }

    private static void ordem() {
        System.out.println();
        System.out.println("O PRECO DE NAO OTIMIZAR: a ordem escrita contra o otimo.");
        System.out.println("Em vezes o custo do melhor plano, com " + Medidas.CONSULTAS
                + " consultas por linha.");
        System.out.println();

        System.out.printf("  %-10s %8s %16s %16s %18s%n",
                "formato", "tabelas", "media", "mediana", "pior");

        for (var l : Medidas.aOrdem())
            System.out.printf(Locale.ROOT, "  %-10s %8d %16.1f %16.1f %18.3e%n",
                    l.formato(), l.tabelas(), l.media(), l.mediana(), l.pior());

        System.out.println();
    }

    private static void forma() {
        System.out.println();
        System.out.println("ARVORE A ESQUERDA CONTRA ARVORE DE QUALQUER FORMA.");
        System.out.println();

        System.out.printf("  %-10s %8s %12s %14s %14s %16s %16s%n",
                "formato", "tabelas", "media", "pior", "perde em", "otimo a esquerda",
                "otimo com produto");

        for (var l : Medidas.aForma())
            System.out.printf(Locale.ROOT, "  %-10s %8d %12.4f %14.3f %14.4f %16.4f %16.4f%n",
                    l.formato(), l.tabelas(), l.media(), l.pior(), l.fracaoQuePerde(),
                    l.fracaoDeOtimosAEsquerda(), l.fracaoDeOtimosComProduto());

        System.out.println();
    }

    private static void guloso() {
        System.out.println();
        System.out.println("A GULOSA CONTRA O OTIMO, e quanto trabalho ela economiza.");
        System.out.println();

        System.out.printf("  %-10s %8s %12s %14s %12s %14s %14s%n",
                "formato", "tabelas", "media", "pior", "acerta em", "pares da gulosa",
                "conjuntos da DP");

        for (var l : Medidas.oGuloso())
            System.out.printf(Locale.ROOT, "  %-10s %8d %12.4f %14.3f %12.4f %14d %14d%n",
                    l.formato(), l.tabelas(), l.media(), l.pior(), l.fracaoQueAcerta(),
                    l.conjuntosDaGulosa(), l.conjuntosDaDinamica());

        System.out.println();
    }

    private static void estimativa() {
        System.out.println();
        System.out.println("O OTIMO COM ESTIMATIVA ERRADA, contra heuristicas com estimativa certa.");
        System.out.println("O plano e escolhido com a consulta estimada e cobrado com a verdadeira.");
        System.out.println();

        System.out.printf("  %8s %-18s %10s %14s %14s %16s%n",
                "tabelas", "otimizador", "erro", "media", "mediana", "pior");

        for (var l : Medidas.aEstimativa())
            System.out.printf(Locale.ROOT, "  %8d %-18s %10.0f %14.3f %14.3f %16.3e%n",
                    l.tabelas(), l.otimizador(), l.fatorDeErro(), l.media(), l.mediana(),
                    l.pior());

        System.out.println();
    }
}
