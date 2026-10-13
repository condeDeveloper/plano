package br.com.conde.plano;

/**
 * O SORTEIO DA CASA: um gerador linear congruente, escrito aqui de proposito.
 *
 * <p>Ele existe para que a mesma semente de as mesmas consultas em qualquer maquina e em
 * qualquer versao de Java. A biblioteca padrao nao promete isso entre versoes, e um
 * repositorio cujos numeros estao escritos no README nao pode depender disso.
 *
 * <p>Sao as constantes de Knuth, as mesmas do gerador de 64 bits que todo mundo usa.
 *
 * <h2>Por que o deslocamento de 32</h2>
 *
 * <p>Num gerador linear congruente os bits de baixo tem periodo curto de um jeito exato e
 * nao estatistico: o ultimo bit alterna com periodo dois, os quatro ultimos repetem com
 * periodo dezesseis.
 *
 * <p>E aqui isso destruiria a tabela principal. O tamanho de cada tabela de uma consulta
 * sorteada sai de ate(16), que e potencia de dois: com os bits de baixo, as tabelas de uma
 * consulta de dezesseis seriam sempre os mesmos dezesseis tamanhos na mesma ordem, e
 * consultas "sorteadas" diferentes teriam os mesmos tamanhos deslocados.
 *
 * <p>Uma amostra de mil consultas mediria uma consulta. Ha um teste so sobre isso.
 */
public final class Sorteio {

    private long estado;

    public Sorteio(long semente) {
        this.estado = semente;
    }

    /** Um inteiro de 32 bits, tirado da metade de cima do estado. */
    public int proximo() {
        estado = estado * 6364136223846793005L + 1442695040888963407L;

        return (int) (estado >>> 32);
    }

    /** Um inteiro de 0 inclusive ate o limite exclusive. */
    public int ate(int limite) {
        if (limite < 1) throw new IllegalArgumentException("limite precisa ser positivo");

        return Math.floorMod(proximo(), limite);
    }

    /** Um numero entre zero inclusive e um exclusive. */
    public double fracao() {
        return (proximo() & 0x7fffffff) / (double) 0x80000000L;
    }

    /** Um numero entre de e ate, com o logaritmo distribuido por igual. */
    public double entreEmEscalaLogaritmica(double de, double ate) {
        double expoente = Math.log10(de) + fracao() * (Math.log10(ate) - Math.log10(de));

        return Math.pow(10, expoente);
    }
}
