package br.com.conde.plano;

/**
 * NA ORDEM ESCRITA: junta as tabelas na ordem em que elas aparecem na consulta.
 *
 * <p>Ela nao e um otimizador. Ela e o que acontece quando nao ha otimizador, e esta aqui
 * exatamente para isso: todas as outras colunas deste repositorio sao lidas contra o otimo,
 * e esta diz quanto o otimo vale em relacao a nao decidir nada.
 *
 * <p>Nao e um espantalho. Bancos de dados antigos faziam isto, alguns bancos embutidos
 * ainda fazem, e todo sistema de verdade tem um jeito de forcar a ordem escrita quando o
 * otimizador erra. Quem ja reordenou a clausula FROM para a consulta ficar rapida estava
 * usando este otimizador.
 */
public final class NaOrdemEscrita implements Otimizador {

    @Override
    public String nome() {
        return "na ordem escrita";
    }

    @Override
    public Plano planejar(Consulta consulta) {
        Plano atual = new Plano.Folha(0);

        for (int i = 1; i < consulta.tabelas(); i++)
            atual = new Plano.Juncao(atual, new Plano.Folha(i));

        return atual;
    }
}
