package com.example.dldtool.model;

import java.util.*;

public class BooleanSimplifier {

    // All detected variables in the expression
    private List<Character> variables = new ArrayList<>();

    // ── PUBLIC ENTRY POINT ────────────────────────────────────
    public String simplify(String expression) {
        variables.clear();
        expression = expression.trim().toUpperCase();

        // Detect variables (A-H only, up to 8)
        for (char c : expression.toCharArray()) {
            if (Character.isLetter(c) && !variables.contains(c)) {
                variables.add(c);
            }
        }
        Collections.sort(variables);

        if (variables.isEmpty()) return "Invalid expression";
        if (variables.size() > 8) return "Max 8 variables supported";

        // Get minterms by evaluating all input combinations
        List<Integer> minterms = new ArrayList<>();
        int numCombinations = (int) Math.pow(2, variables.size());

        for (int i = 0; i < numCombinations; i++) {
            Map<Character, Boolean> values = new HashMap<>();
            for (int j = 0; j < variables.size(); j++) {
                // MSB first
                values.put(variables.get(j),
                        ((i >> (variables.size() - 1 - j)) & 1) == 1);
            }
            if (evaluate(expression, values)) {
                minterms.add(i);
            }
        }

        if (minterms.isEmpty()) return "0";
        if (minterms.size() == numCombinations) return "1";

        return buildSOP(minterms, variables.size());
    }

    // ── GENERATE TRUTH TABLE ──────────────────────────────────
    public List<String[]> getTruthTable(String expression) {
        variables.clear();
        expression = expression.trim().toUpperCase();

        for (char c : expression.toCharArray()) {
            if (Character.isLetter(c) && !variables.contains(c)) {
                variables.add(c);
            }
        }
        Collections.sort(variables);

        List<String[]> rows = new ArrayList<>();
        int n = variables.size();
        int numRows = (int) Math.pow(2, n);

        for (int i = 0; i < numRows; i++) {
            String[] row = new String[n + 1];
            Map<Character, Boolean> values = new HashMap<>();
            for (int j = 0; j < n; j++) {
                boolean val = ((i >> (n - 1 - j)) & 1) == 1;
                values.put(variables.get(j), val);
                row[j] = val ? "1" : "0";
            }
            row[n] = evaluate(expression, values) ? "1" : "0";
            rows.add(row);
        }
        return rows;
    }

    public List<Character> getVariables() { return variables; }

    // ── EXPRESSION EVALUATOR ──────────────────────────────────
    private boolean evaluate(String expr, Map<Character, Boolean> values) {
        expr = expr.trim();

        // Handle OR (+) — lowest precedence, split from right
        int depth = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') depth++;
            else if (c == '(') depth--;
            else if (c == '+' && depth == 0) {
                return evaluate(expr.substring(0, i), values)
                        || evaluate(expr.substring(i + 1), values);
            }
        }

        // Handle AND (.) — next precedence
        depth = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') depth++;
            else if (c == '(') depth--;
            else if (c == '.' && depth == 0) {
                return evaluate(expr.substring(0, i), values)
                        && evaluate(expr.substring(i + 1), values);
            }
        }

        // Handle implicit AND (e.g. "AB" means "A.B")
        depth = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') depth++;
            else if (c == '(') depth--;
            else if (depth == 0 && i > 0
                    && Character.isLetterOrDigit(c)
                    && (expr.charAt(i-1) == '\'' || Character.isLetterOrDigit(expr.charAt(i-1)))) {
                return evaluate(expr.substring(0, i), values)
                        && evaluate(expr.substring(i), values);
            }
        }

        // Handle NOT (')
        if (expr.endsWith("'")) {
            return !evaluate(expr.substring(0, expr.length() - 1), values);
        }

        // Handle parentheses
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return evaluate(expr.substring(1, expr.length() - 1), values);
        }

        // Handle NOT prefix
        if (expr.startsWith("!")) {
            return !evaluate(expr.substring(1), values);
        }

        // Base case — single variable
        if (expr.length() == 1 && Character.isLetter(expr.charAt(0))) {
            Boolean val = values.get(expr.charAt(0));
            return val != null && val;
        }

        return false;
    }

    // ── BUILD SOP FROM MINTERMS ───────────────────────────────
    private String buildSOP(List<Integer> minterms, int numVars) {
        // Group minterms (basic grouping — pairs that differ by 1 bit)
        List<String> terms = new ArrayList<>();
        boolean[] covered = new boolean[minterms.size()];

        // Try to combine pairs
        List<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < minterms.size(); i++) {
            boolean combined = false;
            for (int j = i + 1; j < minterms.size(); j++) {
                int diff = minterms.get(i) ^ minterms.get(j);
                if (diff != 0 && (diff & (diff - 1)) == 0) {
                    // They differ by exactly 1 bit — can be combined
                    String term = buildTerm(minterms.get(i),
                            minterms.get(j), numVars);
                    if (!terms.contains(term)) terms.add(term);
                    covered[i] = true;
                    covered[j] = true;
                    combined = true;
                }
            }
            if (!combined) remaining.add(i);
        }

        // Add uncovered minterms as full terms
        for (int i = 0; i < minterms.size(); i++) {
            if (!covered[i]) {
                terms.add(buildFullTerm(minterms.get(i), numVars));
            }
        }

        // Remove duplicates
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(terms));
        return String.join(" + ", unique);
    }

    private String buildTerm(int m1, int m2, int numVars) {
        int dontCare = m1 ^ m2;
        StringBuilder sb = new StringBuilder();
        for (int i = numVars - 1; i >= 0; i--) {
            if ((dontCare & (1 << i)) == 0) {
                char varName = variables.get(numVars - 1 - i);
                if ((m1 & (1 << i)) == 0) sb.append(varName).append("'");
                else sb.append(varName);
            }
        }
        return sb.toString();
    }

    private String buildFullTerm(int minterm, int numVars) {
        StringBuilder sb = new StringBuilder();
        for (int i = numVars - 1; i >= 0; i--) {
            char varName = variables.get(numVars - 1 - i);
            if ((minterm & (1 << i)) == 0) sb.append(varName).append("'");
            else sb.append(varName);
        }
        return sb.toString();
    }
}