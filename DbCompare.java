package com.test.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Alex Villatoro
 */
public class DbCompare {

    public static void main(String[] args) {

        args = new String[2];

        args[0] = "C:\\Users\\Alex Villatoro\\Desktop\\HRVertical_Migration_Create_Tables_Oracle_5.1.0.sql";
        args[1] = "C:\\Users\\Alex Villatoro\\Desktop\\HRVertical_Create_Tables_Oracle_5.1.0.sql";

        try {
            String sql1 = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
            String sql2 = new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8);

            Map<String, Map<String, String>> schema1 = parseSchema(sql1);
            Map<String, Map<String, String>> schema2 = parseSchema(sql2);

            //Comparación de tablas
            Set<String> tables1 = new TreeSet<>(schema1.keySet());
            Set<String> tables2 = new TreeSet<>(schema2.keySet());

            Set<String> missingIn2 = new TreeSet<>(tables1);
            missingIn2.removeAll(tables2);

            Set<String> missingIn1 = new TreeSet<>(tables2);
            missingIn1.removeAll(tables1);

            System.out.println("- Tablas en " + args[0] + " que faltan en " + args[1] + ":");
            if (missingIn2.isEmpty()) {
                System.out.println("  (Ninguna)");
            } else {
                for (String t : missingIn2) {
                    System.out.println("  - " + t);
                }
            }

            System.out.println("\n- Tablas en " + args[1] + " que faltan en " + args[0] + ":");
            if (missingIn1.isEmpty()) {
                System.out.println("  (Ninguna)");
            } else {
                for (String t : missingIn1) {
                    System.out.println("  - " + t);
                }
            }

            //Comparación de columnas
            System.out.println("\n- Comparación de columnas en tablas comunes:");
            for (String table : tables1) {
                if (!tables2.contains(table)) {
                    continue;
                }

                Map<String, String> cols1 = schema1.get(table);
                Map<String, String> cols2 = schema2.get(table);

                System.out.println("\n- Tabla: " + table);
                System.out.println("   - " + args[0] + " tiene " + cols1.size() + " columnas");
                System.out.println("   - " + args[1] + " tiene " + cols2.size() + " columnas");

                //columnas faltantes
                Set<String> missingColsIn2 = new TreeSet<>(cols1.keySet());
                missingColsIn2.removeAll(cols2.keySet());

                Set<String> missingColsIn1 = new TreeSet<>(cols2.keySet());
                missingColsIn1.removeAll(cols1.keySet());

                if (!missingColsIn2.isEmpty()) {
                    System.out.println("   - Faltan en " + args[1] + ": " + missingColsIn2);
                }

                if (!missingColsIn1.isEmpty()) {
                    System.out.println("   - Faltan en " + args[0] + ": " + missingColsIn1);
                }

                //comparar tipos de columnas comunes
                Set<String> commonCols = new TreeSet<>(cols1.keySet());
                commonCols.retainAll(cols2.keySet());

                for (String col : commonCols) {
                    String type1 = cols1.get(col).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
                    String type2 = cols2.get(col).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();

                    if (!TipoDeDato.tiposEquivalentes(type1, type2)) {
                        System.out.println("   - Columna '" + col + "' difiere de tipo: " + args[0] + "=" + type1 + " vs " + args[1] + "=" + type2);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error leyendo archivos: " + e.getMessage());
        }
    }

    private static Map<String, Map<String, String>> parseSchema(String sql) {
        String cleaned = stripComments(sql);
        Map<String, Map<String, String>> result = new HashMap<>();

        String lower = cleaned.toLowerCase(Locale.ROOT);
        int idx = 0;
        while (true) {
            int start = lower.indexOf("create table", idx);
            if (start < 0) {
                break;
            }
            int openParen = cleaned.indexOf("(", start);
            if (openParen < 0) {
                break;
            }

            String tableIdent = cleaned.substring(start + "create table".length(), openParen).trim();
            String tableName = normalizeTableName(tableIdent);

            int closeParen = findMatchingParen(cleaned, openParen);
            if (closeParen < 0) {
                break;
            }

            String body = cleaned.substring(openParen + 1, closeParen);
            Map<String, String> columns = parseColumns(body);

            result.put(tableName, columns);
            idx = closeParen + 1;
        }

        return result;
    }

    private static String stripComments(String s) {
        s = s.replaceAll("/\\*.*?\\*/", " ");
        s = s.replaceAll("(?m)--.*?$", " ");
        s = s.replaceAll("(?m)#.*?$", " ");
        return s;
    }

    private static int findMatchingParen(String s, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String normalizeTableName(String ident) {
        String s = ident.replaceAll("(?i)if not exists", "").trim();
        s = s.replaceAll("[`\"\\[\\]]", "");
        if (s.contains(".")) {
            String[] parts = s.split("\\.");
            s = parts[parts.length - 1];
        }
        return s.toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> parseColumns(String body) {
        List<String> defs = splitByTopLevelCommas(body);
        Map<String, String> cols = new LinkedHashMap<>();
        for (String def : defs) {
            String d = def.trim();
            if (d.isEmpty()) {
                continue;
            }

            String dl = d.toLowerCase(Locale.ROOT);

            // Ignorar constraints y delimitadores de script
            if (dl.startsWith("constraint") || dl.startsWith("primary") || dl.startsWith("foreign")
                    || dl.startsWith("unique") || dl.startsWith("check") || dl.startsWith("index")
                    || dl.startsWith("key") || d.equals("/") || d.equals("//")) {
                continue;
            }

            // Buscar nombre de columna y tipo con expresión regular
            Pattern colPattern = Pattern.compile(
                    "^([A-Z0-9_\"`\\[\\]]+)\\s+([A-Z0-9_]+\\s*(\\([^)]*\\))?)",
                    Pattern.CASE_INSENSITIVE);

            Matcher m = colPattern.matcher(d.trim());
            if (m.find()) {
                String col = m.group(1).replaceAll("[`\"\\[\\]]", "").toLowerCase(Locale.ROOT);
                String type = m.group(2).toUpperCase(Locale.ROOT).replaceAll("\\s+", "");

                cols.put(col, type);
            }
        }
        return cols;
    }
    
    private static List<String> splitByTopLevelCommas(String s) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSQ = false, inDQ = false, inBT = false, inBR = false;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!inDQ && !inBT && !inBR && c == '\'') {
                inSQ = !inSQ;
                cur.append(c);
                continue;
            }
            if (!inSQ && !inBT && !inBR && c == '"') {
                inDQ = !inDQ;
                cur.append(c);
                continue;
            }
            if (!inSQ && !inDQ && !inBR && c == '`') {
                inBT = !inBT;
                cur.append(c);
                continue;
            }
            if (!inSQ && !inDQ && !inBT && c == '[') {
                inBR = true;
                cur.append(c);
                continue;
            }
            if (inBR && c == ']') {
                inBR = false;
                cur.append(c);
                continue;
            }

            if (inSQ || inDQ || inBT || inBR) {
                cur.append(c);
                continue;
            }

            if (c == '(') {
                depth++;
                cur.append(c);
                continue;
            }
            if (c == ')') {
                depth--;
                cur.append(c);
                continue;
            }

            if (c == ',' && depth == 0) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            parts.add(cur.toString());
        }
        return parts;
    }

}
