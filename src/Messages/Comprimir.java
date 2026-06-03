package Messages;

import java.util.HashMap;
import java.util.Map;

public class Comprimir {
    private Map<String, Integer> indices = new HashMap<>();

    public Comprimir() {}

    public char[] compresion(String textoAComprimir) {
        indices.clear(); 
        // Arreglo temporal del tamaño máximo posible
        char[] tokensTemporales = new char[textoAComprimir.length()];

        if (textoAComprimir.length() <= 3) {
            for (int j = 0; j < textoAComprimir.length(); j++) {
                tokensTemporales[j] = (char) (textoAComprimir.charAt(j) & 0xFF);
            }
            return tokensTemporales;
        }

        int tokensActuales = 0;
        int i = 0;

        while (i < textoAComprimir.length() - 3) {
            String textoActual = "" + textoAComprimir.charAt(i) + textoAComprimir.charAt(i + 1) + textoAComprimir.charAt(i + 2);
            Object coincidencia = indices.get(textoActual);

            if (coincidencia != null && (i - (int) coincidencia) <= 15) {
                int posVieja = (int) coincidencia;
                int tempi = i + 3;
                int tempj = posVieja + 3;

                // Mantenemos la corrección del límite de 15 del paso anterior
                while (tempi < textoAComprimir.length() && 
                       textoAComprimir.charAt(tempi) == textoAComprimir.charAt(tempj) &&
                       (tempi - i) < 15) {
                    tempi++;
                    tempj++;
                }

                int offset = Math.min(i - posVieja, 15);
                int length = tempi - i;
                char nextChar = (tempi < textoAComprimir.length()) ? textoAComprimir.charAt(tempi) : '\0';
                char tokenEmpaquetado = (char) ((offset << 12) | (length << 8) | (nextChar & 0xFF));

                tokensTemporales[tokensActuales] = tokenEmpaquetado;
                tokensActuales++;

                for (int k = i; k < tempi; k++) {
                    if (k <= textoAComprimir.length() - 3) {
                        String sub = "" + textoAComprimir.charAt(k) + textoAComprimir.charAt(k+1) + textoAComprimir.charAt(k+2);
                        indices.put(sub, k);
                    }
                }

                i = tempi + 1;

            } else {
                char nextChar = textoAComprimir.charAt(i);
                tokensTemporales[tokensActuales] = (char) (nextChar & 0xFF);
                tokensActuales++;
                
                indices.put(textoActual, i);
                i++;
            }
        }

        while (i < textoAComprimir.length()) {
            tokensTemporales[tokensActuales] = (char) (textoAComprimir.charAt(i) & 0xFF);
            tokensActuales++;
            i++;
        }
        
        //Devolvemos un arreglo recortado con el tamaño exacto de tokens
        char[] resultado = new char[tokensActuales];
        System.arraycopy(tokensTemporales, 0, resultado, 0, tokensActuales);
        return resultado;
    }

    public static String descomprimir(char[] tokens) {
        StringBuilder textoFinal = new StringBuilder();
        
        for (char t : tokens) {
            int offset = (t >> 12) & 0x0F;
            int length = (t >> 8) & 0x0F;
            char nextChar = (char) (t & 0xFF);

            if (offset == 0 && length == 0) {
                if (nextChar != '\0') {
                    textoFinal.append(nextChar);
                }
            } else {
                int posicionInicio = textoFinal.length() - offset;
                
                for (int k = 0; k < length; k++) {
                    textoFinal.append(textoFinal.charAt(posicionInicio + k));
                }

                if (nextChar != '\0') {
                    textoFinal.append(nextChar);
                }
            }
        }
        return textoFinal.toString();
    }
}