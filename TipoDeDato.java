package com.test.test;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TipoDeDato {

    /**
     * Determina si dos tipos de datos de base de datos son equivalentes. Esta
     * función normaliza las cadenas de tipo de dato para manejar diferentes
     * sintaxis y sinónimos comunes.
     *
     * @param tipo1 La primera cadena de tipo de dato.
     * @param tipo2 La segunda cadena de tipo de dato.
     * @return true si los tipos son equivalentes, false en caso contrario.
     */
    public static boolean tiposEquivalentes(String tipo1, String tipo2) {
        if (tipo1 == null || tipo2 == null) {
            return false;
        }

        // Caso especial de equivalencia entre DATE y TIMESTAMP
        if ((tipo1.equals("DATE") && tipo2.startsWith("TIMESTAMP"))
                || (tipo2.equals("DATE") && tipo1.startsWith("TIMESTAMP"))) {
            return true;
        }

        String tipoNormalizado1 = normalizarTipo(tipo1);
        String tipoNormalizado2 = normalizarTipo(tipo2);

        return tipoNormalizado1.equals(tipoNormalizado2);
    }

    /**
     * Normaliza una cadena de tipo de dato para permitir comparaciones justas.
     *
     * @param tipo La cadena de tipo de dato a normalizar.
     * @return La cadena normalizada.
     */
    private static String normalizarTipo(String tipo) {
        //Normaliza a mayúsculas y quita espacios extra.
        String tipoNormalizado = tipo.toUpperCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", "");
/*
        //Normaliza VARCHAR a VARCHAR2, usando un límite de palabra para evitar VARCHAR22.
        tipoNormalizado = tipoNormalizado.replaceAll("^VARCHAR\\b", "VARCHAR2");

        //Normaliza casos especiales como CHAR, INT e INTEGER.
        if (tipoNormalizado.equals("CHAR")) {
            return "CHAR(1)";
        }
        if (tipoNormalizado.equals("INT") || tipoNormalizado.equals("INTEGER")) {
            return "NUMBER(38)";
        }

        //Normaliza NUMERIC y DECIMAL a NUMBER.
        tipoNormalizado = tipoNormalizado.replaceAll("^NUMERIC", "NUMBER")
                .replaceAll("^DECIMAL", "NUMBER");

        //Normaliza el contenido dentro de los paréntesis.
        Pattern pattern = Pattern.compile("(?<=\\()(.+?)(?=\\))");
        Matcher matcher = pattern.matcher(tipoNormalizado);

        if (matcher.find()) {
            String contenido = matcher.group(1);

            //Elimina "BYTE" o "CHAR" del contenido del paréntesis.
            contenido = contenido.replaceAll("(?i)BYTE", "").replaceAll("(?i)CHAR", "");

            //Vuelve a armar la cadena normalizada.
            tipoNormalizado = tipoNormalizado.replaceAll(Pattern.quote(matcher.group(1)), contenido);
        }

        //Quita especificación de longitud inútil, como NUMBER(p,0).
        tipoNormalizado = tipoNormalizado.replaceAll("NUMBER\\((\\d+),0\\)", "NUMBER($1)");

        //Normaliza espacios en paréntesis y comas (esto se hace después de todas las manipulaciones internas).
        tipoNormalizado = tipoNormalizado.replaceAll("\\s*,\\s*", ",");*/

        return tipoNormalizado;
    }
}
